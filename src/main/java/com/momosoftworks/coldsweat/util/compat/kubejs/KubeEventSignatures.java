package com.momosoftworks.coldsweat.util.compat.kubejs;

import com.momosoftworks.coldsweat.api.event.common.temperautre.TemperatureChangedEvent;
import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.architectury.event.EventResult;

public interface KubeEventSignatures
{
    Event<TemperatureChanged> TEMPERATURE_CHANGED = EventFactory.createEventResult();

    interface TemperatureChanged
    {
        EventResult onTemperatureChanged(TemperatureChangedEvent event);
    }
}
