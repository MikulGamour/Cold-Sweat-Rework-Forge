package com.momosoftworks.coldsweat.api.event.vanilla;

import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

public class ContainerChangedEvent extends Event
{
    Container container;
    ItemStack oldStack;
    ItemStack newStack;
    int slotIndex;

    public ContainerChangedEvent(Container container, ItemStack oldStack, ItemStack newStack, int slotIndex)
    {
        this.container = container;
        this.oldStack = oldStack;
        this.newStack = newStack;
        this.slotIndex = slotIndex;
    }

    public Container getContainer()
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
