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
import com.momosoftworks.coldsweat.core.init.TempModifierInit;
import com.momosoftworks.coldsweat.data.ModRegistries;
import com.momosoftworks.coldsweat.data.codec.configuration.*;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.requirement.BlockRequirement;
import com.momosoftworks.coldsweat.data.tag.ModBlockTags;
import com.momosoftworks.coldsweat.data.tag.ModDimensionTags;
import com.momosoftworks.coldsweat.data.tag.ModEffectTags;
import com.momosoftworks.coldsweat.data.tag.ModItemTags;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber
public class ConfigLoadingHandler
{
    public static final Multimap<ResourceKey<Registry<? extends ConfigData>>, RemoveRegistryData<?>> REMOVED_REGISTRIES = new FastMultiMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void loadConfigs(ServerConfigsLoadedEvent event)
    {
        ConfigSettings.clear();
        BlockTempRegistry.flush();

        RegistryAccess registryAccess = event.getServer().registryAccess();
        Multimap<ResourceKey<Registry<? extends ConfigData>>, Holder<? extends ConfigData>> registries = new FastMultiMap<>();

        // User JSON configs (config folder)
        ColdSweat.LOGGER.info("Loading registries from configs...");
        registries.putAll(collectUserRegistries(registryAccess));

        // JSON configs (data resources)
        ColdSweat.LOGGER.info("Loading registries from data resources...");
        registries.putAll(collectDataRegistries(registryAccess));

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
    public static Multimap<ResourceKey<Registry<? extends ConfigData>>, Holder<? extends ConfigData>> collectDataRegistries(RegistryAccess registryAccess)
    {
        if (registryAccess == null)
        {   ColdSweat.LOGGER.error("Failed to load registries from null RegistryAccess");
            return new FastMultiMap<>();
        }
        /*
         Read mod-related tags for config settings
         */
        ConfigSettings.THERMAL_SOURCE_SPREAD_WHITELIST.get()
                .addAll(registryAccess.registryOrThrow(Registries.BLOCK)
                        .getTag(ModBlockTags.HEARTH_SPREAD_WHITELIST).orElseThrow()
                        .stream().map(holder ->
                        {   ColdSweat.LOGGER.info("Adding block {} to hearth spread whitelist", holder.get());
                            return holder.get();
                        }).toList());

        ConfigSettings.THERMAL_SOURCE_SPREAD_BLACKLIST.get().
                addAll(registryAccess.registryOrThrow(Registries.BLOCK)
                       .getTag(ModBlockTags.HEARTH_SPREAD_BLACKLIST).orElseThrow()
                       .stream().map(holder ->
                       {
                           ColdSweat.LOGGER.info("Adding block {} to hearth spread blacklist", holder.get());
                           return holder.get();
                       }).toList());

        ConfigSettings.SLEEP_CHECK_IGNORE_BLOCKS.get()
                .addAll(registryAccess.registryOrThrow(Registries.BLOCK)
                        .getTag(ModBlockTags.IGNORE_SLEEP_CHECK).orElseThrow()
                        .stream().map(holder ->
                        {   ColdSweat.LOGGER.info("Disabling sleeping conditions check for block {}", holder.get());
                            return holder.get();
                        }).toList());

        ConfigSettings.LAMP_DIMENSIONS.get(registryAccess)
                .addAll(registryAccess.registryOrThrow(Registries.DIMENSION_TYPE)
                        .getTag(ModDimensionTags.SOUL_LAMP_VALID).orElseThrow()
                        .stream().map(holder ->
                        {   ColdSweat.LOGGER.info("Enabling dimension {} for soulspring lamp", holder.value());
                            return holder.value();
                        }).toList());

        ConfigSettings.INSULATION_BLACKLIST.get()
                .addAll(registryAccess.registryOrThrow(Registries.ITEM)
                        .getTag(ModItemTags.NOT_INSULATABLE).orElseThrow()
                        .stream().map(holder ->
                        {   ColdSweat.LOGGER.info("Adding item {} to insulation blacklist", holder.get());
                            return holder.get();
                        }).toList());

        ConfigSettings.HEARTH_POTION_BLACKLIST.get()
                .addAll(registryAccess.registryOrThrow(Registries.MOB_EFFECT)
                        .getTag(ModEffectTags.HEARTH_BLACKLISTED).orElseThrow()
                        .stream().map(holder ->
                        {   ColdSweat.LOGGER.info("Adding effect {} to hearth potion blacklist", holder.get());
                            return holder.get();
                        }).toList());

        /*
         Fetch JSON registries
        */
        Multimap<ResourceKey<Registry<? extends ConfigData>>, Holder<? extends ConfigData>> registries = new FastMultiMap<>();
        for (Map.Entry<String, ModRegistries.RegistryHolder<?>> entry : ModRegistries.getRegistries().entrySet())
        {
            ResourceKey<Registry<? extends ConfigData>> key = (ResourceKey) entry.getValue().registry();
            registries.putAll(key, registryAccess.registryOrThrow(key).holders().collect(Collectors.toSet()));
        }
        return registries;
    }

    /**
     * Loads JSON-based configs from the configs folder
     */
    public static Multimap<ResourceKey<Registry<? extends ConfigData>>, Holder<? extends ConfigData>> collectUserRegistries(RegistryAccess registryAccess)
    {
        if (registryAccess == null)
        {   ColdSweat.LOGGER.error("Failed to load registries from null RegistryAccess");
            return new FastMultiMap<>();
        }

        /*
         Parse user-defined JSON data from the configs folder
        */
        Multimap<ResourceKey<Registry<? extends ConfigData>>, Holder<? extends ConfigData>> registries = new FastMultiMap<>();
        for (Map.Entry<String, ModRegistries.RegistryHolder<?>> entry : ModRegistries.getRegistries().entrySet())
        {
            ResourceKey<Registry<? extends ConfigData>> key = (ResourceKey) entry.getValue().registry();
            Codec<?> codec = entry.getValue().codec();
            registries.putAll(key, parseConfigData((ResourceKey) key, (Codec) codec, registryAccess));
        }
        return registries;
    }

    private static void logAndAddRegistries(RegistryAccess registryAccess, Multimap<ResourceKey<Registry<? extends ConfigData>>, Holder<? extends ConfigData>> registries)
    {
        // Ensure default registry entries load last
        setDefaultRegistryPriority(registries);

        // Load registry removals
        loadRegistryRemovals(registryAccess);

        // Mark holders as "JSON"
        for (Holder<? extends ConfigData> holder : registries.values())
        {   holder.value().setType(ConfigData.Type.JSON);
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
        {
            if (entry instanceof Holder<?> holder)
            {   ColdSweat.LOGGER.info("{}", holder.get());
            }
            else
            {   ColdSweat.LOGGER.info("{}", entry);
            }
        }
    }

    private static void setDefaultRegistryPriority(Multimap<ResourceKey<Registry<? extends ConfigData>>, Holder<? extends ConfigData>> registries)
    {
        for (ResourceKey<Registry<? extends ConfigData>> key : registries.keySet())
        {
            List<Holder<? extends ConfigData>> sortedHolders = new ArrayList<>(registries.get(key));
            sortedHolders.sort(Comparator.comparing(holder ->
            {   return holder.unwrapKey().map(k -> k.location().getPath().equals("default") ? 1 : 0).orElse(0);
            }));
            registries.replaceValues(key, sortedHolders);
        }
    }

    private static void loadRegistryRemovals(RegistryAccess registryAccess)
    {
        // Clear the static map
        REMOVED_REGISTRIES.clear();
        // Gather registry removals & add them to the static map
        Set<Holder<RemoveRegistryData<?>>> removals = registryAccess.registryOrThrow(ModRegistries.REMOVE_REGISTRY_DATA).holders().collect(Collectors.toSet());
        removals.addAll(parseConfigData(ModRegistries.REMOVE_REGISTRY_DATA, RemoveRegistryData.CODEC, registryAccess));
        removals.forEach(holder ->
        {
            RemoveRegistryData<?> data = holder.get();
            ResourceKey<Registry<? extends ConfigData>> key = (ResourceKey) data.registry();
            REMOVED_REGISTRIES.put(key, data);
        });
    }

    private static void removeRegistries(Multimap<ResourceKey<Registry<? extends ConfigData>>, Holder<? extends ConfigData>> registries)
    {
        ColdSweat.LOGGER.info("Handling registry removals...");
        for (Map.Entry<ResourceKey<Registry<? extends ConfigData>>, Collection<RemoveRegistryData<? extends ConfigData>>> entry : REMOVED_REGISTRIES.asMap().entrySet())
        {
            removeEntries((Collection) entry.getValue(), (Collection) registries.get(entry.getKey()));
        }
    }

    private static <T extends ConfigData> void removeEntries(Collection<RemoveRegistryData<T>> removals, Collection<T> registry)
    {
        for (RemoveRegistryData<T> data : removals)
        {   registry.removeIf(data::matches);
        }
    }

    public static <T extends ConfigData> Collection<T> removeEntries(Collection<T> registries, ResourceKey<Registry<T>> registryName)
    {
        REMOVED_REGISTRIES.get((ResourceKey) registryName).forEach(data ->
        {
            RemoveRegistryData<T> removeData = ((RemoveRegistryData<T>) data);
            if (removeData.registry() == registryName)
            {   registries.removeIf(removeData::matches);
            }
        });
        return registries;
    }

    public static <T extends ConfigData> boolean isRemoved(T entry, ResourceKey<Registry<T>> registryName)
    {
        return REMOVED_REGISTRIES.get((ResourceKey) registryName).stream().anyMatch(data -> ((RemoveRegistryData<T>) data).matches(entry));
    }

    private static void addInsulatorConfigs(Collection<Holder<InsulatorData>> insulators)
    {
        insulators.forEach(holder ->
        {
            InsulatorData insulator = holder.get();
            // Check if the required mods are loaded
            if (!insulator.areRequiredModsLoaded())
            {   return;
            }

            // Add listed items as insulators
            List<Item> items = new ArrayList<>();
            insulator.data().items().ifPresent(itemList ->
            {   items.addAll(RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ITEMS, itemList));
            });
            insulator.data().tag().ifPresent(tag ->
            {   items.addAll(ForgeRegistries.ITEMS.tags().getTag(tag).stream().toList());
            });

            for (Item item : items)
            {
                switch (insulator.slot())
                {
                    case ITEM -> ConfigSettings.INSULATION_ITEMS.get().put(item, insulator);
                    case ARMOR -> ConfigSettings.INSULATING_ARMORS.get().put(item, insulator);
                    case CURIO ->
                    {
                        if (CompatManager.isCuriosLoaded())
                        {   ConfigSettings.INSULATING_CURIOS.get().put(item, insulator);
                        }
                    }
                }
            }
        });
    }

    private static void addFuelConfigs(Collection<Holder<FuelData>> fuels)
    {
        fuels.forEach(holder ->
        {
            FuelData fuelData = holder.get();
            // Check if the required mods are loaded
            if (!fuelData.areRequiredModsLoaded())
            {   return;
            }

            List<Item> items = new ArrayList<>();
            fuelData.data().items().ifPresent(itemList ->
            {   items.addAll(RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ITEMS, itemList));
            });
            fuelData.data().tag().ifPresent(tag ->
            {   items.addAll(ForgeRegistries.ITEMS.tags().getTag(tag).stream().toList());
            });

            for (Item item : items)
            {
                switch (fuelData.type())
                {
                    case BOILER -> ConfigSettings.BOILER_FUEL.get().put(item, fuelData);
                    case ICEBOX -> ConfigSettings.ICEBOX_FUEL.get().put(item, fuelData);
                    case HEARTH -> ConfigSettings.HEARTH_FUEL.get().put(item, fuelData);
                    case SOUL_LAMP -> ConfigSettings.SOULSPRING_LAMP_FUEL.get().put(item, fuelData);
                }
            }
        });
    }

