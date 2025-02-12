package com.momosoftworks.coldsweat.compat.kubejs.event;

import com.momosoftworks.coldsweat.api.event.common.temperautre.TemperatureChangedEvent;
import com.momosoftworks.coldsweat.api.util.Temperature;
import dev.latvian.kubejs.entity.EntityJS;
import dev.latvian.kubejs.entity.LivingEntityEventJS;
import dev.latvian.kubejs.entity.LivingEntityJS;

public class TempChangedEventJS extends LivingEntityEventJS
{
    private final TemperatureChangedEvent event;

    public TempChangedEventJS(TemperatureChangedEvent event)
    {   this.event = event;
    }

    @Override
    public EntityJS getEntity()
    {   return new LivingEntityJS(this.worldOf(event.getEntity()), event.getEntity());
    }

    public Temperature.Trait getTrait()
    {   return event.getTrait();
    }

    public double getOldTemperature()
    {   return event.getOldTemperature();
    }

    public double getTemperature()
    {   return event.getTemperature();
    }

    public void setTemperature(double newTemperature)
    {   event.setTemperature(newTemperature);
    }
}
