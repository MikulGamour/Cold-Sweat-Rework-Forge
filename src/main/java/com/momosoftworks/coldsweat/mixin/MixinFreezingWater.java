package com.momosoftworks.coldsweat.mixin;

import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.GameRules;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fluids.IFluidBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(Biome.class)
public abstract class MixinFreezingWater
{
    private static IWorldReader LEVEL = null;
    private static Boolean IS_CHECKING_FREEZING = false;

    Biome self = (Biome) (Object) this;

    @Inject(method = "shouldFreeze(Lnet/minecraft/world/IWorldReader;Lnet/minecraft/util/math/BlockPos;Z)Z",
            at = @At(value = "HEAD"), cancellable = true)
    private void shouldFreezeBlock(IWorldReader levelReader, BlockPos pos, boolean mustBeAtEdge, CallbackInfoReturnable<Boolean> cir)
    {
        if (!ConfigSettings.USE_CUSTOM_WATER_FREEZE_BEHAVIOR.get()) return;
        if (levelReader instanceof ServerWorld && ((ServerWorld) levelReader).getGameRules().getInt(GameRules.RULE_RANDOMTICKING) == 0)
        {   cir.setReturnValue(false);
            return;
        }

        LEVEL = levelReader;
        IS_CHECKING_FREEZING = true;

        if (!ConfigSettings.COLD_SOUL_FIRE.get())
        {   return;
        }
        BlockState blockstate = levelReader.getBlockState(pos);
        FluidState fluidstate = levelReader.getFluidState(pos);
        if (!(fluidstate.getType() == Fluids.WATER && blockstate.getBlock() instanceof IFluidBlock))
        {   return;
        }
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int x = -1; x <= 1; x++)
        {
            for (int y = -1; y <= 1; y++)
            {
                for (int z = -1; z <= 1; z++)
                {
                    mutable.set(pos).move(x, y, z);
                    BlockState state = levelReader.getBlockState(mutable);
                    if (ConfigSettings.COLD_SOUL_FIRE.get() && (state.is(Blocks.SOUL_FIRE) || state.is(Blocks.SOUL_CAMPFIRE) && state.getValue(CampfireBlock.LIT)))
                    {   cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Inject(method = "getTemperature", at = @At("HEAD"), cancellable = true)
    private void getTemperature(BlockPos pos, CallbackInfoReturnable<Float> cir)
    {
        if (!ConfigSettings.USE_CUSTOM_WATER_FREEZE_BEHAVIOR.get()) return;

        DynamicRegistries registries = RegistryHelper.getDynamicRegistries();
        if (registries != null)
        {
            if (self.getBiomeCategory() == Biome.Category.OCEAN
            && pos.getY() <= LEVEL.getSeaLevel())
            {   return;
            }
        }

        if (IS_CHECKING_FREEZING && LEVEL instanceof World)
        {
            double biomeTemp = WorldHelper.getWorldTemperatureAt((World) LEVEL, pos);
            cir.setReturnValue((float) biomeTemp);
        }
        IS_CHECKING_FREEZING = false;
    }

    @Mixin(ServerWorld.class)
    public static abstract class FreezeTickSpeed
    {
        ServerWorld self = (ServerWorld) (Object) this;

        @Redirect(method = "tickChunk", at = @At(target = "Ljava/util/Random;nextInt(I)I", value = "INVOKE"),
                  slice = @Slice(from = @At(target = "Lnet/minecraft/profiler/IProfiler;popPush(Ljava/lang/String;)V", value = "INVOKE", ordinal = 0),
                                 to = @At(target = "Lnet/minecraft/profiler/IProfiler;popPush(Ljava/lang/String;)V", value = "INVOKE", ordinal = 1)))
        private int tickFreezeSpeed(Random instance, int bound)
        {
            if (!ConfigSettings.USE_CUSTOM_WATER_FREEZE_BEHAVIOR.get()) return instance.nextInt(bound);

            int tickSpeed = self.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
            if (tickSpeed == 0) return 1;
            return instance.nextInt(Math.max(1, bound / (tickSpeed / 3)));
        }
    }
}
