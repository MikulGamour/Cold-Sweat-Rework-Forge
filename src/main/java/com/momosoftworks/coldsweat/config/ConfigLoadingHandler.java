package com.momosoftworks.coldsweat.config;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.event.vanilla.ServerConfigsLoadedEvent;
import com.momosoftworks.coldsweat.api.registry.BlockTempRegistry;
import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTemp;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.type.CarriedItemTemperature;
import com.momosoftworks.coldsweat.config.type.InsulatingMount;
import com.momosoftworks.coldsweat.config.type.Insulator;
import com.momosoftworks.coldsweat.config.type.PredicateItem;
import com.momosoftworks.coldsweat.core.init.TempModifierInit;
import com.momosoftworks.coldsweat.data.ModRegistries;
import com.momosoftworks.coldsweat.data.codec.configuration.*;
import com.momosoftworks.coldsweat.data.codec.requirement.BlockRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import com.momosoftworks.coldsweat.data.tag.ModBlockTags;
import com.momosoftworks.coldsweat.data.tag.ModItemTags;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import com.momosoftworks.coldsweat.util.serialization.Triplet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.resources.IResource;
import net.minecraft.tags.ITag;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
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
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void loadConfigs(ServerConfigsLoadedEvent event)
    {
        ConfigSettings.clear();
        BlockTempRegistry.flush();
        getDefaultConfigs(event.getServer());

        DynamicRegistries registries = event.getServer().registryAccess();
        // User JSON configs (config folder)
        ColdSweat.LOGGER.info("Loading registries from configs...");
        collectUserRegistries(registries);

        // JSON configs (data resources)
        ColdSweat.LOGGER.info("Loading registries from data resources...");
        collectRegistries(registries);

        // User configs (TOML)
        ColdSweat.LOGGER.info("Loading TOML configs...");
        ConfigSettings.load(registries, false);
        TempModifierInit.buildBlockConfigs();

        // Post-toml Java BlockTemps
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
    public static void collectRegistries(DynamicRegistries registries)
    {
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
        // Create config entries from JSON files
        for (ModRegistries.CodecRegistry<?> registry : ModRegistries.getRegistries())
        {
            registry.flush();
            try
            {
                ResourceLocation registryPath = new ResourceLocation(ColdSweat.MOD_ID, "config/" + registry.getRegistryName().getPath());
                for (ResourceLocation resourceLocation : ModRegistries.getResourceManager().listResources(registryPath.getPath(), file -> file.endsWith(".json")))
                {
                    IResource resource = ModRegistries.getResourceManager().getResource(resourceLocation);
                    try (InputStream inputStream = resource.getInputStream())
                    {
                        // Create a reader from the input stream
                        registry.getCodec().parse(JsonOps.INSTANCE, JSONUtils.parse(new InputStreamReader(inputStream)))
                                .resultOrPartial(ColdSweat.LOGGER::error)
                                .ifPresent(insulator -> registry.register(insulator));
                    }
                    catch (Exception e)
                    {   ColdSweat.LOGGER.error("Failed to load JSON registry: {}", registry.getRegistryName(), e);
                    }
                }
            }
            catch (IOException ignored) {}
        }

        Set<InsulatorData> insulators = new HashSet<>(ModRegistries.INSULATOR_DATA.getValues());
        Set<FuelData> fuels = new HashSet<>(ModRegistries.FUEL_DATA.getValues());
        Set<FoodData> foods = new HashSet<>(ModRegistries.FOOD_DATA.getValues());
        Set<ItemCarryTempData> carryTemps = new HashSet<>(ModRegistries.CARRY_TEMP_DATA.getValues());

        Set<BlockTempData> blockTemps = new HashSet<>(ModRegistries.BLOCK_TEMP_DATA.getValues());
        Set<BiomeTempData> biomeTemps = new HashSet<>(ModRegistries.BIOME_TEMP_DATA.getValues());
        Set<DimensionTempData> dimensionTemps = new HashSet<>(ModRegistries.DIMENSION_TEMP_DATA.getValues());
        Set<StructureTempData> structureTemps = new HashSet<>(ModRegistries.STRUCTURE_TEMP_DATA.getValues());
        Set<DepthTempData> depthTemps = new HashSet<>(ModRegistries.DEPTH_TEMP_DATA.getValues());

        Set<MountData> mounts = new HashSet<>(ModRegistries.MOUNT_DATA.getValues());
        Set<SpawnBiomeData> spawnBiomes = new HashSet<>(ModRegistries.ENTITY_SPAWN_BIOME_DATA.getValues());
        Set<EntityTempData> entityTemps = new HashSet<>(ModRegistries.ENTITY_TEMP_DATA.getValues());

        logAndAddRegistries(registries, insulators, fuels, foods, carryTemps, blockTemps, biomeTemps,
                            dimensionTemps, structureTemps, depthTemps, mounts, spawnBiomes, entityTemps);
    }

    /**
     * Loads JSON-based configs from the configs folder
     */
    public static void collectUserRegistries(DynamicRegistries registries)
    {
        if (registries == null)
        {   ColdSweat.LOGGER.error("Failed to load registries from null RegistryAccess");
            return;
        }

        /*
         Parse user-defined JSON data from the configs folder
        */
        Set<InsulatorData> insulators = new HashSet<>(parseConfigData(ModRegistries.INSULATOR_DATA, InsulatorData.CODEC));
        Set<FuelData> fuels = new HashSet<>(parseConfigData(ModRegistries.FUEL_DATA, FuelData.CODEC));
        Set<FoodData> foods = new HashSet<>(parseConfigData(ModRegistries.FOOD_DATA, FoodData.CODEC));
        Set<ItemCarryTempData> carryTemps = new HashSet<>(parseConfigData(ModRegistries.CARRY_TEMP_DATA, ItemCarryTempData.CODEC));

        Set<BlockTempData> blockTemps = new HashSet<>(parseConfigData(ModRegistries.BLOCK_TEMP_DATA, BlockTempData.CODEC));
        Set<BiomeTempData> biomeTemps = new HashSet<>(parseConfigData(ModRegistries.BIOME_TEMP_DATA, BiomeTempData.CODEC));
        Set<DimensionTempData> dimensionTemps = new HashSet<>(parseConfigData(ModRegistries.DIMENSION_TEMP_DATA, DimensionTempData.CODEC));
        Set<StructureTempData> structureTemps = new HashSet<>(parseConfigData(ModRegistries.STRUCTURE_TEMP_DATA, StructureTempData.CODEC));
        Set<DepthTempData> depthTemps = new HashSet<>(parseConfigData(ModRegistries.DEPTH_TEMP_DATA, DepthTempData.CODEC));

        Set<MountData> mounts = new HashSet<>(parseConfigData(ModRegistries.MOUNT_DATA, MountData.CODEC));
        Set<SpawnBiomeData> spawnBiomes = new HashSet<>(parseConfigData(ModRegistries.ENTITY_SPAWN_BIOME_DATA, SpawnBiomeData.CODEC));
        Set<EntityTempData> entityTemps = new HashSet<>(parseConfigData(ModRegistries.ENTITY_TEMP_DATA, EntityTempData.CODEC));

        logAndAddRegistries(registries, insulators, fuels, foods, carryTemps, blockTemps, biomeTemps,
                            dimensionTemps, structureTemps, depthTemps, mounts, spawnBiomes, entityTemps);
    }

    private static void logAndAddRegistries(DynamicRegistries registries, Set<InsulatorData> insulators,
                                            Set<FuelData> fuels,
                                            Set<FoodData> foods,
                                            Set<ItemCarryTempData> carryTemps, Set<BlockTempData> blockTemps,
                                            Set<BiomeTempData> biomeTemps, Set<DimensionTempData> dimensionTemps,
                                            Set<StructureTempData> structureTemps, Set<DepthTempData> depthTemps,
                                            Set<MountData> mounts, Set<SpawnBiomeData> spawnBiomes,
                                            Set<EntityTempData> entityTemps)
    {
        /*
         Add JSON data to the config settings
         */
        // insulators
        addInsulatorConfigs(insulators);
        logRegistryLoaded(String.format("Loaded %s insulators", insulators.size()), insulators);
        // fuels
        addFuelConfigs(fuels);
        logRegistryLoaded(String.format("Loaded %s fuels", fuels.size()), fuels);
        // foods
        addFoodConfigs(foods);
        logRegistryLoaded(String.format("Loaded %s foods", foods.size()), foods);
        // carry temperatures
        addCarryTempConfigs(carryTemps);
        logRegistryLoaded(String.format("Loaded %s carried item temperatures", carryTemps.size()), carryTemps);

        // block temperatures
        addBlockTempConfigs(blockTemps);
        logRegistryLoaded(String.format("Loaded %s block temperatures", blockTemps.size()), blockTemps);
        // biome temperatures
        addBiomeTempConfigs(biomeTemps, registries);
        logRegistryLoaded(String.format("Loaded %s biome temperatures", biomeTemps.size()), biomeTemps);
        // dimension temperatures
        addDimensionTempConfigs(dimensionTemps, registries);
        logRegistryLoaded(String.format("Loaded %s dimension temperatures", dimensionTemps.size()), dimensionTemps);
        // structure temperatures
        addStructureTempConfigs(structureTemps, registries);
        logRegistryLoaded(String.format("Loaded %s structure temperatures", structureTemps.size()), structureTemps);
        // depth temperatures
        addDepthTempConfigs(depthTemps);
        logRegistryLoaded(String.format("Loaded %s depth temperatures", depthTemps.size()), depthTemps);

        // mounts
        addMountConfigs(mounts);
        logRegistryLoaded(String.format("Loaded %s insulated mounts", mounts.size()), mounts);
        // spawn biomes
        addSpawnBiomeConfigs(spawnBiomes, registries);
        logRegistryLoaded(String.format("Loaded %s entity spawn biomes", spawnBiomes.size()), spawnBiomes);
        // entity temperatures
        addEntityTempConfigs(entityTemps);
        logRegistryLoaded(String.format("Loaded %s entity temperatures", entityTemps.size()), entityTemps);
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

    private static void getDefaultConfigs(MinecraftServer server)
    {
        DEFAULT_REGION = ConfigHelper.parseResource(server.getDataPackRegistries().getResourceManager(), new ResourceLocation(ColdSweat.MOD_ID, "config/world/temp_region/default.json"), DepthTempData.CODEC).orElseThrow(RuntimeException::new);
    }

    private static void addInsulatorConfigs(Set<InsulatorData> insulators)
    {
        insulators.forEach(insulatorData ->
        {
            // Check if the required mods are loaded
            if (insulatorData.requiredMods.isPresent())
            {
                List<String> requiredMods = insulatorData.requiredMods.get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }
            AttributeModifierMap attributeModifiers = insulatorData.attributes.orElse(new AttributeModifierMap());
            Insulator insulator = new Insulator(insulatorData.insulation, insulatorData.slot, insulatorData.data,
                                                insulatorData.predicate, attributeModifiers, insulatorData.immuneTempModifiers);

            // Add listed items as insulators
            List<Item> items = new ArrayList<>();
            insulatorData.data.items.ifPresent(itemList ->
            {   items.addAll(RegistryHelper.mapTaggableList(itemList));
            });
            insulatorData.data.tag.ifPresent(tag ->
            {   items.addAll(tag.getValues());
            });

            for (Item item : items)
            {
                switch (insulatorData.slot)
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

            FuelData.FuelType type = fuelData.type;
            ItemRequirement data = fuelData.data;
            double fuel = fuelData.fuel;

            PredicateItem predicateItem = new PredicateItem(fuel, data, EntityRequirement.NONE);

            List<Item> items = new ArrayList<>();
            fuelData.data.items.ifPresent(itemList ->
            {   items.addAll(RegistryHelper.mapTaggableList(itemList));
            });
            fuelData.data.tag.ifPresent(tag ->
            {   items.addAll(tag.getValues());
            });

            for (Item item : items)
            {
                switch (type)
                {
                    case BOILER : ConfigSettings.BOILER_FUEL.get().put(item, predicateItem); break;
                    case ICEBOX : ConfigSettings.ICEBOX_FUEL.get().put(item, predicateItem); break;
                    case HEARTH : ConfigSettings.HEARTH_FUEL.get().put(item, predicateItem); break;
                    case SOUL_LAMP : ConfigSettings.SOULSPRING_LAMP_FUEL.get().put(item, predicateItem); break;
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

            EntityRequirement predicate = foodData.entityRequirement.orElse(EntityRequirement.NONE);
            CompoundNBT extraData = null;
            if (foodData.duration.isPresent())
            {
                extraData = new CompoundNBT();
                extraData.putInt("duration", foodData.duration.get());
            }

            PredicateItem predicateItem = new PredicateItem(foodData.value, foodData.data, predicate, extraData);

            List<Item> items = new ArrayList<>();
            foodData.data.items.ifPresent(itemList ->
            {   items.addAll(RegistryHelper.mapTaggableList(itemList));
            });
            foodData.data.tag.ifPresent(tag ->
            {   items.addAll(tag.getValues());
            });

            for (Item item : items)
            {
                ConfigSettings.FOOD_TEMPERATURES.get().put(item, predicateItem);
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

            CarriedItemTemperature carriedItemTemperature = CarriedItemTemperature.createFromData(carryTempData);
            List<Either<ITag<Item>, Item>> items = CSMath.orElse(carryTempData.data.items.orElse(null),
                                                                 Arrays.asList(Either.left(carryTempData.data.tag.orElse(null))),
                                                                 Arrays.asList());
            for (Item item : RegistryHelper.mapTaggableList(items))
            {
                ConfigSettings.CARRIED_ITEM_TEMPERATURES.get().put(item, carriedItemTemperature);
            }
        });
    }

    private static void addBlockTempConfigs(Set<BlockTempData> blockTemps)
    {
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
                Temperature.Units units = biomeTempData.units;
                if (biomeTempData.isOffset)
                {   ConfigSettings.BIOME_OFFSETS.get(registryAccess).put(biome, new Triplet<>(Temperature.convert(biomeTempData.min, units, Temperature.Units.MC, true),
                                                                                Temperature.convert(biomeTempData.max, units, Temperature.Units.MC, true),
                                                                                biomeTempData.units));
                }
                else
                {   ConfigSettings.BIOME_TEMPS.get(registryAccess).put(biome, new Triplet<>(Temperature.convert(biomeTempData.min, units, Temperature.Units.MC, true),
                                                                              Temperature.convert(biomeTempData.max, units, Temperature.Units.MC, true),
                                                                              biomeTempData.units));
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
                Temperature.Units units = dimensionTempData.units;
                if (dimensionTempData.isOffset)
                {   ConfigSettings.DIMENSION_OFFSETS.get(registryAccess).put(dimension, Pair.of(Temperature.convert(dimensionTempData.temperature, units, Temperature.Units.MC, true),
                                                                                  dimensionTempData.units));
                }
                else
                {   ConfigSettings.DIMENSION_TEMPS.get(registryAccess).put(dimension, Pair.of(Temperature.convert(dimensionTempData.temperature, units, Temperature.Units.MC, true),
                                                                                dimensionTempData.units));
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
                double temperature = Temperature.convert(structureTempData.temperature, structureTempData.units, Temperature.Units.MC, !structureTempData.isOffset);
                if (structureTempData.isOffset)
                {   ConfigSettings.STRUCTURE_OFFSETS.get(registryAccess).put(structure, Pair.of(temperature, structureTempData.units));
                }
                else
                {   ConfigSettings.STRUCTURE_TEMPS.get(registryAccess).put(structure, Pair.of(temperature, structureTempData.units));
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
            for (EntityType<?> entity : RegistryHelper.mapTaggableList(mountData.entities))
            {   ConfigSettings.INSULATED_ENTITIES.get().put(entity, new InsulatingMount(entity, mountData.coldInsulation, mountData.heatInsulation, mountData.requirement));
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
            entityTempData.entity.type.ifPresent(type -> types.add(Either.right(type)));
            entityTempData.entity.tag.ifPresent(tag -> types.add(Either.left(tag)));

            for (EntityType<?> entity : RegistryHelper.mapTaggableList(types))
            {   ConfigSettings.ENTITY_TEMPERATURES.get().put(entity, entityTempData);
            }
        });
    }

    private static <T> Set<T> parseConfigData(ModRegistries.CodecRegistry<T> registry, Codec<T> codec)
    {
        Set<T> output = new HashSet<>();

        Path coldSweatDataPath = FMLPaths.CONFIGDIR.get().resolve("coldsweat/data").resolve(registry.getRegistryName().getPath());
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
                {   ColdSweat.LOGGER.error(String.format("Failed to parse JSON config setting in %s: %s", registry.getRegistryName(), file.getName()), e);
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
