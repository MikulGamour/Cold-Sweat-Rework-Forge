package com.momosoftworks.coldsweat.mixin;

import com.momosoftworks.coldsweat.common.event.IceBreakingEvents;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.loot.ModLootTables;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

public class MixinIce
{
    @Mixin(IceBlock.class)
    public static class NoWaterOnBreak
    {
        @Inject(method = "playerDestroy",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"),
                cancellable = true)
        private void noWater(World pLevel, PlayerEntity pPlayer, BlockPos pPos, BlockState pState, TileEntity pTe, ItemStack pStack, CallbackInfo ci)
        {
            if (ConfigSettings.USE_CUSTOM_ICE_DROPS.get())
            {   ci.cancel();
            }
        }
    }

    @Mixin(AbstractBlock.class)
    public static class AddDrops
    {
        @Inject(method = "getDrops", at = @At("HEAD"), cancellable = true)
        private void addDrops(BlockState state, LootContext.Builder params, CallbackInfoReturnable<List<ItemStack>> cir)
        {
            ItemStack stack = params.getOptionalParameter(LootParameters.TOOL);
            if (stack == null) return;
            if (ConfigSettings.USE_CUSTOM_ICE_DROPS.get() && IceBreakingEvents.isModifiableIceBlock(state)
            && stack.isCorrectToolForDrops(state))
            {
                cir.setReturnValue(ModLootTables.getBlockDropsLootTable(params.getLevel(),
                                                                        new BlockPos(params.getParameter(LootParameters.ORIGIN)),
                                                                        state,
                                                                        CSMath.getIfNotNull(params.getOptionalParameter(LootParameters.THIS_ENTITY),
                                                                                            entity -> entity instanceof PlayerEntity ? (PlayerEntity) entity : null,
                                                                                            null),
                                                                        IceBreakingEvents.getLootTableForIce(state)));
            }
        }
    }
}
