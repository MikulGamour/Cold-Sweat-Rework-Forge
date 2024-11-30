package com.momosoftworks.coldsweat.core.event;

import com.google.common.collect.ImmutableMap;
import com.momosoftworks.coldsweat.common.entity.ChameleonEntity;
import com.momosoftworks.coldsweat.common.entity.GoatEntity;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.SpawnBiomeData;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModEntities;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.MobSpawnInfo;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber
public class AddEntitySpawns
{
    private static final Field SPAWNERS = ObfuscationReflectionHelper.findField(MobSpawnInfo.class, "field_242554_e");
    static { SPAWNERS.setAccessible(true); }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBiomeLoading(FMLServerAboutToStartEvent event)
    {
        DynamicRegistries registryAccess = RegistryHelper.getDynamicRegistries();
        if (registryAccess == null) return;

        for (Biome biome : registryAccess.registryOrThrow(Registry.BIOME_REGISTRY))
        {
            // Get spawner map
            Map<EntityClassification, List<MobSpawnInfo.Spawners>> spawnerMap;
            try
            {   spawnerMap = new HashMap<>((Map<EntityClassification, List<MobSpawnInfo.Spawners>>) SPAWNERS.get(biome.getMobSettings()));
            }
            catch (IllegalAccessException e)
            {   return;
            }

            // Add spawns
            CSMath.doIfNotNull(ConfigSettings.ENTITY_SPAWN_BIOMES.get(registryAccess).get(biome), spawns ->
            {
                for (SpawnBiomeData spawnBiomeData : spawns)
                {
                    RegistryHelper.mapTaggableList(spawnBiomeData.entities())
                    .forEach(entityType ->
                    {
                        List<MobSpawnInfo.Spawners> spawners = new ArrayList<>(biome.getMobSettings().getMobs(spawnBiomeData.category()));
                        spawners.removeIf(spawnerData -> spawnerData.type == entityType);
                        spawners.add(new MobSpawnInfo.Spawners(entityType, spawnBiomeData.weight(), 1, 3));
                        spawnerMap.put(spawnBiomeData.category(), spawners);
                    });
                }
            });

            // Write spawner map
            try
            {   SPAWNERS.set(biome.getMobSettings(), ImmutableMap.copyOf(spawnerMap));
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
            EntitySpawnPlacementRegistry.register(ModEntities.GOAT, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, GoatEntity::canSpawn);
            EntitySpawnPlacementRegistry.register(ModEntities.CHAMELEON, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ChameleonEntity::checkMobSpawnRules);
        }
    }
}