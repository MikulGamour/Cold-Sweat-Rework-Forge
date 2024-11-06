package com.momosoftworks.coldsweat.common.blockentity;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.common.block.BoilerBlock;
import com.momosoftworks.coldsweat.common.container.BoilerContainer;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.common.item.FilledWaterskinItem;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.config.type.PredicateItem;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.core.network.message.BlockDataUpdateMessage;
import com.momosoftworks.coldsweat.data.tag.ModItemTags;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.registries.ModBlockEntities;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import com.momosoftworks.coldsweat.util.registries.ModSounds;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BoilerBlockEntity extends HearthBlockEntity implements MenuProvider, WorldlyContainer
{
    public static int[] WATERSKIN_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    public static int[] FUEL_SLOT = {0};

    LazyOptional<? extends IItemHandler>[] slotHandlers =
            SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);

    List<ServerPlayer> usingPlayers = new ArrayList<>();

    public BoilerBlockEntity(BlockPos pos, BlockState state)
    {   super(ModBlockEntities.BOILER, pos, state);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt)
    {   handleUpdateTag(pkt.getTag());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {   return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sendUpdatePacket()
    {
        // Remove the players that aren't interacting with this block anymore
        usingPlayers.removeIf(player -> !(player.containerMenu instanceof BoilerContainer boilerContainer && boilerContainer.te == this));

        // Send data to all players with this block's menu open
        ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.NMLIST.with(()-> usingPlayers.stream().map(player -> player.connection.connection).toList()),
                                             new BlockDataUpdateMessage(this));
    }

    @Override
    protected Component getDefaultName()
    {   return new TranslatableComponent("container." + ColdSweat.MOD_ID + ".boiler");
    }

    @Override
    public Component getDisplayName()
    {   return this.getCustomName() != null ? this.getCustomName() : this.getDefaultName();
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T te)
    {
        if (te instanceof BoilerBlockEntity boilerTE)
        {   boilerTE.tick(level, state, pos);
        }
    }

    public void tick(Level level, BlockState state, BlockPos pos)
    {
        super.tick(level, pos);

        if (this.getFuel() > 0)
        {
            // Set state to lit
            if (!state.getValue(BoilerBlock.LIT))
            {   level.setBlock(pos, state.setValue(BoilerBlock.LIT, true), 3);
            }
            boolean hasItemStacks = false;

            // Warm up waterskins
            if (ticksExisted % (int) (20 / Math.max(1, ConfigSettings.TEMP_RATE.get())) == 0)
            {
                for (int i = 1; i < 10; i++)
                {
                    ItemStack stack = getItem(i);
                    CompoundTag tag = NBTHelper.getTagOrEmpty(stack);
                    double itemTemp = tag.getDouble(FilledWaterskinItem.NBT_TEMPERATURE);

                    if (stack.is(ModItems.FILLED_WATERSKIN) && itemTemp < 50)
                    {   hasItemStacks = true;
                        tag.putDouble(FilledWaterskinItem.NBT_TEMPERATURE, Math.min(50, itemTemp + 1));
                    }
                }
            }
            if (ticksExisted % (200 / Math.max(1, ConfigSettings.TEMP_RATE.get())) == 0)
            {
                for (int i = 1; i < 10; i++)
                {
                    ItemStack stack = getItem(i);
                    if (CompatManager.isThirstLoaded() && CompatManager.hasWaterPurity(stack)
                    && CompatManager.getWaterPurity(stack) < 3)
                    {
                        CompatManager.setWaterPurity(stack, CompatManager.getWaterPurity(stack) + 1);
                        hasItemStacks = true;
                    }
                }
            }
            if (hasItemStacks) setFuel(getFuel() - 1);
        }
        // if no fuel, set state to unlit
        else if (state.getValue(BoilerBlock.LIT))
        {   level.setBlock(pos, state.setValue(BoilerBlock.LIT, false), 3);
        }
    }

    @Override
    public int getMaxPaths()
    {   return 2000;
    }

    @Override
    public int getSpreadRange()
    {   return 16;
    }

    @Override
    public int getMaxInsulationLevel()
    {   return 5;
    }

    @Override
    public SoundEvent getFuelDepleteSound()
    {   return ModSounds.BOILER_DEPLETE;
    }

    @Override
    public boolean hasSmokeStack()
    {   return this.hasSmokestack;
    }

    @Override
    protected void tickPaths(int firstIndex, int lastIndex)
    {
        if (this.hasSmokestack)
        {   super.tickPaths(firstIndex, lastIndex);
        }
    }

    @Override
    protected boolean hasSignalFromBack()
    {
        return Arrays.stream(Direction.values()).anyMatch(direction ->
        {
            return direction != Direction.UP && direction != this.getBlockState().getValue(BoilerBlock.FACING)
                && this.level.hasSignal(this.worldPosition.relative(direction), direction);
        });
    }

    @Override
    protected boolean hasSignalFromSides()
    {   return false;
    }

    @Override
    public int getItemFuel(ItemStack item)
    {   return ConfigHelper.findFirstItemMatching(ConfigSettings.BOILER_FUEL, item)
               .map(PredicateItem::value).orElse(0d).intValue();
    }

    @Override
    protected void storeFuel(ItemStack stack, int amount)
    {
        if (this.getFuel() < this.getMaxFuel() - Math.abs(amount) * 0.75)
        {
            if (!stack.hasContainerItem() || stack.getCount() > 1)
            {   int consumeCount = Math.min((int) Math.floor((this.getMaxFuel() - this.getFuel()) / (double) Math.abs(amount)), stack.getCount());
                stack.shrink(consumeCount);
                addFuel(amount * consumeCount);
            }
            else
            {   this.setItem(0, stack.getContainerItem());
                addFuel(amount);
            }
        }
    }

    public int getFuel()
    {   return this.getHotFuel();
    }

    public void setFuel(int amount)
    {   this.setHotFuel(amount, true);
    }

    @Override
    public void setHotFuel(int amount, boolean update)
    {   super.setHotFuel(amount, update);
        this.sendUpdatePacket();
    }

    @Override
    public void addFuel(int amount)
    {   this.setHotFuelAndUpdate(this.getHotFuel() + amount);
    }

    @Override
    protected boolean isFuelChanged()
    {   return this.ticksExisted % 10 == 0;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory playerInv)
    {
        // Track the players using this block
        if (playerInv.player instanceof ServerPlayer serverPlayer)
        {   usingPlayers.add(serverPlayer);
        }
        return new BoilerContainer(id, playerInv, this);
    }

    @Override
    protected void tickParticles()
    {
        if (this.hasSmokestack)
        {   super.tickParticles();
        }
    }

    @Override
    public int getContainerSize()
    {   return 10;
    }

    @Override
    public int[] getSlotsForFace(Direction dir)
    {   return dir.getAxis() == Direction.Axis.Y ? WATERSKIN_SLOTS : FUEL_SLOT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction)
    {
        if (slot == 0)
            return this.getItemFuel(stack) != 0;
        else return stack.is(ModItemTags.BOILER_VALID) || (CompatManager.isThirstLoaded() && CompatManager.hasWaterPurity(stack));
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction)
    {   return true;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (!this.remove && facing != null && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (facing == Direction.UP)
                return slotHandlers[0].cast();
            else if (facing == Direction.DOWN)
                return slotHandlers[1].cast();
            else
                return slotHandlers[2].cast();
        }
        return super.getCapability(capability, facing);
    }
}
