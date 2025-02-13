package com.momosoftworks.coldsweat.data;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.data.codec.configuration.*;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.util.math.FastMap;
import net.minecraft.resources.FallbackResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.packs.ModFileResourcePack;

import java.util.*;


public class ModRegistries
{
    private static final FallbackResourceManager RESOURCE_MANAGER = new FallbackResourceManager(ResourcePackType.SERVER_DATA, ColdSweat.MOD_ID);
    private static final Map<String, RegistryHolder<?>> REGISTRIES = new FastMap<>();

    public static IResourceManager getResourceManager()
    {   return RESOURCE_MANAGER;
    }

    static
    {   RESOURCE_MANAGER.add(new ModFileResourcePack(ModList.get().getModFileById(ColdSweat.MOD_ID).getFile()));
    }

    // Item Registries
    public static final RegistryKey<Registry<InsulatorData>> INSULATOR_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "item/insulator")), InsulatorData.CODEC, InsulatorData.class);
    public static final RegistryKey<Registry<FuelData>> FUEL_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "item/fuel")), FuelData.CODEC, FuelData.class);
    public static final RegistryKey<Registry<FoodData>> FOOD_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "item/food")), FoodData.CODEC, FoodData.class);
    public static final RegistryKey<Registry<ItemCarryTempData>> CARRY_TEMP_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "item/carried_temp")), ItemCarryTempData.CODEC, ItemCarryTempData.class);
    public static final RegistryKey<Registry<DryingItemData>> DRYING_ITEM_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "item/drying_item")), DryingItemData.CODEC, DryingItemData.class);

    // World Registries
    public static final RegistryKey<Registry<BlockTempData>> BLOCK_TEMP_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "block/block_temp")), BlockTempData.CODEC, BlockTempData.class);
    public static final RegistryKey<Registry<BiomeTempData>> BIOME_TEMP_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "world/biome_temp")), BiomeTempData.CODEC, BiomeTempData.class);
    public static final RegistryKey<Registry<DimensionTempData>> DIMENSION_TEMP_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "world/dimension_temp")), DimensionTempData.CODEC, DimensionTempData.class);
    public static final RegistryKey<Registry<StructureTempData>> STRUCTURE_TEMP_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "world/structure_temp")), StructureTempData.CODEC, StructureTempData.class);
    public static final RegistryKey<Registry<DepthTempData>> DEPTH_TEMP_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "world/temp_region")), DepthTempData.CODEC, DepthTempData.class);

    // Entity Registries
    public static final RegistryKey<Registry<MountData>> MOUNT_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "entity/mount")), MountData.CODEC, MountData.class);
    public static final RegistryKey<Registry<SpawnBiomeData>> ENTITY_SPAWN_BIOME_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "entity/spawn_biome")), SpawnBiomeData.CODEC, SpawnBiomeData.class);
    public static final RegistryKey<Registry<EntityTempData>> ENTITY_TEMP_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "entity/entity_temp")), EntityTempData.CODEC, EntityTempData.class);

    // Special registries
    public static final RegistryKey<Registry<RemoveRegistryData<?>>> REMOVE_REGISTRY_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "remove")), RemoveRegistryData.CODEC, (Class) RemoveRegistryData.class);

    public static <K, V extends ConfigData> RegistryKey<Registry<V>> createRegistry(RegistryKey<Registry<V>> registry, Codec<V> codec, Class<V> type)
    {
        REGISTRIES.put(registry.location().getPath(), new RegistryHolder<>(registry, codec, type));
        return registry;
    }

    public static Map<String, RegistryHolder<?>> getRegistries()
    {   return ImmutableMap.copyOf(REGISTRIES);
    }

    public static RegistryKey<Registry<? extends ConfigData>> getRegistry(String name)
    {
        return Optional.ofNullable(REGISTRIES.get(name)).map(holder -> (RegistryKey) holder.registry)
               .orElseThrow(() -> ColdSweat.LOGGER.throwing(new IllegalArgumentException("Unknown Cold Sweat registry: " + name)));
    }

    public static <T> RegistryKey<Registry<T>> getRegistry(T object)
    {   return (RegistryKey) REGISTRIES.values().stream()
               .filter(holder -> holder.type.isInstance(object))
               .findFirst()
               .map(holder -> holder.registry)
               .orElse(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "unknown")));
    }

    public static String getRegistryName(RegistryKey<Registry<? extends ConfigData>> key)
    {   return key.location().getPath();
    }

    public static <T extends ConfigData> Codec<T> getCodec(RegistryKey<Registry<T>> registry)
    {
        return (Codec<T>) Optional.of(REGISTRIES.get(getRegistryName((RegistryKey) registry))).map(holder -> holder.codec)
               .orElseThrow(() -> ColdSweat.LOGGER.throwing(new IllegalArgumentException("Unknown Cold Sweat registry: " + registry.location().getPath())));
    }

    public static class RegistryHolder<V extends ConfigData>
    {
        public final RegistryKey<Registry<V>> registry;
        public final Codec<V> codec;
        public final Class<V> type;

        public RegistryHolder(RegistryKey<Registry<V>> registry, Codec<V> codec, Class<V> type)
        {   this.registry = registry;
            this.codec = codec;
            this.type = type;
        }

        public RegistryKey<Registry<V>> getRegistry()
        {   return registry;
        }

        public Codec<V> getCodec()
        {   return codec;
        }
    }
}
