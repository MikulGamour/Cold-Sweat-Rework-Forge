package com.momosoftworks.coldsweat.common.container;

import com.momosoftworks.coldsweat.api.event.common.insulation.InsulateItemEvent;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.common.capability.insulation.IInsulatableCap;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.advancement.trigger.ModAdvancementTriggers;
import com.momosoftworks.coldsweat.core.init.ContainerInit;
import com.momosoftworks.coldsweat.util.registries.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.IArmorVanishable;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.AbstractRepairContainer;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

public class SewingContainer extends AbstractRepairContainer
{
    protected boolean quickMoving = false;
    protected IInventory playerInventory;

    public SewingContainer(int containerId, PlayerInventory inventory)
    {   this(ContainerInit.SEWING_CONTAINER_TYPE.get(), containerId, inventory, IWorldPosCallable.NULL);
    }

    public SewingContainer(int i, PlayerInventory inventory, PacketBuffer friendlyByteBuf)
    {   this(i, inventory);
    }

    public SewingContainer(ContainerType<?> menuType, int containerId, PlayerInventory inventory, IWorldPosCallable chunkAccess)
    {
        super(menuType, containerId, inventory, chunkAccess);
        this.playerInventory = inventory;

        this.slots.set(0, new Slot(this.inputSlots, 0, 43, 26)
        {
            {   this.index = 0;
            }
            public boolean mayPlace(ItemStack stack)
            {
                return stack.getItem() instanceof IArmorVanishable && !ConfigSettings.INSULATION_BLACKLIST.get().contains(stack.getItem())
                    && ConfigSettings.INSULATION_ITEMS.get().get(stack.getItem()).isEmpty();
            }
        });
        this.slots.set(1, new Slot(this.inputSlots, 1, 43, 53)
        {
            {   this.index = 1;
            }
            public boolean mayPlace(ItemStack stack)
            {
                return !ConfigSettings.INSULATION_ITEMS.get().get(stack.getItem()).isEmpty()
                    || Tags.Items.SHEARS.contains(stack.getItem());
            }
        });
        this.slots.set(2, new Slot(this.resultSlots, 2, 121, 39)
        {
            {   this.index = 2;
            }
            public boolean mayPlace(ItemStack stack)
            {   return false;
            }
            public boolean mayPickup(PlayerEntity player)
            {   return SewingContainer.this.mayPickup(player, this.hasItem());
            }
            public ItemStack onTake(PlayerEntity player, ItemStack stack)
            {   return SewingContainer.this.onTake(player, stack);
            }
        });
    }

    public int getResultSlot()
    {   return 2;
    }

    public ItemStack getItem(int index)
    {   return this.getContainerForSlot(index).getItem(index);
    }

    public void setItem(int index, ItemStack stack)
    {   this.getContainerForSlot(index).setItem(index, stack);
    }

    protected IInventory getContainerForSlot(int index)
    {   return index == this.getResultSlot() ? this.resultSlots : this.inputSlots;
    }

    public void growItem(int index, int amount)
    {
        ItemStack stack = this.getItem(index);
        stack.grow(amount);
        this.setItem(index, stack);
    }

    @Override
    protected boolean mayPickup(PlayerEntity player, boolean hasStack)
    {   return true;
    }

    /**
     * Calculates the result of taking the output item.<br>
     * <br>
     * Sewing: Takes ingredients from input slots<br>
     * Shearing: Removes insulator(s) from the armor item
     */
    @Override
    protected ItemStack onTake(PlayerEntity player, ItemStack stack)
    {
        if (!ItemInsulationManager.isInsulatable(stack)) return stack;

        ItemStack input1 = this.getItem(0);
        ItemStack input2 = this.getItem(1);

        // If insulation is being removed
        if (isRemovingInsulation())
        {
            ItemInsulationManager.getInsulationCap(input1).ifPresent(cap ->
            {
                if (!cap.getInsulation().isEmpty())
                {   // Damage shears
                    if (!player.abilities.instabuild)
                    {   input2.hurtAndBreak(1, player, item -> {});
                    }

                    // Remove the last insulation item added
                    cap.removeInsulationItem(cap.getInsulationItem(cap.getInsulation().size() - 1));
                    // Play shear sound
                    player.level.playSound(null, player.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundCategory.PLAYERS, 0.8F, 1.0F);
                }
            });
            this.createResult();
            this.broadcastChanges();
        }
        // If insulation is being added
        else
        {
            if (!quickMoving)
            {
                this.growItem(0, -1);
                this.growItem(1, -1);
            }
            // Play insulation sound
            player.level.playSound(null, player.blockPosition(), SoundEvents.LLAMA_SWAG, SoundCategory.BLOCKS, 0.5f, 1f);

            // Trigger advancement criteria
            if (player instanceof ServerPlayerEntity)
                ModAdvancementTriggers.ARMOR_INSULATED.trigger(((ServerPlayerEntity) player), input1, input2);
        }

        if (stack.getItem() instanceof ArmorItem)
        {
            ArmorItem armor = (ArmorItem) stack.getItem();
            // Get equip sound for the armor item
            SoundEvent equipSound = armor.getMaterial().getEquipSound();
            player.level.playSound(null, player.blockPosition(), equipSound, SoundCategory.BLOCKS, 1f, 1f);
        }
        return stack;
    }

