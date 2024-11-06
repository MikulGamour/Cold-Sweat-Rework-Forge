package com.momosoftworks.coldsweat.compat.kubejs.event;

import com.momosoftworks.coldsweat.api.event.common.temperautre.TemperatureChangedEvent;
import com.momosoftworks.coldsweat.api.util.Temperature;
import dev.latvian.mods.kubejs.entity.EntityJS;
import dev.latvian.mods.kubejs.entity.LivingEntityEventJS;
import dev.latvian.mods.kubejs.entity.LivingEntityJS;
import net.minecraft.world.entity.LivingEntity;

public class TempChangedEventJS extends LivingEntityEventJS
{
    private final LivingEntityJS entity;
    private final Temperature.Trait trait;
    private final double oldTemperature;
    private double newTemperature;

    public TempChangedEventJS(LivingEntity entity, Temperature.Trait trait, double oldTemperature, double newTemperature)
    {
        this.entity = new LivingEntityJS(this.levelOf(entity), entity);
        this.trait = trait;
        this.oldTemperature = oldTemperature;
        this.newTemperature = newTemperature;
    }

    public TempChangedEventJS(TemperatureChangedEvent event)
    {   this(event.getEntity(), event.getTrait(), event.getOldTemperature(), event.getTemperature());
    }

    @Override
    public EntityJS getEntity()
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
