package com.momosoftworks.coldsweat.common.container;

import com.momosoftworks.coldsweat.api.event.common.insulation.InsulateItemEvent;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.common.capability.insulation.ItemInsulationCap;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.event.TaskScheduler;
import com.momosoftworks.coldsweat.core.init.ModMenus;
import com.momosoftworks.coldsweat.core.init.ModAdvancementTriggers;
import com.momosoftworks.coldsweat.core.init.ModItemComponents;
import com.momosoftworks.coldsweat.core.network.message.SyncContainerSlotMessage;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nonnull;
import java.util.Optional;

public class SewingContainer extends AbstractContainerMenu
{
    BlockPos pos;
    Inventory playerInventory;
    SewingInventory sewingInventory;
    protected boolean quickMoved = false;

    public static class SewingInventory implements Container
    {
        private final NonNullList<ItemStack> stackList;
        private final AbstractContainerMenu menu;

        public SewingInventory(AbstractContainerMenu menu)
        {
            this.stackList = NonNullList.withSize(3, ItemStack.EMPTY);
            this.menu = menu;
        }

        @Override
        public int getContainerSize()
        {   return 3;
        }

        @Override
        public boolean isEmpty()
        {   return stackList.stream().allMatch(ItemStack::isEmpty);
        }

        @Nonnull
        @Override
        public ItemStack getItem(int index)
        {   return stackList.get(index);
        }

        @Nonnull
        @Override
        public ItemStack removeItem(int index, int count)
        {
            ItemStack itemstack = ContainerHelper.removeItem(this.stackList, index, count);
            if (!itemstack.isEmpty())
            {   this.menu.slotsChanged(this);
            }

            return itemstack;
        }

        @Nonnull
        @Override
        public ItemStack removeItemNoUpdate(int index)
        {   return ContainerHelper.takeItem(this.stackList, index);
        }

        @Override
        public void setItem(int index, ItemStack stack)
        {   this.stackList.set(index, stack);
            this.menu.slotsChanged(this);
        }

        @Override
        public void setChanged() {}

        @Override
        public boolean stillValid(Player player)
        {   return true;
        }

        @Override
        public void clearContent()
        {   stackList.clear();
        }
    }

    public SewingContainer(final int windowId, final Inventory playerInv)
    {
        super(ModMenus.SEWING_CONTAINER_TYPE.get(), windowId);
        this.pos = playerInv.player.blockPosition();
        this.playerInventory = playerInv;
        sewingInventory = new SewingInventory(this);

        // Input 1
        this.addSlot(new Slot(sewingInventory, 0, 43, 26)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {
                return stack.getItem() instanceof Equipable && !ConfigSettings.INSULATION_BLACKLIST.get().contains(stack.getItem())
                    && ConfigSettings.INSULATION_ITEMS.get().get(stack.getItem()).isEmpty();
            }
            @Override
            public void onTake(Player player, ItemStack stack)
            {   super.onTake(player, stack);
                SewingContainer.this.takeInput();
            }
            @Override
            public void setChanged()
            {   super.setChanged();
                TaskScheduler.schedule(SewingContainer.this::testForRecipe, 0);
            }
        });

