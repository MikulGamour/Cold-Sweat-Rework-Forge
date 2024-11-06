package com.momosoftworks.coldsweat.util.compat.kubejs;

import com.momosoftworks.coldsweat.api.event.common.temperautre.TemperatureChangedEvent;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;
import me.shedaniel.architectury.event.EventResult;

public interface KubeEventSignatures
{
    Event<TemperatureChanged> TEMPERATURE_CHANGED = EventFactory.createEventResult();

    interface TemperatureChanged
    {
        EventResult onTemperatureChanged(TemperatureChangedEvent event);
    }
}
