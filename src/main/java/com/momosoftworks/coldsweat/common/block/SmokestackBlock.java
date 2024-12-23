package com.momosoftworks.coldsweat.common.block;

import com.momosoftworks.coldsweat.core.itemgroup.ColdSweatGroup;
import com.momosoftworks.coldsweat.data.tag.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

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
                .strength(2f)
                .explosionResistance(10f)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .dynamicShape();
    }

    public SmokestackBlock(Block.Properties properties)
    {   super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos)
    {   return true;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context)
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {   builder.add(FACING, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean connectAbove = level.getBlockState(pos.above()).is(ModBlockTags.EXTENDS_SMOKESTACK);
        boolean connectBelow = level.getBlockState(pos.below()).is(ModBlockTags.EXTENDS_SMOKESTACK);
        return this.defaultBlockState()
               .setValue(FACING, context.getHorizontalDirection().getOpposite())
               .setValue(UP, connectAbove).setValue(DOWN, connectBelow);
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState updateShape(BlockState state, Direction neighborDir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos)
    {
        level.scheduleTick(pos, this, 0);
        return super.updateShape(state, neighborDir, neighbor, level, pos, neighborPos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, Random random)
    {
        super.tick(state, level, pos, random);
        boolean connectAbove = level.getBlockState(pos.above()).is(ModBlockTags.EXTENDS_SMOKESTACK);
        boolean connectBelow = level.getBlockState(pos.below()).is(ModBlockTags.EXTENDS_SMOKESTACK);
        if (state.getValue(UP) != connectAbove || state.getValue(DOWN) != connectBelow)
        {   level.setBlock(pos, state.setValue(UP, connectAbove).setValue(DOWN, connectBelow), 3);
        }
    }
}
