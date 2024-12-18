package com.momosoftworks.coldsweat.common.block;

import com.momosoftworks.coldsweat.common.blockentity.HearthBlockEntity;
import com.momosoftworks.coldsweat.core.itemgroup.ColdSweatGroup;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModBlockEntities;
import com.momosoftworks.coldsweat.util.registries.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.*;

public class HearthBottomBlock extends Block implements EntityBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty SIDE_POWERED = BooleanProperty.create("side_powered");
    public static final BooleanProperty BACK_POWERED = BooleanProperty.create("back_powered");

    public static Properties getProperties()
    {
        return Properties
                .of(Material.STONE)
                .sound(SoundType.STONE)
                .destroyTime(2.0F)
                .explosionResistance(10.0F)
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
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {   return type == ModBlockEntities.HEARTH ? HearthBlockEntity::tickSelf : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {   return new HearthBlockEntity(pos, state);
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand hand, BlockHitResult rayTraceResult)
    {
        if (worldIn.getBlockEntity(pos) instanceof HearthBlockEntity te)
        {
            ItemStack stack = player.getItemInHand(hand);

            // If the held item is a bucket, try to extract fluids
            if (player.getItemInHand(hand).getItem() == Items.BUCKET)
            {
                Vec3 clickedPos = rayTraceResult.getLocation();

                Vec3i lavaSideOffset = state.getValue(FACING).getClockWise().getNormal();
                Vec3 lavaSidePos = CSMath.getCenterPos(pos).add(lavaSideOffset.getX() * 0.65, lavaSideOffset.getY() * 0.65, lavaSideOffset.getZ() * 0.65);

                Vec3i waterSideOffset = state.getValue(FACING).getCounterClockWise().getNormal();
                Vec3 waterSidePos = CSMath.getCenterPos(pos).add(waterSideOffset.getX() * 0.65, waterSideOffset.getY() * 0.65, waterSideOffset.getZ() * 0.65);

                boolean isLava = clickedPos.distanceTo(lavaSidePos) < clickedPos.distanceTo(waterSidePos);
                Vec3 sidePos = isLava ? lavaSidePos : waterSidePos;
                BucketItem filledBucket = isLava ? ((BucketItem) Items.LAVA_BUCKET)
                                                 : ((BucketItem) Items.WATER_BUCKET);
                int itemFuel = Math.abs(te.getItemFuel(filledBucket.getDefaultInstance()));
                int hearthFuel = isLava ? te.getHotFuel() : te.getColdFuel();

                if (hearthFuel >= itemFuel * 0.99)
                {
                    if (rayTraceResult.getLocation().distanceTo(sidePos) < 0.4)
                    {
                        if (itemFuel > 0)
                        {
                            // Remove fuel
                            if (isLava) te.setHotFuelAndUpdate(hearthFuel - itemFuel);
                            else te.setColdFuelAndUpdate(hearthFuel - itemFuel);
                            // Give filled bucket item
                            stack.shrink(1);
                            player.addItem(filledBucket.getDefaultInstance());
                            // Play bucket sound
                            worldIn.playSound(null, pos, filledBucket.getFluid().getPickupSound().get(), SoundSource.BLOCKS, 1.0F, 0.9f + new Random().nextFloat() * 0.2F);

                            return InteractionResult.SUCCESS;
                        }
                    }
                }
                // Open the GUI
                if (!worldIn.isClientSide)
                    NetworkHooks.openGui((ServerPlayer) player, te, pos);
            }
            else
            {
                // If the held item is fuel, try to insert the fuel
                int itemFuel = te.getItemFuel(stack);
                int hearthFuel = itemFuel > 0 ? te.getHotFuel() : te.getColdFuel();

                if (itemFuel != 0 && hearthFuel + Math.abs(itemFuel) * 0.75 < te.getMaxFuel())
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
                    te.addFuel(itemFuel);

                    // Play the fuel filling sound
                    worldIn.playSound(null, pos, itemFuel > 0
                                                 ? SoundEvents.BUCKET_EMPTY_LAVA
                                                 : SoundEvents.BUCKET_EMPTY,
                                      SoundSource.BLOCKS, 1.0F, 0.9f + new Random().nextFloat() * 0.2F);
                }
                // Open the GUI
                else if (!worldIn.isClientSide)
                {   NetworkHooks.openGui((ServerPlayer) player, te, pos);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState lastState, boolean p_60570_)
    {
        if (level.getBlockState(pos.above()).isAir())
        {   level.setBlock(pos.above(), ModBlocks.HEARTH_TOP.defaultBlockState().setValue(HearthTopBlock.FACING, state.getValue(FACING)), 2);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean isMoving)
    {
        super.neighborChanged(state, level, pos, neighborBlock, fromPos, isMoving);
        if (level.getBlockState(pos.above()).getBlock() != ModBlocks.HEARTH_TOP)
        {   level.destroyBlock(pos, false);
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
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (state.getBlock() != newState.getBlock() && !isMoving)
        {
            BlockEntity tileentity = level.getBlockEntity(pos);
            if (tileentity instanceof HearthBlockEntity)
            {   Containers.dropContents(level, pos, (HearthBlockEntity) tileentity);
                level.updateNeighborsAt(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation direction)
    {   return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {   builder.add(FACING, SIDE_POWERED, BACK_POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        Level level = context.getLevel();
        return level.getBlockState(context.getClickedPos().above()).isAir()
               ? this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
               : null;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction)
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
    public int getAnalogOutputSignal(BlockState pState, Level level, BlockPos pos)
    {   return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}
