package com.momosoftworks.coldsweat.common.container;

import com.momosoftworks.coldsweat.common.blockentity.HearthBlockEntity;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.init.ContainerInit;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketBuffer;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.PotionUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.Objects;

public class HearthContainer extends Container
{
    public final HearthBlockEntity te;
    public HearthContainer(final int windowId, final PlayerInventory playerInv, final HearthBlockEntity te)
    {
        super(ContainerInit.HEARTH_CONTAINER_TYPE.get(), windowId);
        this.te = te;

        // Block entity slots
        this.addSlot(new Slot(te, 0, 80, 48)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {
                if (te.getItemFuel(stack) != 0 || stack.getItem() == Items.MILK_BUCKET) return true;
                // Check if the potion is blacklisted
                Collection< EffectInstance> effects = PotionUtils.getMobEffects(stack);
                return !effects.isEmpty()
                    && effects.stream().noneMatch(eff -> ConfigSettings.HEARTH_POTION_BLACKLIST.get().contains(eff.getEffect()));
            }
        });

        // Main player inventory slots
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {   this.addSlot(new Slot(playerInv, col + (9 * row) + 9, 8 + col * 18, 166 - (4 - row) * 18 - 10));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++)
        {   this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    public HearthContainer(final int windowId, final PlayerInventory playerInv, final PacketBuffer data)
    {   this(windowId, playerInv, getTileEntity(playerInv, data));
    }

    public int getHotFuel()
    {   return te.getHotFuel();
    }

    public int getColdFuel()
    {   return te.getColdFuel();
    }

    private static HearthBlockEntity getTileEntity(final PlayerInventory playerInv, final PacketBuffer data)
    {
        Objects.requireNonNull(playerInv, "Player inventory cannot be null");
        Objects.requireNonNull(data, "PacketBuffer inventory cannot be null");
        final TileEntity te = playerInv.player.level.getBlockEntity(data.readBlockPos());
        if (te instanceof HearthBlockEntity)
        {   return ((HearthBlockEntity) te);
        }
        throw new IllegalStateException("Tile Entity is not correct");
    }

    @Override
    public boolean stillValid(PlayerEntity playerIn)
    {   return playerIn.distanceToSqr(Vector3d.atCenterOf(te.getBlockPos())) <= 64.0D;
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
            if (index == 0)
            {
                if (!this.moveItemStackTo(itemstack1, 1, 37, true))
                {   return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            }
            else
            {
                if (this.getSlot(0).mayPlace(itemstack))
                {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false))
                    {   return ItemStack.EMPTY;
                    }
                }
                else if (CSMath.betweenInclusive(index, slots.size() - 9, slots.size() - 1))
                {
                    if (!this.moveItemStackTo(itemstack1, 1, slots.size() - 10, false))
                    {   slot.onQuickCraft(itemstack1, itemstack);
                        return ItemStack.EMPTY;
                    }
                }
                else if (CSMath.betweenInclusive(index, 1, slots.size() - 10))
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

            if (itemstack1.getCount() == itemstack.getCount())
            {   return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }
}
