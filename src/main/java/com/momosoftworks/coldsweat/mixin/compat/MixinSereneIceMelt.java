package com.momosoftworks.coldsweat.mixin.compat;

import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import sereneseasons.api.season.Season;
import sereneseasons.handler.season.RandomUpdateHandler;

@Mixin(RandomUpdateHandler.class)
public class MixinSereneIceMelt
{
    @Inject(method = "meltInChunk",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/IceBlock;melt(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true,
            remap = false)
    private void getBiomeTemperatureOverride(ChunkMap chunkManager, LevelChunk chunkIn, Season.SubSeason subSeason, CallbackInfo ci,
                                             //locals
                                             ServerLevel level, ChunkPos chunkpos, int i, int j, int meltRand, BlockPos topAirPos)
    {
        if (!ConfigSettings.USE_CUSTOM_WATER_FREEZE_BEHAVIOR.get()) return;

        BlockPos groundPos = topAirPos.below();
        if (WorldHelper.getWorldTemperatureAt(level, groundPos) < 0.15F)
        {   ci.cancel();
        }
    }

    @ModifyVariable(method = "onWorldTick",
                    at = @At(value = "STORE", ordinal = 0),
                    remap = false)
    private int tickSpeedMeltRolls(int rolls, TickEvent.WorldTickEvent event)
    {
        if (!ConfigSettings.USE_CUSTOM_WATER_FREEZE_BEHAVIOR.get()) return rolls;

        if (event.world instanceof ServerLevel level)
        {
            int tickSpeed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
            return rolls * tickSpeed / 3;
        }
        return rolls;
    }
}
