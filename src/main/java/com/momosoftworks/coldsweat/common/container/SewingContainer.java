package com.momosoftworks.coldsweat.common.container;

import com.momosoftworks.coldsweat.api.event.common.insulation.InsulateItemEvent;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.common.capability.insulation.IInsulatableCap;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.advancement.trigger.ModAdvancementTriggers;
import com.momosoftworks.coldsweat.core.event.TaskScheduler;
import com.momosoftworks.coldsweat.core.init.ContainerInit;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.core.network.message.SyncContainerSlotMessage;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.IArmorVanishable;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

public class SewingContainer extends Container
{
    BlockPos pos;
    PlayerInventory playerInventory;
    SewingInventory sewingInventory;
    protected boolean quickMoved = false;

    public static class SewingInventory implements IInventory
    {
        private final NonNullList<ItemStack> stackList;
        private final Container menu;

        public SewingInventory(Container menu)
        {   this.stackList = NonNullList.withSize(3, ItemStack.EMPTY);
            this.menu = menu;
        }

        public int getContainerSize()
        {   return 3;
        }

        public boolean isEmpty()
        {   return stackList.stream().allMatch(ItemStack::isEmpty);
        }

        public ItemStack getItem(int index)
        {   return stackList.get(index);
        }

        @Override
        public ItemStack removeItem(int index, int count)
        {
            ItemStack itemstack = ItemStackHelper.removeItem(this.stackList, index, count);
            if (!itemstack.isEmpty())
            {   this.menu.slotsChanged(this);
            }

            return itemstack;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index)
        {   return ItemStackHelper.takeItem(this.stackList, index);
        }

        @Override
        public void setItem(int index, ItemStack stack)
        {   this.stackList.set(index, stack);
            this.menu.slotsChanged(this);
        }

        @Override
        public void setChanged() {}

        @Override
        public boolean stillValid(PlayerEntity player)
        {   return true;
        }

        @Override
        public void clearContent()
        {   stackList.clear();
        }
    }

