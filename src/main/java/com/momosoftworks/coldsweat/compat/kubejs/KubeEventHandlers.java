package com.momosoftworks.coldsweat.compat.kubejs;

import com.momosoftworks.coldsweat.api.event.common.insulation.InsulateItemEvent;
import com.momosoftworks.coldsweat.api.event.common.temperautre.TempModifierEvent;
import com.momosoftworks.coldsweat.api.event.common.temperautre.TemperatureChangedEvent;
import com.momosoftworks.coldsweat.api.event.core.init.GatherDefaultTempModifiersEvent;
import com.momosoftworks.coldsweat.api.event.core.registry.CreateRegistriesEvent;
import com.momosoftworks.coldsweat.compat.kubejs.event.*;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import net.neoforged.bus.api.SubscribeEvent;

public class KubeEventHandlers
{
    public static final EventGroup COLD_SWEAT = EventGroup.of("ColdSweatEvents");

    public static final EventHandler REGISTER = COLD_SWEAT.server("registries", () -> ModRegistriesEventJS.class);
    public static final EventHandler GATHER_DEFAULT_MODIFIERS = COLD_SWEAT.server("gatherDefaultModifiers", () -> DefaultModifiersEventJS.class);

    public static final EventHandler TEMP_CHANGED = COLD_SWEAT.common("temperatureChanged", () -> TempChangedEventJS.class);
    public static final EventHandler MODIFIER_ADD = COLD_SWEAT.common("addModifier", () -> AddModifierEventJS.class);

    public static final EventHandler APPLY_INSULATION = COLD_SWEAT.server("applyInsulation", () -> ApplyInsulationEventJS.class);

    @SubscribeEvent
    public static void buildRegistries(CreateRegistriesEvent.Pre event)
    {
        if (REGISTER.hasListeners())
        {   REGISTER.post(new ModRegistriesEventJS());
        }
    }

    @SubscribeEvent
    public static void gatherDefaultModifiers(GatherDefaultTempModifiersEvent event)
    {
        if (GATHER_DEFAULT_MODIFIERS.hasListeners())
        {   GATHER_DEFAULT_MODIFIERS.post(new DefaultModifiersEventJS(event));
        }
    }

    @SubscribeEvent
    public static void onTemperatureChanged(TemperatureChangedEvent event)
    {
        if (TEMP_CHANGED.hasListeners())
        {   TEMP_CHANGED.post(new TempChangedEventJS(event)).applyCancel(event);
        }
    }

    @SubscribeEvent
    public static void onInsulateItem(InsulateItemEvent event)
    {
        if (APPLY_INSULATION.hasListeners())
        {   APPLY_INSULATION.post(new ApplyInsulationEventJS(event)).applyCancel(event);
        }
    }

    @SubscribeEvent
    public static void onTempModifierAdd(TempModifierEvent.Add event)
    {
        if (MODIFIER_ADD.hasListeners())
        {   MODIFIER_ADD.post(new AddModifierEventJS(event)).applyCancel(event);
        }
    }
}
