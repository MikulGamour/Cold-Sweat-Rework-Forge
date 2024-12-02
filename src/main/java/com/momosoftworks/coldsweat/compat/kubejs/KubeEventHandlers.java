package com.momosoftworks.coldsweat.compat.kubejs;

import com.momosoftworks.coldsweat.api.event.common.insulation.InsulateItemEvent;
import com.momosoftworks.coldsweat.api.event.common.temperautre.TempModifierEvent;
import com.momosoftworks.coldsweat.api.event.common.temperautre.TemperatureChangedEvent;
import com.momosoftworks.coldsweat.api.event.core.init.GatherDefaultTempModifiersEvent;
import com.momosoftworks.coldsweat.compat.kubejs.event.*;
import dev.architectury.event.EventResult;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;


public class KubeEventHandlers
{
    public static final EventGroup COLD_SWEAT = EventGroup.of("ColdSweatEvents");

    public static final EventHandler REGISTER = COLD_SWEAT.server("registries", () -> ModRegistriesEventJS.class);
    public static final EventHandler GATHER_DEFAULT_MODIFIERS = COLD_SWEAT.server("gatherDefaultModifiers", () -> DefaultModifiersEventJS.class);

    public static final EventHandler TEMP_CHANGED = COLD_SWEAT.common("temperatureChanged", () -> TempChangedEventJS.class);
    public static final EventHandler MODIFIER_ADD = COLD_SWEAT.common("addModifier", () -> AddModifierEventJS.class);

    public static final EventHandler APPLY_INSULATION = COLD_SWEAT.server("applyInsulation", () -> ApplyInsulationEventJS.class);


    public static void init()
    {
        KubeEventSignatures.REGISTRIES.register(KubeEventHandlers::buildRegistries);
        KubeEventSignatures.GATHER_MODIFIERS.register(KubeEventHandlers::gatherDefaultModifiers);
        KubeEventSignatures.TEMPERATURE_CHANGED.register(KubeEventHandlers::onTemperatureChanged);
        KubeEventSignatures.INSULATE_ITEM.register(KubeEventHandlers::onInsulateItem);
        KubeEventSignatures.ADD_MODIFIER.register(KubeEventHandlers::onTempModifierAdd);
    }

    private static void buildRegistries()
    {
        if (REGISTER.hasListeners())
        {   REGISTER.post(new ModRegistriesEventJS());
        }
    }

    private static void gatherDefaultModifiers(GatherDefaultTempModifiersEvent event)
    {
        if (GATHER_DEFAULT_MODIFIERS.hasListeners())
        {   GATHER_DEFAULT_MODIFIERS.post(new DefaultModifiersEventJS(event));
        }
    }

    private static EventResult onTemperatureChanged(TemperatureChangedEvent event)
    {
        if (TEMP_CHANGED.hasListeners())
        {   return convertResult(TEMP_CHANGED.post(new TempChangedEventJS(event)));
        }
        return EventResult.pass();
    }

    private static EventResult onInsulateItem(InsulateItemEvent event)
    {
        if (APPLY_INSULATION.hasListeners())
        {   return convertResult(APPLY_INSULATION.post(new ApplyInsulationEventJS(event)));
        }
        return EventResult.pass();
    }

    private static EventResult onTempModifierAdd(TempModifierEvent.Add event)
    {
        if (MODIFIER_ADD.hasListeners())
        {   return convertResult(MODIFIER_ADD.post(new AddModifierEventJS(event)));
        }
        return EventResult.pass();
    }

    private static dev.architectury.event.EventResult convertResult(dev.latvian.mods.kubejs.event.EventResult result)
    {
        if (result.interruptDefault())
        {   return EventResult.interruptDefault();
        }
        if (result.interruptFalse())
        {   return EventResult.interruptFalse();
        }
        if (result.interruptTrue())
        {   return EventResult.interruptTrue();
        }
        else
        {   return EventResult.pass();
        }
    }
}
