package com.momosoftworks.coldsweat.config;

import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.event.core.registry.CreateRegistriesEvent;
import com.momosoftworks.coldsweat.api.event.vanilla.ServerConfigsLoadedEvent;
import com.momosoftworks.coldsweat.api.registry.BlockTempRegistry;
import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTemp;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.core.init.TempModifierInit;
import com.momosoftworks.coldsweat.data.ModRegistries;
import com.momosoftworks.coldsweat.data.codec.configuration.*;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.requirement.BlockRequirement;
import com.momosoftworks.coldsweat.data.tag.ModBlockTags;
import com.momosoftworks.coldsweat.data.tag.ModItemTags;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.item.Item;
import net.minecraft.tags.ITag;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber
public class ConfigLoadingHandler
{
    public static final Multimap<RegistryKey<Registry<? extends ConfigData>>, RemoveRegistryData<?>> REMOVED_REGISTRIES = new FastMultiMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void loadConfigs(ServerConfigsLoadedEvent event)
    {
        ConfigSettings.clear();
        BlockTempRegistry.flush();

        DynamicRegistries registryAccess = event.getServer().registryAccess();
        Multimap<RegistryKey<Registry<? extends ConfigData>>, ? extends ConfigData> registries = new FastMultiMap<>();

        // User JSON configs (config folder)
        ColdSweat.LOGGER.info("Loading registries from configs...");
        registries.putAll((Multimap) collectUserRegistries(registryAccess));

        // JSON configs (data resources)
        ColdSweat.LOGGER.info("Loading registries from data resources...");
        registries.putAll((Multimap) collectDataRegistries(registryAccess));

        // Load JSON data into the config settings
        logAndAddRegistries(registryAccess, registries);

        // User configs (TOML)
        ColdSweat.LOGGER.info("Loading TOML configs...");
        ConfigSettings.load(registryAccess, false);
        TempModifierInit.buildBlockConfigs();

        // Java BlockTemps
        ColdSweat.LOGGER.info("Loading BlockTemps...");
        TempModifierInit.buildBlockRegistries();
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ClientConfigs
    {
        @SubscribeEvent
        public static void loadClientConfigs(FMLLoadCompleteEvent event)
        {   ConfigSettings.CLIENT_SETTINGS.forEach((id, holder) -> holder.load(true));
        }
    }

    /**
     * Loads JSON-based configs from data resources
     */
    public static Multimap<RegistryKey<Registry<? extends ConfigData>>, ? extends ConfigData> collectDataRegistries(DynamicRegistries registryAccess)
    {
        if (registryAccess == null)
        {   ColdSweat.LOGGER.error("Failed to load registries from null DynamicRegistries");
            return new FastMultiMap<>();
        }
        /*
         Add blocks from tags to configs
         */
        ConfigSettings.THERMAL_SOURCE_SPREAD_WHITELIST.get().addAll(ModBlockTags.HEARTH_SPREAD_WHITELIST.getValues().stream().peek(holder ->
                                                           {   ColdSweat.LOGGER.info("Adding block {} to hearth spread whitelist", holder);
                                                           }).collect(Collectors.toSet()));
        ConfigSettings.THERMAL_SOURCE_SPREAD_BLACKLIST.get().addAll(ModBlockTags.HEARTH_SPREAD_BLACKLIST.getValues().stream().peek(holder ->
                                                           {   ColdSweat.LOGGER.info("Adding block {} to hearth spread blacklist", holder);
                                                           }).collect(Collectors.toSet()));
        ConfigSettings.SLEEP_CHECK_IGNORE_BLOCKS.get().addAll(ModBlockTags.IGNORE_SLEEP_CHECK.getValues().stream().peek(holder ->
                                                           {   ColdSweat.LOGGER.info("Disabling sleeping conditions check for block {}", holder);
                                                           }).collect(Collectors.toSet()));
        ConfigSettings.INSULATION_BLACKLIST.get().addAll(ModItemTags.NOT_INSULATABLE.getValues().stream().peek(holder ->
                                                           {   ColdSweat.LOGGER.info("Adding item {} to insulation blacklist", holder);
                                                           }).collect(Collectors.toSet()));

        /*
         Fetch JSON registries
        */
        Multimap<RegistryKey<Registry<? extends ConfigData>>, ? extends ConfigData> registries = new FastMultiMap<>();
        for (Map.Entry<String, ModRegistries.RegistryHolder<?>> entry : ModRegistries.getRegistries().entrySet())
        {
            RegistryKey key = entry.getValue().registry;
            registries.putAll(key, registryAccess.registryOrThrow(key));
        }
        return registries;
    }

    /**
     * Loads JSON-based configs from the configs folder
     */
    public static Multimap<RegistryKey<Registry<? extends ConfigData>>, ? extends ConfigData> collectUserRegistries(DynamicRegistries registryAccess)
    {
        if (registryAccess == null)
        {   ColdSweat.LOGGER.error("Failed to load registries from null DynamicRegistries");
            return new FastMultiMap<>();
        }

        /*
         Parse user-defined JSON data from the configs folder
        */
        Multimap<RegistryKey<Registry<? extends ConfigData>>, ? extends ConfigData> registries = new FastMultiMap<>();
        for (Map.Entry<String, ModRegistries.RegistryHolder<?>> entry : ModRegistries.getRegistries().entrySet())
        {
            RegistryKey<Registry<? extends ConfigData>> key = (RegistryKey) entry.getValue().registry;
            Codec<?> codec = entry.getValue().codec;
            registries.putAll(key, parseConfigData((RegistryKey) key, (Codec) codec, registryAccess));
        }
        return registries;
    }

    private static void logAndAddRegistries(DynamicRegistries registryAccess, Multimap<RegistryKey<Registry<? extends ConfigData>>, ? extends ConfigData> registries)
    {
        // Ensure default registry entries load last
        setDefaultRegistryPriority(registries, registryAccess);

        // Load registry removals
        loadRegistryRemovals(registryAccess);

        // Mark holders as "JSON"
        for (ConfigData data : registries.values())
        {   data.setType(ConfigData.Type.JSON);
        }

        // Fire registry creation event
        CreateRegistriesEvent event = new CreateRegistriesEvent(registryAccess, registries);
        MinecraftForge.EVENT_BUS.post(event);

        // Remove registry entries that match removal criteria
        removeRegistries(event.getRegistries());

        /*
         Add JSON data to the config settings
         */
        // insulators
        Collection<InsulatorData> insulators = event.getRegistry(ModRegistries.INSULATOR_DATA);
        addInsulatorConfigs(insulators);
        logRegistryLoaded(String.format("Loaded %s insulators", insulators.size()), insulators);
        // fuels
        Collection<FuelData> fuels = event.getRegistry(ModRegistries.FUEL_DATA);
        addFuelConfigs(fuels);
        logRegistryLoaded(String.format("Loaded %s fuels", fuels.size()), fuels);
        // foods
        Collection<FoodData> foods = event.getRegistry(ModRegistries.FOOD_DATA);
        addFoodConfigs(foods);
        logRegistryLoaded(String.format("Loaded %s foods", foods.size()), foods);
        // carry temperatures
        Collection<ItemCarryTempData> carryTemps = event.getRegistry(ModRegistries.CARRY_TEMP_DATA);
        addCarryTempConfigs(carryTemps);
        logRegistryLoaded(String.format("Loaded %s carried item temperatures", carryTemps.size()), carryTemps);
        // drying items
        Collection<DryingItemData> dryingItems = event.getRegistry(ModRegistries.DRYING_ITEM_DATA);
        addDryingItemConfigs(dryingItems);
        logRegistryLoaded(String.format("Loaded %s drying items", dryingItems.size()), dryingItems);

        // block temperatures
        Collection<BlockTempData> blockTemps = event.getRegistry(ModRegistries.BLOCK_TEMP_DATA);
        addBlockTempConfigs(blockTemps);
        logRegistryLoaded(String.format("Loaded %s block temperatures", blockTemps.size()), blockTemps);
        // biome temperatures
        Collection<BiomeTempData> biomeTemps = event.getRegistry(ModRegistries.BIOME_TEMP_DATA);
        addBiomeTempConfigs(biomeTemps, registryAccess);
        logRegistryLoaded(String.format("Loaded %s biome temperatures", biomeTemps.size()), biomeTemps);
        // dimension temperatures
        Collection<DimensionTempData> dimensionTemps = event.getRegistry(ModRegistries.DIMENSION_TEMP_DATA);
        addDimensionTempConfigs(dimensionTemps, registryAccess);
        logRegistryLoaded(String.format("Loaded %s dimension temperatures", dimensionTemps.size()), dimensionTemps);
        // structure temperatures
        Collection<StructureTempData> structureTemps = event.getRegistry(ModRegistries.STRUCTURE_TEMP_DATA);
        addStructureTempConfigs(structureTemps, registryAccess);
        logRegistryLoaded(String.format("Loaded %s structure temperatures", structureTemps.size()), structureTemps);
        // depth temperatures
        Collection<DepthTempData> depthTemps = event.getRegistry(ModRegistries.DEPTH_TEMP_DATA);
        addDepthTempConfigs(depthTemps);
        logRegistryLoaded(String.format("Loaded %s depth temperatures", depthTemps.size()), depthTemps);

        // mounts
        Collection<MountData> mounts = event.getRegistry(ModRegistries.MOUNT_DATA);
        addMountConfigs(mounts);
        logRegistryLoaded(String.format("Loaded %s insulated mounts", mounts.size()), mounts);
        // spawn biomes
        Collection<SpawnBiomeData> spawnBiomes = event.getRegistry(ModRegistries.ENTITY_SPAWN_BIOME_DATA);
        addSpawnBiomeConfigs(spawnBiomes, registryAccess);
        logRegistryLoaded(String.format("Loaded %s entity spawn biomes", spawnBiomes.size()), spawnBiomes);
        // entity temperatures
        Collection<EntityTempData> entityTemps = event.getRegistry(ModRegistries.ENTITY_TEMP_DATA);
        addEntityTempConfigs(entityTemps);
        logRegistryLoaded(String.format("Loaded %s entity temperatures", entityTemps.size()), entityTemps);

        CreateRegistriesEvent.Post postEvent = new CreateRegistriesEvent.Post(registryAccess, event.getRegistries());
        MinecraftForge.EVENT_BUS.post(postEvent);
    }

    private static void logRegistryLoaded(String message, Collection<?> registry)
    {
        if (registry.isEmpty())
        {   message += ".";
        }
        else message += ":";
        ColdSweat.LOGGER.info(message, registry.size());
        if (registry.isEmpty())
        {   return;
        }
        for (Object entry : registry)
        {   ColdSweat.LOGGER.info("{}", entry);
        }
    }

    private static void setDefaultRegistryPriority(Multimap<RegistryKey<Registry<? extends ConfigData>>, ? extends ConfigData> registries, DynamicRegistries dynamicRegistries)
    {
        for (RegistryKey<Registry<? extends ConfigData>> key : registries.keySet())
        {
            List<? extends ConfigData> sortedHolders = new ArrayList<>(registries.get(key));
            sortedHolders.sort(Comparator.comparing(holder ->
            {   return RegistryHelper.getKey(holder, dynamicRegistries).getPath().equals("default") ? 1 : 0;
            }));
            registries.replaceValues(key, (Iterable) sortedHolders);
        }
    }

    private static void loadRegistryRemovals(DynamicRegistries registryAccess)
    {
        // Clear the static map
        REMOVED_REGISTRIES.clear();
        // Gather registry removals & add them to the static map
        Set<RemoveRegistryData<?>> removals = registryAccess.registryOrThrow(ModRegistries.REMOVE_REGISTRY_DATA).stream().collect(Collectors.toSet());
        removals.addAll(parseConfigData(ModRegistries.REMOVE_REGISTRY_DATA, RemoveRegistryData.CODEC, registryAccess));
        removals.forEach(data ->
        {
            RegistryKey<Registry<? extends ConfigData>> key = (RegistryKey) data.registry();
            REMOVED_REGISTRIES.put(key, data);
        });
    }

    private static void removeRegistries(Multimap<RegistryKey<Registry<? extends ConfigData>>, ? extends ConfigData> registries)
    {
        ColdSweat.LOGGER.info("Handling registry removals...");
        for (Map.Entry<RegistryKey<Registry<? extends ConfigData>>, Collection<RemoveRegistryData<? extends ConfigData>>> entry : REMOVED_REGISTRIES.asMap().entrySet())
        {
            removeEntries((Collection) entry.getValue(), (Collection) registries.get(entry.getKey()));
        }
    }

    private static <T extends ConfigData, H extends T> void removeEntries(Collection<RemoveRegistryData<T>> removals, Collection<H> registry)
    {
        for (RemoveRegistryData<T> data : removals)
        {   registry.removeIf(data::matches);
        }
    }

    public static <T extends ConfigData> Collection<T> removeEntries(Collection<T> registries, RegistryKey<Registry<T>> registryName)
    {
        REMOVED_REGISTRIES.get((RegistryKey) registryName).forEach(data ->
        {
            RemoveRegistryData<T> removeData = ((RemoveRegistryData<T>) data);
            if (removeData.registry() == registryName)
            {   registries.removeIf(removeData::matches);
            }
        });
        return registries;
    }

    public static <T extends ConfigData> boolean isRemoved(T entry, RegistryKey<Registry<T>> registryName)
    {
        return REMOVED_REGISTRIES.get((RegistryKey) registryName).stream().anyMatch(data -> ((RemoveRegistryData<T>) data).matches(entry));
    }

    private static void addInsulatorConfigs(Collection<InsulatorData> insulators)
    {
        insulators.forEach(insulator ->
        {
            // Check if the required mods are loaded
            if (!insulator.areRequiredModsLoaded())
            {   return;
            }

            // Add listed items as insulators
            List<Item> items = new ArrayList<>();
            insulator.data().items().ifPresent(itemList ->
            {   items.addAll(RegistryHelper.mapTaggableList(itemList));
            });
            insulator.data().tag().ifPresent(tag ->
            {   items.addAll(tag.getValues());
            });

            for (Item item : items)
            {
                switch (insulator.slot())
                {
                    case ITEM  : ConfigSettings.INSULATION_ITEMS.get().put(item, insulator); break;
                    case ARMOR : ConfigSettings.INSULATING_ARMORS.get().put(item, insulator); break;
                    case CURIO :
                    {
                        if (CompatManager.isCuriosLoaded())
                        {   ConfigSettings.INSULATING_CURIOS.get().put(item, insulator);
                        }
                        break;
                    }
                }
            }
        });
    }

    private static void addFuelConfigs(Collection<FuelData> fuels)
    {
        fuels.forEach(fuelData ->
        {
            // Check if the required mods are loaded
            if (!fuelData.areRequiredModsLoaded())
            {   return;
            }

            List<Item> items = new ArrayList<>();
            fuelData.data().items().ifPresent(itemList ->
            {   items.addAll(RegistryHelper.mapTaggableList(itemList));
            });
            fuelData.data().tag().ifPresent(tag ->
            {   items.addAll(tag.getValues());
            });

            for (Item item : items)
            {
                switch (fuelData.type())
                {
                    case BOILER : ConfigSettings.BOILER_FUEL.get().put(item, fuelData); break;
                    case ICEBOX : ConfigSettings.ICEBOX_FUEL.get().put(item, fuelData); break;
                    case HEARTH : ConfigSettings.HEARTH_FUEL.get().put(item, fuelData); break;
                    case SOUL_LAMP : ConfigSettings.SOULSPRING_LAMP_FUEL.get().put(item, fuelData); break;
                }
            }
        });
    }

    private static void addFoodConfigs(Collection<FoodData> foods)
    {
        foods.forEach(foodData ->
        {
            // Check if the required mods are loaded
            if (!foodData.areRequiredModsLoaded())
            {   return;
            }

            List<Item> items = new ArrayList<>();
            foodData.data().items().ifPresent(itemList ->
            {   items.addAll(RegistryHelper.mapTaggableList(itemList));
            });
            foodData.data().tag().ifPresent(tag ->
            {   items.addAll(tag.getValues());
            });

            for (Item item : items)
            {   ConfigSettings.FOOD_TEMPERATURES.get().put(item, foodData);
            }
        });
    }

    private static void addCarryTempConfigs(Collection<ItemCarryTempData> carryTemps)
    {
        carryTemps.forEach(carryTempData ->
        {
            // Check if the required mods are loaded
            if (!carryTempData.areRequiredModsLoaded())
            {   return;
            }

            List<Item> items = new ArrayList<>();
            carryTempData.data().items().ifPresent(itemList ->
            {   items.addAll(RegistryHelper.mapTaggableList(itemList));
            });
            carryTempData.data().tag().ifPresent(tag ->
            {   items.addAll(tag.getValues());
            });
            for (Item item : items)
            {   ConfigSettings.CARRIED_ITEM_TEMPERATURES.get().put(item, carryTempData);
            }
        });
    }

    private static void addDryingItemConfigs(Collection<DryingItemData> dryingItems)
    {
        dryingItems.forEach(dryingItemData ->
        {
            // Check if the required mods are loaded
            if (!dryingItemData.areRequiredModsLoaded())
            {   return;
            }

            List<Item> items = new ArrayList<>();
            dryingItemData.data().items().ifPresent(itemList ->
            {   items.addAll(RegistryHelper.mapTaggableList(itemList));
            });
            dryingItemData.data().tag().ifPresent(tag ->
            {   items.addAll(tag.getValues().stream().collect(Collectors.toList()));
            });
            for (Item item : items)
            {   ConfigSettings.DRYING_ITEMS.get().put(item, dryingItemData);
            }
        });
    }

    private static void addBlockTempConfigs(Collection<BlockTempData> blockTemps)
    {
        blockTemps.forEach(blockTempData ->
        {
            // Check if the required mods are loaded
            if (!blockTempData.areRequiredModsLoaded())
            {   return;
            }
            Block[] blocks = RegistryHelper.mapTaggableList(blockTempData.blocks()).toArray(new Block[0]);
            BlockTemp blockTemp = new BlockTemp(blockTempData.getTemperature() < 0 ? -blockTempData.getMaxEffect() : -Double.MAX_VALUE,
                                                blockTempData.getTemperature() > 0 ? blockTempData.getMaxEffect() : Double.MAX_VALUE,
                                                blockTempData.getMinTemp(),
                                                blockTempData.getMaxTemp(),
                                                blockTempData.range(),
                                                blockTempData.fade(),
                                                blocks)
            {
                final double temperature = blockTempData.getTemperature();
                final List<BlockRequirement> conditions = blockTempData.conditions();

                @Override
                public double getTemperature(World level, LivingEntity entity, BlockState state, BlockPos pos, double distance)
                {
                    if (level instanceof ServerWorld)
                    {
                        ServerWorld serverLevel = (ServerWorld) level;
                        for (int i = 0; i < conditions.size(); i++)
                        {
                            if (!conditions.get(i).test(serverLevel, pos))
                            {   return 0;
                            }
                        }
                    }
                    return temperature;
                }
            };

            BlockTempRegistry.register(blockTemp);
        });
    }

    private static void addBiomeTempConfigs(Collection<BiomeTempData> biomeTemps, DynamicRegistries registryAccess)
    {
        biomeTemps.forEach(biomeTempData ->
        {
            // Check if the required mods are loaded
            if (!biomeTempData.areRequiredModsLoaded())
            {   return;
            }
            for (Biome biome : biomeTempData.biomes())
            {
                if (biomeTempData.isOffset())
                {   ConfigSettings.BIOME_OFFSETS.get(registryAccess).put(biome, biomeTempData);
                }
                else
                {   ConfigSettings.BIOME_TEMPS.get(registryAccess).put(biome, biomeTempData);
                }
            }
        });
    }

    private static void addDimensionTempConfigs(Collection<DimensionTempData> dimensionTemps, DynamicRegistries registryAccess)
    {
        dimensionTemps.forEach(dimensionTempData ->
        {
            // Check if the required mods are loaded
            if (!dimensionTempData.areRequiredModsLoaded())
            {   return;
            }

            for (DimensionType dimension : dimensionTempData.dimensions())
            {
                if (dimensionTempData.isOffset())
                {   ConfigSettings.DIMENSION_OFFSETS.get(registryAccess).put(dimension, dimensionTempData);
                }
                else
                {   ConfigSettings.DIMENSION_TEMPS.get(registryAccess).put(dimension, dimensionTempData);
                }
            }
        });
    }

    private static void addStructureTempConfigs(Collection<StructureTempData> structureTemps, DynamicRegistries registryAccess)
    {
        structureTemps.forEach(structureTempData ->
        {
            // Check if the required mods are loaded
            if (!structureTempData.areRequiredModsLoaded())
            {   return;
            }
            for (Structure<?> structure : structureTempData.structures())
            {
                if (structureTempData.isOffset())
                {   ConfigSettings.STRUCTURE_OFFSETS.get(registryAccess).put(structure, structureTempData);
                }
                else
                {   ConfigSettings.STRUCTURE_TEMPS.get(registryAccess).put(structure, structureTempData);
                }
            }
        });
    }

    private static void addDepthTempConfigs(Collection<DepthTempData> depthTemps)
    {
        // Add the depth temps to the config
        for (DepthTempData depthData : depthTemps)
        {
            // Check if the required mods are loaded
            if (!depthData.areRequiredModsLoaded())
            {   return;
            }
            ConfigSettings.DEPTH_REGIONS.get().add(depthData);
        }
    }

    private static void addMountConfigs(Collection<MountData> mounts)
    {
        mounts.forEach(mountData ->
        {
            // Check if the required mods are loaded
            if (!mountData.areRequiredModsLoaded())
            {   return;
            }
            List<EntityType<?>> entities = RegistryHelper.mapTaggableList(mountData.entityData().entities().orElse(Arrays.asList()));
            for (EntityType<?> entity : entities)
            {   ConfigSettings.INSULATED_MOUNTS.get().put(entity, mountData);
            }
        });
    }

    private static void addSpawnBiomeConfigs(Collection<SpawnBiomeData> spawnBiomes, DynamicRegistries registryAccess)
    {
        spawnBiomes.forEach(spawnBiomeData ->
        {
            // Check if the required mods are loaded
            if (!spawnBiomeData.areRequiredModsLoaded())
            {   return;
            }
            for (Biome biome : spawnBiomeData.biomes())
            {   ConfigSettings.ENTITY_SPAWN_BIOMES.get(registryAccess).put(biome, spawnBiomeData);
            }
        });
    }

    private static void addEntityTempConfigs(Collection<EntityTempData> entityTemps)
    {
        entityTemps.forEach(entityTempData ->
        {
            // Check if the required mods are loaded
            if (!entityTempData.areRequiredModsLoaded())
            {   return;
            }
            // Gather entity types and tags
            List<Either<ITag<EntityType<?>>, EntityType<?>>> types = new ArrayList<>();
            entityTempData.entity().entities.ifPresent(type -> types.addAll(type));

            for (EntityType<?> entity : RegistryHelper.mapTaggableList(types))
            {   ConfigSettings.ENTITY_TEMPERATURES.get().put(entity, entityTempData);
            }
        });
    }

    private static <T> List<T> parseConfigData(RegistryKey<Registry<T>> registry, Codec<T> codec, DynamicRegistries registryAccess)
    {
        List<T> output = new ArrayList<>();
        DynamicOps<JsonElement> registryOps = JsonOps.INSTANCE;

        Path coldSweatDataPath = FMLPaths.CONFIGDIR.get().resolve("coldsweat/data").resolve(registry.location().getPath());
        File jsonDirectory = coldSweatDataPath.toFile();

        if (!jsonDirectory.exists())
        {   return output;
        }
        else for (File file : findFilesRecursive(jsonDirectory))
        {
            if (file.getName().endsWith(".json"))
            {
                try (FileReader reader = new FileReader(file))
                {
                    codec.decode(registryOps, JSONUtils.parse(reader))
                            .resultOrPartial(ColdSweat.LOGGER::error)
                            .map(Pair::getFirst)
                            .ifPresent(insulator -> output.add(insulator));
                }
                catch (Exception e)
                {   ColdSweat.LOGGER.error("Failed to parse JSON config setting in {}: {}", registry.location(), file.getName(), e);
                }
            }
        }
        return output;
    }

    public static List<File> findFilesRecursive(File directory)
    {
        List<File> files = new ArrayList<>();
        File[] filesInDirectory = directory.listFiles();
        if (filesInDirectory == null)
        {   return files;
        }
        for (File file : filesInDirectory)
        {
            if (file.isDirectory())
            {   files.addAll(findFilesRecursive(file));
            }
            else
            {   files.add(file);
            }
        }
        return files;
    }
}
