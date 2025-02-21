package com.momosoftworks.coldsweat.common.block;

import com.momosoftworks.coldsweat.core.init.ItemInit;
import com.momosoftworks.coldsweat.util.registries.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class HearthTopBlock extends SmokestackBlock
{
    public static Properties getProperties()
    {
        return Properties
                .of(Material.STONE)
                .sound(SoundType.STONE)
                .strength(2, 10)
                .requiresCorrectToolForDrops();
    }

    public HearthTopBlock(Block.Properties properties)
    {   super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(UP, false).setValue(DOWN, false));
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
    {
        if (!world.isClientSide && world.getBlockState(pos.below()).getBlock() == ModBlocks.HEARTH_BOTTOM)
        {
            ModBlocks.HEARTH_BOTTOM.use(world.getBlockState(pos.below()), world, pos.below(), player, hand, rayTraceResult);
        }
        return ActionResultType.SUCCESS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving)
    {   super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        if (world.getBlockState(pos.below()).getBlock() != ModBlocks.HEARTH_BOTTOM)
        {   this.destroy(world, pos, state);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (state.getBlock() != newState.getBlock())
        {
            if (world.getBlockState(pos.below()).getBlock() == ModBlocks.HEARTH_BOTTOM)
            {   world.destroyBlock(pos.below(), false);
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    public ItemStack getCloneItemStack(IBlockReader getter, BlockPos pos, BlockState state)
    {   return new ItemStack(ItemInit.HEARTH.get());
    }
}
