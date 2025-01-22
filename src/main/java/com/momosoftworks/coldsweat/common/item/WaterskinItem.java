package com.momosoftworks.coldsweat.common.item;

import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import com.momosoftworks.coldsweat.util.registries.ModSounds;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class WaterskinItem extends Item
{
    public static final int FLUID_VALUE_MB = 250;

    public WaterskinItem()
    {
        super(new Properties().stacksTo(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();

        if (player == null)
        {   WorldHelper.dropItem(level, pos, getFilledItem(context.getItemInHand(), level, pos));
            return super.useOn(context);
        }

        // Drain water from cauldron
        if (player.getAbilities().mayBuild && state.getBlock() == Blocks.WATER_CAULDRON
        && state.getValue(BlockStateProperties.LEVEL_CAULDRON) > 0)
        {
            if (!player.isCreative())
            {   LayeredCauldronBlock.lowerFillLevel(state, level, pos);
            }
            WaterskinItem.handleFillWaterskin(player, context.getItemInHand(), context.getHand(), pos);
            WorldHelper.spawnParticleBatch(level, ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.65, pos.getZ() + 0.5, 0.5, 0.5, 0.5, 10, 0);

            return InteractionResult.SUCCESS;
        }
        // Drain fluid from IFluidHandler block
        else
        {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null)
            {
                blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, context.getClickedFace()).ifPresent(cap ->
                {
                    for (int i = 0; i < cap.getTanks(); i++)
                    {
                        FluidStack fluidStack = cap.getFluidInTank(i);
                        if (fluidStack.getFluid().is(FluidTags.WATER) && fluidStack.getAmount() >= FLUID_VALUE_MB)
                        {
                            FluidStack drainStack = fluidStack.copy();
                            drainStack.setAmount(FLUID_VALUE_MB);
                            cap.drain(drainStack, IFluidHandler.FluidAction.EXECUTE);
                            WaterskinItem.handleFillWaterskin(player, context.getItemInHand(), context.getHand(), pos);
                            return;
                        }
                    }
                });
            }
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        InteractionResultHolder<ItemStack> ar = super.use(level, player, hand);
        ItemStack itemstack = ar.getObject();

        BlockHitResult blockhitresult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        BlockPos hitPos = blockhitresult.getBlockPos();
        BlockState lookingAt = level.getBlockState(hitPos);

        if (blockhitresult.getType() != HitResult.Type.BLOCK)
        {   return InteractionResultHolder.pass(itemstack);
        }
        else
        {
            if (lookingAt.getFluidState().isSource() && lookingAt.getFluidState().getType().isSame(Fluids.WATER))
            {
                WaterskinItem.handleFillWaterskin(player, itemstack, hand, hitPos);
                WorldHelper.spawnParticleBatch(level, ParticleTypes.SPLASH, hitPos.getX() + 0.5, hitPos.getY() + 1, hitPos.getZ() + 0.5, 0.5, 0.5, 0.5, 10, 0);
            }
            return ar;
        }
    }

    public static ItemStack getFilledItem(ItemStack stack, Level level, BlockPos pos)
    {
        ItemStack filledWaterskin = ModItems.FILLED_WATERSKIN.getDefaultInstance();
        // copy NBT to new item
        filledWaterskin.setTag(stack.getTag());
        // Set temperature based on temperature of the biome
        filledWaterskin.getOrCreateTag().putDouble(FilledWaterskinItem.NBT_TEMPERATURE,
                                                   CSMath.clamp((WorldHelper.getTemperatureAt(level, pos)
                                                           - (CSMath.average(ConfigSettings.MAX_TEMP.get(), ConfigSettings.MIN_TEMP.get()))) * 15, -50, 50));
        // Set purity of water based on water source, if Thirst Was Taken is loaded
        if (CompatManager.isThirstLoaded())
        {   filledWaterskin = CompatManager.Thirst.setWaterPurity(filledWaterskin, pos, level);
        }
        return filledWaterskin;
    }

    public static void handleFillWaterskin(Player player, ItemStack thisStack, InteractionHand usedHand, BlockPos filledAtPos)
    {
        Level level = player.level();
        ItemStack filledWaterskin = getFilledItem(thisStack, level, filledAtPos);

        //Replace 1 of the stack with a FilledWaterskinItem
        if (thisStack.getCount() > 1 || player.getAbilities().instabuild)
        {
            if (!player.addItem(filledWaterskin))
            {
                ItemEntity itementity = player.drop(filledWaterskin, false);
                if (itementity != null)
                {   itementity.setNoPickUpDelay();
                    itementity.setThrower(player.getUUID());
                }
            }
            thisStack.shrink(1);
        }
        else
        {   player.setItemInHand(usedHand, filledWaterskin);
        }
        player.swing(usedHand);
        player.getCooldowns().addCooldown(ModItems.FILLED_WATERSKIN, 10);
        player.getCooldowns().addCooldown(ModItems.WATERSKIN, 10);
        player.awardStat(Stats.ITEM_USED.get(thisStack.getItem()));
        level.playSound(null, filledAtPos, ModSounds.WATERSKIN_FILL, SoundSource.PLAYERS, 2f, (float) Math.random() / 5 + 0.9f);
    }

    @Override
    public boolean canAttackBlock(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer)
    {   return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {   return slotChanged;
    }
}
