package com.momosoftworks.coldsweat.config;

import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.event.core.registry.CreateRegistriesEvent;
import com.momosoftworks.coldsweat.api.event.vanilla.ServerConfigsLoadedEvent;
import com.momosoftworks.coldsweat.api.registry.BlockTempRegistry;
import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTemp;
import com.momosoftworks.coldsweat.core.init.TempModifierInit;
import com.momosoftworks.coldsweat.data.ModRegistries;
import com.momosoftworks.coldsweat.data.codec.configuration.*;
import com.momosoftworks.coldsweat.data.codec.requirement.BlockRequirement;
import com.momosoftworks.coldsweat.data.tag.ModBlockTags;
import com.momosoftworks.coldsweat.data.tag.ModItemTags;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
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

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber
public class ConfigLoadingHandler
{
    public static final Multimap<RegistryKey<Registry<?>>, RemoveRegistryData<?>> REMOVED_REGISTRIES = new FastMultiMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void loadConfigs(ServerConfigsLoadedEvent event)
    {
        ConfigSettings.clear();
        BlockTempRegistry.flush();
        getDefaultConfigs(event.getServer());

        DynamicRegistries registryAccess = event.getServer().registryAccess();
        Multimap<RegistryKey<Registry<?>>, ?> registries = new FastMultiMap<>();

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
    public static Multimap<RegistryKey<Registry<?>>, ?> collectDataRegistries(DynamicRegistries registryAccess)
    {
        if (registryAccess == null)
        {   ColdSweat.LOGGER.error("Failed to load registries from null DynamicRegistries");
            return new FastMultiMap<>();
        }
        /*
         Add blocks from tags to configs
         */
        ConfigSettings.HEARTH_SPREAD_WHITELIST.get().addAll(ModBlockTags.HEARTH_SPREAD_WHITELIST.getValues().stream().peek(holder ->
                                                           {   ColdSweat.LOGGER.info("Adding block {} to hearth spread whitelist", holder);
                                                           }).collect(Collectors.toSet()));
        ConfigSettings.HEARTH_SPREAD_BLACKLIST.get().addAll(ModBlockTags.HEARTH_SPREAD_BLACKLIST.getValues().stream().peek(holder ->
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
        Multimap<RegistryKey<Registry<?>>, ?> registries = new FastMultiMap<>();
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
    public static Multimap<RegistryKey<Registry<?>>, ?> collectUserRegistries(DynamicRegistries registryAccess)
    {
        if (registryAccess == null)
        {   ColdSweat.LOGGER.error("Failed to load registries from null DynamicRegistries");
            return new FastMultiMap<>();
        }

        /*
         Parse user-defined JSON data from the configs folder
        */
        Multimap<RegistryKey<Registry<?>>, ?> registries = new FastMultiMap<>();
        for (Map.Entry<String, ModRegistries.RegistryHolder<?>> entry : ModRegistries.getRegistries().entrySet())
        {
            RegistryKey<Registry<?>> key = (RegistryKey) entry.getValue().registry;
            Codec<?> codec = entry.getValue().codec;
            registries.putAll(key, parseConfigData((RegistryKey) key, (Codec) codec));
        }
        return registries;
    }

    private static void logAndAddRegistries(DynamicRegistries registryAccess, Multimap<RegistryKey<Registry<?>>, ?> registries)
    {
        // Clear the static map
        REMOVED_REGISTRIES.clear();
        // Gather registry removals & add them to the static map
        Set<RemoveRegistryData<?>> removals = registryAccess.registryOrThrow(ModRegistries.REMOVE_REGISTRY_DATA).stream().collect(Collectors.toSet());
        removals.addAll(parseConfigData(ModRegistries.REMOVE_REGISTRY_DATA, RemoveRegistryData.CODEC));
        removals.forEach(data ->
        {
            RegistryKey<Registry<?>> key = data.registry;
            REMOVED_REGISTRIES.put(key, data);
        });

        // Fire registry creation event
        CreateRegistriesEvent event = new CreateRegistriesEvent(registryAccess, registries);
        MinecraftForge.EVENT_BUS.post(event);

        // Remove registry entries that match removal criteria
        removeRegistries(event.getRegistries());

        /*
         Add JSON data to the config settings
         */
        // insulators
        addInsulatorConfigs(event.getInsulators());
        logRegistryLoaded(String.format("Loaded %s insulators", event.getInsulators().size()), event.getInsulators());
        // fuels
        addFuelConfigs(event.getFuels());
        logRegistryLoaded(String.format("Loaded %s fuels", event.getFuels().size()), event.getFuels());
        // foods
        addFoodConfigs(event.getFoods());
        logRegistryLoaded(String.format("Loaded %s foods", event.getFoods().size()), event.getFoods());
        // carry temperatures
        addCarryTempConfigs(event.getCarryTemps());
        logRegistryLoaded(String.format("Loaded %s carried item temperatures", event.getCarryTemps().size()), event.getCarryTemps());

        // block temperatures
        addBlockTempConfigs(event.getBlockTemps());
        logRegistryLoaded(String.format("Loaded %s block temperatures", event.getBlockTemps().size()), event.getBlockTemps());
        // biome temperatures
        addBiomeTempConfigs(event.getBiomeTemps(), registryAccess);
        logRegistryLoaded(String.format("Loaded %s biome temperatures", event.getBiomeTemps().size()), event.getBiomeTemps());
        // dimension temperatures
        addDimensionTempConfigs(event.getDimensionTemps(), registryAccess);
        logRegistryLoaded(String.format("Loaded %s dimension temperatures", event.getDimensionTemps().size()), event.getDimensionTemps());
        // structure temperatures
        addStructureTempConfigs(event.getStructureTemps(), registryAccess);
        logRegistryLoaded(String.format("Loaded %s structure temperatures", event.getStructureTemps().size()), event.getStructureTemps());
        // depth temperatures
        addDepthTempConfigs(event.getDepthTemps());
        logRegistryLoaded(String.format("Loaded %s depth temperatures", event.getDepthTemps().size()), event.getDepthTemps());

        // mounts
        addMountConfigs(event.getMounts());
        logRegistryLoaded(String.format("Loaded %s insulated mounts", event.getMounts().size()), event.getMounts());
        // spawn biomes
        addSpawnBiomeConfigs(event.getSpawnBiomes(), registryAccess);
        logRegistryLoaded(String.format("Loaded %s entity spawn biomes", event.getSpawnBiomes().size()), event.getSpawnBiomes());
        // entity temperatures
        addEntityTempConfigs(event.getEntityTemps());
        logRegistryLoaded(String.format("Loaded %s entity temperatures", event.getEntityTemps().size()), event.getEntityTemps());
    }

    private static void logRegistryLoaded(String message, Set<?> registry)
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

    private static void removeRegistries(Multimap<RegistryKey<Registry<?>>, ?> registries)
    {
        ColdSweat.LOGGER.info("Handling registry removals...");
        for (Map.Entry<RegistryKey<Registry<?>>, Collection<RemoveRegistryData<?>>> entry : REMOVED_REGISTRIES.asMap().entrySet())
        {
            removeEntries((Collection) entry.getValue(), (Collection) registries.get(entry.getKey()));
        }
    }

    private static <T> void removeEntries(Collection<RemoveRegistryData<T>> removals, Collection<T> registry)
    {
        for (RemoveRegistryData<T> data : removals)
        {   registry.removeIf(entry -> data.matches(((T) entry)));
        }
    }

    public static <T> Collection<T> removeEntries(Collection<T> registries, RegistryKey<Registry<T>> registryName)
    {
        REMOVED_REGISTRIES.get((RegistryKey) registryName).forEach(data ->
        {
            RemoveRegistryData<T> removeData = ((RemoveRegistryData<T>) data);
            if (removeData.getRegistry() == registryName)
            {   registries.removeIf(removeData::matches);
            }
        });
        return registries;
    }

    public static <T> boolean isRemoved(T entry, RegistryKey<Registry<T>> registryName)
    {
        return REMOVED_REGISTRIES.get((RegistryKey) registryName).stream().anyMatch(data -> ((RemoveRegistryData<T>) data).matches(entry));
    }

    private static void getDefaultConfigs(MinecraftServer server)
    {
        DEFAULT_REGION = ConfigHelper.parseResource(server.getDataPackRegistries().getResourceManager(), new ResourceLocation(ColdSweat.MOD_ID, "config/world/temp_region/default.json"), DepthTempData.CODEC).orElseThrow(RuntimeException::new);
    }

    private static void addInsulatorConfigs(Set<InsulatorData> insulators)
    {
        insulators.forEach(insulator ->
        {
            // Check if the required mods are loaded
            if (insulator.requiredMods.isPresent())
            {
                List<String> requiredMods = insulator.requiredMods.get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }

            // Add listed items as insulators
            List<Item> items = new ArrayList<>();
            insulator.data.items.ifPresent(itemList ->
            {   items.addAll(RegistryHelper.mapTaggableList(itemList));
            });
            insulator.data.tag.ifPresent(tag ->
            {   items.addAll(tag.getValues());
            });

            for (Item item : items)
            {
                switch (insulator.slot)
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

    private static void addFuelConfigs(Set<FuelData> fuels)
    {
        fuels.forEach(fuelData ->
        {
            // Check if the required mods are loaded
            if (fuelData.requiredMods.isPresent())
            {
                List<String> requiredMods = fuelData.requiredMods.get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }

            List<Item> items = new ArrayList<>();
            fuelData.data.items.ifPresent(itemList ->
            {   items.addAll(RegistryHelper.mapTaggableList(itemList));
            });
            fuelData.data.tag.ifPresent(tag ->
            {   items.addAll(tag.getValues());
            });

            for (Item item : items)
            {
                switch (fuelData.type)
                {
                    case BOILER : ConfigSettings.BOILER_FUEL.get().put(item, fuelData); break;
                    case ICEBOX : ConfigSettings.ICEBOX_FUEL.get().put(item, fuelData); break;
                    case HEARTH : ConfigSettings.HEARTH_FUEL.get().put(item, fuelData); break;
                    case SOUL_LAMP : ConfigSettings.SOULSPRING_LAMP_FUEL.get().put(item, fuelData); break;
                }
            }
        });
    }

    private static void addFoodConfigs(Set<FoodData> foods)
    {
        foods.forEach(foodData ->
        {
            // Check if the required mods are loaded
            if (foodData.requiredMods.isPresent())
            {
                List<String> requiredMods = foodData.requiredMods.get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }

            List<Item> items = new ArrayList<>();
            foodData.data.items.ifPresent(itemList ->
            {   items.addAll(RegistryHelper.mapTaggableList(itemList));
            });
            foodData.data.tag.ifPresent(tag ->
            {   items.addAll(tag.getValues());
            });

            for (Item item : items)
            {   ConfigSettings.FOOD_TEMPERATURES.get().put(item, foodData);
            }
        });
    }

    private static void addCarryTempConfigs(Set<ItemCarryTempData> carryTemps)
    {
        carryTemps.forEach(carryTempData ->
        {
            // Check if the required mods are loaded
            if (carryTempData.requiredMods.isPresent())
            {
                List<String> requiredMods = carryTempData.requiredMods.get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }

            List<Item> items = new ArrayList<>();
            carryTempData.data.items.ifPresent(itemList ->
            {   items.addAll(RegistryHelper.mapTaggableList(itemList));
            });
            carryTempData.data.tag.ifPresent(tag ->
            {   items.addAll(tag.getValues());
            });
            for (Item item : items)
            {
                ConfigSettings.CARRIED_ITEM_TEMPERATURES.get().put(item, carryTempData);
            }
        });
    }

    private static void addBlockTempConfigs(Set<BlockTempData> holders)
    {
        List<BlockTempData> blockTemps = new ArrayList<>(holders);
        // Handle entries removed by configs
        removeEntries(blockTemps, ModRegistries.BLOCK_TEMP_DATA);

        blockTemps.forEach(blockTempData ->
        {
            // Check if the required mods are loaded
            if (blockTempData.requiredMods.isPresent())
            {
                List<String> requiredMods = blockTempData.requiredMods.get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }
            Block[] blocks = RegistryHelper.mapTaggableList(blockTempData.blocks).toArray(new Block[0]);
            BlockTemp blockTemp = new BlockTemp(blockTempData.temperature < 0 ? -blockTempData.maxEffect : -Double.MAX_VALUE,
                                                blockTempData.temperature > 0 ? blockTempData.maxEffect : Double.MAX_VALUE,
                                                blockTempData.minTemp,
                                                blockTempData.maxTemp,
                                                blockTempData.range,
                                                blockTempData.fade,
                                                blocks)
            {
                final double temperature = blockTempData.temperature;
                final List<BlockRequirement> conditions = blockTempData.conditions;

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

    private static void addBiomeTempConfigs(Set<BiomeTempData> biomeTemps, DynamicRegistries registryAccess)
    {
        biomeTemps.forEach(biomeTempData ->
        {
            // Check if the required mods are loaded
            if (biomeTempData.requiredMods.isPresent())
            {
                List<String> requiredMods = biomeTempData.requiredMods.get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }
            for (Biome biome : biomeTempData.biomes)
            {
                if (biomeTempData.isOffset)
                {   ConfigSettings.BIOME_OFFSETS.get(registryAccess).put(biome, biomeTempData);
                }
                else
                {   ConfigSettings.BIOME_TEMPS.get(registryAccess).put(biome, biomeTempData);
                }
            }
        });
    }

    private static void addDimensionTempConfigs(Set<DimensionTempData> dimensionTemps, DynamicRegistries registryAccess)
    {
        dimensionTemps.forEach(dimensionTempData ->
        {
            // Check if the required mods are loaded
            if (dimensionTempData.requiredMods.isPresent())
            {
                List<String> requiredMods = dimensionTempData.requiredMods.get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }

            for (DimensionType dimension : dimensionTempData.dimensions)
            {
                if (dimensionTempData.isOffset)
                {   ConfigSettings.DIMENSION_OFFSETS.get(registryAccess).put(dimension, dimensionTempData);
                }
                else
                {   ConfigSettings.DIMENSION_TEMPS.get(registryAccess).put(dimension, dimensionTempData);
                }
            }
        });
    }

    private static void addStructureTempConfigs(Set<StructureTempData> structureTemps, DynamicRegistries registryAccess)
    {
        structureTemps.forEach(structureTempData ->
        {
            // Check if the required mods are loaded
            if (structureTempData.requiredMods.isPresent())
            {
                List<String> requiredMods = structureTempData.requiredMods.get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }
            for (Structure<?> structure : structureTempData.structures)
            {
                if (structureTempData.isOffset)
                {   ConfigSettings.STRUCTURE_OFFSETS.get(registryAccess).put(structure, structureTempData);
                }
                else
                {   ConfigSettings.STRUCTURE_TEMPS.get(registryAccess).put(structure, structureTempData);
                }
            }
        });
    }

    private static DepthTempData DEFAULT_REGION = null;

    private static void addDepthTempConfigs(Set<DepthTempData> depthTemps)
    {
        // If other depth temps are being registered, remove the default one
        if (depthTemps.size() > 2 || depthTemps.stream().noneMatch(temp -> temp.equals(DEFAULT_REGION)))
        {   ConfigSettings.DEPTH_REGIONS.get().remove(DEFAULT_REGION);
            depthTemps.removeIf(depthTemp -> depthTemp.equals(DEFAULT_REGION));
        }
        // Add the depth temps to the config
        for (DepthTempData depthTemp : depthTemps)
        {
            // Check if the required mods are loaded
            if (depthTemp.requiredMods.isPresent())
            {
                List<String> requiredMods = depthTemp.requiredMods.get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }
            ConfigSettings.DEPTH_REGIONS.get().add(depthTemp);
        }
    }

    private static void addMountConfigs(Set<MountData> mounts)
    {
        mounts.forEach(mountData ->
        {
            // Check if the required mods are loaded
            if (mountData.requiredMods.isPresent())
            {
                List<String> requiredMods = mountData.requiredMods.get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }
            List<EntityType<?>> entities = RegistryHelper.mapTaggableList(mountData.entities);
            for (EntityType<?> entity : entities)
            {   ConfigSettings.INSULATED_MOUNTS.get().put(entity, new MountData(entities, mountData.coldInsulation, mountData.heatInsulation, mountData.requirement));
            }
        });
    }

    private static void addSpawnBiomeConfigs(Set<SpawnBiomeData> spawnBiomes, DynamicRegistries registryAccess)
    {
        spawnBiomes.forEach(spawnBiomeData ->
        {
            // Check if the required mods are loaded
            if (spawnBiomeData.requiredMods.isPresent())
            {
                List<String> requiredMods = spawnBiomeData.requiredMods.get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }
            for (Biome biome : spawnBiomeData.biomes)
            {   ConfigSettings.ENTITY_SPAWN_BIOMES.get(registryAccess).put(biome, spawnBiomeData);
            }
        });
    }

    private static void addEntityTempConfigs(Set<EntityTempData> entityTemps)
    {
        entityTemps.forEach(entityTempData ->
        {
            // Check if the required mods are loaded
            if (entityTempData.requiredMods.isPresent())
            {
                List<String> requiredMods = entityTempData.requiredMods.get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }
            // Gather entity types and tags
            List<Either<ITag<EntityType<?>>, EntityType<?>>> types = new ArrayList<>();
            entityTempData.entity.entities.ifPresent(type -> types.addAll(type));

            for (EntityType<?> entity : RegistryHelper.mapTaggableList(types))
            {   ConfigSettings.ENTITY_TEMPERATURES.get().put(entity, entityTempData);
            }
        });
    }

    private static <T> Set<T> parseConfigData(RegistryKey<Registry<T>> registry, Codec<T> codec)
    {
        Set<T> output = new HashSet<>();

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
                    codec.parse(JsonOps.INSTANCE, JSONUtils.parse(reader))
                            .resultOrPartial(ColdSweat.LOGGER::error)
                            .ifPresent(insulator -> output.add(insulator));
                }
                catch (Exception e)
                {   ColdSweat.LOGGER.error(String.format("Failed to parse JSON config setting in %s: %s", registry.location(), file.getName()), e);
                }
            }
        }
        return output;
    }

    private static List<File> findFilesRecursive(File directory)
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
