package com.momosoftworks.coldsweat.common.block;

import com.momosoftworks.coldsweat.common.blockentity.HearthBlockEntity;
import com.momosoftworks.coldsweat.core.init.BlockEntityInit;
import com.momosoftworks.coldsweat.core.itemgroup.ColdSweatGroup;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.AbstractFurnaceContainer;
import net.minecraft.item.*;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.*;

public class HearthBottomBlock extends Block
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty SIDE_POWERED = BooleanProperty.create("side_powered");
    public static final BooleanProperty BACK_POWERED = BooleanProperty.create("back_powered");

    public static Properties getProperties()
    {
        return Properties
                .of(Material.STONE)
                .sound(SoundType.STONE)
                .strength(2.0F, 10)
                .requiresCorrectToolForDrops()
                .isRedstoneConductor((state, level, pos) -> false);
    }

    public static Item.Properties getItemProperties()
    {   return new Item.Properties().tab(ColdSweatGroup.COLD_SWEAT).stacksTo(1);
    }

    public HearthBottomBlock(Block.Properties properties)
    {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH)
                                                          .setValue(SIDE_POWERED, false)
                                                          .setValue(BACK_POWERED, false));
    }

    @Nullable
    @Override
    public boolean hasTileEntity(BlockState state)
    {   return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    {   return BlockEntityInit.HEARTH_BLOCK_ENTITY_TYPE.get().create();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
    {
        TileEntity te = world.getBlockEntity(pos);
        if (te instanceof HearthBlockEntity)
        {
            HearthBlockEntity hearth = (HearthBlockEntity) te;
            ItemStack stack = player.getItemInHand(hand);

            // If the held item is a bucket, try to extract fluids
            if (player.getItemInHand(hand).getItem() == Items.BUCKET)
            {
                Vector3d clickedPos = rayTraceResult.getLocation();

                Vector3i lavaSideOffset = state.getValue(FACING).getClockWise().getNormal();
                Vector3d lavaSidePos = CSMath.getCenterPos(pos).add(lavaSideOffset.getX() * 0.65, lavaSideOffset.getY() * 0.65, lavaSideOffset.getZ() * 0.65);

                Vector3i waterSideOffset = state.getValue(FACING).getCounterClockWise().getNormal();
                Vector3d waterSidePos = CSMath.getCenterPos(pos).add(waterSideOffset.getX() * 0.65, waterSideOffset.getY() * 0.65, waterSideOffset.getZ() * 0.65);

                boolean isLava = clickedPos.distanceTo(lavaSidePos) < clickedPos.distanceTo(waterSidePos);
                Vector3d sidePos = isLava ? lavaSidePos : waterSidePos;
                BucketItem filledBucket = isLava ? ((BucketItem) Items.LAVA_BUCKET)
                                                 : ((BucketItem) Items.WATER_BUCKET);
                int itemFuel = Math.abs(hearth.getItemFuel(filledBucket.getDefaultInstance()));
                int hearthFuel = isLava ? hearth.getHotFuel() : hearth.getColdFuel();

                if (hearthFuel >= itemFuel * 0.99)
                {
                    if (rayTraceResult.getLocation().distanceTo(sidePos) < 0.4)
                    {
                        if (itemFuel > 0)
                        {
                            // Remove fuel
                            if (isLava) hearth.setHotFuelAndUpdate(hearthFuel - itemFuel);
                            else hearth.setColdFuelAndUpdate(hearthFuel - itemFuel);
                            // Give filled bucket item
                            stack.shrink(1);
                            player.addItem(filledBucket.getDefaultInstance());
                            // Play bucket sound
                            world.playSound(null, pos, filledBucket.getFluid().getAttributes().getFillSound(), SoundCategory.BLOCKS, 1.0F, 0.9f + new Random().nextFloat() * 0.2F);

                            return ActionResultType.SUCCESS;
                        }
                    }
                }
                // Open the GUI
                if (!world.isClientSide)
                    NetworkHooks.openGui((ServerPlayerEntity) player, hearth, pos);
            }
            else
            {
                // If the held item is fuel, try to insert the fuel
                int itemFuel = hearth.getItemFuel(stack);
                int hearthFuel = itemFuel > 0 ? hearth.getHotFuel() : hearth.getColdFuel();

                if (itemFuel != 0 && hearthFuel + Math.abs(itemFuel) * 0.75 < hearth.getMaxFuel())
                {
                    // Consume the item if not in creative
                    if (!player.isCreative())
                    {
                        if (stack.hasContainerItem())
                        {   ItemStack container = stack.getContainerItem();
                            player.setItemInHand(hand, container);
                        }
                        else
                        {   stack.shrink(1);
                        }
                    }
                    // Add the fuel
                    hearth.addFuel(itemFuel);

                    // Play the fuel filling sound
                    world.playSound(null, pos, itemFuel > 0
                                                 ? SoundEvents.BUCKET_EMPTY_LAVA
                                                 : SoundEvents.BUCKET_EMPTY,
                                    SoundCategory.BLOCKS, 1.0F, 0.9f + new Random().nextFloat() * 0.2F);
                }
                // Open the GUI
                else if (!world.isClientSide)
                {   NetworkHooks.openGui((ServerPlayerEntity) player, hearth, pos);
                }
            }
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public void onPlace(BlockState state, World world, BlockPos pos, BlockState lastState, boolean p_60570_)
    {
        if (world.getBlockState(pos.above()).isAir())
        {   world.setBlock(pos.above(), ModBlocks.HEARTH_TOP.defaultBlockState().setValue(HearthTopBlock.FACING, state.getValue(FACING)), 2);
        }
    }

    @Override
    public void neighborChanged(BlockState state, World level, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean isMoving)
    {
        super.neighborChanged(state, level, pos, neighborBlock, fromPos, isMoving);
        if (level.getBlockState(pos.above()).getBlock() != ModBlocks.HEARTH_TOP)
        {   this.destroy(level, pos, state);
        }
        else
        {   // Check for redstone power to this block
            HearthBlockEntity hearth = (HearthBlockEntity) level.getBlockEntity(pos);
            if (hearth != null)
            {   hearth.checkInputSignal();
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (state.getBlock() != newState.getBlock())
        {
            if (world.getBlockState(pos.above()).getBlock() == ModBlocks.HEARTH_TOP)
            {   world.destroyBlock(pos.above(), false);
            }

            TileEntity tileentity = world.getBlockEntity(pos);
            if (tileentity instanceof HearthBlockEntity)
            {   InventoryHelper.dropContents(world, pos, (HearthBlockEntity) tileentity);
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
    {   builder.add(FACING, SIDE_POWERED, BACK_POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context)
    {
        World level = context.getLevel();
        return level.getBlockState(context.getClickedPos().above()).isAir()
               ? this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
               : null;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, IBlockReader level, BlockPos pos, @Nullable Direction direction)
    {
        return direction != null
            && direction.getAxis() != Direction.Axis.Y
            && direction != state.getValue(FACING).getOpposite();
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState pState)
    {   return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState pState, World level, BlockPos pos)
    {   return AbstractFurnaceContainer.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}
