package com.momosoftworks.coldsweat.mixin.compat;

import com.momosoftworks.coldsweat.config.ConfigSettings;
import glitchcore.event.TickEvent;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sereneseasons.season.RandomUpdateHandler;

@Mixin(RandomUpdateHandler.class)
public class MixinSereneIceMelt
{
    @Inject(method = "meltInChunk",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/IceBlock;melt(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"),
            cancellable = true,
            remap = false)
    private static void getBiomeTemperatureOverride(ChunkMap chunkManager, LevelChunk chunkIn, float meltChance, CallbackInfo ci)
    {
        if (ConfigSettings.USE_CUSTOM_WATER_FREEZE_BEHAVIOR.get())
        {   ci.cancel();
        }
    }

    @ModifyVariable(method = "onWorldTick",
                    at = @At(value = "STORE", ordinal = 0),
                    remap = false)
    private static int tickSpeedMeltRolls(int rolls, TickEvent.Level event)
    {
        if (!ConfigSettings.USE_CUSTOM_WATER_FREEZE_BEHAVIOR.get()) return rolls;

        if (event.getLevel() instanceof ServerLevel level)
        {
            int tickSpeed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
            return rolls * tickSpeed / 3;
        }
        return rolls;
    }
}