    private static void addFoodConfigs(Collection<Holder<FoodData>> foods)
    {
        foods.forEach(holder ->
        {
            FoodData foodData = holder.get();
            // Check if the required mods are loaded
            if (!foodData.areRequiredModsLoaded())
            {   return;
            }

            List<Item> items = new ArrayList<>();
            foodData.data().items().ifPresent(itemList ->
            {   items.addAll(RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ITEMS, itemList));
            });
            foodData.data().tag().ifPresent(tag ->
            {   items.addAll(ForgeRegistries.ITEMS.tags().getTag(tag).stream().toList());
            });

            for (Item item : items)
            {   ConfigSettings.FOOD_TEMPERATURES.get().put(item, foodData);
            }
        });
    }

    private static void addCarryTempConfigs(Collection<Holder<ItemCarryTempData>> carryTemps)
    {
        carryTemps.forEach(holder ->
        {
            ItemCarryTempData carryTempData = holder.get();
            // Check if the required mods are loaded
            if (!carryTempData.areRequiredModsLoaded())
            {   return;
            }

            List<Item> items = new ArrayList<>();
            carryTempData.data().items().ifPresent(itemList ->
            {   items.addAll(RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ITEMS, itemList));
            });
            carryTempData.data().tag().ifPresent(tag ->
            {   items.addAll(ForgeRegistries.ITEMS.tags().getTag(tag).stream().toList());
            });
            for (Item item : items)
            {   ConfigSettings.CARRIED_ITEM_TEMPERATURES.get().put(item, carryTempData);
            }
        });
    }

    private static void addBlockTempConfigs(Collection<Holder<BlockTempData>> blockTemps)
    {
        blockTemps.forEach(holder ->
        {
            BlockTempData blockTempData = holder.get();
            // Check if the required mods are loaded
            if (!blockTempData.areRequiredModsLoaded())
            {   return;
            }
            Block[] blocks = RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.BLOCKS, blockTempData.blocks()).toArray(Block[]::new);
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
                public double getTemperature(Level level, LivingEntity entity, BlockState state, BlockPos pos, double distance)
                {
                    if (level instanceof ServerLevel serverLevel)
                    {
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

    private static void addBiomeTempConfigs(Collection<Holder<BiomeTempData>> biomeTemps, RegistryAccess registryAccess)
    {
        biomeTemps.forEach(holder ->
        {
            BiomeTempData biomeTempData = holder.get();
            // Check if the required mods are loaded
            if (!biomeTempData.areRequiredModsLoaded())
            {   return;
            }
            for (Holder<Biome> biome : RegistryHelper.mapVanillaRegistryTagList(Registries.BIOME, biomeTempData.biomes(), registryAccess))
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

    private static void addDimensionTempConfigs(Collection<Holder<DimensionTempData>> dimensionTemps, RegistryAccess registryAccess)
    {
        dimensionTemps.forEach(holder ->
        {
            DimensionTempData dimensionTempData = holder.get();
            // Check if the required mods are loaded
            if (!dimensionTempData.areRequiredModsLoaded())
            {   return;
            }

            for (Holder<DimensionType> dimension : RegistryHelper.mapVanillaRegistryTagList(Registries.DIMENSION_TYPE, dimensionTempData.dimensions(), registryAccess))
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

    private static void addStructureTempConfigs(Collection<Holder<StructureTempData>> structureTemps, RegistryAccess registryAccess)
    {
        structureTemps.forEach(holder ->
        {
            StructureTempData structureTempData = holder.get();
            // Check if the required mods are loaded
            if (!structureTempData.areRequiredModsLoaded())
            {   return;
            }
            for (Holder<Structure> structure : RegistryHelper.mapVanillaRegistryTagList(Registries.STRUCTURE, structureTempData.structures(), registryAccess))
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

    private static void addDepthTempConfigs(Collection<Holder<DepthTempData>> depthTemps)
    {
        // Add the depth temps to the config
        for (Holder<DepthTempData> holder : depthTemps)
        {
            DepthTempData depthData = holder.value();
            // Check if the required mods are loaded
            if (!depthData.areRequiredModsLoaded())
            {   return;
            }
            ConfigSettings.DEPTH_REGIONS.get().add(depthData);
        }
    }

    private static void addMountConfigs(Collection<Holder<MountData>> mounts)
    {
        mounts.forEach(holder ->
        {
            MountData mountData = holder.get();
            // Check if the required mods are loaded
            if (!mountData.areRequiredModsLoaded())
            {   return;
            }
            List<EntityType<?>> entities = RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ENTITY_TYPES, mountData.entityData().entities().orElse(List.of()));
            for (EntityType<?> entity : entities)
            {   ConfigSettings.INSULATED_MOUNTS.get().put(entity, mountData);
            }
        });
    }

    private static void addSpawnBiomeConfigs(Collection<Holder<SpawnBiomeData>> spawnBiomes, RegistryAccess registryAccess)
    {
        spawnBiomes.forEach(holder ->
        {
            SpawnBiomeData spawnBiomeData = holder.get();
            // Check if the required mods are loaded
            if (!spawnBiomeData.areRequiredModsLoaded())
            {   return;
            }
            for (Holder<Biome> biome : RegistryHelper.mapVanillaRegistryTagList(Registries.BIOME, spawnBiomeData.biomes(), registryAccess))
            {   ConfigSettings.ENTITY_SPAWN_BIOMES.get(registryAccess).put(biome, spawnBiomeData);
            }
        });
    }

    private static void addEntityTempConfigs(Collection<Holder<EntityTempData>> entityTemps)
    {
        entityTemps.forEach(holder ->
        {
            EntityTempData entityTempData = holder.get();
            // Check if the required mods are loaded
            if (!entityTempData.areRequiredModsLoaded())
            {   return;
            }
            // Gather entity types and tags
            List<Either<TagKey<EntityType<?>>, EntityType<?>>> types = new ArrayList<>();
            entityTempData.entity().entities().ifPresent(type -> types.addAll(type));

            for (EntityType<?> entity : RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ENTITY_TYPES, types))
            {   ConfigSettings.ENTITY_TEMPERATURES.get().put(entity, entityTempData);
            }
        });
    }

    private static <T> List<Holder<T>> parseConfigData(ResourceKey<Registry<T>> registry, Codec<T> codec, RegistryAccess registryAccess)
    {
        List<Holder<T>> output = new ArrayList<>();
        DynamicOps<JsonElement> registryOps = RegistryOps.create(JsonOps.INSTANCE, registryAccess);

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
                    codec.decode(registryOps, GsonHelper.parse(reader))
                            .resultOrPartial(ColdSweat.LOGGER::error)
                            .map(Pair::getFirst)
                            .ifPresent(insulator -> output.add(Holder.direct(insulator)));
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
