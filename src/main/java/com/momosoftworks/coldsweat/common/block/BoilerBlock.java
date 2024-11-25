package com.momosoftworks.coldsweat.common.block;

import com.momosoftworks.coldsweat.common.blockentity.BoilerBlockEntity;
import com.momosoftworks.coldsweat.core.init.BlockEntityInit;
import com.momosoftworks.coldsweat.common.blockentity.HearthBlockEntity;
import com.momosoftworks.coldsweat.core.itemgroup.ColdSweatGroup;
import com.momosoftworks.coldsweat.util.registries.ModBlocks;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.settings.ParticleStatus;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.function.ToIntFunction;

public class BoilerBlock extends Block
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public static Properties getProperties()
    {
        return Properties
                .of(Material.STONE)
                .sound(SoundType.STONE)
                .strength(2, 10)
                .lightLevel(getLightValueLit(13))
                .isRedstoneConductor(BoilerBlock::conductsRedstone)
                .requiresCorrectToolForDrops();
    }

    public static Item.Properties getItemProperties()
    {
        return new Item.Properties().tab(ColdSweatGroup.COLD_SWEAT);
    }

    private static boolean conductsRedstone(BlockState state, IBlockReader level, BlockPos pos)
    {
        TileEntity be = level.getBlockEntity(pos);
        if (be instanceof HearthBlockEntity)
        {   return !((HearthBlockEntity) be).hasSmokeStack();
        }
        return false;
    }

    private static ToIntFunction<BlockState> getLightValueLit(int lightValue)
    {
        return (state) -> state.getValue(BlockStateProperties.LIT) ? lightValue : 0;
    }

    public BoilerBlock(Block.Properties properties)
    {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType use(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
    {
        ItemStack stack = player.getItemInHand(hand);
        // If the player is trying to put a smokestack on top, don't do anything
        if (stack.getItem() == ModItems.SMOKESTACK && rayTraceResult.getDirection() == Direction.UP
        && level.getBlockState(pos.above()).isAir())
        {   return ActionResultType.FAIL;
        }
        if (!level.isClientSide)
        {
            TileEntity te = level.getBlockEntity(pos);
            if (te instanceof BoilerBlockEntity)
            {
                BoilerBlockEntity blockEntity = (BoilerBlockEntity) te;
                int itemFuel = blockEntity.getItemFuel(stack);

                if (itemFuel != 0 && blockEntity.getFuel() + itemFuel * 0.75 < blockEntity.getMaxFuel())
                {
                    if (!player.isCreative())
                    {
                        if (stack.hasContainerItem())
                        {   ItemStack container = stack.getContainerItem();
                            stack.shrink(1);
                            player.inventory.add(container);
                        }
                        else
                        {   stack.shrink(1);
                        }
                    }
                    blockEntity.setFuel(blockEntity.getFuel() + itemFuel);

                    level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundCategory.BLOCKS, 1.0F, 0.9f + new Random().nextFloat() * 0.2F);
                }
                else
                {   NetworkHooks.openGui((ServerPlayerEntity) player, blockEntity, pos);
                }
            }
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, IWorld level, BlockPos pos, BlockPos neighborPos)
    {
        TileEntity be = level.getBlockEntity(pos);
        if (neighborPos.equals(pos.above()) && be instanceof BoilerBlockEntity)
        {
            BoilerBlockEntity boiler = (BoilerBlockEntity) be;
            boolean hasSmokestack = boiler.hasSmokeStack();
            boiler.checkForSmokestack();
            if (hasSmokestack != boiler.hasSmokeStack())
            {   level.blockUpdated(pos, this);
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void neighborChanged(BlockState state, World level, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean isMoving)
    {
        super.neighborChanged(state, level, pos, neighborBlock, fromPos, isMoving);
        // Check for redstone power to this block
        HearthBlockEntity hearth = (HearthBlockEntity) level.getBlockEntity(pos);
        if (hearth != null)
        {   hearth.checkInputSignal();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (state.getBlock() != newState.getBlock())
        {
            TileEntity te = world.getBlockEntity(pos);
            if (te instanceof BoilerBlockEntity)
            {
                BoilerBlockEntity boiler = (BoilerBlockEntity) te;
                InventoryHelper.dropContents(world, pos, boiler);
                world.updateNeighborsAt(pos, this);
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation direction)
    {   return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
    {   builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context)
    {   return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(LIT, false);
    }

    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState stateIn, World world, BlockPos pos, Random rand)
    {
        ParticleStatus status = Minecraft.getInstance().options.particles;
        if (stateIn.getValue(LIT) && status != ParticleStatus.MINIMAL)
        {
            double d0 = pos.getX() + 0.5D;
            double d1 = pos.getY();
            double d2 = pos.getZ() + 0.5D;
            Direction direction = stateIn.getValue(FACING);
            Direction.Axis direction$axis = direction.getAxis();

            double d4 = rand.nextDouble() * 0.6D - 0.3D;
            double d5 = direction$axis == Direction.Axis.X ? (double)direction.getStepX() * 0.52D : d4;
            double d6 = rand.nextDouble() * 3.0D / 16.0D + 3 / 16.0;
            double d7 = direction$axis == Direction.Axis.Z ? (double)direction.getStepZ() * 0.52D : d4;
            world.addParticle(ParticleTypes.SMOKE, d0 + d5, d1 + d6, d2 + d7, 0.0D, 0.0D, 0.0D);
            world.addParticle(ParticleTypes.FLAME, d0 + d5, d1 + d6, d2 + d7, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public boolean hasTileEntity(BlockState state)
    {   return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    {   return BlockEntityInit.BOILER_BLOCK_ENTITY_TYPE.get().create();
    }

    @Override
    public boolean canConnectRedstone(BlockState state, IBlockReader level, BlockPos pos, @Nullable Direction direction)
    {
        return direction != null
            && direction.getAxis() != Direction.Axis.Y
            && direction != state.getValue(FACING).getOpposite()
            && level.getBlockState(pos.above()).is(ModBlocks.SMOKESTACK);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState pState)
    {   return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState pState, World level, BlockPos pos)
    {   return Container.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}
