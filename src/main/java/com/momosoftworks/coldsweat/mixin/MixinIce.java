package com.momosoftworks.coldsweat.mixin;

import com.momosoftworks.coldsweat.common.event.IceBreakingEvents;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.loot.ModLootTables;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
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
                at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"),
                cancellable = true)
        private void noWater(Level pLevel, Player pPlayer, BlockPos pPos, BlockState pState, BlockEntity pTe, ItemStack pStack, CallbackInfo ci)
        {
            if (ConfigSettings.USE_CUSTOM_ICE_DROPS.get())
            {   ci.cancel();
            }
        }
    }

    @Mixin(BlockBehaviour.class)
    public static class AddDrops
    {
        @Inject(method = "getDrops", at = @At("HEAD"), cancellable = true)
        private void addDrops(BlockState state, LootParams.Builder params, CallbackInfoReturnable<List<ItemStack>> cir)
        {
            ItemStack stack = params.getOptionalParameter(LootContextParams.TOOL);
            if (stack == null) return;
            if (ConfigSettings.USE_CUSTOM_ICE_DROPS.get() && IceBreakingEvents.isModifiableIceBlock(state)
            && stack.isCorrectToolForDrops(state))
            {
                cir.setReturnValue(ModLootTables.getBlockDropsLootTable(params.getLevel(),
                                                                        BlockPos.containing(params.getParameter(LootContextParams.ORIGIN)),
                                                                        state,
                                                                        CSMath.getIfNotNull(params.getOptionalParameter(LootContextParams.THIS_ENTITY),
                                                                                            entity -> entity instanceof Player ? (Player) entity : null,
                                                                                            null),
                                                                        IceBreakingEvents.getLootTableForIce(state)));
            }
        }
    }
}
