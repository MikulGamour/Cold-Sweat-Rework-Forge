package com.momosoftworks.coldsweat.util.compat.kubejs;

import com.momosoftworks.coldsweat.api.event.common.temperautre.TemperatureChangedEvent;
import dev.architectury.event.EventResult;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class KubePlugin extends KubeJSPlugin
{
    @Override
    public void init()
    {   MinecraftForge.EVENT_BUS.register(KubePlugin.class);
        KubeEventHandlers.init();
    }

    @Override
    public void addBindings(BindingsEvent event)
    {   event.add("coldsweat", new KubeBindings());
    }

    @SubscribeEvent
    public static void onTempChanged(TemperatureChangedEvent event)
    {
        EventResult result = KubeEventSignatures.TEMPERATURE_CHANGED.invoker().onTemperatureChanged(event);
        if (result.isFalse())
        {   event.setCanceled(true);
        }
    }
}
