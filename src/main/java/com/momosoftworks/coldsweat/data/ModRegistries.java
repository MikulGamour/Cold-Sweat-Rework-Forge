package com.momosoftworks.coldsweat.data;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.data.codec.configuration.*;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.util.exceptions.RegistryFailureException;
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
    private static final Map<String, ConfigRegistry<?>> REGISTRIES = new FastMap<>();

    public static IResourceManager getResourceManager()
    {   return RESOURCE_MANAGER;
    }

    static
    {   RESOURCE_MANAGER.add(new ModFileResourcePack(ModList.get().getModFileById(ColdSweat.MOD_ID).getFile()));
    }

    // Item Registries
    public static final ConfigRegistry<InsulatorData> INSULATOR_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "item/insulator")), InsulatorData.CODEC, InsulatorData.class);
    public static final ConfigRegistry<FuelData> FUEL_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "item/fuel")), FuelData.CODEC, FuelData.class);
    public static final ConfigRegistry<FoodData> FOOD_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "item/food")), FoodData.CODEC, FoodData.class);
    public static final ConfigRegistry<ItemCarryTempData> CARRY_TEMP_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "item/carried_temp")), ItemCarryTempData.CODEC, ItemCarryTempData.class);
    public static final ConfigRegistry<DryingItemData> DRYING_ITEM_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "item/drying_item")), DryingItemData.CODEC, DryingItemData.class);

    // World Registries
    public static final ConfigRegistry<BlockTempData> BLOCK_TEMP_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "block/block_temp")), BlockTempData.CODEC, BlockTempData.class);
    public static final ConfigRegistry<BiomeTempData> BIOME_TEMP_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "world/biome_temp")), BiomeTempData.CODEC, BiomeTempData.class);
    public static final ConfigRegistry<DimensionTempData> DIMENSION_TEMP_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "world/dimension_temp")), DimensionTempData.CODEC, DimensionTempData.class);
    public static final ConfigRegistry<StructureTempData> STRUCTURE_TEMP_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "world/structure_temp")), StructureTempData.CODEC, StructureTempData.class);
    public static final ConfigRegistry<DepthTempData> DEPTH_TEMP_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "world/temp_region")), DepthTempData.CODEC, DepthTempData.class);

    // Entity Registries
    public static final ConfigRegistry<MountData> MOUNT_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "entity/mount")), MountData.CODEC, MountData.class);
    public static final ConfigRegistry<SpawnBiomeData> ENTITY_SPAWN_BIOME_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "entity/spawn_biome")), SpawnBiomeData.CODEC, SpawnBiomeData.class);
    public static final ConfigRegistry<EntityTempData> ENTITY_TEMP_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "entity/entity_temp")), EntityTempData.CODEC, EntityTempData.class);

    // Special registries
    public static final ConfigRegistry<RemoveRegistryData<?>> REMOVE_REGISTRY_DATA = createRegistry(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "remove")), RemoveRegistryData.CODEC, (Class) RemoveRegistryData.class);

    public static <V extends ConfigData> ConfigRegistry<V> createRegistry(RegistryKey<Registry<V>> registry, Codec<V> codec, Class<V> type)
    {
        ConfigRegistry<V> holder = new ConfigRegistry<>(registry, codec, type);
        REGISTRIES.put(registry.location().getPath(), new ConfigRegistry<>(registry, codec, type));
        return holder;
    }

    public static Map<String, ConfigRegistry<?>> getRegistries()
    {   return ImmutableMap.copyOf(REGISTRIES);
    }

    public static RegistryKey<Registry<? extends ConfigData>> getRegistry(String name)
    {
        return Optional.ofNullable(REGISTRIES.get(name)).map(holder -> (RegistryKey) holder.key())
               .orElseThrow(() -> ColdSweat.LOGGER.throwing(new IllegalArgumentException("Unknown Cold Sweat registry: " + name)));
    }

    public static <T> RegistryKey<Registry<T>> getRegistry(T object)
    {   return (RegistryKey) REGISTRIES.values().stream()
               .filter(holder -> holder.type.isInstance(object))
               .findFirst()
               .map(holder -> holder.key())
               .orElse(RegistryKey.createRegistryKey(new ResourceLocation(ColdSweat.MOD_ID, "unknown")));
    }

    public static String getRegistryName(RegistryKey<Registry<? extends ConfigData>> key)
    {   return key.location().getPath();
    }

    public static <T extends ConfigData> Codec<T> getCodec(RegistryKey<ConfigRegistry<T>> registry)
    {
        return (Codec<T>) Optional.of(REGISTRIES.get(getRegistryName((RegistryKey) registry))).map(holder -> holder.codec)
               .orElseThrow(() -> ColdSweat.LOGGER.throwing(new IllegalArgumentException("Unknown Cold Sweat registry: " + registry.location().getPath())));
    }

    public static class ConfigRegistry<V extends ConfigData>
    {
        private final RegistryKey<Registry<V>> key;
        private final Codec<V> codec;
        private final Class<V> type;
        private final Map<ResourceLocation, V> data = new HashMap<>();

        public ConfigRegistry(RegistryKey<Registry<V>> key, Codec<V> codec, Class<V> type)
        {   this.key = key;
            this.codec = codec;
            this.type = type;
        }

        public RegistryKey<Registry<V>> key()
        {   return key;
        }
        public Codec<V> codec()
        {   return codec;
        }
        public Class<V> type()
        {   return type;
        }
        public Map<ResourceLocation, V> data()
        {   return data;
        }

        public void register(ResourceLocation id, V data)
        {
            if (this.data.put(id, data) != null)
            {   throw ColdSweat.LOGGER.throwing(new RegistryFailureException(data, key.location().toString(), "Duplicate entry", null));
            }
        }
        public void flush()
        {   data.clear();
        }
    }
}
