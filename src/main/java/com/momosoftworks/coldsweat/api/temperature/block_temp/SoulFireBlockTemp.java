package com.momosoftworks.coldsweat.api.temperature.block_temp;

import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SoulFireBlockTemp extends BlockTemp
{
    public SoulFireBlockTemp()
    {   super(-0.9, 0.9, -3, Double.MAX_VALUE, 7, true, Blocks.SOUL_FIRE, Blocks.SOUL_CAMPFIRE);
    }

    @Override
    public double getTemperature(World level, LivingEntity entity, BlockState state, BlockPos pos, double distance)
    {
        if (state.is(Blocks.SOUL_FIRE) || state.getValue(CampfireBlock.LIT))
        {
            int coldness = ConfigSettings.COLD_SOUL_FIRE.get() ? -1 : 1;
            return 0.476 * coldness;
        }
        return 0;
    }
}
