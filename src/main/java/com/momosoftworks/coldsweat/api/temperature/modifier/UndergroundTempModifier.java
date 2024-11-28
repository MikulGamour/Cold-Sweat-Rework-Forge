package com.momosoftworks.coldsweat.api.temperature.modifier;

import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.BiomeTempData;
import com.momosoftworks.coldsweat.data.codec.configuration.DepthTempData;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.FastMap;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class UndergroundTempModifier extends TempModifier
{
    public UndergroundTempModifier()
    {   this(49);
    }

    public UndergroundTempModifier(int samples)
    {   this.getNBT().putInt("Samples", samples);
    }

    @Override
    public Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait)
    {
        if (entity.level().dimensionType().hasCeiling()) return temp -> temp;

        Level level = entity.level();

        List<Pair<BlockPos, Double>> depthTable = new ArrayList<>();

        // Collect a list of depths taken at regular intervals around the entity, and their distances from the player
        for (BlockPos pos : WorldHelper.getPositionGrid(entity.blockPosition(), this.getNBT().getInt("Samples"), 10))
        {
            depthTable.add(Pair.of(pos, CSMath.getDistance(entity.blockPosition(), pos)));
        }

        // Calculate the average temperature of underground biomes
        double biomeTempTotal = 0;
        int caveBiomeCount = 0;

        for (BlockPos pos : WorldHelper.getPositionCube(entity.blockPosition(), 5, 10))
        {
            if (!level.isInWorldBounds(pos)) continue;

            if (WorldHelper.getHeight(pos, level) <= entity.getY()) continue;

            // Get temperature of underground biomes
            Holder<Biome> biome = level.getBiomeManager().getBiome(pos);
            if (biome.is(Tags.Biomes.IS_UNDERGROUND))
            {
                double biomeTemp = CSMath.averagePair(WorldHelper.getBiomeTemperatureRange(level, biome));

                biomeTempTotal += biomeTemp;
                caveBiomeCount++;
            }
        }
        if (depthTable.isEmpty() && caveBiomeCount == 0)
        {   return temp -> temp;
        }

        int finalCaveBiomeCount = caveBiomeCount;
        double biomeTempAvg = biomeTempTotal / Math.max(1, caveBiomeCount);

        int skylight = entity.level().getBrightness(LightLayer.SKY, entity.blockPosition());

        Map<BlockPos, Pair<DepthTempData, Double>> depthRegions = new FastMap<>();

        for (Pair<BlockPos, Double> pair : depthTable)
        {
            BlockPos originalPos = pair.getFirst();
            int originalY = originalPos.getY();
            int minY = level.getMinBuildHeight();
            BlockPos pos = new BlockPos(originalPos.getX(),
                                        originalY <= minY ? originalY : Math.max(minY, originalY + skylight - 4),
                                        originalPos.getZ());
            double distance = pair.getSecond();
            DepthTempData tempData = null;
            for (DepthTempData data : ConfigSettings.DEPTH_REGIONS.get())
            {
                if (data.withinBounds(level, pos))
                {
                    tempData = data;
                    break;
                }
            }
            if (tempData != null)
            {   depthRegions.put(pos, Pair.of(tempData, distance));
            }
        }

        return temp ->
        {
            List<Pair<Double, Double>> depthTemps = new ArrayList<>();

            for (Map.Entry<BlockPos, Pair<DepthTempData, Double>> entry : depthRegions.entrySet())
            {
                BlockPos pos = entry.getKey();
                DepthTempData depthData = entry.getValue().getFirst();
                double distance = entry.getValue().getSecond();

                double depthTemp = CSMath.orElse(depthData.getTemperature(temp, pos, level), temp);
                double weight = 1 / (distance / 10 + 1);
                // Add the weighted temperature to the list
                depthTemps.add(new Pair<>(depthTemp, weight));
            }
            if (depthTemps.isEmpty()) return temp;
            // Calculate the weighted average of the depth temperatures
            double weightedDepthTemp = CSMath.weightedAverage(depthTemps);

            // Weigh the depth temperature against the number of underground biomes with temperature
            return CSMath.blend(weightedDepthTemp, biomeTempAvg, finalCaveBiomeCount, 0, depthTable.size());
        };
    }
}
