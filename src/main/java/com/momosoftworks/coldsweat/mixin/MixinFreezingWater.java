package com.momosoftworks.coldsweat.mixin;

import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Biome.class, priority = 900)
public class MixinFreezingWater
{
    @Inject(method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z",
            at = @At(value = "HEAD"), cancellable = true)
    private void shouldFreezeBlock(LevelReader levelReader, BlockPos pos, boolean mustBeAtEdge, CallbackInfoReturnable<Boolean> cir)
    {
        if (!ConfigSettings.USE_CUSTOM_WATER_FREEZE_BEHAVIOR.get()) return;
        if (!(levelReader instanceof ServerLevel level)) return;

        if (level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING) == 0)
        {   cir.setReturnValue(false);
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (!(state.getFluidState().getType() == Fluids.WATER && state.getBlock() instanceof LiquidBlock))
        {   return;
        }

        if (ConfigSettings.COLD_SOUL_FIRE.get())
        {
            if (WorldHelper.nextToSoulFire(level, pos))
            {   cir.setReturnValue(true);
                return;
            }
        }

        if (WorldHelper.shouldFreeze(level, pos, mustBeAtEdge))
        {   cir.setReturnValue(true);
            return;
        }

        cir.setReturnValue(false);
    }

    @Mixin(IceBlock.class)
    public static abstract class IceMelt
    {
        @Shadow
        protected abstract void melt(BlockState pState, Level pLevel, BlockPos pPos);

        @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
        private void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource pRandom, CallbackInfo ci)
        {
            if (!ConfigSettings.USE_CUSTOM_WATER_FREEZE_BEHAVIOR.get()) return;

            if (WorldHelper.shouldMelt(level, pos, true)
            && !(ConfigSettings.COLD_SOUL_FIRE.get() && WorldHelper.nextToSoulFire(level, pos)))
            {   this.melt(state, level, pos);
                ci.cancel();
            }
        }
    }
}
