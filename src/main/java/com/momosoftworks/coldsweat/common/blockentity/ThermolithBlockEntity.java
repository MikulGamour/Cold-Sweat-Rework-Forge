package com.momosoftworks.coldsweat.common.blockentity;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.block.ThermolithBlock;
import com.momosoftworks.coldsweat.core.init.BlockEntityInit;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public class ThermolithBlockEntity extends TileEntity implements ITickableTileEntity
{
    private int signal = 0;

    public ThermolithBlockEntity()
    {
        super(BlockEntityInit.THERMOLITH_BLOCK_ENTITY_TYPE.get());
    }

    @Override
    public void tick()
    {
        if (level.getGameTime() % 20 == 0 && !level.isClientSide)
        {
            BlockPos pos = this.getBlockPos();
            BlockState state = this.getBlockState();
            // Handle signal output / neighbor updates
            double temperature = Temperature.getTemperatureAt(pos, level);
            int newSignal = (int) CSMath.blend(0, 15, temperature, ConfigSettings.MIN_TEMP.get(), ConfigSettings.MAX_TEMP.get());
            Direction facing = this.getBlockState().getValue(ThermolithBlock.FACING);

            if (newSignal != signal)
            {
                signal = newSignal;
                this.updateFacingNeighbors();
            }

            // Handle turning on/off
            if (signal == 0)
            {   if (state.getValue(ThermolithBlock.POWERED))
                {   level.setBlockAndUpdate(pos, state.setValue(ThermolithBlock.POWERED, false));
                }
            }
            else if (!state.getValue(ThermolithBlock.POWERED))
            {   level.setBlockAndUpdate(pos, state.setValue(ThermolithBlock.POWERED, true));
            }
        }
    }

    @Override
    public void setRemoved()
    {
        super.setRemoved();
        this.updateFacingNeighbors();
    }

    public int getSignal()
    {
        return signal;
    }

    public void updateFacingNeighbors()
    {
        if (this.level == null) return;

        BlockPos pos = this.getBlockPos();
        BlockState state = this.getBlockState();
        Direction facing = state.getValue(ThermolithBlock.FACING);
        level.updateNeighborsAt(pos, state.getBlock());
        level.updateNeighborsAt(pos.relative(facing), this.level.getBlockState(pos.relative(facing)).getBlock());
    }
}
