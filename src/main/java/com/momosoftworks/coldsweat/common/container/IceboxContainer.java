package com.momosoftworks.coldsweat.common.container;

import com.momosoftworks.coldsweat.common.blockentity.IceboxBlockEntity;
import com.momosoftworks.coldsweat.core.init.ContainerInit;
import com.momosoftworks.coldsweat.data.tag.ModItemTags;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;

import java.util.Objects;

public class IceboxContainer extends Container
{
    public final IceboxBlockEntity te;

    public IceboxContainer(final int windowId, final PlayerInventory playerInv, final IceboxBlockEntity te)
    {
        super(ContainerInit.ICEBOX_CONTAINER_TYPE.get(), windowId);
        this.te = te;

        // Fuel slot
        this.addSlot(new Slot(te, 0, 80, 62)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {   return te.getItemFuel(stack) > 0;
            }
        });

        // Waterskins
        for (int in = 1; in < 10; in++)
        {
            this.addSlot(new Slot(te, in, -10 + in * 18, 35)
            {
                @Override
                public boolean mayPlace(ItemStack stack)
                {   return ModItemTags.ICEBOX_VALID.contains(stack.getItem()) || (CompatManager.isSpoiledLoaded() && stack.isEdible());
                }
            });
        }

        // Main player inventory
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {   this.addSlot(new Slot(playerInv, col + (9 * row) + 9, 8 + col * 18, 163 - (4 - row) * 18));
            }
        }

        // Player Hotbar
        for (int col = 0; col < 9; col++)
        {   this.addSlot(new Slot(playerInv, col, 8 + col * 18, 149));
        }
    }

    public IceboxContainer(final int windowId, final PlayerInventory playerInv, final PacketBuffer data)
    {   this(windowId, playerInv, getTileEntity(playerInv, data));
    }

    public int getFuel()
    {
        return this.te.getFuel();
    }


    private static IceboxBlockEntity getTileEntity(final PlayerInventory playerInv, final PacketBuffer data)
    {
        Objects.requireNonNull(playerInv, "Player inventory cannot be null");
        Objects.requireNonNull(data, "PacketBuffer inventory cannot be null");
        final TileEntity te = playerInv.player.level.getBlockEntity(data.readBlockPos());

        if (te instanceof IceboxBlockEntity)
        {   return (IceboxBlockEntity) te;
        }
        throw new IllegalStateException("Tile Entity is not correct");
    }

    @Override
    public boolean stillValid(PlayerEntity playerIn)
    {   return playerIn.distanceToSqr(this.te.getBlockPos().getX(), this.te.getBlockPos().getY(), this.te.getBlockPos().getZ()) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int index)
    {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem())
        {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (CSMath.betweenInclusive(index, 0, 9))
            {
                if (!this.moveItemStackTo(itemstack1, 10, 46, true))
                {   return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            }
            else
            {
                if (this.slots.get(1).mayPlace(itemstack))
                {
                    if (!this.moveItemStackTo(itemstack1, 1, 10, false))
                    {   slot.onQuickCraft(itemstack1, itemstack);
                        return ItemStack.EMPTY;
                    }
                }
                else if (this.slots.get(0).mayPlace(itemstack))
                {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false))
                    {   slot.onQuickCraft(itemstack1, itemstack);
                        return ItemStack.EMPTY;
                    }
                }
                else if (CSMath.betweenInclusive(index, slots.size() - 9, slots.size()))
                {
                    if (!this.moveItemStackTo(itemstack1, 10, 36, false))
                    {   slot.onQuickCraft(itemstack1, itemstack);
                        return ItemStack.EMPTY;
                    }
                }
                else if (CSMath.betweenInclusive(index, 10, slots.size() - 9))
                {
                    if (!this.moveItemStackTo(itemstack1, slots.size() - 9, slots.size(), false))
                    {   slot.onQuickCraft(itemstack1, itemstack);
                        return ItemStack.EMPTY;
                    }
                }
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty())
            {   slot.set(ItemStack.EMPTY);
            }
            else slot.setChanged();

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }
}
