package com.momosoftworks.coldsweat.compat.kubejs;

import com.momosoftworks.coldsweat.api.event.common.temperautre.TemperatureChangedEvent;
import dev.latvian.kubejs.KubeJSPlugin;
import dev.latvian.kubejs.script.BindingsEvent;
import me.shedaniel.architectury.event.EventResult;
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
        if (!result.value())
        {   event.setCanceled(true);
        }
    }
}
