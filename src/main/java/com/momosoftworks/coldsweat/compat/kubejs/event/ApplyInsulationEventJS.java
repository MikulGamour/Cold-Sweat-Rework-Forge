package com.momosoftworks.coldsweat.compat.kubejs.event;

import com.momosoftworks.coldsweat.api.event.common.insulation.InsulateItemEvent;
import dev.latvian.kubejs.bindings.UtilsWrapper;
import dev.latvian.kubejs.player.PlayerEventJS;
import dev.latvian.kubejs.player.PlayerJS;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;

public class ApplyInsulationEventJS extends PlayerEventJS
{
    private final InsulateItemEvent event;

    public ApplyInsulationEventJS(InsulateItemEvent event)
    {
        this.event = event;
    }

    @Override
    public PlayerJS<ServerPlayerEntity> getEntity()
    {   return UtilsWrapper.getServer().getPlayer(event.getPlayer());
    }

    public ItemStack getArmorItem()
    {   return event.getArmorItem();
    }

    public ItemStack getInsulator()
    {   return event.getInsulator();
    }

    public void setInsulator(ItemStack insulator)
    {   event.setInsulator(insulator);
    }
}
