package com.momosoftworks.coldsweat.common.event;

import com.momosoftworks.coldsweat.compat.CompatManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class WerewolfMountEvent
{
    @SubscribeEvent
    public static void onPlayerRightClick(PlayerInteractEvent.EntityInteract event)
    {
        if (event.getTarget() instanceof PlayerEntity && CompatManager.isWerewolvesLoaded()
        && CompatManager.isWerewolf(((PlayerEntity) event.getTarget())) && !CompatManager.isWerewolf(event.getPlayer()))
        {   event.getPlayer().startRiding(event.getTarget());
        }
    }
}
