package com.momosoftworks.coldsweat.common.block;

import com.momosoftworks.coldsweat.common.blockentity.HearthBlockEntity;
import com.momosoftworks.coldsweat.common.blockentity.IceboxBlockEntity;
import com.momosoftworks.coldsweat.core.init.BlockEntityInit;
import com.momosoftworks.coldsweat.core.init.ParticleTypesInit;
import com.momosoftworks.coldsweat.core.itemgroup.ColdSweatGroup;
import com.momosoftworks.coldsweat.util.registries.ModBlocks;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.client.settings.ParticleStatus;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Random;

public class IceboxBlock extends Block
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FROSTED = BooleanProperty.create("frosted");
    public static final BooleanProperty SMOKESTACK = BooleanProperty.create("smokestack");

    public static final VoxelShape SHAPE = VoxelShapes.block();
    private static final VoxelShape SHAPE_OPEN = VoxelShapes.box(0f, 0f, 0f, 1f, 13/16f, 1f);

    public static Properties getProperties()
    {
        return Properties
                .of(Material.WOOD)
                .sound(SoundType.WOOD)
                .strength(2f, 5f)
                .isRedstoneConductor(IceboxBlock::conductsRedstone)
                .noOcclusion();
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

    public IceboxBlock(Block.Properties properties)
    {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(FROSTED, false).setValue(SMOKESTACK, false));
    }

    @Override
    public BlockRenderType getRenderShape(BlockState pState)
    {   return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader level, BlockPos pos, ISelectionContext selection)
    {
        TileEntity te = level.getBlockEntity(pos);
        if (te instanceof IceboxBlockEntity)
        {   return ((IceboxBlockEntity) te).getOpenNess(0) > 0 ? SHAPE_OPEN : SHAPE;
        }
        return SHAPE;
    }

    @Override
    public boolean triggerEvent(BlockState state, World level, BlockPos pos, int id, int param)
    {
        super.triggerEvent(state, level, pos, id, param);
        TileEntity blockentity = level.getBlockEntity(pos);

        return blockentity != null && blockentity.triggerEvent(id, param);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType use(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
    {
        if (level.getBlockEntity(pos) instanceof IceboxBlockEntity)
        {
            IceboxBlockEntity te = (IceboxBlockEntity) level.getBlockEntity(pos);
            ItemStack stack = player.getItemInHand(hand);
            // If the player is trying to put a smokestack on top, don't do anything
            if (stack.getItem() == ModItems.SMOKESTACK && rayTraceResult.getDirection() == Direction.UP
            && level.getBlockState(pos.above()).getBlock() instanceof AirBlock)
            {   return ActionResultType.FAIL;
            }
            int itemFuel = te.getItemFuel(stack);

            if (itemFuel != 0 && te.getFuel() + itemFuel * 0.75 < te.getMaxFuel())
            {
                if (!player.isCreative())
                {
                    if (stack.hasContainerItem())
                    {
                        ItemStack container = stack.getContainerItem();
                        stack.shrink(1);
                        player.inventory.add(container);
                    }
                    else
                    {   stack.shrink(1);
                    }
                }
                te.setFuel(te.getFuel() + itemFuel);

                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 0.9f + new Random().nextFloat() * 0.2F);
            }
            else if (!level.isClientSide && !ChestBlock.isChestBlockedAt(level, pos))
            {   NetworkHooks.openGui((ServerPlayerEntity) player, te, pos);
            }
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public boolean hasTileEntity(BlockState state)
    {   return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    {   return BlockEntityInit.ICEBOX_BLOCK_ENTITY_TYPE.get().create();
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, IWorld level, BlockPos pos, BlockPos neighborPos)
    {
        TileEntity te = level.getBlockEntity(pos);
        if (neighborPos.equals(pos.above()) && te instanceof IceboxBlockEntity)
        {
            IceboxBlockEntity icebox = ((IceboxBlockEntity) te);
            boolean hasSmokestack = icebox.hasSmokeStack();
            icebox.checkForSmokestack();
            if (hasSmokestack != icebox.hasSmokeStack())
            {
                state = state.setValue(SMOKESTACK, icebox.hasSmokeStack());
                level.setBlock(pos, state, 3);
                level.blockUpdated(pos, this);
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
            if (te instanceof IceboxBlockEntity)
            {   IceboxBlockEntity icebox = (IceboxBlockEntity) te;
                InventoryHelper.dropContents(world, pos, icebox);
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
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING, FROSTED, SMOKESTACK);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void animateTick(BlockState state, World level, BlockPos pos, Random rand)
    {
        ParticleStatus status = Minecraft.getInstance().options.particles;
        if (!state.getValue(FROSTED) || status == ParticleStatus.MINIMAL) return;

        double d0 = pos.getX() + 0.5;
        double d1 = pos.getY();
        double d2 = pos.getZ() + 0.5;
        boolean side = new Random().nextBoolean();
        double d5 = side ? Math.random() - 0.5 : (Math.random() < 0.5 ? 0.55 : -0.55);
        double d6 = Math.random() * 0.3;
        double d7 = !side ? Math.random() - 0.5 : (Math.random() < 0.5 ? 0.55 : -0.55);
        level.addParticle(ParticleTypesInit.GROUND_MIST.get(), d0 + d5, d1 + d6, d2 + d7, d5 / 40, 0.0D, d7 / 40);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, IBlockReader level, BlockPos pos, @Nullable Direction direction)
    {
        return direction != null
            && direction.getAxis() != Direction.Axis.Y
            && level.getBlockState(pos.above()).is(ModBlocks.SMOKESTACK);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state)
    {   return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, World level, BlockPos pos)
    {   return Container.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}
