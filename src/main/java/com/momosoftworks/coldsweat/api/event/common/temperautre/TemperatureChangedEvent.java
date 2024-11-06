package com.momosoftworks.coldsweat.api.event.common.temperautre;

import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class TemperatureChangedEvent extends Event
{
    private final LivingEntity entity;
    private final Temperature.Trait trait;
    private final double oldTemperature;
    private double newTemperature;

    public TemperatureChangedEvent(LivingEntity entity, Temperature.Trait trait, double oldTemperature, double newTemperature)
    {
        this.entity = entity;
        this.trait = trait;
        this.oldTemperature = oldTemperature;
        this.newTemperature = newTemperature;
    }

    public LivingEntity getEntity()
    {   return entity;
    }

    public Temperature.Trait getTrait()
    {   return trait;
    }

    public double getOldTemperature()
    {   return oldTemperature;
    }

    public double getTemperature()
    {   return newTemperature;
    }

    public void setTemperature(double newTemperature)
    {   this.newTemperature = newTemperature;
    }
}
