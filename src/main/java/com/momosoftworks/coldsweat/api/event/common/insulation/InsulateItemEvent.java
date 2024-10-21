package com.momosoftworks.coldsweat.api.event.common.insulation;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when insulation is applied to an armor item.<br>
 * <br>
 * Cancelling this event will prevent the item from being insulated.
 */
@Cancelable
public class InsulateItemEvent extends Event
{
    private final ItemStack armorItem;
    private ItemStack insulator;
    private final PlayerEntity player;

    public InsulateItemEvent(ItemStack armorItem, ItemStack insulator, PlayerEntity player)
    {
        this.armorItem = armorItem;
        this.insulator = insulator;
        this.player = player;
    }

    public ItemStack getArmorItem()
    {   return armorItem;
    }

    public ItemStack getInsulator()
    {   return insulator;
    }

    public void setInsulator(ItemStack insulator)
    {   this.insulator = insulator;
    }

    public PlayerEntity getPlayer()
    {   return player;
    }
}
