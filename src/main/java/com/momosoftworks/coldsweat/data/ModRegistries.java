package com.momosoftworks.coldsweat.data;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.data.codec.configuration.*;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.util.math.FastMap;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

public class ModRegistries
{
    private static final Map<String, RegistryHolder<?>> REGISTRIES = new FastMap<>();

    // Item Registries
    public static final ResourceKey<Registry<InsulatorData>> INSULATOR_DATA = createRegistry(ResourceKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "item/insulator")), InsulatorData.CODEC, InsulatorData.class);
    public static final ResourceKey<Registry<FuelData>> FUEL_DATA = createRegistry(ResourceKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "item/fuel")), FuelData.CODEC, FuelData.class);
    public static final ResourceKey<Registry<FoodData>> FOOD_DATA = createRegistry(ResourceKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "item/food")), FoodData.CODEC, FoodData.class);
    public static final ResourceKey<Registry<ItemCarryTempData>> CARRY_TEMP_DATA = createRegistry(ResourceKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "item/carried_temp")), ItemCarryTempData.CODEC, ItemCarryTempData.class);
    public static final ResourceKey<Registry<DryingItemData>> DRYING_ITEM_DATA = createRegistry(ResourceKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "item/drying_item")), DryingItemData.CODEC, DryingItemData.class);

    // World Registries
    public static final ResourceKey<Registry<BlockTempData>> BLOCK_TEMP_DATA = createRegistry(ResourceKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "block/block_temp")), BlockTempData.CODEC, BlockTempData.class);
    public static final ResourceKey<Registry<BiomeTempData>> BIOME_TEMP_DATA = createRegistry(ResourceKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "world/biome_temp")), BiomeTempData.CODEC, BiomeTempData.class);
    public static final ResourceKey<Registry<DimensionTempData>> DIMENSION_TEMP_DATA = createRegistry(ResourceKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "world/dimension_temp")), DimensionTempData.CODEC, DimensionTempData.class);
    public static final ResourceKey<Registry<StructureTempData>> STRUCTURE_TEMP_DATA = createRegistry(ResourceKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "world/structure_temp")), StructureTempData.CODEC, StructureTempData.class);
    public static final ResourceKey<Registry<DepthTempData>> DEPTH_TEMP_DATA = createRegistry(ResourceKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "world/temp_region")), DepthTempData.CODEC, DepthTempData.class);

    // Entity Registries
    public static final ResourceKey<Registry<MountData>> MOUNT_DATA = createRegistry(ResourceKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "entity/mount")), MountData.CODEC, MountData.class);
    public static final ResourceKey<Registry<SpawnBiomeData>> ENTITY_SPAWN_BIOME_DATA = createRegistry(ResourceKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "entity/spawn_biome")), SpawnBiomeData.CODEC, SpawnBiomeData.class);
    public static final ResourceKey<Registry<EntityTempData>> ENTITY_TEMP_DATA = createRegistry(ResourceKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "entity/entity_temp")), EntityTempData.CODEC, EntityTempData.class);

    // Special registries
    public static final ResourceKey<Registry<RemoveRegistryData<?>>> REMOVE_REGISTRY_DATA = createRegistry(ResourceKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "remove")), RemoveRegistryData.CODEC, (Class) RemoveRegistryData.class);

    public static <K, V extends ConfigData> ResourceKey<Registry<V>> createRegistry(ResourceKey<Registry<V>> registry, Codec<V> codec, Class<V> type)
    {
        REGISTRIES.put(registry.location().getPath(), new RegistryHolder<>(registry, codec, type));
        return registry;
    }

    public static Map<String, RegistryHolder<?>> getRegistries()
    {   return ImmutableMap.copyOf(REGISTRIES);
    }

    public static ResourceKey<Registry<? extends ConfigData>> getRegistry(String name)
    {
        return Optional.ofNullable(REGISTRIES.get(name)).map(holder -> (ResourceKey) holder.registry())
               .orElseThrow(() -> ColdSweat.LOGGER.throwing(new IllegalArgumentException("Unknown Cold Sweat registry: " + name)));
    }

    public static <T> ResourceKey<Registry<T>> getRegistry(T object)
    {   return (ResourceKey) REGISTRIES.values().stream()
               .filter(holder -> holder.type.isInstance(object))
               .findFirst()
               .map(RegistryHolder::registry)
               .orElse(ResourceKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "unknown")));
    }

    public static String getRegistryName(ResourceKey<? extends ConfigData> key)
    {   return key.location().getPath();
    }

    public static <T extends ConfigData> Codec<T> getCodec(ResourceKey<Registry<T>> registry)
    {
        return (Codec<T>) Optional.of(REGISTRIES.get(getRegistryName((ResourceKey) registry))).map(RegistryHolder::codec)
               .orElseThrow(() -> ColdSweat.LOGGER.throwing(new IllegalArgumentException("Unknown Cold Sweat registry: " + registry.location().getPath())));
    }

    public record RegistryHolder<V extends ConfigData>(ResourceKey<Registry<V>> registry, Codec<V> codec, Class<V> type)
    {}
}
