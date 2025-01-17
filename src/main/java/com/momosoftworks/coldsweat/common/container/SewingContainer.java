package com.momosoftworks.coldsweat.common.container;

import com.momosoftworks.coldsweat.api.event.common.insulation.InsulateItemEvent;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.common.capability.insulation.IInsulatableCap;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.advancement.trigger.ModAdvancementTriggers;
import com.momosoftworks.coldsweat.core.init.MenuInit;
import com.momosoftworks.coldsweat.util.registries.ModBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Wearable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class SewingContainer extends ItemCombinerMenu
{
    protected boolean quickMoving = false;
    protected Inventory playerInventory;

    public SewingContainer(int containerId, Inventory inventory)
    {   this(MenuInit.SEWING_CONTAINER_TYPE.get(), containerId, inventory, ContainerLevelAccess.NULL);
    }

    public SewingContainer(int i, Inventory inventory, FriendlyByteBuf friendlyByteBuf)
    {   this(i, inventory);
    }

    public SewingContainer(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, ContainerLevelAccess chunkAccess)
    {
        super(menuType, containerId, inventory, chunkAccess);
        this.playerInventory = inventory;

        this.slots.set(0, new Slot(this.inputSlots, 0, 43, 26)
        {
            {   this.index = 0;
            }
            public boolean mayPlace(ItemStack stack)
            {
                return stack.getItem() instanceof Wearable && !ConfigSettings.INSULATION_BLACKLIST.get().contains(stack.getItem())
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
                    || stack.is(Tags.Items.SHEARS);
            }
        });
        this.slots.set(2, new Slot(this.resultSlots, 2, 121, 39)
        {
            {   this.index = 2;
            }
            public boolean mayPlace(ItemStack stack)
            {   return false;
            }
            public boolean mayPickup(Player player)
            {   return SewingContainer.this.mayPickup(player, this.hasItem());
            }
            public void onTake(Player player, ItemStack stack)
            {   SewingContainer.this.onTake(player, stack);
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

    protected Container getContainerForSlot(int index)
    {   return index == this.getResultSlot() ? this.resultSlots : this.inputSlots;
    }

    public void growItem(int index, int amount)
    {
        ItemStack stack = this.getItem(index);
        stack.grow(amount);
        this.setItem(index, stack);
    }

    @Override
    protected boolean mayPickup(Player player, boolean hasStack)
    {   return true;
    }

    /**
     * Calculates the result of taking the output item.<br>
     * <br>
     * Sewing: Takes ingredients from input slots<br>
     * Shearing: Removes insulator(s) from the armor item
     */
    @Override
    protected void onTake(Player player, ItemStack stack)
    {
        if (!ItemInsulationManager.isInsulatable(stack)) return;

        ItemStack input1 = this.getItem(0);
        ItemStack input2 = this.getItem(1);

        // If insulation is being removed
        if (isRemovingInsulation())
        {
            ItemInsulationManager.getInsulationCap(input1).ifPresent(cap ->
            {
                if (!cap.getInsulation().isEmpty())
                {   // Damage shears
                    if (!player.getAbilities().instabuild)
                    {   input2.hurtAndBreak(1, player, item -> {});
                    }

                    // Remove the last insulation item added
                    cap.removeInsulationItem(cap.getInsulationItem(cap.getInsulation().size() - 1));
                    // Play shear sound
                    player.level.playSound(null, player.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 0.8F, 1.0F);
                }
            });
            this.createResult();
            this.sendAllDataToRemote();
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
            player.level.playSound(null, player.blockPosition(), SoundEvents.LLAMA_SWAG, SoundSource.BLOCKS, 0.5f, 1f);

            // Trigger advancement criteria
            if (player instanceof ServerPlayer serverPlayer)
                ModAdvancementTriggers.ARMOR_INSULATED.trigger(serverPlayer, input1, input2);
        }

        // Get equip sound for the armor item
        SoundEvent equipSound = stack.getItem().getEquipSound();
        if (equipSound != null) player.level.playSound(null, player.blockPosition(), equipSound, SoundSource.BLOCKS, 1f, 1f);
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
            && (!(insulatorItem.getItem() instanceof Wearable)
            || LivingEntity.getEquipmentSlotForItem(wearableItem) == LivingEntity.getEquipmentSlotForItem(insulatorItem)))
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
            CompoundTag enchantTag = ((CompoundTag) nbt);
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
    {
        return this.getItem(1).is(Tags.Items.SHEARS);
    }

    @Override
    public void removed(Player player)
    {
        // Drop the contents of the input slots
        if (player instanceof ServerPlayer serverPlayer)
        {
            for (int i = 0; i < this.inputSlots.getContainerSize(); i++)
            {
                ItemStack itemStack = this.getSlot(i).getItem();
                if (!itemStack.isEmpty())
                {
                    if (player.isAlive() && !serverPlayer.hasDisconnected())
                    {   player.getInventory().placeItemBackInInventory(itemStack);
                    }
                    else player.drop(itemStack, true);
                }
            }
        }
        super.removed(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
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
