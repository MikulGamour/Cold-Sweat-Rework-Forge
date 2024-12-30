package com.momosoftworks.coldsweat.api.temperature.modifier;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.Tags;

import java.util.function.Function;

public class DepthBiomeTempModifier extends TempModifier
{
    public DepthBiomeTempModifier()
    {   this(6);
    }

    public DepthBiomeTempModifier(int samples)
    {   this.getNBT().putInt("SampleRoot", samples);
    }

    @Override
    protected Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait)
    {
        int sampleRoot = this.getNBT().getInt("SampleRoot");
        Level level = entity.level();

        // Calculate the average temperature of underground biomes
        double biomeTempTotal = 0;
        int caveBiomeCount = 0;

        for (BlockPos pos : WorldHelper.getPositionCube(entity.blockPosition(), sampleRoot, 6))
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
        if (caveBiomeCount == 0)
        {   return temp -> temp;
        }

        int finalCaveBiomeCount = caveBiomeCount;
        double biomeTempAvg = biomeTempTotal / caveBiomeCount;

        return temp -> CSMath.blend(temp, biomeTempAvg, finalCaveBiomeCount, 0, Math.pow(sampleRoot, 3));
    }
}
