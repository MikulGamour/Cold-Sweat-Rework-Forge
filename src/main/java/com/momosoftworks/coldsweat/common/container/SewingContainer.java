package com.momosoftworks.coldsweat.common.container;

import com.momosoftworks.coldsweat.api.event.common.insulation.InsulateItemEvent;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.common.capability.insulation.ItemInsulationCap;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.init.ModAdvancementTriggers;
import com.momosoftworks.coldsweat.core.init.ModBlocks;
import com.momosoftworks.coldsweat.core.init.ModItemComponents;
import com.momosoftworks.coldsweat.core.init.ModMenus;
import com.momosoftworks.coldsweat.util.item.ItemStackHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class SewingContainer extends ItemCombinerMenu
{
    protected boolean quickMoving = false;
    protected Inventory playerInventory;

    public SewingContainer(int containerId, Inventory inventory)
    {   this(ModMenus.SEWING_CONTAINER_TYPE.get(), containerId, inventory, ContainerLevelAccess.NULL);
    }

    public SewingContainer(int i, Inventory inventory, FriendlyByteBuf friendlyByteBuf)
    {   this(i, inventory);
    }

    public SewingContainer(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, ContainerLevelAccess chunkAccess)
    {
        super(menuType, containerId, inventory, chunkAccess);
        this.playerInventory = inventory;
    }

    @Override
    protected ItemCombinerMenuSlotDefinition createInputSlotDefinitions()
    {
        return ItemCombinerMenuSlotDefinition.create()
        .withSlot(0, 43, 26, (stack) ->
        {   return stack.getItem() instanceof Equipable && !ConfigSettings.INSULATION_BLACKLIST.get().contains(stack.getItem())
                && ConfigSettings.INSULATION_ITEMS.get().get(stack.getItem()).isEmpty();
        })
        .withSlot(1, 43, 53, (stack) ->
        {   return !ConfigSettings.INSULATION_ITEMS.get().get(stack.getItem()).isEmpty()
                || stack.is(Tags.Items.TOOLS_SHEAR);
        })
        .withResultSlot(2, 121, 39).build();
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
                    if (!player.getAbilities().instabuild && player instanceof ServerPlayer serverPlayer)
                    {   input2.hurtAndBreak(1, (ServerLevel) serverPlayer.level(), serverPlayer, item -> {});
                    }

                    // Remove the last insulation item added
                    cap = cap.removeInsulationItem(cap.getInsulationItem(cap.getInsulation().size() - 1));
                    // Play shear sound
                    player.level().playSound(null, player.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 0.8F, 1.0F);
                }
                input1.set(ModItemComponents.ARMOR_INSULATION, cap);
            });
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
            player.level().playSound(null, player.blockPosition(), SoundEvents.LLAMA_SWAG.value(), SoundSource.BLOCKS, 0.5f, 1f);

            // Trigger advancement criteria
            if (player instanceof ServerPlayer serverPlayer)
                ModAdvancementTriggers.ARMOR_INSULATED.value().trigger(serverPlayer, input1, input2);
        }

        // Get equip sound for the armor item
        if (stack.getItem() instanceof ArmorItem armor)
        {   SoundEvent equipSound = armor.getMaterial().value().equipSound().value();
            player.level().playSound(null, player.blockPosition(), equipSound, SoundSource.BLOCKS, 1f, 1f);
        }
        this.createResult();
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
            && (!(insulatorItem.getItem() instanceof Equipable otherArmor)
            || wearableItem.getEquipmentSlot() == otherArmor.getEquipmentSlot()))
            {
                ItemStack processed = wearableItem.copy();
                if (insulateArmorItem(processed, insulatorItem))
                {
                    // Set slot to result
                    this.setItem(this.getResultSlot(), processed);
                }
            }
        }
    }

    /**
     * Tries to apply the given insulator to the armor item.<br>
     * Fails if the
     * @param armorItem The armor item to insulate
     * @param insulatorItem The insulator item to apply
     * @return True if the insulator was applied, false otherwise
     */
    private boolean insulateArmorItem(ItemStack armorItem, ItemStack insulatorItem)
    {
        if (!ItemInsulationManager.isInsulatable(armorItem)) return false;

        InsulateItemEvent insulateEvent = new InsulateItemEvent(armorItem, insulatorItem, this.player);
        NeoForge.EVENT_BUS.post(insulateEvent);
        if (insulateEvent.isCanceled())
        {   return false;
        }
        insulatorItem = insulateEvent.getInsulator();

        Optional<ItemInsulationCap> insulCap = ItemInsulationManager.getInsulationCap(armorItem);
        if (insulCap.isEmpty()) return false;
        ItemInsulationCap cap = insulCap.get();

        ItemStack insulator = insulatorItem.copy();
        insulator.setCount(1);
        // Prevent exceeding the armor item's insulation capacity
        if (!cap.canAddInsulationItem(armorItem, insulator))
        {   return false;
        }

        cap = cap.addInsulationItem(insulator);

        // Transfer enchantments
        if (armorItem.has(DataComponents.ENCHANTMENTS) && insulator.has(DataComponents.ENCHANTMENTS))
        {
            ItemEnchantments armorEnch = ItemStackHelper.getOrCreateComponent(armorItem, DataComponents.ENCHANTMENTS, () -> ItemEnchantments.EMPTY);
            ItemEnchantments.Mutable insulatorEnch = new ItemEnchantments.Mutable(ItemStackHelper.getOrCreateComponent(insulator, DataComponents.ENCHANTMENTS, () -> ItemEnchantments.EMPTY));
            insulatorEnch.removeIf(ench ->
            {
                if (ench == null) return false;

                if (ench.value().canEnchant(armorItem) && EnchantmentHelper.isEnchantmentCompatible(armorEnch.keySet(), ench))
                {   armorItem.enchant(ench, insulatorEnch.getLevel(ench));
                    return true;
                }
                return false;
            });
        }

        armorItem.set(ModItemComponents.ARMOR_INSULATION, cap);
        return true;
    }

    public boolean isRemovingInsulation()
    {
        return this.getItem(1).is(Tags.Items.TOOLS_SHEAR);
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
