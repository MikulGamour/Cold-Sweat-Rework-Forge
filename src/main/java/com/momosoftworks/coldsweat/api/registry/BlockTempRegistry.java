package com.momosoftworks.coldsweat.api.registry;

import com.google.common.collect.Multimap;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTemp;
import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTempConfig;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.stream.Collectors;

public class BlockTempRegistry
{
    public static final List<BlockTemp> BLOCK_TEMPS = new ArrayList<>();
    public static final FastMultiMap<Block, BlockTemp> MAPPED_BLOCKS = new FastMultiMap<>();
    public static final BlockTemp DEFAULT_BLOCK_TEMP = new BlockTemp()
    {
        @Override
        public double getTemperature(World world, LivingEntity entity, BlockState state, BlockPos pos, double distance)
        {   return 0;
        }
    };

    public static synchronized void register(BlockTemp blockTemp)
    {   register(blockTemp, false);
    }

    public static synchronized void registerFirst(BlockTemp blockTemp)
    {   register(blockTemp, true);
    }

    private static synchronized void register(BlockTemp blockTemp, boolean front)
    {
        blockTemp.getAffectedBlocks().forEach(block ->
        {
            LinkedHashSet<BlockTemp> blockTemps = MAPPED_BLOCKS.get(block);
            if (!blockTemps.isEmpty())
            {
                if (blockTemp instanceof BlockTempConfig)
                {
                    BlockTempConfig cfg = (BlockTempConfig) blockTemp;
                    for (BlockTemp temp : blockTemps)
                    {
                        if (temp instanceof BlockTempConfig)
                        {
                            BlockTempConfig cfg2 = (BlockTempConfig) temp;
                            if (cfg2.comparePredicates(cfg))
                            {
                                ColdSweat.LOGGER.error("Skipping duplicate BlockTemp for \"{}\" as it already has one with the same predicates: \n{}",
                                                       block.getName().getString(), cfg2.getPredicates());
                                return;
                            }
                        }
                    }
                }
            }
            if (front)
            {
                List<BlockTemp> blockTempList = new ArrayList<>(blockTemps);
                blockTempList.add(0, blockTemp);
                blockTemps.clear();
                blockTemps.addAll(blockTempList);
            }
            else blockTemps.add(blockTemp);
        });
        if (front) BLOCK_TEMPS.add(0, blockTemp);
        else BLOCK_TEMPS.add(blockTemp);
    }

    public static synchronized void flush()
    {
        MAPPED_BLOCKS.clear();
        BLOCK_TEMPS.clear();
    }

    public static Collection<BlockTemp> getBlockTempsFor(BlockState blockstate)
    {
        if (blockstate.isAir()) return Arrays.asList(DEFAULT_BLOCK_TEMP);

        Block block = blockstate.getBlock();
        Collection<BlockTemp> blockTemps = MAPPED_BLOCKS.get(block);
        if (blockTemps.isEmpty())
        {
            blockTemps = new ArrayList<>(BLOCK_TEMPS.stream().filter(bt -> bt.hasBlock(block)).collect(Collectors.toList()));
            // If this block has no associated BlockTemps, give default implementation
            if (blockTemps.isEmpty())
            {   blockTemps.add(DEFAULT_BLOCK_TEMP);
            }
            MAPPED_BLOCKS.putAll(block, blockTemps);
            return blockTemps;
        }
        return blockTemps;
    }
}
