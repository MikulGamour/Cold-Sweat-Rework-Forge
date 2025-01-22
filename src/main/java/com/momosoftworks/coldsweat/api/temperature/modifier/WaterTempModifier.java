package com.momosoftworks.coldsweat.api.temperature.modifier;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Function;

public class WaterTempModifier extends TempModifier
{
    private static final double WATER_SOAK_SPEED = 0.1;
    private static final double RAIN_SOAK_SPEED = 0.0125;
    private static final double DRY_SPEED = 0.0015;

    public WaterTempModifier()
    {
        this(0.01);
    }

    public WaterTempModifier(double strength)
    {   this.getNBT().putDouble("Strength", strength);
    }

    public double getWetness()
    {   return this.getNBT().getDouble("Strength");
    }

    public double getMaxStrength(LivingEntity entity)
    {
        double worldTemp = Temperature.get(entity, Temperature.Trait.WORLD);
        double maxTemp = ConfigSettings.MAX_TEMP.get();
        double minTemp = ConfigSettings.MIN_TEMP.get();
        return CSMath.clamp(Math.abs(CSMath.average(maxTemp, minTemp) - worldTemp) / 2, 0.23d, 0.5d);
    }

    @Override
    public Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait)
    {
        double worldTemp = Temperature.get(entity, Temperature.Trait.WORLD);
        double minTemp = ConfigSettings.MIN_TEMP.get();
        double maxTemp = ConfigSettings.MAX_TEMP.get();
        double midTemp = CSMath.average(minTemp, maxTemp);

        double strength = this.getNBT().getDouble("Strength");
        double addAmount = WorldHelper.isInWater(entity) ? WATER_SOAK_SPEED // In water
                         : WorldHelper.isRainingAt(entity.level, entity.blockPosition()) ? RAIN_SOAK_SPEED // In rain
                         : -CSMath.blendExp(DRY_SPEED, DRY_SPEED * 10, worldTemp, minTemp, maxTemp, 100); // Drying off
        double maxStrength = CSMath.clamp(Math.abs(midTemp - worldTemp) / 2, 0.23d, 0.5d);

        double newStrength = CSMath.clamp(strength + addAmount, 0d, maxStrength);
        this.getNBT().putDouble("Strength", newStrength);
        if (strength != newStrength)
        {   this.markDirty();
        }

        // If the strength is 0, this TempModifier expires
        if (strength <= 0.0)
        {   this.expires(0);
        }

        return temp ->
        {
            if (ConfigSettings.WATER_EFFECT_SETTING.get().showParticles() && !entity.isInWater())
            {
                if (Math.random() < strength * 2)
                {
                    double randX = entity.getBbWidth() * (Math.random() - 0.5);
                    double randY = entity.getBbHeight() * Math.random();
                    double randZ = entity.getBbWidth() * (Math.random() - 0.5);
                    entity.level.addParticle(ParticleTypes.FALLING_WATER, entity.getX() + randX, entity.getY() + randY, entity.getZ() + randZ, 0, 0, 0);
                }
            }
            return temp - newStrength;
        };
    }
}
