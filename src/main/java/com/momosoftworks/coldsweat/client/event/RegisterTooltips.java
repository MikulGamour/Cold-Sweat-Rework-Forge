package com.momosoftworks.coldsweat.client.event;

import com.momosoftworks.coldsweat.client.gui.tooltip.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RegisterTooltips
{
    @SubscribeEvent
    public static void registerTooltips(final FMLClientSetupEvent event)
    {
        MinecraftForgeClient.registerTooltipComponentFactory(SoulspringTooltip.class, tooltip -> new ClientSoulspringTooltip(tooltip.getFuel()));
        MinecraftForgeClient.registerTooltipComponentFactory(InsulationTooltip.class, tooltip -> new ClientInsulationTooltip(tooltip.getInsulation(), tooltip.getSlot(), tooltip.getItem(), tooltip.isStrikethrough()));
        MinecraftForgeClient.registerTooltipComponentFactory(InsulationAttributeTooltip.class, tooltip -> new ClientInsulationAttributeTooltip(tooltip.getOriginal(), tooltip.getFont(), tooltip.isStrikethrough()));
    }
}
