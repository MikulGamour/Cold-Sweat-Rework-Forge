package com.momosoftworks.coldsweat.common.block;

import com.momosoftworks.coldsweat.common.blockentity.ThermolithBlockEntity;
import com.momosoftworks.coldsweat.core.init.BlockEntityInit;
import com.momosoftworks.coldsweat.core.itemgroup.ColdSweatGroup;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.client.Minecraft;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.settings.ParticleStatus;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.ToIntFunction;

public class ThermolithBlock extends Block implements ITileEntityProvider
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final Map<Direction, VoxelShape> SHAPES = new HashMap<>();

    public ThermolithBlock(Properties properties)
    {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
        calculateFacingShapes(VoxelShapes.or(
                Block.box(4, 0, 5, 12, 16, 16),
                Block.box(6, 0, 0, 10, 6, 5)));
    }

    public static Properties getProperties()
    {
        return Properties
                .of(Material.STONE)
                .sound(SoundType.GILDED_BLACKSTONE)
                .strength(2f)
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
    public VoxelShape getShape(BlockState state, IBlockReader getter, BlockPos pos, ISelectionContext context)
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
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
    {   builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context)
    {   return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(POWERED, false);
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    {   return BlockEntityInit.THERMOLITH_BLOCK_ENTITY_TYPE.get().create();
    }

    @Nullable
    @Override
    public TileEntity newBlockEntity(IBlockReader world)
    {   return BlockEntityInit.THERMOLITH_BLOCK_ENTITY_TYPE.get().create();
    }

    @Override
    public int getSignal(BlockState state, IBlockReader level, BlockPos pos, Direction direction)
    {
        TileEntity blockEntity = level.getBlockEntity(pos);
        if (direction == state.getValue(FACING).getOpposite() && blockEntity instanceof ThermolithBlockEntity)
        {   return ((ThermolithBlockEntity) blockEntity).getSignal();
        }
        return 0;
    }

    @Override
    public int getDirectSignal(BlockState state, IBlockReader level, BlockPos pos, Direction direction)
    {   return state.getSignal(level, pos, direction);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, IBlockReader level, BlockPos pos, Direction direction)
    {   return direction == state.getValue(FACING).getOpposite();
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
    public void animateTick(BlockState state, World world, BlockPos pos, Random random)
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
            Vector3f particleColor = new Vector3f(Vector3d.fromRGB24(4895036));
            world.addParticle(new RedstoneParticleData(particleColor.x(), particleColor.y(), particleColor.z(), random.nextFloat() * 0.5f + 0.5f), pos.getX() + pX, pos.getY() + pY, pos.getZ() + pZ, 0, 0, 0);

            if (random.nextDouble() < 0.5)
            {
                float rX = xAxis ? (float) (Math.random()) * 0.8f + offset : 0.5f;
                float rZ = xAxis ? 0.5f : (float) (Math.random()) * 0.8f + offset;
                world.addParticle(new RedstoneParticleData(particleColor.x(), particleColor.y(), particleColor.z(), random.nextFloat() * 0.5f + 0.5f), pos.getX() + rX, pos.getY() + 1.05, pos.getZ() + rZ, 0, 0, 0);
            }
        }
    }
}