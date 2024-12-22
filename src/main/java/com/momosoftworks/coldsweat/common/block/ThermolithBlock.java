package com.momosoftworks.coldsweat.common.block;

import com.mojang.math.Vector3f;
import com.momosoftworks.coldsweat.common.blockentity.ThermolithBlockEntity;
import com.momosoftworks.coldsweat.core.itemgroup.ColdSweatGroup;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModBlockEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.Random;

public class ThermolithBlock extends Block implements EntityBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final Map<Direction, VoxelShape> SHAPES = new HashMap<>();

    public ThermolithBlock(Properties properties)
    {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
        calculateFacingShapes(Shapes.or(
                Block.box(4, 0, 5, 12, 16, 16),
                Block.box(6, 0, 0, 10, 6, 5)));
    }

    public static Properties getProperties()
    {
        return Properties
                .of(Material.STONE)
                .sound(SoundType.GILDED_BLACKSTONE)
                .strength(2f)
                .explosionResistance(10f)
                .noOcclusion()
                .dynamicShape()
                .lightLevel(getLightValueLit(5))
                .isRedstoneConductor((state, level, pos) -> true)
                .requiresCorrectToolForDrops();
    }

    private static ToIntFunction<BlockState> getLightValueLit(int lightValue)
    {   return (state) -> state.getValue(BlockStateProperties.POWERED) ? lightValue : 0;
    }

    public static Item.Properties getItemProperties()
    {   return new Item.Properties().tab(ColdSweatGroup.COLD_SWEAT).stacksTo(64);
    }

    static void calculateFacingShapes(VoxelShape shape)
    {   for (Direction direction : Direction.values())
        {   SHAPES.put(direction, CSMath.rotateShape(direction, shape));
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context)
    {   return SHAPES.get(state.getValue(FACING));
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
    {   builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {   return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(POWERED, false);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {   return new ThermolithBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.THERMOLITH ? ThermolithBlockEntity::tick : null;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction)
    {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (direction == state.getValue(FACING).getOpposite() && blockEntity instanceof ThermolithBlockEntity thermolith)
        {
            return thermolith.getSignal();
        }
        return 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction)
    {   return state.getSignal(level, pos, direction);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction)
    {
        return direction == state.getValue(FACING).getOpposite();
    }

    @Override
    public boolean isSignalSource(BlockState pState)
    {   return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved)
    {
        super.onRemove(state, level, pos, newState, moved);
        ThermolithBlock.updateFacingNeighbors(level, state, pos);
    }

    public static void updateFacingNeighbors(Level level, BlockState state, BlockPos pos)
    {
        Direction facing = state.getValue(ThermolithBlock.FACING);
        level.updateNeighborsAt(pos, state.getBlock());
        level.updateNeighborsAt(pos.relative(facing), level.getBlockState(pos.relative(facing)).getBlock());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, Random random)
    {
        ParticleStatus status = Minecraft.getInstance().options.particles;
        if (state.getValue(POWERED) && status != ParticleStatus.MINIMAL)
        {
            Direction direction = state.getValue(ThermolithBlock.FACING);
            boolean xAxis = direction.getAxis() == Direction.Axis.X;
            float headLength = 0.25f;

            // The main body of the block is offset, so move the particles accordingly
            float offset = xAxis ? direction.getStepX() < 0 ? headLength : -0.05f : direction.getStepZ() < 0 ? headLength : -0.05f;
            double pY = Math.random() * 0.625 + 0.375;
            // nextInt ensures particles don't spawn inside the block
            double pX = xAxis ? random.nextInt(2) * 0.8 + offset: 0.5;
            double pZ = xAxis ? 0.5 : random.nextInt(2) * 0.8 + offset;
            level.addParticle(new DustParticleOptions(new Vector3f(Vec3.fromRGB24(4895036)), random.nextFloat() * 0.5f + 0.5f), pos.getX() + pX, pos.getY() + pY, pos.getZ() + pZ, 0, 0, 0);

            if (random.nextDouble() < 0.5)
            {
                float rX = xAxis ? (float) (Math.random()) * 0.8f + offset : 0.5f;
                float rZ = xAxis ? 0.5f : (float) (Math.random()) * 0.8f + offset;
                level.addParticle(new DustParticleOptions(new Vector3f(Vec3.fromRGB24(4895036)), random.nextFloat() * 0.5f + 0.5f), pos.getX() + rX, pos.getY() + 1.05, pos.getZ() + rZ, 0, 0, 0);
            }
        }
    }
}