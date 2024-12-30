package com.momosoftworks.coldsweat.api.temperature.modifier;

import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.DepthTempData;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.FastMap;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ElevationTempModifier extends TempModifier
{
    public ElevationTempModifier()
    {   this(49);
    }

    public ElevationTempModifier(int samples)
    {   this.getNBT().putInt("Samples", samples);
    }

    @Override
    public Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait)
    {
        if (entity.level.dimensionType().hasCeiling()) return temp -> temp;

        World level = entity.level;

        // Depth, Weight
        List<Pair<BlockPos, Double>> depthTable = new ArrayList<>();

        // Collect a list of depths taken at regular intervals around the entity, and their distances from the player
        for (BlockPos pos : WorldHelper.getPositionGrid(entity.blockPosition(), this.getNBT().getInt("Samples"), 10))
        {
            depthTable.add(Pair.of(pos, CSMath.getDistance(entity.blockPosition(), pos)));
        }

        int skylight = entity.level.getBrightness(LightType.SKY, entity.blockPosition());

        Map<BlockPos, Pair<DepthTempData.TempRegion, Double>> depthRegions = new FastMap<>();

        for (Pair<BlockPos, Double> pair : depthTable)
        {
            BlockPos originalPos = pair.getFirst();
            int originalY = originalPos.getY();
            int minY = 0;
            BlockPos pos = new BlockPos(originalPos.getX(),
                                        originalY <= minY ? originalY : Math.max(minY, originalY + skylight - 4),
                                        originalPos.getZ());
            double distance = pair.getSecond();
            findRegion:
            {
                for (DepthTempData data : ConfigSettings.DEPTH_REGIONS.get())
                {
                    DepthTempData.TempRegion region = data.getRegion(level, pos);
                    if (region == null) continue;
                    depthRegions.put(pos, Pair.of(region, distance));
                    break findRegion;
                }
                depthRegions.put(pos, Pair.of(null, distance));
            }
        }

        return temp ->
        {
            List<Pair<Double, Double>> depthTemps = new ArrayList<>();

            for (Map.Entry<BlockPos, Pair<DepthTempData.TempRegion, Double>> entry : depthRegions.entrySet())
            {
                BlockPos pos = entry.getKey();
                DepthTempData.TempRegion region = entry.getValue().getFirst();
                double distance = entry.getValue().getSecond();

                double depthTemp = CSMath.getIfNotNull(region, reg -> reg.getTemperature(temp, pos, level), temp);
                double weight = 1 / (distance / 10 + 1);
                // Add the weighted temperature to the list
                depthTemps.add(new Pair<>(depthTemp, weight));
            }
            if (depthTemps.isEmpty()) return temp;
            // Calculate the weighted average of the depth temperatures
            return CSMath.weightedAverage(depthTemps);
        };
    }
}
