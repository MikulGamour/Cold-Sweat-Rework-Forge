package com.momosoftworks.coldsweat.common.block;

import com.momosoftworks.coldsweat.core.init.ItemInit;
import com.momosoftworks.coldsweat.util.registries.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class HearthTopBlock extends Block
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

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
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, IBlockReader world, BlockPos pos)
    {
        return true;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context)
    {   return Block.box(4, 0, 4, 12, 16, 12);
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

    @Override
    public BlockState rotate(BlockState state, Rotation direction)
    {   return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirror)
    {   return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
    {   builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context)
    {   return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
}
