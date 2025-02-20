package com.momosoftworks.coldsweat.compat.kubejs;

import com.momosoftworks.coldsweat.api.event.common.insulation.InsulateItemEvent;
import com.momosoftworks.coldsweat.api.event.common.temperautre.TempModifierEvent;
import com.momosoftworks.coldsweat.api.event.common.temperautre.TemperatureChangedEvent;
import com.momosoftworks.coldsweat.api.event.core.init.GatherDefaultTempModifiersEvent;
import com.momosoftworks.coldsweat.compat.kubejs.event.*;
import dev.latvian.kubejs.script.ScriptType;
import me.shedaniel.architectury.event.EventResult;

public class KubeEventHandlers
{
    public static final String COLD_SWEAT = "cs:";

    public static final String REGISTER = event("registries");
    public static final String GATHER_DEFAULT_MODIFIERS = event("gatherDefaultModifiers");

    public static final String TEMP_CHANGED = event("temperatureChanged");
    public static final String MODIFIER_ADD = event("addModifier");

    public static final String APPLY_INSULATION = event("applyInsulation");


    public static void init()
    {
        KubeEventSignatures.REGISTRIES.register(KubeEventHandlers::buildRegistries);
        KubeEventSignatures.GATHER_MODIFIERS.register(KubeEventHandlers::gatherDefaultModifiers);
        KubeEventSignatures.TEMPERATURE_CHANGED.register(KubeEventHandlers::onTemperatureChanged);
        KubeEventSignatures.INSULATE_ITEM.register(KubeEventHandlers::onInsulateItem);
        KubeEventSignatures.ADD_MODIFIER.register(KubeEventHandlers::onTempModifierAdd);
    }

    private static void buildRegistries()
    {   new ModRegistriesEventJS().post(ScriptType.SERVER, REGISTER);
    }

    private static void gatherDefaultModifiers(GatherDefaultTempModifiersEvent event)
    {   new DefaultModifiersEventJS(event).post(ScriptType.SERVER, GATHER_DEFAULT_MODIFIERS);
    }

    private static String event(String name)
    {
        return COLD_SWEAT + name;
    }

    static EventResult onTemperatureChanged(TemperatureChangedEvent event)
    {   return EventResult.interrupt(!new TempChangedEventJS(event).post(ScriptType.SERVER, TEMP_CHANGED));
    }

    private static EventResult onInsulateItem(InsulateItemEvent event)
    {   return EventResult.interrupt(!new ApplyInsulationEventJS(event).post(ScriptType.SERVER, APPLY_INSULATION));
    }

    private static EventResult onTempModifierAdd(TempModifierEvent.Add event)
    {   return EventResult.interrupt(!new AddModifierEventJS(event).post(ScriptType.SERVER, MODIFIER_ADD));
    }
}
