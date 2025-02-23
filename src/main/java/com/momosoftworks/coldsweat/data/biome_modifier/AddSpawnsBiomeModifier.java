package com.momosoftworks.coldsweat.data.biome_modifier;

import com.mojang.serialization.Codec;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.init.BiomeCodecInit;
import com.momosoftworks.coldsweat.data.codec.util.FunctionalSpawnerData;
import com.momosoftworks.coldsweat.data.codec.configuration.SpawnBiomeData;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;


public record AddSpawnsBiomeModifier(boolean useConfigs) implements BiomeModifier
{
    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder)
    {
        if (phase == Phase.ADD && useConfigs)
        {
            Collection<SpawnBiomeData> spawns = ConfigSettings.ENTITY_SPAWN_BIOMES.get(RegistryHelper.getRegistryAccess()).get(biome);
            for (SpawnBiomeData spawn : spawns)
            {
                RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ENTITY_TYPES, spawn.entities())
                .forEach(entityType ->
                {
                    FunctionalSpawnerData spawnerData = new FunctionalSpawnerData(entityType, spawn.weight(), spawn.count().min(), spawn.count().max(),
                                                                                 (level, structureManager, chunkGenerator, category, data, pos) ->
                                                                                 {
                                                                                     return spawn.location().test(level, pos)
                                                                                         && spawn.blockBelow().test(level, pos.below());
                                                                                 });
                    builder.getMobSpawnSettings().getSpawner(MobCategory.CREATURE).removeIf(oldData -> oldData.type == entityType);
                    builder.getMobSpawnSettings().addSpawn(MobCategory.CREATURE, spawnerData);
                });
            }
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec()
    {   return BiomeCodecInit.ADD_SPAWNS_CODEC.get();
    }
}
