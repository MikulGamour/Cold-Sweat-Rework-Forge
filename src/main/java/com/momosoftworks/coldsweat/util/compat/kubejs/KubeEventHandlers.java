package com.momosoftworks.coldsweat.util.compat.kubejs;


import com.momosoftworks.coldsweat.api.event.common.temperautre.TemperatureChangedEvent;
import com.momosoftworks.coldsweat.util.compat.kubejs.event.TempChangedEventJS;
import dev.architectury.event.EventResult;
import dev.latvian.mods.kubejs.script.ScriptType;

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
