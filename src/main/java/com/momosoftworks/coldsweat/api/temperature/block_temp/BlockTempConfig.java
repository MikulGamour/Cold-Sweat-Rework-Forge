package com.momosoftworks.coldsweat.api.temperature.block_temp;

import com.momosoftworks.coldsweat.data.codec.requirement.BlockRequirement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

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
    public boolean isValid(Level level, BlockPos pos, BlockState state)
    {   return predicates.stream().allMatch(predicate -> predicate.test(level, pos));
    }

    public boolean comparePredicates(BlockTempConfig other)
    {   return predicates.equals(other.predicates);
    }

    public List<BlockRequirement> getPredicates()
    {   return predicates;
    }
}
