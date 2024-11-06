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
import com.momosoftworks.coldsweat.data.tag.ModDimensionTags;
import com.momosoftworks.coldsweat.data.tag.ModEffectTags;
import com.momosoftworks.coldsweat.data.tag.ModItemTags;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.math.CSMath;
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
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import oshi.util.tuples.Triplet;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

        RegistryAccess registries = event.getServer().registryAccess();

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
    public static void collectRegistries(RegistryAccess registries)
    {
        /*
         Add blocks from tags to configs
         */
        ConfigSettings.HEARTH_SPREAD_WHITELIST.get().addAll(registries.registryOrThrow(Registry.BLOCK_REGISTRY)
                                                                    .getTag(ModBlockTags.HEARTH_SPREAD_WHITELIST).orElseThrow()
                                                                    .stream().map(holder ->
                                                                                  {   ColdSweat.LOGGER.info("Adding block {} to hearth spread whitelist", holder.value());
                                                                                      return holder.value();
                                                                                  }).toList());
        ConfigSettings.HEARTH_SPREAD_BLACKLIST.get().addAll(registries.registryOrThrow(Registry.BLOCK_REGISTRY)
                                                                    .getTag(ModBlockTags.HEARTH_SPREAD_BLACKLIST).orElseThrow()
                                                                    .stream().map(holder ->
                                                                                  {
                                                                                      ColdSweat.LOGGER.info("Adding block {} to hearth spread blacklist", holder.value());
                                                                                      return holder.value();
                                                                                  }).toList());
        ConfigSettings.SLEEP_CHECK_IGNORE_BLOCKS.get().addAll(registries.registryOrThrow(Registry.BLOCK_REGISTRY)
                                                                      .getTag(ModBlockTags.IGNORE_SLEEP_CHECK).orElseThrow()
                                                                      .stream().map(holder ->
                                                                                    {   ColdSweat.LOGGER.info("Disabling sleeping conditions check for block {}", holder.value());
                                                                                        return holder.value();
                                                                                    }).toList());
        ConfigSettings.LAMP_DIMENSIONS.get(registries).addAll(registries.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY)
                                                                      .getTag(ModDimensionTags.SOUL_LAMP_VALID).orElseThrow()
                                                                      .stream().map(holder ->
                                                                                    {   ColdSweat.LOGGER.info("Enabling dimension {} for soulspring lamp", holder.value());
                                                                                        return holder.value();
                                                                                    }).toList());
        ConfigSettings.INSULATION_BLACKLIST.get().addAll(registries.registryOrThrow(Registry.ITEM_REGISTRY)
                                                                 .getTag(ModItemTags.NOT_INSULATABLE).orElseThrow()
                                                                 .stream().map(holder ->
                                                                               {   ColdSweat.LOGGER.info("Adding item {} to insulation blacklist", holder.value());
                                                                                   return holder.value();
                                                                               }).toList());
        ConfigSettings.HEARTH_POTION_BLACKLIST.get().addAll(registries.registryOrThrow(Registry.MOB_EFFECT_REGISTRY)
                                                                    .getTag(ModEffectTags.HEARTH_BLACKLISTED).orElseThrow()
                                                                    .stream().map(holder ->
                                                                                  {   ColdSweat.LOGGER.info("Adding effect {} to hearth potion blacklist", holder.value());
                                                                                      return holder.value();
                                                                                  }).toList());

        /*
         Fetch JSON registries
        */
        Set<Holder<InsulatorData>> insulators = registries.registryOrThrow(ModRegistries.INSULATOR_DATA).holders().collect(Collectors.toSet());
        Set<Holder<FuelData>> fuels = registries.registryOrThrow(ModRegistries.FUEL_DATA).holders().collect(Collectors.toSet());
        Set<Holder<FoodData>> foods = registries.registryOrThrow(ModRegistries.FOOD_DATA).holders().collect(Collectors.toSet());
        Set<Holder<ItemCarryTempData>> carryTemps = registries.registryOrThrow(ModRegistries.CARRY_TEMP_DATA).holders().collect(Collectors.toSet());

        Set<Holder<BlockTempData>> blockTemps = registries.registryOrThrow(ModRegistries.BLOCK_TEMP_DATA).holders().collect(Collectors.toSet());
        Set<Holder<BiomeTempData>> biomeTemps = registries.registryOrThrow(ModRegistries.BIOME_TEMP_DATA).holders().collect(Collectors.toSet());
        Set<Holder<DimensionTempData>> dimensionTemps = registries.registryOrThrow(ModRegistries.DIMENSION_TEMP_DATA).holders().collect(Collectors.toSet());
        Set<Holder<StructureTempData>> structureTemps = registries.registryOrThrow(ModRegistries.STRUCTURE_TEMP_DATA).holders().collect(Collectors.toSet());
        Set<Holder<DepthTempData>> depthTemps = registries.registryOrThrow(ModRegistries.DEPTH_TEMP_DATA).holders().collect(Collectors.toSet());

        Set<Holder<MountData>> mounts = registries.registryOrThrow(ModRegistries.MOUNT_DATA).holders().collect(Collectors.toSet());
        Set<Holder<SpawnBiomeData>> spawnBiomes = registries.registryOrThrow(ModRegistries.ENTITY_SPAWN_BIOME_DATA).holders().collect(Collectors.toSet());
        Set<Holder<EntityTempData>> entityTemps = registries.registryOrThrow(ModRegistries.ENTITY_TEMP_DATA).holders().collect(Collectors.toSet());

        logAndAddRegistries(registries, insulators, fuels, foods, carryTemps, blockTemps, biomeTemps,
                            dimensionTemps, structureTemps, depthTemps, mounts, spawnBiomes, entityTemps);
    }

    /**
     * Loads JSON-based configs from the configs folder
     */
    public static void collectUserRegistries(RegistryAccess registries)
    {
        if (registries == null)
        {   ColdSweat.LOGGER.error("Failed to load registries from null RegistryAccess");
            return;
        }

        /*
         Parse user-defined JSON data from the configs folder
        */
        Set<Holder<InsulatorData>> insulators = new HashSet<>(parseConfigData(ModRegistries.INSULATOR_DATA, InsulatorData.CODEC));
        Set<Holder<FuelData>> fuels = new HashSet<>(parseConfigData(ModRegistries.FUEL_DATA, FuelData.CODEC));
        Set<Holder<FoodData>> foods = new HashSet<>(parseConfigData(ModRegistries.FOOD_DATA, FoodData.CODEC));
        Set<Holder<ItemCarryTempData>> carryTemps = new HashSet<>(parseConfigData(ModRegistries.CARRY_TEMP_DATA, ItemCarryTempData.CODEC));

        Set<Holder<BlockTempData>> blockTemps = new HashSet<>(parseConfigData(ModRegistries.BLOCK_TEMP_DATA, BlockTempData.CODEC));
        Set<Holder<BiomeTempData>> biomeTemps = new HashSet<>(parseConfigData(ModRegistries.BIOME_TEMP_DATA, BiomeTempData.CODEC));
        Set<Holder<DimensionTempData>> dimensionTemps = new HashSet<>(parseConfigData(ModRegistries.DIMENSION_TEMP_DATA, DimensionTempData.CODEC));
        Set<Holder<StructureTempData>> structureTemps = new HashSet<>(parseConfigData(ModRegistries.STRUCTURE_TEMP_DATA, StructureTempData.CODEC));
        Set<Holder<DepthTempData>> depthTemps = new HashSet<>(parseConfigData(ModRegistries.DEPTH_TEMP_DATA, DepthTempData.CODEC));

        Set<Holder<MountData>> mounts = new HashSet<>(parseConfigData(ModRegistries.MOUNT_DATA, MountData.CODEC));
        Set<Holder<SpawnBiomeData>> spawnBiomes = new HashSet<>(parseConfigData(ModRegistries.ENTITY_SPAWN_BIOME_DATA, SpawnBiomeData.CODEC));
        Set<Holder<EntityTempData>> entityTemps = new HashSet<>(parseConfigData(ModRegistries.ENTITY_TEMP_DATA, EntityTempData.CODEC));

        logAndAddRegistries(registries, insulators, fuels, foods, carryTemps, blockTemps, biomeTemps,
                            dimensionTemps, structureTemps, depthTemps, mounts, spawnBiomes, entityTemps);
    }

    private static void logAndAddRegistries(RegistryAccess registries, Set<Holder<InsulatorData>> insulators,
                                            Set<Holder<FuelData>> fuels, Set<Holder<FoodData>> foods,
                                            Set<Holder<ItemCarryTempData>> carryTemps, Set<Holder<BlockTempData>> blockTemps,
                                            Set<Holder<BiomeTempData>> biomeTemps, Set<Holder<DimensionTempData>> dimensionTemps,
                                            Set<Holder<StructureTempData>> structureTemps, Set<Holder<DepthTempData>> depthTemps,
                                            Set<Holder<MountData>> mounts, Set<Holder<SpawnBiomeData>> spawnBiomes,
                                            Set<Holder<EntityTempData>> entityTemps)
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
        {
            if (entry instanceof Holder<?> holder)
            {   ColdSweat.LOGGER.info("{}", holder.value());
            }
            else
            {   ColdSweat.LOGGER.info("{}", entry);
            }
        }
    }

    private static void getDefaultConfigs(MinecraftServer server)
    {
        DEFAULT_REGION = ConfigHelper.parseResource(server.getResourceManager(), new ResourceLocation(ColdSweat.MOD_ID, "cold_sweat/world/temp_region/default.json"), DepthTempData.CODEC).orElseThrow();
    }

    private static void addInsulatorConfigs(Set<Holder<InsulatorData>> insulators)
    {
        insulators.forEach(holder ->
        {
            InsulatorData insulatorData = holder.value();
            // Check if the required mods are loaded
            if (insulatorData.requiredMods().isPresent())
            {
                List<String> requiredMods = insulatorData.requiredMods().get();
                if (requiredMods.stream().anyMatch(mod -> !CompatManager.modLoaded(mod)))
                {   return;
                }
            }
            AttributeModifierMap attributeModifiers = insulatorData.attributes().orElse(new AttributeModifierMap());
            Insulator insulator = new Insulator(insulatorData.insulation(), insulatorData.slot(), insulatorData.data(),
                                                insulatorData.predicate(), attributeModifiers, insulatorData.immuneTempModifiers());

            // Add listed items as insulators
            List<Item> items = new ArrayList<>();
            insulatorData.data().items().ifPresent(itemList ->
            {   items.addAll(RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ITEMS, itemList));
            });
            insulatorData.data().tag().ifPresent(tag ->
            {   items.addAll(ForgeRegistries.ITEMS.tags().getTag(tag).stream().toList());
            });

            for (Item item : items)
            {
                switch (insulatorData.slot())
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

            FuelData.FuelType type = fuelData.type();
            ItemRequirement data = fuelData.data();
            double fuel = fuelData.fuel();

            PredicateItem predicateItem = new PredicateItem(fuel, data, EntityRequirement.NONE);

            List<Item> items = new ArrayList<>();
            fuelData.data().items().ifPresent(itemList ->
            {   items.addAll(RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ITEMS, itemList));
            });
            fuelData.data().tag().ifPresent(tag ->
            {   items.addAll(ForgeRegistries.ITEMS.tags().getTag(tag).stream().toList());
            });

            for (Item item : items)
            {
                switch (type)
                {
                    case BOILER -> ConfigSettings.BOILER_FUEL.get().put(item, predicateItem);
                    case ICEBOX -> ConfigSettings.ICEBOX_FUEL.get().put(item, predicateItem);
                    case HEARTH -> ConfigSettings.HEARTH_FUEL.get().put(item, predicateItem);
                    case SOUL_LAMP -> ConfigSettings.SOULSPRING_LAMP_FUEL.get().put(item, predicateItem);
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

            EntityRequirement predicate = foodData.entityRequirement().orElse(EntityRequirement.NONE);
            CompoundTag extraData = null;
            if (foodData.duration().isPresent())
            {
                extraData = new CompoundTag();
                extraData.putInt("duration", foodData.duration().get());
            }

            PredicateItem predicateItem = new PredicateItem(foodData.value(), foodData.data(), predicate, extraData);

            List<Item> items = new ArrayList<>();
            foodData.data().items().ifPresent(itemList ->
            {   items.addAll(RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ITEMS, itemList));
            });
            foodData.data().tag().ifPresent(tag ->
            {   items.addAll(ForgeRegistries.ITEMS.tags().getTag(tag).stream().toList());
            });

            for (Item item : items)
            {
                ConfigSettings.FOOD_TEMPERATURES.get().put(item, predicateItem);
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

            CarriedItemTemperature carriedItemTemperature = CarriedItemTemperature.createFromData(carryTempData);
            List<Either<TagKey<Item>, Item>> items = CSMath.orElse(carryTempData.data().items().orElse(null),
                                                                   List.of(Either.left(carryTempData.data().tag().orElse(null))),
                                                                   List.of());
            for (Item item : RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ITEMS, items))
            {
                ConfigSettings.CARRIED_ITEM_TEMPERATURES.get().put(item, carriedItemTemperature);
            }
        });
    }

    private static void addBlockTempConfigs(Set<Holder<BlockTempData>> blockTemps)
    {
        blockTemps.forEach(holder ->
        {
            BlockTempData blockTempData = holder.value();
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
            for (Biome biome : RegistryHelper.mapVanillaRegistryTagList(Registry.BIOME_REGISTRY, biomeTempData.biomes(), registryAccess))
            {
                Temperature.Units units = biomeTempData.units();
                if (biomeTempData.isOffset())
                {   ConfigSettings.BIOME_OFFSETS.get(registryAccess).put(biome, new Triplet<>(Temperature.convert(biomeTempData.min(), units, Temperature.Units.MC, true),
                                                                                Temperature.convert(biomeTempData.max(), units, Temperature.Units.MC, true),
                                                                                biomeTempData.units()));
                }
                else
                {   ConfigSettings.BIOME_TEMPS.get(registryAccess).put(biome, new Triplet<>(Temperature.convert(biomeTempData.min(), units, Temperature.Units.MC, true),
                                                                              Temperature.convert(biomeTempData.max(), units, Temperature.Units.MC, true),
                                                                              biomeTempData.units()));
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
            for (DimensionType dimension : RegistryHelper.mapVanillaRegistryTagList(Registry.DIMENSION_TYPE_REGISTRY, dimensionTempData.dimensions(), registryAccess))
            {
                Temperature.Units units = dimensionTempData.units();
                if (dimensionTempData.isOffset())
                {   ConfigSettings.DIMENSION_OFFSETS.get(registryAccess).put(dimension, Pair.of(Temperature.convert(dimensionTempData.temperature(), units, Temperature.Units.MC, true),
                                                                                  dimensionTempData.units()));
                }
                else
                {   ConfigSettings.DIMENSION_TEMPS.get(registryAccess).put(dimension, Pair.of(Temperature.convert(dimensionTempData.temperature(), units, Temperature.Units.MC, true),
                                                                                dimensionTempData.units()));
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
            for (ConfiguredStructureFeature<?, ?> structure : RegistryHelper.mapVanillaRegistryTagList(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, structureTempData.structures(), registryAccess))
            {
                double temperature = Temperature.convert(structureTempData.temperature(), structureTempData.units(), Temperature.Units.MC, !structureTempData.isOffset());
                if (structureTempData.isOffset())
                {   ConfigSettings.STRUCTURE_OFFSETS.get(registryAccess).put(structure.feature, Pair.of(temperature, structureTempData.units()));
                }
                else
                {   ConfigSettings.STRUCTURE_TEMPS.get(registryAccess).put(structure.feature, Pair.of(temperature, structureTempData.units()));
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
            for (EntityType<?> entity : RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ENTITIES, mountData.entities()))
            {   ConfigSettings.INSULATED_ENTITIES.get().put(entity, new InsulatingMount(entity, mountData.coldInsulation(), mountData.heatInsulation(), mountData.requirement()));
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
            for (Biome biome : RegistryHelper.mapVanillaRegistryTagList(Registry.BIOME_REGISTRY, spawnBiomeData.biomes(), registryAccess))
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
            entityTempData.entity().type().ifPresent(type -> types.add(Either.right(type)));
            entityTempData.entity().tag().ifPresent(tag -> types.add(Either.left(tag)));

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
