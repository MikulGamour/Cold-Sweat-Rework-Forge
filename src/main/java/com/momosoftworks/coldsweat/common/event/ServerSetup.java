package com.momosoftworks.coldsweat.common.event;

import com.momosoftworks.coldsweat.api.event.core.registry.EdiblesRegisterEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;

@Mod.EventBusSubscriber
public class ServerSetup
{
    @SubscribeEvent
    public static void serverStartSetup(FMLServerAboutToStartEvent event)
    {   // Register chameleon edibles
        EdiblesRegisterEvent edibleEvent = new EdiblesRegisterEvent();
        MinecraftForge.EVENT_BUS.post(edibleEvent);
    }
}