        // Input 2
        this.addSlot(new Slot(sewingInventory, 1, 43, 53)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {
                return !ConfigSettings.INSULATION_ITEMS.get().get(stack.getItem()).isEmpty()
                    || stack.getItem() instanceof ShearsItem;
            }
            @Override
            public void onTake(Player player, ItemStack stack)
            {   super.onTake(player, stack);
                SewingContainer.this.takeInput();
            }
            @Override
            public void setChanged()
            {   super.setChanged();
                TaskScheduler.schedule(SewingContainer.this::testForRecipe, 0);
            }
        });

        // Output
        this.addSlot(new Slot(sewingInventory, 2, 121, 39)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {   return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack)
            {
                super.onTake(player, stack);
                SewingContainer.this.takeOutput(stack);

                if (!SewingContainer.this.playerInventory.player.level().isClientSide)
                    TaskScheduler.scheduleServer(() -> testForRecipe(), 0);
            }
        });

        // Main player inventory
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {   this.addSlot(new Slot(playerInv, col + (9 * row) + 9, 8 + col * 18, 166 - (4 - row) * 18 - 10));
            }
        }

        // Player Hotbar
        for (int col = 0; col < 9; col++)
        {   this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    public SewingContainer(int i, Inventory inventory, FriendlyByteBuf friendlyByteBuf)
    {
        this(i, inventory);
        try
        {   this.pos = BlockPos.of(friendlyByteBuf.readLong());
        }
        catch (Exception ignored) {}
    }

    public void setItem(int index, ItemStack stack)
    {   this.sewingInventory.setItem(index, stack);
        this.setRemoteSlot(index, stack);
    }
    public void syncSlot(int index)
    {   if (this.playerInventory.player instanceof ServerPlayer serverPlayer)
        {
            TaskScheduler.scheduleServer(() -> {
                // Send the new item to the client
                PacketDistributor.sendToPlayer(serverPlayer, new SyncContainerSlotMessage(index, this.getItem(index), this));
            }, 0);

        }
    }

    public void growItem(int index, int amount)
    {   ItemStack stack = this.sewingInventory.getItem(index);
        stack.grow(amount);
        this.sewingInventory.setItem(index, stack);
        this.setRemoteSlot(index, stack);
    }

    public ItemStack getItem(int index)
    {   return this.sewingInventory.getItem(index);
    }

    private void takeInput()
    {   this.sewingInventory.setItem(2, ItemStack.EMPTY);
    }

    private void takeOutput(ItemStack stack)
    {
        Player player = this.playerInventory.player;
        ItemStack input1 = this.getItem(0);
        ItemStack input2 = this.getItem(1);


        // If insulation is being removed
        if (input2.getItem() instanceof ShearsItem)
        {
            CSMath.doIfNotNull(input1.get(ModItemComponents.ARMOR_INSULATION), cap ->
            {
                if (!cap.getInsulation().isEmpty())
                {   // Damage shears
                    if (!player.isCreative() && player.level() instanceof ServerLevel level)
                    {   input2.hurtAndBreak(1, level, player, (item) -> {});
                    }

                    // Remove the last insulation item added
                    cap = cap.removeInsulationItem(cap.getInsulationItem(cap.getInsulation().size() - 1));
                    // Play shear sound
                    player.level().playSound(null, player.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 0.8F, 1.0F);

                    input1.set(ModItemComponents.ARMOR_INSULATION, cap);
                }
            });
        }
        // If insulation is being added
        else
        {
                if (!quickMoved)
                {   this.growItem(0, -1);
                    this.growItem(1, -1);
                }
                this.quickMoved = false;
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
    }

    private void testForRecipe()
    {
        ItemStack wearableItem = this.getItem(0);
        ItemStack insulatorItem = this.getItem(1);

        // Is the first item armor, and the second item an insulator
        if (wearableItem.getItem() instanceof Equipable armor)
        {
            // Shears are used to remove insulation
            if (insulatorItem.getItem() instanceof ShearsItem)
            {
                ItemInsulationManager.getInsulationCap(wearableItem).ifPresent(cap ->
                {
                    if (!cap.getInsulation().isEmpty())
                    {   this.setItem(2, cap.getInsulationItem(cap.getInsulation().size() - 1).copy());
                    }
                });
            }
            // Item is for insulation
            else if (!ConfigSettings.INSULATION_ITEMS.get().get(insulatorItem.getItem()).isEmpty()
            && (!(insulatorItem.getItem() instanceof Equipable otherArmor)
            || armor.getEquipmentSlot() == otherArmor.getEquipmentSlot()))
            {
                ItemStack processed = wearableItem.copy();
                if (insulateArmorItem(processed, insulatorItem))
                {
                    this.setItem(2, processed);
                    //this.syncSlot(2);
                    this.sendAllDataToRemote();
                }
            }
        }
        this.broadcastChanges();
    }

    private boolean insulateArmorItem(ItemStack armorItem, ItemStack insulatorItem)
    {
        InsulateItemEvent insulateEvent = new InsulateItemEvent(armorItem, insulatorItem, this.playerInventory.player);
        NeoForge.EVENT_BUS.post(insulateEvent);
        if (insulateEvent.isCanceled()) return false;
        insulatorItem = insulateEvent.getInsulator();

        Optional<ItemInsulationCap> insulCap = ItemInsulationManager.getInsulationCap(armorItem);
        if (insulCap.isEmpty()) return false;
        ItemInsulationCap cap = insulCap.get();

        ItemStack insulator = insulatorItem.copy();
        insulator.setCount(1);
        // Prevent exceeding the armor item's insulation capacity
        if (!cap.canAddInsulationItem(armorItem, insulator)) return false;

        cap = cap.addInsulationItem(insulator);

        // Transfer enchantments
        if (armorItem.has(DataComponents.ENCHANTMENTS) && insulator.has(DataComponents.ENCHANTMENTS))
        {
            ItemEnchantments armorEnch = armorItem.get(DataComponents.ENCHANTMENTS);
            ItemEnchantments.Mutable insulatorEnch = new ItemEnchantments.Mutable(insulator.get(DataComponents.ENCHANTMENTS));
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

    public SewingContainer(final int windowId, final Player playerInv, BlockPos pos)
    {   this(windowId, playerInv.getInventory());
        this.pos = pos;
    }

    @Override
    public void removed(Player player)
    {   super.removed(player);
        // Drop the contents of the input slots
        if (player instanceof ServerPlayer serverPlayer)
        {
            for (int i = 0; i < sewingInventory.getContainerSize(); i++)
            {
                ItemStack itemStack = this.getSlot(i).getItem();
                if (!itemStack.isEmpty() && i != 2)
                {
                    if (player.isAlive() && !serverPlayer.hasDisconnected())
                    {   player.getInventory().placeItemBackInInventory(itemStack);
                    }
                    else player.drop(itemStack, true);
                }
            }
        }
    }

    @Override
    public boolean stillValid(Player playerIn)
    {
        if (this.pos != null)
        {   return playerIn.distanceToSqr(Vec3.atCenterOf(this.pos)) <= 64.0D;
        }
        else return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem())
        {   ItemStack slotItem = slot.getItem();
            newStack = slotItem.copy();
            if (CSMath.betweenInclusive(index, 0, 2))
            {
                if (index == 2 && !(this.getItem(1).getItem() instanceof ShearsItem))
                {   this.quickMoved = true;
                    do
                    {   this.growItem(0, -1);
                        this.growItem(1, -1);
                    }
                    while (insulateArmorItem(slotItem, this.getItem(1)));
                }
                if (this.moveItemStackTo(slotItem, 3, 39, true))
                {   slot.onTake(player, newStack);
                }
                else return ItemStack.EMPTY;
            }
            else
            {
                // Item is valid insulation
                if (this.slots.get(1).mayPlace(slotItem))
                {
                    if (this.moveItemStackTo(slotItem, 1, 2, false))
                    {   slot.onQuickCraft(slotItem, newStack);
                    }
                    else return ItemStack.EMPTY;
                }
                // Item is valid wearable
                else if (this.slots.get(0).mayPlace(slotItem))
                {
                    if (!this.moveItemStackTo(slotItem, 0, 1, false))
                    {   slot.onQuickCraft(slotItem, newStack);
                    }
                    else return ItemStack.EMPTY;
                }
                // Item is in hotbar
                else if (CSMath.betweenInclusive(index, slots.size() - 9, slots.size()))
                {
                    if (!this.moveItemStackTo(slotItem, 3, 29, false))
                    {   slot.onQuickCraft(slotItem, newStack);
                    }
                    else return ItemStack.EMPTY;
                }
                // Item is in inventory
                else if (CSMath.betweenInclusive(index, 3, slots.size() - 9))
                {
                    if (!this.moveItemStackTo(slotItem, slots.size() - 9, slots.size(), false))
                    {   slot.onQuickCraft(slotItem, newStack);
                    }
                    else return ItemStack.EMPTY;
                }
                return ItemStack.EMPTY;
            }

            if (slotItem.isEmpty())
            {   slot.set(ItemStack.EMPTY);
            }
            else slot.setChanged();
        }

        return newStack;
    }
}
