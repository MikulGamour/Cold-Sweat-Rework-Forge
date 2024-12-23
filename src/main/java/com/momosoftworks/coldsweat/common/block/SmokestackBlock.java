package com.momosoftworks.coldsweat.common.block;

import com.momosoftworks.coldsweat.core.itemgroup.ColdSweatGroup;
import com.momosoftworks.coldsweat.data.tag.ModBlockTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.Random;

public class SmokestackBlock extends Block
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public static Properties getProperties()
    {
        return Properties
                .of(Material.STONE)
                .sound(SoundType.STONE)
                .strength(2f, 10f)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .dynamicShape();
    }

    public SmokestackBlock(Properties properties)
    {   super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, IBlockReader level, BlockPos pos)
    {   return true;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader getter, BlockPos pos, ISelectionContext context)
    {   return Block.box(4, 0, 4, 12, 16, 12);
    }

    public static Item.Properties getItemProperties()
    {   return new Item.Properties().tab(ColdSweatGroup.COLD_SWEAT);
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
    {   builder.add(FACING, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context)
    {
        World level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean connectAbove = level.getBlockState(pos.above()).is(ModBlockTags.EXTENDS_SMOKESTACK);
        boolean connectBelow = level.getBlockState(pos.below()).is(ModBlockTags.EXTENDS_SMOKESTACK);
        return this.defaultBlockState()
               .setValue(FACING, context.getHorizontalDirection().getOpposite())
               .setValue(UP, connectAbove).setValue(DOWN, connectBelow);
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState updateShape(BlockState state, Direction neighborDir, BlockState neighbor, IWorld level, BlockPos pos, BlockPos neighborPos)
    {
        level.getBlockTicks().scheduleTick(pos, this, 0);
        return super.updateShape(state, neighborDir, neighbor, level, pos, neighborPos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void tick(BlockState state, ServerWorld level, BlockPos pos, Random random)
    {
        super.tick(state, level, pos, random);
        boolean connectAbove = level.getBlockState(pos.above()).is(ModBlockTags.EXTENDS_SMOKESTACK);
        boolean connectBelow = level.getBlockState(pos.below()).is(ModBlockTags.EXTENDS_SMOKESTACK);
        if (state.getValue(UP) != connectAbove || state.getValue(DOWN) != connectBelow)
        {   level.setBlock(pos, state.setValue(UP, connectAbove).setValue(DOWN, connectBelow), 3);
        }
    }
}
