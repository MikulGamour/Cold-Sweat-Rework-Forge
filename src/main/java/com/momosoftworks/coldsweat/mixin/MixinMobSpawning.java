package com.momosoftworks.coldsweat.mixin;

import com.momosoftworks.coldsweat.data.codec.util.FunctionalSpawnerData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.class)
public class MixinMobSpawning
{
    @Inject(method = "canSpawnMobAt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/world/level/biome/MobSpawnSettings$SpawnerData;Lnet/minecraft/core/BlockPos;)Z",
            at = @At("RETURN"), cancellable = true)
    private static void checkFunctionalSpawnerData(ServerLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator, MobCategory category,
                                                   MobSpawnSettings.SpawnerData spawnerData, BlockPos pos, CallbackInfoReturnable<Boolean> cir)
    {
        boolean canSpawn = cir.getReturnValue();
        if (spawnerData instanceof FunctionalSpawnerData functional)
        {   canSpawn = canSpawn && functional.canSpawn(level, structureManager, chunkGenerator, category, spawnerData, pos);
        }
        cir.setReturnValue(canSpawn);
    }
}
