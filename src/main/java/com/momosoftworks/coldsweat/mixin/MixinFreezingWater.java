package com.momosoftworks.coldsweat.mixin;

import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.IceBlock;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fluids.IFluidBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(value = Biome.class, priority = 900)
public abstract class MixinFreezingWater
{
    @Inject(method = "shouldFreeze(Lnet/minecraft/world/IWorldReader;Lnet/minecraft/util/math/BlockPos;Z)Z",
            at = @At(value = "HEAD"), cancellable = true)
    private void shouldFreezeBlock(IWorldReader levelReader, BlockPos pos, boolean mustBeAtEdge, CallbackInfoReturnable<Boolean> cir)
    {
        if (!ConfigSettings.USE_CUSTOM_WATER_FREEZE_BEHAVIOR.get()) return;
        if (!(levelReader instanceof ServerWorld)) return;
        ServerWorld level = (ServerWorld) levelReader;

        if (level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING) == 0)
        {   cir.setReturnValue(false);
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (!(state.getFluidState().getType() == Fluids.WATER && state.getBlock() instanceof IFluidBlock))
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

    @Mixin(value = ServerWorld.class)
    public static abstract class FreezeTickSpeed
    {
        ServerWorld self = (ServerWorld) (Object) this;

        @ModifyArg(method = "tickChunk", at = @At(target = "Ljava/util/Random;nextInt(I)I", value = "INVOKE"),
                   slice = @Slice(from = @At(target = "Lnet/minecraft/profiler/IProfiler;popPush(Ljava/lang/String;)V", value = "INVOKE", ordinal = 0),
                                  to = @At(target = "Lnet/minecraft/profiler/IProfiler;popPush(Ljava/lang/String;)V", value = "INVOKE", ordinal = 1)),
                   require = 0)
        private int tickFreezeSpeed(int bound)
        {
            if (!ConfigSettings.USE_CUSTOM_WATER_FREEZE_BEHAVIOR.get()) return bound;

            int tickSpeed = self.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
            if (tickSpeed == 0) return 999999;
            return Math.max(1, bound / Math.max(1, tickSpeed / 3));
        }
    }

    @Mixin(IceBlock.class)
    public static abstract class IceMelt
    {
        @Shadow
        protected abstract void melt(BlockState pState, World pLevel, BlockPos pPos);

        @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
        private void randomTick(BlockState state, ServerWorld level, BlockPos pos, Random random, CallbackInfo ci)
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
