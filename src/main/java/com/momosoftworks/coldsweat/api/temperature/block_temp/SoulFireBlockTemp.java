package com.momosoftworks.coldsweat.api.temperature.block_temp;

import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SoulFireBlockTemp extends BlockTemp
{
    public SoulFireBlockTemp()
    {   super(-0.9, 0.9, -3, Double.MAX_VALUE, 7, true, Blocks.SOUL_FIRE, Blocks.SOUL_CAMPFIRE);
    }

    @Override
    public double getTemperature(Level level, LivingEntity entity, BlockState state, BlockPos pos, double distance)
    {
        if (state.is(Blocks.SOUL_FIRE) || state.getValue(CampfireBlock.LIT))
        {
            int coldness = ConfigSettings.COLD_SOUL_FIRE.get() ? -1 : 1;
            return 0.476 * coldness;
        }
        return 0;
    }
}
