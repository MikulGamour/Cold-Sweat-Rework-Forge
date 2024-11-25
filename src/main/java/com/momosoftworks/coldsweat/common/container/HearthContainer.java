package com.momosoftworks.coldsweat.common.container;

import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.init.MenuInit;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.network.FriendlyByteBuf;
import com.momosoftworks.coldsweat.common.blockentity.HearthBlockEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Objects;

public class HearthContainer extends AbstractContainerMenu
{
    public final HearthBlockEntity te;
    public HearthContainer(final int windowId, final Inventory playerInv, final HearthBlockEntity te)
    {
        super(MenuInit.HEARTH_CONTAINER_TYPE.get(), windowId);
        this.te = te;

        // Block entity slots
        this.addSlot(new Slot(te, 0, 80, 48)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {
                if (te.getItemFuel(stack) != 0 || stack.is(Items.MILK_BUCKET)) return true;
                // Check if the potion is blacklisted
                Collection< MobEffectInstance> effects = PotionUtils.getMobEffects(stack);
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

    public HearthContainer(final int windowId, final Inventory playerInv, final FriendlyByteBuf data)
    {   this(windowId, playerInv, getTileEntity(playerInv, data));
    }

    public int getHotFuel()
    {   return te.getHotFuel();
    }

    public int getColdFuel()
    {   return te.getColdFuel();
    }

    private static HearthBlockEntity getTileEntity(final Inventory playerInv, final FriendlyByteBuf data)
    {
        Objects.requireNonNull(playerInv, "Player inventory cannot be null");
        Objects.requireNonNull(data, "PacketBuffer inventory cannot be null");
        final BlockEntity te = playerInv.player.level.getBlockEntity(data.readBlockPos());
        if (te instanceof HearthBlockEntity hearth)
        {   return hearth;
        }
        throw new IllegalStateException("Tile Entity is not correct");
    }

    @Override
    public boolean stillValid(Player playerIn)
    {   return playerIn.distanceToSqr(Vec3.atCenterOf(te.getBlockPos())) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
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
