package com.momosoftworks.coldsweat.common.blockentity;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.common.block.BoilerBlock;
import com.momosoftworks.coldsweat.common.container.BoilerContainer;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.init.ModBlockEntities;
import com.momosoftworks.coldsweat.core.init.ModItemComponents;
import com.momosoftworks.coldsweat.core.init.ModItems;
import com.momosoftworks.coldsweat.core.init.ModSounds;
import com.momosoftworks.coldsweat.data.codec.configuration.FuelData;
import com.momosoftworks.coldsweat.data.tag.ModItemTags;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BoilerBlockEntity extends HearthBlockEntity
{
    public static int[] WATERSKIN_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    public static int[] FUEL_SLOT = {0};

    public BoilerBlockEntity(BlockPos pos, BlockState state)
    {   super(ModBlockEntities.BOILER.value(), pos, state);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries)
    {   handleUpdateTag(pkt.getTag(), registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {   return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected Component getDefaultName()
    {   return Component.translatable("container." + ColdSweat.MOD_ID + ".boiler");
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
                    double itemTemp = stack.getOrDefault(ModItemComponents.WATER_TEMPERATURE, 0d);

                    if (stack.is(ModItems.FILLED_WATERSKIN) && itemTemp < 50)
                    {   hasItemStacks = true;
                        stack.set(ModItemComponents.WATER_TEMPERATURE, Math.min(50, itemTemp + 1));
                    }
                }
            }
            if (ticksExisted % (200 / Math.max(1, ConfigSettings.TEMP_RATE.get())) == 0)
            {
                for (int i = 1; i < 10; i++)
                {
                    ItemStack stack = getItem(i);
                    if (CompatManager.isThirstLoaded() && CompatManager.Thirst.hasWaterPurity(stack)
                    && CompatManager.Thirst.getWaterPurity(stack) < 3)
                    {
                        CompatManager.Thirst.setWaterPurity(stack, CompatManager.Thirst.getWaterPurity(stack) + 1);
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
    public int getSpreadRange()
    {   return ConfigSettings.BOILER_RANGE.get();
    }

    @Override
    public int getMaxRange()
    {   return ConfigSettings.BOILER_MAX_RANGE.get();
    }

    @Override
    public int getMaxPaths()
    {   return ConfigSettings.BOILER_MAX_VOLUME.get();
    }

    @Override
    public int getInsulationTime()
    {   return ConfigSettings.BOILER_WARM_UP_TIME.get();
    }

    @Override
    public int getMaxInsulationLevel()
    {   return ConfigSettings.BOILER_MAX_INSULATION.get();
    }

    @Override
    public SoundEvent getFuelDepleteSound()
    {   return ModSounds.BOILER_DEPLETE.value();
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
        return Direction.stream().anyMatch(direction ->
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
    {   return ConfigHelper.findFirstFuelMatching(ConfigSettings.BOILER_FUEL, item)
               .map(FuelData::fuel).orElse(0d).intValue();
    }

    @Override
    protected void storeFuel(ItemStack stack, int amount)
    {
        if (this.getFuel() < this.getMaxFuel() - Math.abs(amount) * 0.75)
        {
            if (!stack.hasCraftingRemainingItem() || stack.getCount() > 1)
            {   int consumeCount = Math.min((int) Math.floor((this.getMaxFuel() - this.getFuel()) / (double) Math.abs(amount)), stack.getCount());
                stack.shrink(consumeCount);
                addFuel(amount * consumeCount);
            }
            else
            {   this.setItem(0, stack.getCraftingRemainingItem());
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
    {   return new BoilerContainer(id, playerInv, this);
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
    public int[] getSlotsForFace(Direction side)
    {   return side.getAxis() == Direction.Axis.Y ? WATERSKIN_SLOTS : FUEL_SLOT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction)
    {
        if (slot == 0)
            return this.getItemFuel(stack) != 0;
        else return stack.is(ModItemTags.BOILER_VALID) || (CompatManager.isThirstLoaded() && CompatManager.Thirst.hasWaterPurity(stack));
    }
}