    @Override
    protected boolean isValidBlock(BlockState state)
    {   return state.is(ModBlocks.SEWING_TABLE);
    }

    /**
     * Creates the result (output item) from the input items.<br>
     * <br>
     * Sewing: Outputs the armor item + the insulator<br>
     * Shearing: Outputs the last-applied insulator on the armor piece
     */
    @Override
    public void createResult()
    {
        if (this.quickMoving) return;
        ItemStack wearableItem = this.getItem(0);
        ItemStack insulatorItem = this.getItem(1);

        // If either input slot is taken, remove the result
        if (wearableItem.isEmpty() || insulatorItem.isEmpty())
        {
            this.setItem(this.getResultSlot(), ItemStack.EMPTY);
            return;
        }

        if (ItemInsulationManager.isInsulatable(wearableItem))
        {
            // Shears are used to remove insulation
            if (isRemovingInsulation())
            {
                ItemInsulationManager.getInsulationCap(wearableItem).ifPresent(cap ->
                {
                    if (!cap.getInsulation().isEmpty())
                    {   this.setItem(this.getResultSlot(), cap.getInsulationItem(cap.getInsulation().size() - 1).copy());
                    }
                });
            }
            // Item is for insulation
            else if (!ConfigSettings.INSULATION_ITEMS.get().get(insulatorItem.getItem()).isEmpty()
            && (!(insulatorItem.getItem() instanceof IArmorVanishable)
            || MobEntity.getEquipmentSlotForItem(wearableItem) == MobEntity.getEquipmentSlotForItem(insulatorItem)))
            {
                ItemStack processed = wearableItem.copy();
                if (insulateArmorItem(processed, insulatorItem))
                {
                    // Serialize insulation data for client syncing
                    ItemInsulationManager.getInsulationCap(wearableItem).ifPresent(cap ->
                    {   processed.getOrCreateTag().merge(cap.serializeNBT());
                    });
                    // Set slot to result
                    this.setItem(this.getResultSlot(), processed);
                }
            }
        }
    }

    /**
     * Tries to apply the given insulator to the armor item.<br>
     * Fails if the
     * @param armorItem
     * @param insulatorItem
     * @return
     */
    private boolean insulateArmorItem(ItemStack armorItem, ItemStack insulatorItem)
    {
        if (!ItemInsulationManager.isInsulatable(armorItem)) return false;

        InsulateItemEvent insulateEvent = new InsulateItemEvent(armorItem, insulatorItem, this.player);
        MinecraftForge.EVENT_BUS.post(insulateEvent);
        if (insulateEvent.isCanceled())
        {   return false;
        }
        insulatorItem = insulateEvent.getInsulator();

        IInsulatableCap insulCap = ItemInsulationManager.getInsulationCap(armorItem).orElseThrow(() -> new IllegalStateException(String.format("Item %s does not have insulation capability", armorItem)));
        ItemStack insulator = insulatorItem.copy();
        insulator.setCount(1);
        // Prevent exceeding the armor item's insulation capacity
        if (!insulCap.canAddInsulationItem(armorItem, insulator))
        {   return false;
        }

        insulCap.addInsulationItem(insulator);

        // Transfer enchantments
        Map<Enchantment, Integer> armorEnch = EnchantmentHelper.getEnchantments(armorItem);
        insulator.getEnchantmentTags().removeIf(nbt ->
        {
            CompoundNBT enchantTag = ((CompoundNBT) nbt);
            Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(enchantTag.getString("id")));
            if (ench == null) return false;

            if (ench.canEnchant(armorItem) && armorEnch.keySet().stream().allMatch(ench2 -> ench2.isCompatibleWith(ench)))
            {
                armorItem.enchant(ench, enchantTag.getInt("lvl"));
                return true;
            }
            return false;
        });
        return true;
    }

    public boolean isRemovingInsulation()
    {   return Tags.Items.SHEARS.contains(this.getItem(1).getItem());
    }

    @Override
    public void removed(PlayerEntity player)
    {
        // Drop the contents of the input slots
        if (player instanceof ServerPlayerEntity)
        {
            ServerPlayerEntity serverPlayer = ((ServerPlayerEntity) player);
            for (int i = 0; i < this.inputSlots.getContainerSize(); i++)
            {
                ItemStack itemStack = this.getSlot(i).getItem();
                if (!itemStack.isEmpty())
                {
                    if (player.isAlive() && !serverPlayer.hasDisconnected())
                    {   player.inventory.placeItemBackInInventory(player.level, itemStack);
                    }
                    else player.drop(itemStack, true);
                }
            }
        }
        super.removed(player);
    }

    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int index)
    {
        if (index == this.getResultSlot() && !isRemovingInsulation())
        {
            this.quickMoving = true;
            Slot resultSlot = this.slots.get(index);
            ItemStack result = resultSlot.getItem();
            do
            {
                this.growItem(0, -1);
                this.growItem(1, -1);
            }
            while (this.insulateArmorItem(result, this.getItem(1)));
            this.onTake(player, result);
        }
        ItemStack movedStack = super.quickMoveStack(player, index);
        this.quickMoving = false;
        return movedStack;
    }
}
