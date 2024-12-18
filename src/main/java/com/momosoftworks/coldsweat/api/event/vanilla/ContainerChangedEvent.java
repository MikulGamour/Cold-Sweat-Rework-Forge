package com.momosoftworks.coldsweat.api.event.vanilla;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

public class ContainerChangedEvent extends Event
{
    AbstractContainerMenu container;
    ItemStack oldStack;
    ItemStack newStack;
    int slotIndex;

    public ContainerChangedEvent(AbstractContainerMenu container, ItemStack oldStack, ItemStack newStack, int slotIndex)
    {
        this.container = container;
        this.oldStack = oldStack;
        this.newStack = newStack;
        this.slotIndex = slotIndex;
    }

    public AbstractContainerMenu getContainer()
    {   return container;
    }

    public ItemStack getOldStack()
    {   return oldStack;
    }

    public ItemStack getNewStack()
    {   return newStack;
    }

    public int getSlotIndex()
    {   return slotIndex;
    }
}
