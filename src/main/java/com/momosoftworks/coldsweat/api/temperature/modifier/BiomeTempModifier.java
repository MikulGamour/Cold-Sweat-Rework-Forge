package com.momosoftworks.coldsweat.api.temperature.modifier;

import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.data.codec.configuration.DimensionTempData;
import com.momosoftworks.coldsweat.data.codec.configuration.StructureTempData;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.StructureFeature;

import java.util.function.Function;

public class BiomeTempModifier extends TempModifier
{
    public BiomeTempModifier()
    {
        this(16);
    }

    public BiomeTempModifier(int samples)
    {   this.getNBT().putInt("Samples", samples);
    }

    @Override
    public Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait)
    {
        int samples = this.getNBT().getInt("Samples");
        try
        {
            World level = entity.level;
            double worldTemp = 0;
            BlockPos entPos = entity.blockPosition();

            // If a dimension temperature override is defined, return
            DimensionTempData dimTempOverride = ConfigSettings.DIMENSION_TEMPS.get(entity.level.registryAccess()).get(level.dimensionType());
            if (dimTempOverride != null)
            {   return temp -> temp + dimTempOverride.getTemperature();
            }

            // If a structure temperature override is defined, return
            Pair<Double, Double> structureTemp = getStructureTemp(entity.level, entity.blockPosition());
            if (structureTemp.getFirst() != null)
            {   return temp -> structureTemp.getFirst();
            }

            int biomeCount = 0;
            for (BlockPos blockPos : WorldHelper.getPositionGrid(entPos, samples, 10))
            {
                // Get the holder for the biome
                Biome biome = level.getBiomeManager().getBiome(blockPos);

                // Tally number of biomes
                biomeCount++;

                DimensionType dimension = level.dimensionType();
                if (!dimension.hasCeiling())
                {
                    // Biome temp with time of day
                    double biomeTemp = WorldHelper.getBiomeTemperature(level, biome);
                    if (CompatManager.isPrimalWinterLoaded())
                    {   biomeTemp = Math.min(biomeTemp, biomeTemp / 2) - Math.max(biomeTemp / 2, 0);
                    }
                    worldTemp += biomeTemp;
                }
                // If dimension has ceiling (don't use time or altitude)
                else worldTemp += CSMath.averagePair(WorldHelper.getBiomeTemperatureRange(level, biome));
            }

            worldTemp /= Math.max(1, biomeCount);

            // Slightly decrease temperature if overcast
            if (!level.dimensionType().hasCeiling() && level.isRaining())
            {
                long time = level.getDayTime();
                double overcastTemp = ConfigSettings.OVERCAST_TEMP_OFFSET.get();
                worldTemp += CSMath.blend(0, overcastTemp, Math.abs(6000 - time), 6000, 0);
            }

            // Add dimension offset, if present
            DimensionTempData dimTempOffsetConf = ConfigSettings.DIMENSION_OFFSETS.get(entity.level.registryAccess()).get(level.dimensionType());
            if (dimTempOffsetConf != null)
            {   worldTemp += dimTempOffsetConf.getTemperature();
            }

            // Add structure offset, if present
            worldTemp += structureTemp.getSecond();

            double finalWorldTemp = worldTemp;
            return temp -> temp + finalWorldTemp;
        }
        catch (Exception e)
        {   return temp -> temp;
        }
    }

    public static Pair<Double, Double> getStructureTemp(World level, BlockPos pos)
    {
        StructureFeature<?, ?> structure = WorldHelper.getStructureAt(level, pos);
        if (structure == null) return Pair.of(null, 0d);

        Double strucTemp = CSMath.getIfNotNull(ConfigSettings.STRUCTURE_TEMPS.get(level.registryAccess()).get(structure), StructureTempData::getTemperature, null);
        Double strucOffset = CSMath.getIfNotNull(ConfigSettings.STRUCTURE_OFFSETS.get(level.registryAccess()).get(structure), StructureTempData::getTemperature, 0d);

        return Pair.of(strucTemp, strucOffset);
    }
}