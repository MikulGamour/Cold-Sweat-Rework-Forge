package com.momosoftworks.coldsweat.compat.kubejs;

import com.momosoftworks.coldsweat.api.event.common.insulation.InsulateItemEvent;
import com.momosoftworks.coldsweat.api.event.common.temperautre.TempModifierEvent;
import com.momosoftworks.coldsweat.api.event.common.temperautre.TemperatureChangedEvent;
import com.momosoftworks.coldsweat.api.event.core.init.GatherDefaultTempModifiersEvent;
import com.momosoftworks.coldsweat.api.event.core.registry.CreateRegistriesEvent;
import dev.architectury.event.EventResult;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

public class KubePlugin implements KubeJSPlugin
{
    @Override
    public void registerEvents(EventGroupRegistry registry)
    {
        registry.register(KubeEventHandlers.COLD_SWEAT);
        KubeEventHandlers.init();
        NeoForge.EVENT_BUS.register(KubePlugin.class);
    }

    @Override
    public void registerBindings(BindingRegistry bindings)
    {
        bindings.add("coldsweat", new KubeBindings());
    }

    @SubscribeEvent
    public static void fireRegistries(CreateRegistriesEvent event)
    {   KubeEventSignatures.REGISTRIES.invoker().buildRegistries();
    }

    @SubscribeEvent
    public static void gatherDefaultModifiers(GatherDefaultTempModifiersEvent event)
    {   KubeEventSignatures.GATHER_MODIFIERS.invoker().gatherDefaultModifiers(event);
    }

    @SubscribeEvent
    public static void onTempChanged(TemperatureChangedEvent event)
    {
        EventResult result = KubeEventSignatures.TEMPERATURE_CHANGED.invoker().onTemperatureChanged(event);
        if (result.isFalse())
        {   event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemInsulated(InsulateItemEvent event)
    {
        EventResult result = KubeEventSignatures.INSULATE_ITEM.invoker().insulateItem(event);
        if (result.isFalse())
        {   event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onModifierAdded(TempModifierEvent.Add event)
    {
        EventResult result = KubeEventSignatures.ADD_MODIFIER.invoker().addModifier(event);
        if (result.isFalse())
        {   event.setCanceled(true);
        }
    }
}
