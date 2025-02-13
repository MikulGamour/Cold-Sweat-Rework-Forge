package com.momosoftworks.coldsweat.core.event;

import com.google.common.collect.ImmutableMap;
import com.momosoftworks.coldsweat.common.entity.Chameleon;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.SpawnBiomeData;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModEntities;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber
public class AddEntitySpawns
{
    private static final Field SPAWNERS = ObfuscationReflectionHelper.findField(MobSpawnSettings.class, "f_48329_");
    static { SPAWNERS.setAccessible(true); }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBiomeLoading(ServerAboutToStartEvent event)
    {
        RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();
        if (registryAccess == null) return;

        for (Holder<Biome> biome : registryAccess.registryOrThrow(Registry.BIOME_REGISTRY).holders().toList())
        {
            // Get spawner map
            Map<MobCategory, WeightedRandomList<MobSpawnSettings.SpawnerData>> spawnerMap;
            try
            {   spawnerMap = new HashMap<>((Map<MobCategory, WeightedRandomList<MobSpawnSettings.SpawnerData>>) SPAWNERS.get(biome.value().getMobSettings()));
            }
            catch (IllegalAccessException e)
            {   return;
            }

            // Add spawns
            CSMath.doIfNotNull(ConfigSettings.ENTITY_SPAWN_BIOMES.get(registryAccess).get(biome), spawns ->
            {
                for (SpawnBiomeData spawnBiomeData : spawns)
                {
                    RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ENTITIES, spawnBiomeData.entities())
                    .forEach(entityType ->
                    {
                        List<MobSpawnSettings.SpawnerData> spawners = new ArrayList<>(biome.value().getMobSettings().getMobs(spawnBiomeData.category()).unwrap());
                        spawners.removeIf(spawnerData -> spawnerData.type == entityType);
                        spawners.add(new MobSpawnSettings.SpawnerData(entityType, spawnBiomeData.weight(), 1, 3));
                        spawnerMap.put(spawnBiomeData.category(), WeightedRandomList.create(spawners));
                    });
                }
            });

            // Write spawner map
            try
            {   SPAWNERS.set(biome.value().getMobSettings(), ImmutableMap.copyOf(spawnerMap));
            }
            catch (IllegalAccessException e)
            {   e.printStackTrace();
            }
        }
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegisterSpawnPlacements
    {
        @SubscribeEvent
        public static void registerSpawnPlacements(FMLCommonSetupEvent event)
        {
            SpawnPlacements.register(ModEntities.CHAMELEON, SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Chameleon::checkMobSpawnRules);
        }
    }
}