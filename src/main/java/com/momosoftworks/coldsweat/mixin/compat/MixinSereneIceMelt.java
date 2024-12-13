package com.momosoftworks.coldsweat.mixin.compat;

import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sereneseasons.api.season.Season;
import sereneseasons.handler.season.RandomUpdateHandler;

@Mixin(RandomUpdateHandler.class)
public class MixinSereneIceMelt
{
    @Inject(method = "meltInChunk",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/IceBlock;melt(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"),
            cancellable = true,
            remap = false)
    private void getBiomeTemperatureOverride(ChunkManager chunkManager, Chunk chunkIn, Season.SubSeason subSeason, CallbackInfo ci)
    {
        if (ConfigSettings.USE_CUSTOM_WATER_FREEZE_BEHAVIOR.get())
        {   ci.cancel();
        }
    }
}
