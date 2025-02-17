package com.momosoftworks.coldsweat.compat.kubejs;

import com.momosoftworks.coldsweat.api.event.common.temperautre.TemperatureChangedEvent;
import com.momosoftworks.coldsweat.compat.kubejs.event.TempChangedEventJS;
import dev.latvian.kubejs.script.ScriptType;
import me.shedaniel.architectury.event.EventResult;

public class KubeEventHandlers
{
    public static final String COLD_SWEAT = "cs:";

    public static final String TEMP_CHANGED = event("temperatureChanged");

    public static void init()
    {
        KubeEventSignatures.TEMPERATURE_CHANGED.register(KubeEventHandlers::onTemperatureChanged);
    }

    private static String event(String name)
    {
        return COLD_SWEAT + name;
    }

    static EventResult onTemperatureChanged(TemperatureChangedEvent event)
    {   return EventResult.interrupt(new TempChangedEventJS(event).post(ScriptType.SERVER, TEMP_CHANGED));
    }
}
