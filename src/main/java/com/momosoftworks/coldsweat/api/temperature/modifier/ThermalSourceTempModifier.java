package com.momosoftworks.coldsweat.api.temperature.modifier;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Function;

public abstract class ThermalSourceTempModifier extends TempModifier
{
    public ThermalSourceTempModifier(int cooling, int warming)
    {
        this.getNBT().putInt("Cooling", cooling);
        this.getNBT().putInt("Warming", warming);
    }

    public abstract int getStrength();

    protected int getCooling()
    {   return this.getNBT().getInt("Cooling");
    }

    protected int getWarming()
    {   return this.getNBT().getInt("Warming");
    }

    @Override
    public Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait)
    {
        double min = ConfigSettings.MIN_TEMP.get();
        double max = ConfigSettings.MAX_TEMP.get();
        double mid = (min + max) / 2;
        double hearthStrength = ConfigSettings.THERMAL_SOURCE_STRENGTH.get();

        double cooling = this.getCooling() * hearthStrength;
        double warming = this.getWarming() * hearthStrength;

        return temp ->
        {
            if (temp > mid)
            {   return CSMath.blend(temp, mid, cooling, 0, 10);
            }
            if (temp < mid)
            {   return CSMath.blend(temp, mid, warming, 0, 10);
            }
            return temp;
        };
    }
}