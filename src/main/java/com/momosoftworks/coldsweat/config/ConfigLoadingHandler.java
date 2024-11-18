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
import com.momosoftworks.coldsweat.data.tag.ModDimensionTags;
import com.momosoftworks.coldsweat.data.tag.ModEffectTags;
import com.momosoftworks.coldsweat.data.tag.ModItemTags;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
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
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
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
    public static final Multimap<ResourceKey<Registry<?>>, RemoveRegistryData<?>> REMOVED_REGISTRIES = new FastMultiMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void loadConfigs(ServerConfigsLoadedEvent event)
    {
        ConfigSettings.clear();
        BlockTempRegistry.flush();
        getDefaultConfigs(event.getServer());

        RegistryAccess registryAccess = event.getServer().registryAccess();
        Multimap<ResourceKey<Registry<?>>, Holder<?>> registries = new FastMultiMap<>();

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
    public static Multimap<ResourceKey<Registry<?>>, Holder<?>> collectDataRegistries(RegistryAccess registryAccess)
    {
        if (registryAccess == null)
        {   ColdSweat.LOGGER.error("Failed to load registries from null RegistryAccess");
            return new FastMultiMap<>();
        }
        /*
         Add blocks from tags to configs
         */
        ConfigSettings.THERMAL_SOURCE_SPREAD_WHITELIST.get()
                .addAll(registryAccess.registryOrThrow(Registry.BLOCK_REGISTRY)
                        .getTag(ModBlockTags.HEARTH_SPREAD_WHITELIST).orElseThrow()
                        .stream().map(holder ->
                        {   ColdSweat.LOGGER.info("Adding block {} to hearth spread whitelist", holder.value());
                            return holder.value();
                        }).toList());

        ConfigSettings.THERMAL_SOURCE_SPREAD_BLACKLIST.get().
                addAll(registryAccess.registryOrThrow(Registry.BLOCK_REGISTRY)
                       .getTag(ModBlockTags.HEARTH_SPREAD_BLACKLIST).orElseThrow()
                       .stream().map(holder ->
                       {
                           ColdSweat.LOGGER.info("Adding block {} to hearth spread blacklist", holder.value());
                           return holder.value();
                       }).toList());

        ConfigSettings.SLEEP_CHECK_IGNORE_BLOCKS.get()
                .addAll(registryAccess.registryOrThrow(Registry.BLOCK_REGISTRY)
                        .getTag(ModBlockTags.IGNORE_SLEEP_CHECK).orElseThrow()
                        .stream().map(holder ->
                        {   ColdSweat.LOGGER.info("Disabling sleeping conditions check for block {}", holder.value());
                            return holder.value();
                        }).toList());

        ConfigSettings.LAMP_DIMENSIONS.get(registryAccess)
                .addAll(registryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY)
                        .getTag(ModDimensionTags.SOUL_LAMP_VALID).orElseThrow()
                        .stream().map(holder ->
                        {   ColdSweat.LOGGER.info("Enabling dimension {} for soulspring lamp", holder.value());
                            return holder.value();
                        }).toList());

        ConfigSettings.INSULATION_BLACKLIST.get()
                .addAll(registryAccess.registryOrThrow(Registry.ITEM_REGISTRY)
                        .getTag(ModItemTags.NOT_INSULATABLE).orElseThrow()
                        .stream().map(holder ->
                        {   ColdSweat.LOGGER.info("Adding item {} to insulation blacklist", holder.value());
                            return holder.value();
                        }).toList());

        ConfigSettings.HEARTH_POTION_BLACKLIST.get()
                .addAll(registryAccess.registryOrThrow(Registry.MOB_EFFECT_REGISTRY)
                        .getTag(ModEffectTags.HEARTH_BLACKLISTED).orElseThrow()
                        .stream().map(holder ->
                        {   ColdSweat.LOGGER.info("Adding effect {} to hearth potion blacklist", holder.value());
                            return holder.value();
                        }).toList());

        /*
         Fetch JSON registries
        */
        Multimap<ResourceKey<Registry<?>>, Holder<?>> registries = new FastMultiMap<>();
        for (Map.Entry<String, ModRegistries.RegistryHolder<?>> entry : ModRegistries.getRegistries().entrySet())
        {
            ResourceKey<Registry<?>> key = (ResourceKey) entry.getValue().registry();
            registries.putAll(key, registryAccess.registryOrThrow(key).holders().collect(Collectors.toSet()));
        }
        return registries;
    }

    /**
     * Loads JSON-based configs from the configs folder
     */
    public static Multimap<ResourceKey<Registry<?>>, Holder<?>> collectUserRegistries(RegistryAccess registryAccess)
    {
        if (registryAccess == null)
        {   ColdSweat.LOGGER.error("Failed to load registries from null RegistryAccess");
            return new FastMultiMap<>();
        }

        /*
         Parse user-defined JSON data from the configs folder
        */
        Multimap<ResourceKey<Registry<?>>, Holder<?>> registries = new FastMultiMap<>();
        for (Map.Entry<String, ModRegistries.RegistryHolder<?>> entry : ModRegistries.getRegistries().entrySet())
        {
            ResourceKey<Registry<?>> key = (ResourceKey) entry.getValue().registry();
            Codec<?> codec = entry.getValue().codec();
            registries.putAll(key, parseConfigData((ResourceKey) key, (Codec) codec));
        }
        return registries;
    }

    private static void logAndAddRegistries(RegistryAccess registryAccess, Multimap<ResourceKey<Registry<?>>, Holder<?>> registries)
    {
        // Clear the static map
        REMOVED_REGISTRIES.clear();
        // Gather registry removals & add them to the static map
        Set<Holder<RemoveRegistryData<?>>> removals = registryAccess.registryOrThrow(ModRegistries.REMOVE_REGISTRY_DATA).holders().collect(Collectors.toSet());
        removals.addAll(parseConfigData(ModRegistries.REMOVE_REGISTRY_DATA, RemoveRegistryData.CODEC));
        removals.forEach(holder ->
        {
            RemoveRegistryData<?> data = holder.value();
            ResourceKey<Registry<?>> key = data.registry();
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
        {
            if (entry instanceof Holder<?> holder)
            {   ColdSweat.LOGGER.info("{}", holder.value());
            }
            else
            {   ColdSweat.LOGGER.info("{}", entry);
            }
        }
    }

    private static void removeRegistries(Multimap<ResourceKey<Registry<?>>, Holder<?>> registries)
    {
        ColdSweat.LOGGER.info("Handling registry removals...");
        for (Map.Entry<ResourceKey<Registry<?>>, Collection<RemoveRegistryData<?>>> entry : REMOVED_REGISTRIES.asMap().entrySet())
        {
            removeEntries((Collection) entry.getValue(), (Collection) registries.get(entry.getKey()));
        }
    }

    private static <T> void removeEntries(Collection<RemoveRegistryData<T>> removals, Collection<T> registry)
    {
        for (RemoveRegistryData<T> data : removals)
        {   registry.removeIf(entry -> data.matches(((Holder<T>) entry).value()));
        }
    }

    public static <T> Collection<T> removeEntries(Collection<T> registries, ResourceKey<Registry<T>> registryName)
    {
        REMOVED_REGISTRIES.get((ResourceKey) registryName).forEach(data ->
        {
            RemoveRegistryData<T> removeData = ((RemoveRegistryData<T>) data);
            if (removeData.getRegistry() == registryName)
            {   registries.removeIf(removeData::matches);
            }
        });
        return registries;
    }

    public static <T> boolean isRemoved(T entry, ResourceKey<Registry<T>> registryName)
    {
        return REMOVED_REGISTRIES.get((ResourceKey) registryName).stream().anyMatch(data -> ((RemoveRegistryData<T>) data).matches(entry));
    }

    private static void getDefaultConfigs(MinecraftServer server)
    {
        DEFAULT_REGION = ConfigHelper.parseResource(server.getResourceManager(), new ResourceLocation(ColdSweat.MOD_ID, "cold_sweat/world/temp_region/default.json"), DepthTempData.CODEC).orElseThrow();
    }

    private static void addInsulatorConfigs(Set<Holder<InsulatorData>> insulators)
    {
        insulators.forEach(holder ->
        {
            InsulatorData insulator = holder.value();
            // Check if the required mods are loaded
            if (insulator.requiredMods().isPresent())
            {
                List<String> requiredMods = insulator.requiredMods().get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
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

    private static void addFuelConfigs(Set<Holder<FuelData>> fuels)
    {
        fuels.forEach(holder ->
        {
            FuelData fuelData = holder.value();
            // Check if the required mods are loaded
            if (fuelData.requiredMods().isPresent())
            {
                List<String> requiredMods = fuelData.requiredMods().get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
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

    private static void addFoodConfigs(Set<Holder<FoodData>> foods)
    {
        foods.forEach(holder ->
        {
            FoodData foodData = holder.value();
            // Check if the required mods are loaded
            if (foodData.requiredMods().isPresent())
            {
                List<String> requiredMods = foodData.requiredMods().get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
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

    private static void addCarryTempConfigs(Set<Holder<ItemCarryTempData>> carryTemps)
    {
        carryTemps.forEach(holder ->
        {
            ItemCarryTempData carryTempData = holder.value();
            // Check if the required mods are loaded
            if (carryTempData.requiredMods().isPresent())
            {
                List<String> requiredMods = carryTempData.requiredMods().get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }

            List<Item> items = new ArrayList<>();
            carryTempData.data().items().ifPresent(itemList ->
            {   items.addAll(RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ITEMS, itemList));
            });
            carryTempData.data().tag().ifPresent(tag ->
            {   items.addAll(ForgeRegistries.ITEMS.tags().getTag(tag).stream().toList());
            });
            for (Item item : items)
            {
                ConfigSettings.CARRIED_ITEM_TEMPERATURES.get().put(item, carryTempData);
            }
        });
    }

    private static void addBlockTempConfigs(Set<Holder<BlockTempData>> holders)
    {
        List<BlockTempData> blockTemps = new ArrayList<>(holders.stream().map(Holder::value).toList());
        // Handle entries removed by configs
        removeEntries(blockTemps, ModRegistries.BLOCK_TEMP_DATA);

        blockTemps.forEach(blockTempData ->
        {
            // Check if the required mods are loaded
            if (blockTempData.requiredMods().isPresent())
            {
                List<String> requiredMods = blockTempData.requiredMods().get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }
            Block[] blocks = RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.BLOCKS, blockTempData.blocks()).toArray(Block[]::new);
            BlockTemp blockTemp = new BlockTemp(blockTempData.temperature() < 0 ? -blockTempData.maxEffect() : -Double.MAX_VALUE,
                                                blockTempData.temperature() > 0 ? blockTempData.maxEffect() : Double.MAX_VALUE,
                                                blockTempData.minTemp(),
                                                blockTempData.maxTemp(),
                                                blockTempData.range(),
                                                blockTempData.fade(),
                                                blocks)
            {
                final double temperature = blockTempData.temperature();
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

    private static void addBiomeTempConfigs(Set<Holder<BiomeTempData>> biomeTemps, RegistryAccess registryAccess)
    {
        biomeTemps.forEach(holder ->
        {
            BiomeTempData biomeTempData = holder.value();
            // Check if the required mods are loaded
            if (biomeTempData.requiredMods().isPresent())
            {
                List<String> requiredMods = biomeTempData.requiredMods().get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }
            for (Holder<Biome> biome : RegistryHelper.mapVanillaRegistryTagList(Registry.BIOME_REGISTRY, biomeTempData.biomes(), registryAccess))
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

    private static void addDimensionTempConfigs(Set<Holder<DimensionTempData>> dimensionTemps, RegistryAccess registryAccess)
    {
        dimensionTemps.forEach(holder ->
        {
            DimensionTempData dimensionTempData = holder.value();
            // Check if the required mods are loaded
            if (dimensionTempData.requiredMods().isPresent())
            {
                List<String> requiredMods = dimensionTempData.requiredMods().get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }

            for (Holder<DimensionType> dimension : RegistryHelper.mapVanillaRegistryTagList(Registry.DIMENSION_TYPE_REGISTRY, dimensionTempData.dimensions(), registryAccess))
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

    private static void addStructureTempConfigs(Set<Holder<StructureTempData>> structureTemps, RegistryAccess registryAccess)
    {
        structureTemps.forEach(holder ->
        {
            StructureTempData structureTempData = holder.value();
            // Check if the required mods are loaded
            if (structureTempData.requiredMods().isPresent())
            {
                List<String> requiredMods = structureTempData.requiredMods().get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }
            for (Holder<ConfiguredStructureFeature<?, ?>> structure : RegistryHelper.mapVanillaRegistryTagList(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, structureTempData.structures(), registryAccess))
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

    private static DepthTempData DEFAULT_REGION = null;

    private static void addDepthTempConfigs(Set<Holder<DepthTempData>> depthTemps)
    {
        // If other depth temps are being registered, remove the default one
        if (depthTemps.size() > 2 || depthTemps.stream().noneMatch(temp -> temp.value().equals(DEFAULT_REGION)))
        {   ConfigSettings.DEPTH_REGIONS.get().remove(DEFAULT_REGION);
            depthTemps.removeIf(holder -> holder.value().equals(DEFAULT_REGION));
        }
        // Add the depth temps to the config
        for (Holder<DepthTempData> holder : depthTemps)
        {
            DepthTempData depthTemp = holder.value();
            // Check if the required mods are loaded
            if (depthTemp.requiredMods().isPresent())
            {
                List<String> requiredMods = depthTemp.requiredMods().get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }
            ConfigSettings.DEPTH_REGIONS.get().add(depthTemp);
        }
    }

    private static void addMountConfigs(Set<Holder<MountData>> mounts)
    {
        mounts.forEach(holder ->
        {
            MountData mountData = holder.value();
            // Check if the required mods are loaded
            if (mountData.requiredMods().isPresent())
            {
                List<String> requiredMods = mountData.requiredMods().get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }
            List<EntityType<?>> entities = RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ENTITIES, mountData.entities());
            for (EntityType<?> entity : entities)
            {   ConfigSettings.INSULATED_MOUNTS.get().put(entity, new MountData(entities, mountData.coldInsulation(), mountData.heatInsulation(), mountData.requirement()));
            }
        });
    }

    private static void addSpawnBiomeConfigs(Set<Holder<SpawnBiomeData>> spawnBiomes, RegistryAccess registryAccess)
    {
        spawnBiomes.forEach(holder ->
        {
            SpawnBiomeData spawnBiomeData = holder.value();
            // Check if the required mods are loaded
            if (spawnBiomeData.requiredMods().isPresent())
            {
                List<String> requiredMods = spawnBiomeData.requiredMods().get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }
            for (Holder<Biome> biome : RegistryHelper.mapVanillaRegistryTagList(Registry.BIOME_REGISTRY, spawnBiomeData.biomes(), registryAccess))
            {   ConfigSettings.ENTITY_SPAWN_BIOMES.get(registryAccess).put(biome, spawnBiomeData);
            }
        });
    }

    private static void addEntityTempConfigs(Set<Holder<EntityTempData>> entityTemps)
    {
        entityTemps.forEach(holder ->
        {
            EntityTempData entityTempData = holder.value();
            // Check if the required mods are loaded
            if (entityTempData.requiredMods().isPresent())
            {
                List<String> requiredMods = entityTempData.requiredMods().get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }
            // Gather entity types and tags
            List<Either<TagKey<EntityType<?>>, EntityType<?>>> types = new ArrayList<>();
            entityTempData.entity().entities().ifPresent(type -> types.addAll(type));

            for (EntityType<?> entity : RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ENTITIES, types))
            {   ConfigSettings.ENTITY_TEMPERATURES.get().put(entity, entityTempData);
            }
        });
    }

    private static <T> Set<Holder<T>> parseConfigData(ResourceKey<Registry<T>> registry, Codec<T> codec)
    {
        Set<Holder<T>> output = new HashSet<>();

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
                    codec.parse(JsonOps.INSTANCE, GsonHelper.parse(reader))
                            .resultOrPartial(ColdSweat.LOGGER::error)
                            .ifPresent(insulator -> output.add(Holder.direct(insulator)));
                }
                catch (Exception e)
                {   ColdSweat.LOGGER.error(String.format("Failed to parse JSON config setting in %s: %s", registry.location(), file.getName()), e);
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
