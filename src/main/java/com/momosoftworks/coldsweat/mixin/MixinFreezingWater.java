package com.momosoftworks.coldsweat.mixin;

import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public class MixinFreezingWater
{
    private static LevelReader LEVEL = null;
    private static Boolean IS_CHECKING_FREEZING = false;

    Biome self = (Biome) (Object) this;

    @Inject(method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z",
            at = @At(value = "HEAD"), cancellable = true)
    private void shouldFreezeBlock(LevelReader levelReader, BlockPos pos, boolean mustBeAtEdge, CallbackInfoReturnable<Boolean> cir)
    {
        if (!ConfigSettings.USE_CUSTOM_WATER_FREEZE_BEHAVIOR.get()) return;
        if (levelReader instanceof ServerLevel level && level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING) == 0)
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
        if (!(fluidstate.getType() == Fluids.WATER && blockstate.getBlock() instanceof LiquidBlock))
        {   return;
        }
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
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

        RegistryAccess registries = RegistryHelper.getRegistryAccess();
        if (registries != null)
        {
            if (RegistryHelper.getHolder(self, Registry.BIOME_REGISTRY, registries).map(holder -> holder.is(BiomeTags.IS_OCEAN)).orElse(false)
            && pos.getY() <= LEVEL.getSeaLevel())
            {   return;
            }
        }

        if (IS_CHECKING_FREEZING && LEVEL instanceof Level level)
        {
            double biomeTemp = WorldHelper.getWorldTemperatureAt(level, pos);
            cir.setReturnValue((float) biomeTemp);
        }
        IS_CHECKING_FREEZING = false;
    }

    @Mixin(value = ServerLevel.class, priority = 1001)
    public static abstract class FreezeTickSpeed
    {
        ServerLevel self = (ServerLevel) (Object) this;

        @Redirect(method = "tickChunk", at = @At(target = "Lnet/minecraft/util/RandomSource;nextInt(I)I", value = "INVOKE"),
                  slice = @Slice(from = @At(target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", value = "INVOKE", ordinal = 0),
                                 to = @At(target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", value = "INVOKE", ordinal = 1)))
        private int tickFreezeSpeed(RandomSource instance, int bound)
        {
            if (!ConfigSettings.USE_CUSTOM_WATER_FREEZE_BEHAVIOR.get()) return instance.nextInt(bound);

            int tickSpeed = self.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
            if (tickSpeed == 0) return 1;
            return instance.nextInt(Math.max(1, bound / (tickSpeed / 3)));
        }
    }
}
