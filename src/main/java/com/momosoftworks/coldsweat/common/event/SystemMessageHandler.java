package com.momosoftworks.coldsweat.common.event;

import com.momosoftworks.coldsweat.api.event.vanilla.ChatComponentClickedEvent;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(Dist.CLIENT)
public class SystemMessageHandler
{
    private static MultiPlayerGameMode GAME_MODE = null;

    //@SubscribeEvent
    public static void onPlayerEnterCreative(PlayerTickEvent.Pre event)
    {
        Player player = event.getEntity();
        if (Minecraft.getInstance().gameMode != GAME_MODE && Minecraft.getInstance().gameMode.getPlayerMode().isCreative()
        && ConfigSettings.SHOW_CREATIVE_WARNING.get() && !Minecraft.getInstance().isLocalServer())
        {
            player.displayClientMessage(getSystemPrefix()
                                .append(Component.translatable("message.cold_sweat.warning").append(" ").withStyle(ChatFormatting.BOLD, ChatFormatting.RED))
                                .append(Component.translatable("message.cold_sweat.creative_warning_message").append(" ").withStyle(ChatFormatting.GRAY))
                                .append(Component.translatable("message.cold_sweat.disable")
                                                 .withStyle(Style.EMPTY
                                                 .withColor(ChatFormatting.LIGHT_PURPLE)
                                                 .withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, "cold sweat disable message")))), false);
            GAME_MODE = Minecraft.getInstance().gameMode;
        }
    }

    @SubscribeEvent
    //called when a player clicks a clickable chat message
    public static void onPlayerClickChatMessage(ChatComponentClickedEvent event)
    {
        if (ConfigSettings.SHOW_CREATIVE_WARNING.get()
        && event.getStyle().getClickEvent() != null && event.getStyle().getClickEvent().getValue().equals("cold sweat disable message"))
        {
            ConfigSettings.SHOW_CREATIVE_WARNING.set(false);
            event.getPlayer().displayClientMessage(getSystemPrefix()
                                           .append(Component.translatable("message.cold_sweat.disable_feedback").withStyle(ChatFormatting.GRAY)), false);
        }
    }

    public static MutableComponent getSystemPrefix()
    {   return Component.literal("[").append(Component.translatable("message.cold_sweat.mod_name")).append("]: ").withStyle(ChatFormatting.LIGHT_PURPLE);
    }
}