    public SewingContainer(final int windowId, final PlayerInventory playerInv)
    {
        super(ContainerInit.SEWING_CONTAINER_TYPE.get(), windowId);
        this.pos = playerInv.player.blockPosition();
        this.playerInventory = playerInv;
        sewingInventory = new SewingInventory(this);

        // Input 1
        this.addSlot(new Slot(sewingInventory, 0, 43, 26)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {
                return stack.getItem() instanceof IArmorVanishable && !ConfigSettings.INSULATION_BLACKLIST.get().contains(stack.getItem())
                    && ConfigSettings.INSULATION_ITEMS.get().get(stack.getItem()).isEmpty();
            }

            @Override
            public ItemStack onTake(PlayerEntity player, ItemStack stack)
            {   super.onTake(player, stack);
                SewingContainer.this.takeInput();

                return stack;
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
            public ItemStack onTake(PlayerEntity player, ItemStack stack)
            {   super.onTake(player, stack);
                SewingContainer.this.takeInput();

                return stack;
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
            public ItemStack onTake(PlayerEntity player, ItemStack stack)
            {
                super.onTake(player, stack);
                SewingContainer.this.takeOutput(stack);

                if (!SewingContainer.this.playerInventory.player.level.isClientSide)
                    TaskScheduler.scheduleServer(() -> testForRecipe(), 0);
                return stack;
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

    public SewingContainer(int i, PlayerInventory inventory, PacketBuffer friendlyByteBuf)
    {
        this(i, inventory);
        try
        {   this.pos = BlockPos.of(friendlyByteBuf.readLong());
        }
        catch (Exception ignored) {}
    }

    public void setItem(int index, ItemStack stack)
    {
        if (index >= sewingInventory.stackList.size())
        {   return;
        }
        this.sewingInventory.setItem(index, stack);
    }
    public void syncSlot(int index)
    {   if (this.playerInventory.player instanceof ServerPlayerEntity)
        {
            ServerPlayerEntity serverPlayer = ((ServerPlayerEntity) this.playerInventory.player);
            TaskScheduler.scheduleServer(() -> {
                // Send the new item to the client
                ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                                                     new SyncContainerSlotMessage(index, this.getItem(index), this));
            }, 0);

        }
    }

    public void growItem(int index, int amount)
    {   ItemStack stack = this.sewingInventory.getItem(index);
        stack.grow(amount);
        this.sewingInventory.setItem(index, stack);
    }

    public ItemStack getItem(int index)
    {   return this.sewingInventory.getItem(index);
    }

    private void takeInput()
    {   this.sewingInventory.setItem(2, ItemStack.EMPTY);
    }

    private void takeOutput(ItemStack stack)
    {
        PlayerEntity player = this.playerInventory.player;
        ItemStack input1 = this.getItem(0);
        ItemStack input2 = this.getItem(1);

        ItemInsulationManager.getInsulationCap(input1).ifPresent(cap ->
        {
            // If insulation is being removed
            if (input2.getItem() instanceof ShearsItem)
            {
                if (!cap.getInsulation().isEmpty())
                {   // Damage shears
                    if (!player.isCreative())
                    {   input2.hurt(1, player.getRandom(), null);
                    }

                    // Remove the last insulation item added
                    cap.removeInsulationItem(cap.getInsulationItem(cap.getInsulation().size() - 1));
                    // Play shear sound
                    player.level.playSound(null, player.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundCategory.PLAYERS, 0.8F, 1.0F);

                    input1.getOrCreateTag().merge(cap.serializeNBT());
                }
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
                player.level.playSound(null, player.blockPosition(), SoundEvents.LLAMA_SWAG, SoundCategory.BLOCKS, 0.5f, 1f);

                // Trigger advancement criteria
                if (player instanceof ServerPlayerEntity)
                    ModAdvancementTriggers.ARMOR_INSULATED.trigger(((ServerPlayerEntity) player), input1, input2);
            }
        });

        if (stack.getItem() instanceof ArmorItem)
        {
            ArmorItem armor = (ArmorItem) stack.getItem();
            // Get equip sound for the armor item
            SoundEvent equipSound = armor.getMaterial().getEquipSound();
            player.level.playSound(null, player.blockPosition(), equipSound, SoundCategory.BLOCKS, 1f, 1f);
        }
    }

    private void testForRecipe()
    {
        ItemStack wearableItem = this.getItem(0);
        ItemStack insulatorItem = this.getItem(1);

        // Is the first item armor, and the second item an insulator
        if (wearableItem.getItem() instanceof IArmorVanishable)
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
            && (!(insulatorItem.getItem() instanceof IArmorVanishable)
            || MobEntity.getEquipmentSlotForItem(wearableItem) == MobEntity.getEquipmentSlotForItem(insulatorItem)))
            {
                ItemStack processed = wearableItem.copy();
                if (insulateArmorItem(processed, insulatorItem))
                {
                    this.setItem(2, processed);
                    //this.syncSlot(2);
                    this.slotsChanged(this.sewingInventory);
                }
            }
        }
        this.broadcastChanges();
    }

    private boolean insulateArmorItem(ItemStack armorItem, ItemStack insulatorItem)
    {
        InsulateItemEvent insulateEvent = new InsulateItemEvent(armorItem, insulatorItem, this.playerInventory.player);
        MinecraftForge.EVENT_BUS.post(insulateEvent);
        if (insulateEvent.isCanceled()) return false;
        insulatorItem = insulateEvent.getInsulator();

        IInsulatableCap insulCap = ItemInsulationManager.getInsulationCap(armorItem).orElseThrow(() -> new IllegalStateException("Item does not have insulation capability"));
        ItemStack insulator = insulatorItem.copy();
        insulator.setCount(1);
        // Prevent exceeding the armor item's insulation capacity
        if (!insulCap.canAddInsulationItem(armorItem, insulator)) return false;

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

        if (this.playerInventory.player instanceof ServerPlayerEntity)
        {
            ServerPlayerEntity serverPlayer = ((ServerPlayerEntity) this.playerInventory.player);
            TaskScheduler.scheduleServer(() ->
            {
                // Send the new item to the client
                ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new SyncContainerSlotMessage(2, armorItem, this));
            }, 1);
        }
        return true;
    }

    public SewingContainer(final int windowId, final PlayerEntity playerInv, BlockPos pos)
    {   this(windowId, playerInv.inventory);
        this.pos = pos;
    }

    @Override
    public void removed(PlayerEntity player)
    {   super.removed(player);
        // Drop the contents of the input slots
        if (player instanceof ServerPlayerEntity)
        {
            ServerPlayerEntity serverPlayer = ((ServerPlayerEntity) player);
            for (int i = 0; i < sewingInventory.getContainerSize(); i++)
            {
                ItemStack itemStack = this.sewingInventory.getItem(i);
                if (!itemStack.isEmpty() && i != 2)
                {
                    if (player.isAlive() && !serverPlayer.hasDisconnected())
                    {   player.inventory.placeItemBackInInventory(player.level, itemStack);
                    }
                    else player.drop(itemStack, true);
                }
            }
        }
    }

    @Override
    public boolean stillValid(PlayerEntity playerIn)
    {
        if (this.pos != null)
        {   return playerIn.distanceToSqr(Vector3d.atCenterOf(this.pos)) <= 64.0D;
        }
        else return true;
    }

    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int index)
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
