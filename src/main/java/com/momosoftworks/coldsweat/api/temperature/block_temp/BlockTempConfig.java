package com.momosoftworks.coldsweat.api.temperature.block_temp;

import com.momosoftworks.coldsweat.data.codec.requirement.BlockRequirement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public abstract class BlockTempConfig extends BlockTemp
{
    private final List<BlockRequirement> predicates;

    public BlockTempConfig(double minEffect, double maxEffect, double minTemp, double maxTemp, double range, boolean fade,
                           List<BlockRequirement> predicates, Block... blocks)
    {
        super(minEffect, maxEffect, minTemp, maxTemp, range, fade, blocks);
        this.predicates = predicates;
    }

    @Override
    public boolean isValid(World level, BlockPos pos, BlockState state)
    {
        for (int i = 0; i < this.predicates.size(); i++)
        {
            if (!this.predicates.get(i).test(level, pos, state))
            {   return false;
            }
        }
        return true;
    }

    public boolean comparePredicates(BlockTempConfig other)
    {   return predicates.equals(other.predicates);
    }

    public List<BlockRequirement> getPredicates()
    {   return predicates;
    }
}
