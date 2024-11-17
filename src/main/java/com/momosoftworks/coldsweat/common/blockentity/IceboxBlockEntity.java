package com.momosoftworks.coldsweat.common.blockentity;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.common.block.IceboxBlock;
import com.momosoftworks.coldsweat.common.container.IceboxContainer;
import com.momosoftworks.coldsweat.common.item.FilledWaterskinItem;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.init.ParticleTypesInit;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.core.network.message.BlockDataUpdateMessage;
import com.momosoftworks.coldsweat.data.codec.configuration.FuelData;
import com.momosoftworks.coldsweat.data.tag.ModItemTags;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.registries.ModBlockEntities;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import com.momosoftworks.coldsweat.util.registries.ModSounds;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
import java.util.Random;

public class IceboxBlockEntity extends HearthBlockEntity implements MenuProvider, WorldlyContainer, LidBlockEntity
{
    public static int[] WATERSKIN_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    public static int[] FUEL_SLOT = {0};

    LazyOptional<? extends IItemHandler>[] slotHandlers =
            SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);

    List<ServerPlayer> usingPlayers = new ArrayList<>();

    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter()
    {
        protected void onOpen(Level level, BlockPos pos, BlockState state)
        {
            if (!IceboxBlockEntity.this.hasSmokeStack())
            {   IceboxBlockEntity.this.level.playSound(null, pos, ModSounds.ICEBOX_OPEN, SoundSource.BLOCKS, 1f, level.random.nextFloat() * 0.2f + 0.9f);
            }
        }

        protected void onClose(Level level, BlockPos pos, BlockState state)
        {
            if (!IceboxBlockEntity.this.hasSmokeStack())
            {   IceboxBlockEntity.this.level.playSound(null, pos, ModSounds.ICEBOX_CLOSE, SoundSource.BLOCKS, 1f, 1f);
            }
        }

        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int eventId, int eventParam)
        {   IceboxBlockEntity.this.signalOpenCount(level, pos, state, eventId, eventParam);
        }

        protected boolean isOwnContainer(Player player)
        {   return player.containerMenu instanceof IceboxContainer container && container.te.equals(IceboxBlockEntity.this);
        }
    };

    ChestLidController lidController = new ChestLidController();

    public IceboxBlockEntity(BlockPos pos, BlockState state)
    {   super(ModBlockEntities.ICEBOX, pos, state);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt)
    {   handleUpdateTag(pkt.getTag());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {   return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public boolean triggerEvent(int id, int type)
    {
        if (id == 1)
        {   this.lidController.shouldBeOpen(type > 0);
            return true;
        }
        else
        {   return super.triggerEvent(id, type);
        }
    }

    protected void signalOpenCount(Level level, BlockPos pos, BlockState state, int eventId, int eventParam)
    {   Block block = state.getBlock();
        level.blockEvent(pos, block, eventId, eventParam);
    }

    private void sendUpdatePacket()
    {
        // Remove the players that aren't interacting with this block anymore
        usingPlayers.removeIf(player -> !(player.containerMenu instanceof IceboxContainer iceboxContainer && iceboxContainer.te == this));

        // Send data to all players with this block's menu open
        ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.NMLIST.with(()-> usingPlayers.stream().map(player -> player.connection.connection).toList()),
                                             new BlockDataUpdateMessage(this));
    }

    @Override
    protected Component getDefaultName()
    {   return new TranslatableComponent("container." + ColdSweat.MOD_ID + ".icebox");
    }

    @Override
    public Component getDisplayName()
    {   return this.getCustomName() != null ? this.getCustomName() : this.getDefaultName();
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T te)
    {
        if (te instanceof IceboxBlockEntity iceboxTE)
        {
            iceboxTE.tick(level, state, pos);
            // Tick lid animation on client
            if (level.isClientSide())
            {   tickAnimateLid(level, pos, state, iceboxTE);
            }
        }
    }

    public static <T extends BlockEntity> void tickAnimateLid(Level level, BlockPos pos, BlockState state, T blockEntity)
    {
        if (blockEntity instanceof IceboxBlockEntity icebox)
        {   icebox.lidController.tickLid();
        }
    }

    public void tick(Level level, BlockState state, BlockPos pos)
    {
        super.tick(level, pos);

        // Recheck openers
        if (!this.remove)
        {   this.openersCounter.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
        }

        if (getFuel() > 0)
        {
            // Set state to frosted
            if (!state.getValue(IceboxBlock.FROSTED))
                level.setBlock(pos, state.setValue(IceboxBlock.FROSTED, true), 3);

            // Cool down waterskins
            if (ticksExisted % (int) (20 / Math.max(1, ConfigSettings.TEMP_RATE.get())) == 0)
            {
                boolean hasItemStacks = false;
                for (int i = 1; i < 10; i++)
                {
                    ItemStack stack = getItem(i);
                    CompoundTag tag = NBTHelper.getTagOrEmpty(stack);
                    double itemTemp = tag.getDouble(FilledWaterskinItem.NBT_TEMPERATURE);

                    if (stack.is(ModItems.FILLED_WATERSKIN) && itemTemp > -50)
                    {   hasItemStacks = true;
                        tag.putDouble(FilledWaterskinItem.NBT_TEMPERATURE, Math.max(-50, itemTemp - 1));
                    }
                }
                if (hasItemStacks) setFuel(getFuel() - 1);
            }
        }
        // if no fuel, set state to unfrosted
        else if (state.getValue(IceboxBlock.FROSTED))
        {   level.setBlock(pos, state.setValue(IceboxBlock.FROSTED, false), 3);
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
    {   return ModSounds.ICEBOX_DEPLETE;
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
    protected boolean hasSignalFromSides()
    {   return Arrays.stream(Direction.values()).anyMatch(dir -> dir != Direction.UP && this.level.hasSignal(this.getBlockPos().relative(dir), dir));
    }

    @Override
    protected boolean hasSignalFromBack()
    {   return false;
    }

    @Override
    public int getItemFuel(ItemStack item)
    {   return ConfigHelper.findFirstFuelMatching(ConfigSettings.ICEBOX_FUEL, item)
               .map(FuelData::fuel).orElse(0d).intValue();
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
    {   return this.getColdFuel();
    }

    public void setFuel(int amount)
    {   this.setColdFuel(amount, true);
    }

    @Override
    public void setColdFuel(int amount, boolean update)
    {   super.setColdFuel(amount, update);
        this.sendUpdatePacket();
    }

    @Override
    public void addFuel(int amount)
    {   this.setColdFuelAndUpdate(this.getColdFuel() + amount);
    }

    @Override
    protected boolean isFuelChanged()
    {   return this.ticksExisted % 10 == 0;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory playerInv)
    {   return new IceboxContainer(id, playerInv, this);
    }

    @Override
    public void startOpen(Player player)
    {
        super.startOpen(player);
        this.openersCounter.incrementOpeners(player, this.level, this.getBlockPos(), this.getBlockState());
        if (player instanceof ServerPlayer serverPlayer)
        {   this.usingPlayers.add(serverPlayer);
            this.sendUpdatePacket();
        }
    }

    @Override
    public void stopOpen(Player player)
    {
        super.stopOpen(player);
        this.openersCounter.decrementOpeners(player, this.level, this.getBlockPos(), this.getBlockState());
        if (player instanceof ServerPlayer serverPlayer)
        {   this.usingPlayers.remove(serverPlayer);
        }
    }

    @Override
    protected void tickParticles()
    {
        if (this.hasSmokestack)
        {   super.tickParticles();
        }
    }

    @Override
    public ParticleOptions getAirParticle()
    {   return ParticleTypesInit.GROUND_MIST.get();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void spawnAirParticle(int x, int y, int z, Random rand)
    {
        ParticleStatus status = Minecraft.getInstance().options.particles;
        if (status != ParticleStatus.ALL)
        {   return;
        }

        BlockPos pos = new BlockPos(x, y, z);
        boolean onGround = !this.level.getBlockState(pos.below()).isAir();
        if (rand.nextFloat() > (spreading ? 0.016f : 0.032f))
        {   return;
        }

        float xr = rand.nextFloat();
        float yr = onGround ? 0.1f : rand.nextFloat();
        float zr = rand.nextFloat();
        float xm = rand.nextFloat() / 20 - 0.025f;
        float zm = rand.nextFloat() / 20 - 0.025f;

        level.addParticle(onGround ? ParticleTypesInit.GROUND_MIST.get()
                                   : ParticleTypesInit.MIST.get(), false, x + xr, y + yr, z + zr, xm, 0, zm);
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
        else return stack.is(ModItemTags.ICEBOX_VALID) || stack.isEdible();
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction)
    {   return true;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing)
    {
        if (!this.remove && facing != null && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            if (facing == Direction.UP)
                return slotHandlers[0].cast();
            else if (facing == Direction.DOWN)
                return slotHandlers[1].cast();
            else
                return slotHandlers[2].cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public float getOpenNess(float partialTick)
    {   return this.lidController.getOpenness(partialTick);
    }
}
