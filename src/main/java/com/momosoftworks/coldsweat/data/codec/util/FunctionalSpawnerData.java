package com.momosoftworks.coldsweat.data.codec.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;

public class FunctionalSpawnerData extends MobSpawnSettings.SpawnerData
{
    private final SpawnFunction spawnFunction;

    public FunctionalSpawnerData(EntityType<?> entityType, int weight, int min, int max, SpawnFunction spawnFunction)
    {   super(entityType, weight, min, max);
        this.spawnFunction = spawnFunction;
    }

    public boolean canSpawn(ServerLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator, MobCategory category,
                            MobSpawnSettings.SpawnerData spawnerData, BlockPos pos)
    {   return spawnFunction == null || spawnFunction.canSpawn(level, structureManager, chunkGenerator, category, spawnerData, pos);
    }

    @FunctionalInterface
    public interface SpawnFunction
    {   boolean canSpawn(ServerLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator, MobCategory category,
                         MobSpawnSettings.SpawnerData spawnerData, BlockPos pos);
    }
}
