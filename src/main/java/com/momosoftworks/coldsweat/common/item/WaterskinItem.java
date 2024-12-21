package com.momosoftworks.coldsweat.common.item;

import com.momosoftworks.coldsweat.core.itemgroup.ColdSweatGroup;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import com.momosoftworks.coldsweat.util.registries.ModSounds;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class WaterskinItem extends Item
{
    public WaterskinItem()
    {
        super(new Properties().tab(ColdSweatGroup.COLD_SWEAT).stacksTo(16));
    }

    @Override
    public ActionResultType useOn(ItemUseContext context)
    {
        BlockPos pos = context.getClickedPos();
        World level = context.getLevel();
        BlockState state = level.getBlockState(pos);
        PlayerEntity player = context.getPlayer();

        if (player == null)
        {   WorldHelper.dropItem(level, pos, getFilledItem(context.getItemInHand(), level, pos));
            return super.useOn(context);
        }

        if (player.abilities.mayBuild && state.getBlock() == Blocks.CAULDRON
        && state.getValue(BlockStateProperties.LEVEL_CAULDRON) > 0)
        {
            if (!player.isCreative())
            {   int newLevel = state.getValue(BlockStateProperties.LEVEL_CAULDRON) - 1;
                level.setBlockAndUpdate(pos, newLevel == 0 ? Blocks.CAULDRON.defaultBlockState() : state.setValue(BlockStateProperties.LEVEL_CAULDRON, newLevel));
            }
            WaterskinItem.setFilled(player, context.getItemInHand(), context.getHand(), pos);
            WorldHelper.spawnParticleBatch(level, ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.65, pos.getZ() + 0.5, 0.5, 0.5, 0.5, 10, 0);
            level.playSound(null, pos, ModSounds.WATERSKIN_FILL, SoundCategory.PLAYERS, 2f, (float) Math.random() / 5 + 0.9f);

            return ActionResultType.SUCCESS;
        }
        return super.useOn(context);
    }

    @Override
    public ActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand)
    {
        ActionResult<ItemStack> ar = super.use(level, player, hand);
        ItemStack itemstack = ar.getObject();

        BlockRayTraceResult blockhitresult = getPlayerPOVHitResult(level, player, RayTraceContext.FluidMode.SOURCE_ONLY);
        BlockPos hitPos = blockhitresult.getBlockPos();
        BlockState lookingAt = level.getBlockState(hitPos);

        if (blockhitresult.getType() != RayTraceResult.Type.BLOCK)
        {   return ActionResult.pass(itemstack);
        }
        else
        {
            if (lookingAt.getFluidState().isSource() && lookingAt.getFluidState().getType().isSame(Fluids.WATER))
            {
                WaterskinItem.setFilled(player, itemstack, hand, hitPos);
                level.playSound(null, hitPos, ModSounds.WATERSKIN_FILL, SoundCategory.PLAYERS, 2f, (float) Math.random() / 5 + 0.9f);
                WorldHelper.spawnParticleBatch(level, ParticleTypes.SPLASH, hitPos.getX() + 0.5, hitPos.getY() + 1, hitPos.getZ() + 0.5, 0.5, 0.5, 0.5, 10, 0);
            }
            return ar;
        }
    }

    public static ItemStack getFilledItem(ItemStack stack, World level, BlockPos pos)
    {
        ItemStack filledWaterskin = ModItems.FILLED_WATERSKIN.getDefaultInstance();
        // copy NBT to new item
        filledWaterskin.setTag(stack.getTag());
        // Set temperature based on temperature of the biome
        filledWaterskin.getOrCreateTag().putDouble(FilledWaterskinItem.NBT_TEMPERATURE,
                                                   CSMath.clamp((WorldHelper.getTemperatureAt(level, pos)
                                                           - (CSMath.average(ConfigSettings.MAX_TEMP.get(), ConfigSettings.MIN_TEMP.get()))) * 15, -50, 50));
        return filledWaterskin;
    }

    public static void setFilled(PlayerEntity player, ItemStack thisStack, Hand usedHand, BlockPos filledAtPos)
    {
        World level = player.level;
        ItemStack filledWaterskin = getFilledItem(thisStack, level, filledAtPos);

        //Replace 1 of the stack with a FilledWaterskinItem
        if (thisStack.getCount() > 1)
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
        //Play filling sound
        level.playSound(null, player, SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundCategory.PLAYERS, 1, (float) Math.random() / 5 + 0.9f);
        player.swing(usedHand);
        player.getCooldowns().addCooldown(ModItems.FILLED_WATERSKIN, 10);
        player.getCooldowns().addCooldown(ModItems.WATERSKIN, 10);
        player.awardStat(Stats.ITEM_USED.get(thisStack.getItem()));
    }

    @Override
    public boolean canAttackBlock(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity pPlayer)
    {   return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {   return slotChanged;
    }
}
