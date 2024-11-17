package com.momosoftworks.coldsweat.config;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.insulation.slot.ScalingFormula;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.config.spec.*;
import com.momosoftworks.coldsweat.data.ModRegistries;
import com.momosoftworks.coldsweat.data.codec.configuration.*;
import com.momosoftworks.coldsweat.util.math.FastMap;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import com.momosoftworks.coldsweat.util.math.Vec2i;
import com.momosoftworks.coldsweat.util.registries.ModEntities;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.DynamicHolder;
import com.momosoftworks.coldsweat.util.serialization.ListBuilder;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.util.TriConsumer;
import oshi.util.tuples.Triplet;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Holds almost all configs for Cold Sweat in memory for easy access.
 * Handles syncing configs between the client/server.
 */
public class ConfigSettings
{
    public static final BiMap<String, DynamicHolder<?>> CONFIG_SETTINGS = HashBiMap.create();
    public static final BiMap<String, DynamicHolder<?>> CLIENT_SETTINGS = HashBiMap.create();

    public static Difficulty DEFAULT_DIFFICULTY = Difficulty.NORMAL;

    // Settings visible in the config screen
    public static final DynamicHolder<Difficulty> DIFFICULTY;
    public static final DynamicHolder<Double> MAX_TEMP;
    public static final DynamicHolder<Double> MIN_TEMP;
    public static final DynamicHolder<Double> TEMP_RATE;
    public static final DynamicHolder<Double> TEMP_DAMAGE;
    public static final DynamicHolder<Boolean> FIRE_RESISTANCE_ENABLED;
    public static final DynamicHolder<Boolean> ICE_RESISTANCE_ENABLED;
    public static final DynamicHolder<Boolean> USE_PEACEFUL_MODE;
    public static final DynamicHolder<Boolean> REQUIRE_THERMOMETER;
    public static final DynamicHolder<Integer> GRACE_LENGTH;
    public static final DynamicHolder<Boolean> GRACE_ENABLED;

    // Other Difficulty Settings
    public static final DynamicHolder<Double> HEARTS_FREEZING_PERCENTAGE;
    public static final DynamicHolder<Double> COLD_MINING_IMPAIRMENT;
    public static final DynamicHolder<Double> COLD_MOVEMENT_SLOWDOWN;
    public static final DynamicHolder<Double> COLD_KNOCKBACK_REDUCTION;
    public static final DynamicHolder<Double> HEATSTROKE_FOG_DISTANCE;

    // World Settings
    public static final DynamicHolder<Map<Biome, BiomeTempData>> BIOME_TEMPS;
    public static final DynamicHolder<Map<Biome, BiomeTempData>> BIOME_OFFSETS;
    public static final DynamicHolder<Map<DimensionType, DimensionTempData>> DIMENSION_TEMPS;
    public static final DynamicHolder<Map<DimensionType, DimensionTempData>> DIMENSION_OFFSETS;
    public static final DynamicHolder<Map<ConfiguredStructureFeature<?,?>, StructureTempData>> STRUCTURE_TEMPS;
    public static final DynamicHolder<Map<ConfiguredStructureFeature<?,?>, StructureTempData>> STRUCTURE_OFFSETS;
    public static final DynamicHolder<List<DepthTempData>> DEPTH_REGIONS;
    public static final DynamicHolder<Boolean> CHECK_SLEEP_CONDITIONS;
    public static final DynamicHolder<Double[]> SUMMER_TEMPS;
    public static final DynamicHolder<Double[]> AUTUMN_TEMPS;
    public static final DynamicHolder<Double[]> WINTER_TEMPS;
    public static final DynamicHolder<Double[]> SPRING_TEMPS;

    // Block settings
    public static final DynamicHolder<Integer> BLOCK_RANGE;
    public static final DynamicHolder<Boolean> COLD_SOUL_FIRE;
    public static final DynamicHolder<List<Block>> HEARTH_SPREAD_WHITELIST;
    public static final DynamicHolder<List<Block>> HEARTH_SPREAD_BLACKLIST;
    public static final DynamicHolder<Double> HEARTH_STRENGTH;
    public static final DynamicHolder<Boolean> SMART_HEARTH;
    public static final DynamicHolder<List<Block>> SLEEP_CHECK_IGNORE_BLOCKS;
    public static final DynamicHolder<Boolean> USE_CUSTOM_WATER_FREEZE_BEHAVIOR;
    public static final DynamicHolder<Boolean> USE_CUSTOM_ICE_DROPS;

    // Item settings
    public static final DynamicHolder<Multimap<Item, InsulatorData>> INSULATION_ITEMS;
    public static final DynamicHolder<Multimap<Item, InsulatorData>> INSULATING_ARMORS;
    public static final DynamicHolder<Multimap<Item, InsulatorData>> INSULATING_CURIOS;
    public static final DynamicHolder<ScalingFormula> INSULATION_SLOTS;
    public static final DynamicHolder<List<Item>> INSULATION_BLACKLIST;

    public static final DynamicHolder<Multimap<Item, FoodData>> FOOD_TEMPERATURES;

    public static final DynamicHolder<Multimap<Item, ItemCarryTempData>> CARRIED_ITEM_TEMPERATURES;

    public static final DynamicHolder<Integer> WATERSKIN_STRENGTH;
    public static final DynamicHolder<Double> SOULSPRING_LAMP_STRENGTH;

    public static final DynamicHolder<List<DimensionType>> LAMP_DIMENSIONS;

    public static final DynamicHolder<Multimap<Item, FuelData>> BOILER_FUEL;
    public static final DynamicHolder<Multimap<Item, FuelData>> ICEBOX_FUEL;
    public static final DynamicHolder<Multimap<Item, FuelData>> HEARTH_FUEL;
    public static final DynamicHolder<Multimap<Item, FuelData>> SOULSPRING_LAMP_FUEL;

    public static final DynamicHolder<Boolean> HEARTH_POTIONS_ENABLED;
    public static final DynamicHolder<List<MobEffect>> HEARTH_POTION_BLACKLIST;

    // Entity Settings
    public static final DynamicHolder<Triplet<Integer, Integer, Double>> FUR_TIMINGS;
    public static final DynamicHolder<Triplet<Integer, Integer, Double>> SHED_TIMINGS;
    public static final DynamicHolder<Multimap<Biome, SpawnBiomeData>> ENTITY_SPAWN_BIOMES;
    public static final DynamicHolder<Multimap<EntityType<?>, MountData>> INSULATED_MOUNTS;
    public static final DynamicHolder<Multimap<EntityType<?>, EntityTempData>> ENTITY_TEMPERATURES;

    // Misc Settings
    public static final DynamicHolder<Double> INSULATION_STRENGTH;
    public static final DynamicHolder<List<ResourceLocation>> DISABLED_MODIFIERS;

    // Client Settings
    /* NULL ON THE SERVER */
    public static final DynamicHolder<Boolean> CELSIUS;
    public static final DynamicHolder<Integer> TEMP_OFFSET;
    public static final DynamicHolder<Double> TEMP_SMOOTHING;

    public static final DynamicHolder<Vec2i> BODY_ICON_POS;
    public static final DynamicHolder<Boolean> BODY_ICON_ENABLED;
    public static final DynamicHolder<Boolean> MOVE_BODY_ICON_WHEN_ADVANCED;

    public static final DynamicHolder<Vec2i> BODY_READOUT_POS;
    public static final DynamicHolder<Boolean> BODY_READOUT_ENABLED;

    public static final DynamicHolder<Vec2i> WORLD_GAUGE_POS;
    public static final DynamicHolder<Boolean> WORLD_GAUGE_ENABLED;

    public static final DynamicHolder<Boolean> CUSTOM_HOTBAR_LAYOUT;
    public static final DynamicHolder<Boolean> ICON_BOBBING;

    public static final DynamicHolder<Boolean> HEARTH_DEBUG;

    public static final DynamicHolder<Boolean> SHOW_CONFIG_BUTTON;
    public static final DynamicHolder<Vec2i> CONFIG_BUTTON_POS;

    public static final DynamicHolder<Boolean> DISTORTION_EFFECTS;
    public static final DynamicHolder<Boolean> HIGH_CONTRAST;

    public static final DynamicHolder<Boolean> SHOW_CREATIVE_WARNING;
    public static final DynamicHolder<Boolean> HIDE_TOOLTIPS;
    public static final DynamicHolder<Boolean> EXPAND_TOOLTIPS;

    public static final DynamicHolder<Boolean> SHOW_WATER_EFFECT;


    // Makes the settings instantiation collapsible & easier to read
    static
    {
        DIFFICULTY = addSyncedSetting("difficulty", () -> Difficulty.NORMAL, holder -> holder.set(Difficulty.byId(MainSettingsConfig.getInstance().getDifficulty())),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder.getId(), "Difficulty"),
        (decoder) -> Difficulty.byId(decoder.getInt("Difficulty")),
        (saver) -> MainSettingsConfig.getInstance().setDifficulty(saver.getId()));

        MAX_TEMP = addSyncedSetting("max_temp", () -> 1.7, holder -> holder.set(MainSettingsConfig.getInstance().getMaxTempHabitable()),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "MaxTemp"),
        (decoder) -> decoder.getDouble("MaxTemp"),
        (saver) -> MainSettingsConfig.getInstance().setMaxHabitable(saver));

        MIN_TEMP = addSyncedSetting("min_temp", () -> 0.5, holder -> holder.set(MainSettingsConfig.getInstance().getMinTempHabitable()),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "MinTemp"),
        (decoder) -> decoder.getDouble("MinTemp"),
        (saver) -> MainSettingsConfig.getInstance().setMinHabitable(saver));

        TEMP_RATE = addSyncedSetting("temp_rate", () -> 1d, holder -> holder.set(MainSettingsConfig.getInstance().getRateMultiplier()),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "TempRate"),
        (decoder) -> decoder.getDouble("TempRate"),
        (saver) -> MainSettingsConfig.getInstance().setRateMultiplier(saver));

        TEMP_DAMAGE = addSyncedSetting("temp_damage", () -> 2d, holder -> holder.set(MainSettingsConfig.getInstance().getTempDamage()),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "TempDamage"),
        (decoder) -> decoder.getDouble("TempDamage"),
        (saver) -> MainSettingsConfig.getInstance().setTempDamage(saver));

        FIRE_RESISTANCE_ENABLED = addSyncedSetting("fire_resistance_enabled", () -> true, holder -> holder.set(MainSettingsConfig.getInstance().isFireResistanceEnabled()),
        (encoder) -> ConfigHelper.serializeNbtBool(encoder, "FireResistanceEnabled"),
        (decoder) -> decoder.getBoolean("FireResistanceEnabled"),
        (saver) -> MainSettingsConfig.getInstance().setFireResistanceEnabled(saver));

        ICE_RESISTANCE_ENABLED = addSyncedSetting("ice_resistance_enabled", () -> true, holder -> holder.set(MainSettingsConfig.getInstance().isIceResistanceEnabled()),
        (encoder) -> ConfigHelper.serializeNbtBool(encoder, "IceResistanceEnabled"),
        (decoder) -> decoder.getBoolean("IceResistanceEnabled"),
        (saver) -> MainSettingsConfig.getInstance().setIceResistanceEnabled(saver));

        USE_PEACEFUL_MODE = addSyncedSetting("use_peaceful", () -> true, holder -> holder.set(MainSettingsConfig.getInstance().nullifyInPeaceful()),
        (encoder) -> ConfigHelper.serializeNbtBool(encoder, "UsePeaceful"),
        (decoder) -> decoder.getBoolean("UsePeaceful"),
        (saver) -> MainSettingsConfig.getInstance().setNullifyInPeaceful(saver));

        REQUIRE_THERMOMETER = addSyncedSetting("require_thermometer", () -> true, holder -> holder.set(MainSettingsConfig.getInstance().thermometerRequired()),
        (encoder) -> ConfigHelper.serializeNbtBool(encoder, "RequireThermometer"),
        (decoder) -> decoder.getBoolean("RequireThermometer"),
        (saver) -> MainSettingsConfig.getInstance().setRequireThermometer(saver));

        GRACE_LENGTH = addSyncedSetting("grace_length", () -> 6000, holder -> holder.set(MainSettingsConfig.getInstance().getGracePeriodLength()),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "GraceLength"),
        (decoder) -> decoder.getInt("GraceLength"),
        (saver) -> MainSettingsConfig.getInstance().setGracePeriodLength(saver));

        GRACE_ENABLED = addSyncedSetting("grace_enabled", () -> true, holder -> holder.set(MainSettingsConfig.getInstance().isGracePeriodEnabled()),
        (encoder) -> ConfigHelper.serializeNbtBool(encoder, "GraceEnabled"),
        (decoder) -> decoder.getBoolean("GraceEnabled"),
        (saver) -> MainSettingsConfig.getInstance().setGracePeriodEnabled(saver));


        HEARTS_FREEZING_PERCENTAGE = addSyncedSetting("hearts_freezing_percentage", () -> 0.5, holder -> holder.set(MainSettingsConfig.getInstance().getHeartsFreezingPercentage()),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "HeartsFreezingPercentage"),
        (decoder) -> decoder.getDouble("HeartsFreezingPercentage"),
        (saver) -> MainSettingsConfig.getInstance().setHeartsFreezingPercentage(saver));

        COLD_MINING_IMPAIRMENT = addSyncedSetting("cold_mining_slowdown", () -> 0.5, holder -> holder.set(MainSettingsConfig.getInstance().getColdMiningImpairment()),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "ColdMiningImpairment"),
        (decoder) -> decoder.getDouble("ColdMiningImpairment"),
        (saver) -> MainSettingsConfig.getInstance().setColdMiningImpairment(saver));

        COLD_MOVEMENT_SLOWDOWN = addSyncedSetting("cold_movement_slowdown", () -> 0.5, holder -> holder.set(MainSettingsConfig.getInstance().getColdMovementSlowdown()),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "ColdMovementSlowdown"),
        (decoder) -> decoder.getDouble("ColdMovementSlowdown"),
        (saver) -> MainSettingsConfig.getInstance().setColdMovementSlowdown(saver));

        COLD_KNOCKBACK_REDUCTION = addSyncedSetting("cold_knockback_reduction", () -> 0.5, holder -> holder.set(MainSettingsConfig.getInstance().getColdKnockbackReduction()),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "ColdKnockbackReduction"),
        (decoder) -> decoder.getDouble("ColdKnockbackReduction"),
        (saver) -> MainSettingsConfig.getInstance().setColdKnockbackReduction(saver));

        HEATSTROKE_FOG_DISTANCE = addSyncedSetting("heatstroke_fog_distance", () -> 6d, holder -> holder.set(MainSettingsConfig.getInstance().getHeatstrokeFogDistance()),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "HeatstrokeFogDistance"),
        (decoder) -> decoder.getDouble("HeatstrokeFogDistance"),
        (saver) -> MainSettingsConfig.getInstance().setHeatstrokeFogDistance(saver));


        BIOME_TEMPS = addSyncedSettingWithRegistries("biome_temps", FastMap::new, (holder, registryAccess) ->
        {
            Map<Biome, BiomeTempData> dataMap = ConfigHelper.getRegistryMap(WorldSettingsConfig.BIOME_TEMPERATURES.get(), registryAccess, Registry.BIOME_REGISTRY,
                                                                            toml -> BiomeTempData.fromToml(toml, true, registryAccess), BiomeTempData::biomes);
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.BIOME_TEMP_DATA);

            for (Map.Entry<Biome, BiomeTempData> entry : dataMap.entrySet())
            {   holder.get(registryAccess).put(entry.getKey(), entry.getValue());
            }
        },
        (encoder, registryAccess) -> ConfigHelper.serializeRegistry(encoder, "BiomeTemps", Registry.BIOME_REGISTRY, ModRegistries.BIOME_TEMP_DATA, registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeRegistry(decoder, "BiomeTemps", Registry.BIOME_REGISTRY, ModRegistries.BIOME_TEMP_DATA, registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.setBiomeTemperatures(saver.entrySet().stream()
                                                            .map(entry ->
                                                            {
                                                                ResourceLocation biome = RegistryHelper.getBiomeId(entry.getKey(), registryAccess);
                                                                if (biome == null) return null;

                                                                Temperature.Units units = entry.getValue().units();
                                                                double min = Temperature.convert(entry.getValue().min(), Temperature.Units.MC, units, true);
                                                                double max = Temperature.convert(entry.getValue().max(), Temperature.Units.MC, units, true);

                                                                return Arrays.asList(biome.toString(), min, max, units.toString());
                                                            })
                                                            .filter(Objects::nonNull)
                                                            .collect(Collectors.toList())));

        BIOME_OFFSETS = addSyncedSettingWithRegistries("biome_offsets", FastMap::new, (holder, registryAccess) ->
        {
            Map<Biome, BiomeTempData> dataMap = ConfigHelper.getRegistryMap(WorldSettingsConfig.BIOME_TEMP_OFFSETS.get(), registryAccess, Registry.BIOME_REGISTRY,
                                                                            toml -> BiomeTempData.fromToml(toml, false, registryAccess), BiomeTempData::biomes);
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.BIOME_TEMP_DATA);

            for (Map.Entry<Biome, BiomeTempData> entry : dataMap.entrySet())
            {   holder.get(registryAccess).put(entry.getKey(), entry.getValue());
            }
        },
        (encoder, registryAccess) -> ConfigHelper.serializeRegistry(encoder, "BiomeOffsets", Registry.BIOME_REGISTRY, ModRegistries.BIOME_TEMP_DATA, registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeRegistry(decoder, "BiomeOffsets", Registry.BIOME_REGISTRY, ModRegistries.BIOME_TEMP_DATA, registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.setBiomeTempOffsets(saver.entrySet().stream()
                                                            .map(entry ->
                                                            {
                                                                BiomeTempData data = entry.getValue();
                                                                ResourceLocation biome = RegistryHelper.getBiomeId(entry.getKey(), registryAccess);
                                                                if (biome == null) return null;

                                                                Temperature.Units units = data.units();
                                                                double min = Temperature.convert(data.min(), Temperature.Units.MC, units, false);
                                                                double max = Temperature.convert(data.max(), Temperature.Units.MC, units, false);

                                                                return Arrays.asList(biome.toString(), min, max, units.toString());
                                                            })
                                                            .filter(Objects::nonNull)
                                                            .collect(Collectors.toList())));

        DIMENSION_TEMPS = addSyncedSettingWithRegistries("dimension_temps", FastMap::new, (holder, registryAccess) ->
        {
            Map<DimensionType, DimensionTempData> dataMap = ConfigHelper.getRegistryMap(WorldSettingsConfig.DIMENSION_TEMPERATURES.get(), registryAccess, Registry.DIMENSION_TYPE_REGISTRY,
                                                                                       toml -> DimensionTempData.fromToml(toml, true, registryAccess), DimensionTempData::dimensions);
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.DIMENSION_TEMP_DATA);

            for (Map.Entry<DimensionType, DimensionTempData> entry : dataMap.entrySet())
            {   holder.get(registryAccess).put(entry.getKey(), entry.getValue());
            }
        },
        (encoder, registryAccess) -> ConfigHelper.serializeRegistry(encoder, "DimensionTemps", Registry.DIMENSION_TYPE_REGISTRY, ModRegistries.DIMENSION_TEMP_DATA, registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeRegistry(decoder, "DimensionTemps", Registry.DIMENSION_TYPE_REGISTRY, ModRegistries.DIMENSION_TEMP_DATA, registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.setDimensionTemperatures(saver.entrySet().stream()
                                                     .map(entry ->
                                                     {
                                                         ResourceLocation dim = RegistryHelper.getDimensionId(entry.getKey(), registryAccess);
                                                         if (dim == null) return null;

                                                         Temperature.Units units = entry.getValue().units();
                                                         double temp = Temperature.convert(entry.getValue().temperature(), Temperature.Units.MC, units, true);

                                                         return Arrays.asList(dim.toString(), temp, units.toString());
                                                     })
                                                     .filter(Objects::nonNull)
                                                     .collect(Collectors.toList())));

        DIMENSION_OFFSETS = addSyncedSettingWithRegistries("dimension_offsets", FastMap::new, (holder, registryAccess) ->
        {
            Map<DimensionType, DimensionTempData> dataMap = ConfigHelper.getRegistryMap(WorldSettingsConfig.DIMENSION_TEMP_OFFSETS.get(), registryAccess, Registry.DIMENSION_TYPE_REGISTRY,
                                                                                       toml -> DimensionTempData.fromToml(toml, false, registryAccess), DimensionTempData::dimensions);
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.DIMENSION_TEMP_DATA);

            for (Map.Entry<DimensionType, DimensionTempData> entry : dataMap.entrySet())
            {   holder.get(registryAccess).put(entry.getKey(), entry.getValue());
            }
        },
        (encoder, registryAccess) -> ConfigHelper.serializeRegistry(encoder, "DimensionOffsets", Registry.DIMENSION_TYPE_REGISTRY, ModRegistries.DIMENSION_TEMP_DATA, registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeRegistry(decoder, "DimensionOffsets", Registry.DIMENSION_TYPE_REGISTRY, ModRegistries.DIMENSION_TEMP_DATA, registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.setDimensionTempOffsets(saver.entrySet().stream()
                                                     .map(entry ->
                                                     {
                                                         ResourceLocation dim = RegistryHelper.getDimensionId(entry.getKey(), registryAccess);
                                                         if (dim == null) return null;

                                                         Temperature.Units units = entry.getValue().units();
                                                         double temp = Temperature.convert(entry.getValue().temperature(), Temperature.Units.MC, units, false);

                                                         return Arrays.asList(dim.toString(), temp, units.toString());
                                                     })
                                                     .filter(Objects::nonNull)
                                                     .collect(Collectors.toList())));

        STRUCTURE_TEMPS = addSyncedSettingWithRegistries("structure_temperatures", FastMap::new, (holder, registryAccess) ->
        {
            Map<ConfiguredStructureFeature<?, ?>, StructureTempData> dataMap = ConfigHelper.getRegistryMap(WorldSettingsConfig.STRUCTURE_TEMPERATURES.get(), registryAccess, Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY,
                                                                                   toml -> StructureTempData.fromToml(toml, true, registryAccess), StructureTempData::structures);
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.STRUCTURE_TEMP_DATA);

            for (Map.Entry<ConfiguredStructureFeature<?, ?>, StructureTempData> entry : dataMap.entrySet())
            {   holder.get(registryAccess).put(entry.getKey(), entry.getValue());
            }
        },
        (encoder, registryAccess) -> ConfigHelper.serializeRegistry(encoder, "StructureTemperatures", Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, ModRegistries.STRUCTURE_TEMP_DATA, registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeRegistry(decoder, "StructureTemperatures", Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, ModRegistries.STRUCTURE_TEMP_DATA, registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.setStructureTemperatures(saver.entrySet().stream()
                                                     .map(entry ->
                                                     {
                                                         ResourceLocation struct = RegistryHelper.getStructureId(entry.getKey(), registryAccess);
                                                         if (struct == null) return null;

                                                         Temperature.Units units = entry.getValue().units();
                                                         double temp = Temperature.convert(entry.getValue().temperature(), Temperature.Units.MC, units, true);

                                                         return Arrays.asList(struct.toString(), temp, units.toString());
                                                     })
                                                     .filter(Objects::nonNull)
                                                     .collect(Collectors.toList())));

        STRUCTURE_OFFSETS = addSyncedSettingWithRegistries("structure_offsets", FastMap::new, (holder, registryAccess) ->
        {
            Map<ConfiguredStructureFeature<?, ?>, StructureTempData> dataMap = ConfigHelper.getRegistryMap(WorldSettingsConfig.STRUCTURE_TEMP_OFFSETS.get(), registryAccess, Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY,
                                                                                   toml -> StructureTempData.fromToml(toml, false, registryAccess), StructureTempData::structures);
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.STRUCTURE_TEMP_DATA);

            for (Map.Entry<ConfiguredStructureFeature<?, ?>, StructureTempData> entry : dataMap.entrySet())
            {   holder.get(registryAccess).put(entry.getKey(), entry.getValue());
            }
        },
        (encoder, registryAccess) -> ConfigHelper.serializeRegistry(encoder, "StructureOffsets", Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, ModRegistries.STRUCTURE_TEMP_DATA, registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeRegistry(decoder, "StructureOffsets", Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, ModRegistries.STRUCTURE_TEMP_DATA, registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.setStructureTempOffsets(saver.entrySet().stream()
                                                     .map(entry ->
                                                     {
                                                         ResourceLocation struct = RegistryHelper.getStructureId(entry.getKey(), registryAccess);
                                                         if (struct == null) return null;

                                                         Temperature.Units units = entry.getValue().units();
                                                         double temp = Temperature.convert(entry.getValue().temperature(), Temperature.Units.MC, units, false);

                                                         return Arrays.asList(struct.toString(), temp, units.toString());
                                                     })
                                                     .filter(Objects::nonNull)
                                                     .collect(Collectors.toList())));

        DEPTH_REGIONS = addSetting("depth_regions", ArrayList::new, holder -> {});

        TriConsumer<FuelData.FuelType, ForgeConfigSpec.ConfigValue<List<? extends List<?>>>, DynamicHolder<Multimap<Item, FuelData>>> fuelAdder =
        (fuelType, configValue, holder) ->
        {
            Multimap<Item, FuelData> dataMap = new FastMultiMap<>();
            for (List<?> list : configValue.get())
            {
                FuelData data = FuelData.fromToml(list, fuelType);
                if (data == null) continue;

                for (Item item : RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ITEMS, data.data().items().get()))
                {   dataMap.put(item, data);
                }
            }
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.FUEL_DATA);
            for (Map.Entry<Item, FuelData> entry : dataMap.entries())
            {   holder.get().put(entry.getKey(), entry.getValue());
            }
        };
        BOILER_FUEL = addSetting("boiler_fuel_items", FastMultiMap::new, holder -> fuelAdder.accept(FuelData.FuelType.BOILER, ItemSettingsConfig.BOILER_FUELS, holder));
        ICEBOX_FUEL = addSetting("icebox_fuel_items", FastMultiMap::new, holder -> fuelAdder.accept(FuelData.FuelType.ICEBOX, ItemSettingsConfig.ICEBOX_FUELS, holder));
        HEARTH_FUEL = addSetting("hearth_fuel_items", FastMultiMap::new, holder -> fuelAdder.accept(FuelData.FuelType.HEARTH, ItemSettingsConfig.HEARTH_FUELS, holder));

        SOULSPRING_LAMP_FUEL = addSyncedSetting("lamp_fuel_items", FastMultiMap::new, holder -> fuelAdder.accept(FuelData.FuelType.SOUL_LAMP, ItemSettingsConfig.SOULSPRING_LAMP_FUELS, holder),
        (encoder) -> ConfigHelper.serializeItemMultimap(encoder, "LampFuelItems", FuelData::serialize),
        (decoder) -> ConfigHelper.deserializeItemMultimap(decoder, "LampFuelItems", FuelData::deserialize),
        (saver) -> ConfigHelper.writeItemMultimap(saver,
                                             list -> ItemSettingsConfig.getInstance().setSoulLampFuelItems(list),
                                             fuel -> List.of(fuel.fuel(), fuel.data().nbt().tag().toString())));

        HEARTH_POTIONS_ENABLED = addSetting("hearth_potions_enabled", () -> true, holder -> holder.set(ItemSettingsConfig.getInstance().arePotionsEnabled()));
        HEARTH_POTION_BLACKLIST = addSetting("hearth_potion_blacklist", ArrayList::new,
                                             holder -> holder.get().addAll(ItemSettingsConfig.getInstance().getPotionBlacklist()
                                                       .stream()
                                                       .map(entry -> ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(entry)))
                                                       .collect(ArrayList::new, List::add, List::addAll)));

        TriConsumer<ForgeConfigSpec.ConfigValue<List<? extends List<?>>>, DynamicHolder<Multimap<Item, InsulatorData>>, Insulation.Slot> insulatorAdder =
        (configValue, holder, slot) ->
        {
            // Read the insulation items from the config
            Multimap<Item, InsulatorData> dataMap = new FastMultiMap<>();
            for (List<?> list : configValue.get())
            {
                InsulatorData data = InsulatorData.fromToml(list, slot);
                if (data == null) continue;

                for (Item item : RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ITEMS, data.data().items().get()))
                {   dataMap.put(item, data);
                }
            }
            // Handle registry removals
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.INSULATOR_DATA);
            // Add entries
            holder.get().putAll(dataMap);
        };
        INSULATION_ITEMS = addSyncedSetting("insulation_items", FastMultiMap::new, holder ->
        {   insulatorAdder.accept(ItemSettingsConfig.INSULATION_ITEMS, holder, Insulation.Slot.ITEM);
        },
        (encoder) -> ConfigHelper.serializeItemMultimap(encoder, "InsulationItems", InsulatorData::serialize),
        (decoder) -> ConfigHelper.deserializeItemMultimap(decoder, "InsulationItems", InsulatorData::deserialize),
        (saver) -> ConfigHelper.writeItemInsulations(saver, list -> ItemSettingsConfig.getInstance().setInsulationItems(list)));

        INSULATING_ARMORS = addSyncedSetting("insulating_armors", FastMultiMap::new, holder ->
        {   insulatorAdder.accept(ItemSettingsConfig.INSULATING_ARMOR, holder, Insulation.Slot.ARMOR);
        },
        (encoder) -> ConfigHelper.serializeItemMultimap(encoder, "InsulatingArmors", InsulatorData::serialize),
        (decoder) -> ConfigHelper.deserializeItemMultimap(decoder, "InsulatingArmors", InsulatorData::deserialize),
        (saver) -> ConfigHelper.writeItemInsulations(saver, list -> ItemSettingsConfig.getInstance().setInsulatingArmorItems(list)));

        INSULATING_CURIOS = addSyncedSetting("insulating_curios", FastMultiMap::new, holder ->
        {
            if (CompatManager.isCuriosLoaded())
            {   insulatorAdder.accept(ItemSettingsConfig.INSULATING_CURIOS, holder, Insulation.Slot.CURIO);
            }
        },
        (encoder) -> ConfigHelper.serializeItemMultimap(encoder, "InsulatingCurios", InsulatorData::serialize),
        (decoder) -> ConfigHelper.deserializeItemMultimap(decoder, "InsulatingCurios", InsulatorData::deserialize),
        (saver) ->
        {   if (CompatManager.isCuriosLoaded())
            {   ConfigHelper.writeItemInsulations(saver, list -> ItemSettingsConfig.getInstance().setInsulatingCurios(list));
            }
        });

        INSULATION_SLOTS = addSyncedSetting("insulation_slots", () -> new ScalingFormula.Static(0, 0, 0, 0), holder ->
        {
            List<?> list = ItemSettingsConfig.getInstance().getArmorInsulationSlots();
            // Handle legacy insulation notation
            if (list.size() == 4 && list.stream().allMatch(el -> el instanceof Integer))
            {   list = List.of("static", list.get(0), list.get(1), list.get(2), list.get(3));
            }
            String mode = ((String) list.get(0));

            ScalingFormula.Type scalingType = ScalingFormula.Type.byName(mode);
            List<? extends Number> values = list.subList(1, list.size()).stream().map(o -> (Number) o).toList();

            holder.set(scalingType == ScalingFormula.Type.STATIC
                                      ? new ScalingFormula.Static(values.get(0).intValue(),
                                                                  values.get(1).intValue(),
                                                                  values.get(2).intValue(),
                                                                  values.get(3).intValue())
                                      : new ScalingFormula.Dynamic(scalingType,
                                                                   values.get(0).doubleValue(),
                                                                   values.size() > 2 ? values.get(2).doubleValue() : Double.MAX_VALUE));
        },
        (encoder) -> encoder.serialize(),
        (decoder) -> ScalingFormula.deserialize(decoder),
        (saver) ->
        {
            List<?> list = ListBuilder.begin(saver.getType().getSerializedName())
                                      .addAll(saver.getValues())
                                      .build();
            ItemSettingsConfig.getInstance().setArmorInsulationSlots(list);
        });

        INSULATION_BLACKLIST = addSetting("insulation_blacklist", ArrayList::new,
                                          holder -> holder.get().addAll(ItemSettingsConfig.getInstance().getInsulationBlacklist()
                                                    .stream()
                                                    .map(entry -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(entry)))
                                                    .collect(ArrayList::new, List::add, List::addAll)));

        CHECK_SLEEP_CONDITIONS = addSetting("check_sleep_conditions", () -> true, holder -> holder.set(WorldSettingsConfig.getInstance().isSleepChecked()));

        SLEEP_CHECK_IGNORE_BLOCKS = addSetting("sleep_check_override_blocks", ArrayList::new, holder -> holder.get().addAll(ConfigHelper.getBlocks(WorldSettingsConfig.getInstance().getSleepOverrideBlocks().toArray(new String[0]))));

        USE_CUSTOM_WATER_FREEZE_BEHAVIOR = addSetting("custom_freeze_check", () -> true, holder -> holder.set(WorldSettingsConfig.CUSTOM_WATER_FREEZE_BEHAVIOR.get()));

        USE_CUSTOM_ICE_DROPS = addSetting("custom_ice_drops", () -> true, holder -> holder.set(WorldSettingsConfig.CUSTOM_ICE_DROPS.get()));

        FOOD_TEMPERATURES = addSyncedSetting("food_temperatures", FastMultiMap::new, holder ->
        {
            // Read the food temperatures from the config
            Multimap<Item, FoodData> dataMap = new FastMultiMap<>();
            for (List<?> list : ItemSettingsConfig.FOOD_TEMPERATURES.get())
            {
                FoodData data = FoodData.fromToml(list);
                if (data == null) continue;

                for (Item item : RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ITEMS, data.data().items().get()))
                {   dataMap.put(item, data);
                }
            }
            // Handle registry removals
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.FOOD_DATA);
            // Add entries
            holder.get().putAll(dataMap);
        },
        (encoder) -> ConfigHelper.serializeItemMultimap(encoder, "FoodTemperatures", FoodData::serialize),
        (decoder) -> ConfigHelper.deserializeItemMultimap(decoder, "FoodTemperatures", FoodData::deserialize),
        (saver) -> ConfigHelper.writeItemMultimap(saver,
                                                  list -> ItemSettingsConfig.getInstance().setFoodTemperatures(list),
                                                  food -> ListBuilder.begin(food.temperature(), food.data().nbt().tag().toString())
                                                                     .addIf(food.duration() > 0, food::duration).build()));

        CARRIED_ITEM_TEMPERATURES = addSyncedSetting("carried_item_temps", FastMultiMap::new, holder ->
        {
            // Read the insulation items from the config
            Multimap<Item, ItemCarryTempData> dataMap = new FastMultiMap<>();
            for (List<?> list : ItemSettingsConfig.CARRIED_ITEM_TEMPERATURE.get())
            {
                ItemCarryTempData data = ItemCarryTempData.fromToml(list);
                if (data == null) continue;

                for (Item item : RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ITEMS, data.data().items().get()))
                {   dataMap.put(item, data);
                }
            }
            // Handle registry removals
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.CARRY_TEMP_DATA);
            // Add entries
            holder.get().putAll(dataMap);
        },
        (encoder) -> ConfigHelper.serializeItemMultimap(encoder, "CarriedItemTemps", ItemCarryTempData::serialize),
        (decoder) -> ConfigHelper.deserializeItemMultimap(decoder, "CarriedItemTemps", ItemCarryTempData::deserialize),
        (saver) ->
        {
            ConfigHelper.writeItemMultimap(saver,
            list -> ItemSettingsConfig.getInstance().setCarriedTemps(list),
            temp ->
            {
                List<Object> entry = new ArrayList<>();
                // Temperature
                entry.add(temp.temperature());
                // Slot types
                String strictType = temp.getSlotRangeName();
                if (strictType.isEmpty()) return null;
                entry.add(strictType);
                // Trait
                entry.add(temp.trait().getSerializedName());
                // NBT data
                if (!temp.data().nbt().tag().isEmpty())
                {   entry.add(temp.data().nbt().tag().toString());
                }
                return entry;
            });
        });

        WATERSKIN_STRENGTH = addSetting("waterskin_strength", () -> 50, holder -> holder.set(ItemSettingsConfig.getInstance().getWaterskinStrength()));

        SOULSPRING_LAMP_STRENGTH = addSetting("soulspring_lamp_strength", () -> 0.6d, holder -> holder.set(ItemSettingsConfig.SOULSPRING_LAMP_STRENGTH.get()));

        LAMP_DIMENSIONS = addSettingWithRegistries("valid_lamp_dimensions", ArrayList::new,
                                                   (holder, registryAccess) -> holder.get(registryAccess).addAll(new ArrayList<>(ItemSettingsConfig.getInstance().getValidSoulLampDimensions()
                                                                           .stream()
                                                                           .map(entry -> registryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).get(new ResourceLocation(entry)))
                                                                           .collect(ArrayList::new, List::add, List::addAll))));

        FUR_TIMINGS = addSyncedSetting("fur_timings", () -> new Triplet<>(0, 0, 0d), holder ->
        {   List<?> entry = EntitySettingsConfig.getInstance().getGoatFurStats();
            holder.set(new Triplet<>(((Number) entry.get(0)).intValue(), ((Number) entry.get(1)).intValue(), ((Number) entry.get(2)).doubleValue()));
        },
        (encoder) ->
        {   CompoundTag tag = new CompoundTag();
            tag.put("Interval", IntTag.valueOf(encoder.getA()));
            tag.put("Cooldown", IntTag.valueOf(encoder.getB()));
            tag.put("Chance", DoubleTag.valueOf(encoder.getC()));
            return tag;
        },
        (decoder) ->
        {   int interval = decoder.getInt("Interval");
            int cooldown = decoder.getInt("Cooldown");
            double chance = decoder.getDouble("Chance");
            return new Triplet<>(interval, cooldown, chance);
        },
        (saver) ->
        {   List<Number> list = new ArrayList<>();
            list.add(saver.getA());
            list.add(saver.getB());
            list.add(saver.getC());
            EntitySettingsConfig.getInstance().setGoatFurStats(list);
        });

        SHED_TIMINGS = addSyncedSetting("shed_timings", () -> new Triplet<>(0, 0, 0d), holder ->
        {
            List<?> entry = EntitySettingsConfig.getInstance().getChameleonShedStats();
            holder.set(new Triplet<>(((Number) entry.get(0)).intValue(), ((Number) entry.get(1)).intValue(), ((Number) entry.get(2)).doubleValue()));
        },
        (encoder) ->
        {   CompoundTag tag = new CompoundTag();
            tag.put("Interval", IntTag.valueOf(encoder.getA()));
            tag.put("Cooldown", IntTag.valueOf(encoder.getB()));
            tag.put("Chance", DoubleTag.valueOf(encoder.getC()));
            return tag;
        },
        (decoder) ->
        {   int interval = decoder.getInt("Interval");
            int cooldown = decoder.getInt("Cooldown");
            double chance = decoder.getDouble("Chance");
            return new Triplet<>(interval, cooldown, chance);
        },
        (saver) ->
        {   List<Number> list = new ArrayList<>();
            list.add(saver.getA());
            list.add(saver.getB());
            list.add(saver.getC());
            EntitySettingsConfig.getInstance().setChameleonShedStats(list);
        });

        ENTITY_SPAWN_BIOMES = addSettingWithRegistries("entity_spawn_biomes", FastMultiMap::new, (holder, registryAccess) ->
        {
            // Function to read biomes from configs and put them in the config settings
            BiConsumer<List<? extends List<?>>, EntityType<?>> configReader = (configBiomes, entityType) ->
            {
                Multimap<Biome, SpawnBiomeData> dataMap = HashMultimap.create();
                for (List<?> list : configBiomes)
                {
                    SpawnBiomeData data = SpawnBiomeData.fromToml(list, entityType, registryAccess);
                    if (data == null) continue;

                    for (Biome biome : RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.BIOMES, data.biomes()))
                    {   dataMap.put(biome, data);
                    }
                }
                holder.get(registryAccess).putAll(dataMap);
            };

            // Parse goat and chameleon biomes
            configReader.accept(EntitySettingsConfig.getInstance().getChameleonSpawnBiomes(), ModEntities.CHAMELEON);
            configReader.accept(EntitySettingsConfig.getInstance().getGoatSpawnBiomes(), EntityType.GOAT);
        });

        INSULATED_MOUNTS = addSetting("insulated_entities", FastMultiMap::new, holder ->
        {
            // Read the insulation items from the config
            Multimap<EntityType<?>, MountData> dataMap = new FastMultiMap<>();
            for (List<?> list : EntitySettingsConfig.INSULATED_MOUNTS.get())
            {
                MountData data = MountData.fromToml(list);
                for (EntityType<?> entityType : RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ENTITIES, data.entities()))
                {   dataMap.put(entityType, MountData.fromToml(list));
                }
            }
            // Handle registry removals
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.MOUNT_DATA);
            // Add entries
            holder.get().putAll(dataMap);
        });

        ENTITY_TEMPERATURES = addSetting("entity_temperatures", FastMultiMap::new, holder ->
        {
            // Read the insulation items from the config
            Multimap<EntityType<?>, EntityTempData> dataMap = new FastMultiMap<>();
            for (List<?> list : EntitySettingsConfig.ENTITY_TEMPERATURES.get())
            {
                EntityTempData data = EntityTempData.fromToml(list);
                if (data == null) continue;

                for (EntityType<?> entityType : RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ENTITIES, data.entity().entities().get()))
                {   dataMap.put(entityType, data);
                }
            }
            // Handle registry removals
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.ENTITY_TEMP_DATA);
            // Add entries
            holder.get().putAll(dataMap);
        });

        BLOCK_RANGE = addSyncedSetting("block_range", () -> 7, holder -> holder.set(WorldSettingsConfig.MAX_BLOCK_TEMP_RANGE.get()),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "BlockRange"),
        (decoder) -> decoder.getInt("BlockRange"),
        (saver) -> WorldSettingsConfig.MAX_BLOCK_TEMP_RANGE.set(saver));

        COLD_SOUL_FIRE = addSetting("cold_soul_fire", () -> true, holder -> holder.set(WorldSettingsConfig.getInstance().isSoulFireCold()));

        HEARTH_SPREAD_WHITELIST = addSyncedSetting("hearth_spread_whitelist", ArrayList::new, holder -> holder.get().addAll(ConfigHelper.getBlocks(WorldSettingsConfig.getInstance().getHearthSpreadWhitelist().toArray(new String[0]))),
        (encoder) ->
        {
            CompoundTag tag = new CompoundTag();
            ListTag list = new ListTag();
            for (Block entry : encoder)
            {   list.add(StringTag.valueOf(ForgeRegistries.BLOCKS.getKey(entry).toString()));
            }
            tag.put("HearthWhitelist", list);
            return tag;
        },
        (decoder) ->
        {
            List<Block> list = new ArrayList<>();
            for (Tag entry : decoder.getList("HearthWhitelist", 8))
            {   list.add(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.getAsString())));
            }
            return list;
        },
        saver -> WorldSettingsConfig.setHearthSpreadWhitelist(saver.stream().map(ForgeRegistries.BLOCKS::getKey).toList()));

        HEARTH_SPREAD_BLACKLIST = addSyncedSetting("hearth_spread_blacklist", ArrayList::new, holder -> holder.get().addAll(ConfigHelper.getBlocks(WorldSettingsConfig.HEARTH_SPREAD_BLACKLIST.get().toArray(new String[0]))),
        (encoder) ->
        {
            CompoundTag tag = new CompoundTag();
            ListTag list = new ListTag();
            for (Block entry : encoder)
            {   list.add(StringTag.valueOf(ForgeRegistries.BLOCKS.getKey(entry).toString()));
            }
            tag.put("HearthBlacklist", list);
            return tag;
        },
        (decoder) ->
        {
            List<Block> list = new ArrayList<>();
            for (Tag entry : decoder.getList("HearthBlacklist", 8))
            {   list.add(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.getAsString())));
            }
            return list;
        },
        saver -> WorldSettingsConfig.setHearthSpreadBlacklist(saver.stream().map(ForgeRegistries.BLOCKS::getKey).toList()));

        HEARTH_STRENGTH = addSetting("hearth_effect", () -> 0.75, holder -> holder.set(WorldSettingsConfig.HEARTH_INSULATION_STRENGTH.get()));

        SMART_HEARTH = addSetting("smart_hearth", () -> false, holder -> holder.set(WorldSettingsConfig.ENABLE_SMART_HEARTH.get()));

        INSULATION_STRENGTH = addSetting("insulation_strength", () -> 1d, holder -> holder.set(ItemSettingsConfig.INSULATION_STRENGTH.get()));

        DISABLED_MODIFIERS = addSetting("disabled_modifiers", ArrayList::new, holder -> holder.get().addAll(MainSettingsConfig.DISABLED_TEMP_MODIFIERS.get().stream().map(ResourceLocation::new).toList()));

        // Client

        CELSIUS = addClientSetting("celsius", () -> false, holder -> holder.set(ClientSettingsConfig.USE_CELSIUS.get()));

        TEMP_OFFSET = addClientSetting("temp_offset", () -> 0, holder -> holder.set(ClientSettingsConfig.TEMPERATURE_OFFSET.get()));

        TEMP_SMOOTHING = addClientSetting("temp_smoothing", () -> 10d, holder -> holder.set(ClientSettingsConfig.TEMPERATURE_SMOOTHING.get()));

        BODY_ICON_POS = addClientSetting("body_icon_pos", Vec2i::new, holder -> holder.set(new Vec2i(ClientSettingsConfig.getBodyIconX(),
                                                                  ClientSettingsConfig.getBodyIconY())));
        BODY_ICON_ENABLED = addClientSetting("body_icon_enabled", () -> true, holder -> holder.set(ClientSettingsConfig.SHOW_BODY_TEMP_ICON.get()));

        MOVE_BODY_ICON_WHEN_ADVANCED = addClientSetting("move_body_icon_for_advanced", () -> true, holder -> holder.set(ClientSettingsConfig.MOVE_BODY_TEMP_ICON_ADVANCED.get()));

        BODY_READOUT_POS = addClientSetting("body_readout_pos", Vec2i::new, holder -> holder.set(new Vec2i(ClientSettingsConfig.getBodyReadoutX(),
                                                                                ClientSettingsConfig.getBodyReadoutY())));
        BODY_READOUT_ENABLED = addClientSetting("body_readout_enabled", () -> true, holder -> holder.set(ClientSettingsConfig.SHOW_BODY_TEMP_READOUT.get()));

        WORLD_GAUGE_POS = addClientSetting("world_gauge_pos", Vec2i::new, holder -> holder.set(new Vec2i(ClientSettingsConfig.getWorldGaugeX(),
                                                                    ClientSettingsConfig.getWorldGaugeY())));
        WORLD_GAUGE_ENABLED = addClientSetting("world_gauge_enabled", () -> true, holder -> holder.set(ClientSettingsConfig.SHOW_WORLD_TEMP_GAUGE.get()));

        CUSTOM_HOTBAR_LAYOUT = addClientSetting("custom_hotbar_layout", () -> true, holder -> holder.set(ClientSettingsConfig.USE_CUSTOM_HOTBAR_LAYOUT.get()));
        ICON_BOBBING = addClientSetting("icon_bobbing", () -> true, holder -> holder.set(ClientSettingsConfig.ENABLE_ICON_BOBBING.get()));

        HEARTH_DEBUG = addClientSetting("hearth_debug", () -> true, holder -> holder.set(ClientSettingsConfig.SHOW_HEARTH_DEBUG_VISUALS.get()));

        SHOW_CONFIG_BUTTON = addClientSetting("show_config_button", () -> true, holder -> holder.set(ClientSettingsConfig.SHOW_CONFIG_BUTTON.get()));
        CONFIG_BUTTON_POS = addClientSetting("config_button_pos", Vec2i::new, holder -> holder.set(new Vec2i(ClientSettingsConfig.getConfigButtonX(),
                                                                          ClientSettingsConfig.getConfigButtonY())));

        DISTORTION_EFFECTS = addClientSetting("distortion_effects", () -> true, holder -> holder.set(ClientSettingsConfig.SHOW_SCREEN_DISTORTIONS.get()));

        HIGH_CONTRAST = addClientSetting("high_contrast", () -> false, holder -> holder.set(ClientSettingsConfig.HIGH_CONTRAST_MODE.get()));

        SHOW_CREATIVE_WARNING = addClientSetting("show_creative_warning", () -> true, holder -> holder.set(ClientSettingsConfig.ENABLE_CREATIVE_WARNING.get()));

        HIDE_TOOLTIPS = addClientSetting("hide_tooltips", () -> false, holder -> holder.set(ClientSettingsConfig.HIDE_INSULATION_TOOLTIPS.get()));
        EXPAND_TOOLTIPS = addClientSetting("expand_tooltips", () -> true, holder -> holder.set(ClientSettingsConfig.EXPAND_TOOLTIPS.get()));

        SHOW_WATER_EFFECT = addClientSetting("show_water_effect", () -> true, holder -> holder.set(ClientSettingsConfig.SHOW_WATER_EFFECT.get()));

        boolean ssLoaded = CompatManager.isSereneSeasonsLoaded();
        SUMMER_TEMPS = addSetting("summer_temps", () -> new Double[]{0d, 0d, 0d}, holder -> holder.set(ssLoaded ? WorldSettingsConfig.getSummerTemps() : new Double[3]));
        AUTUMN_TEMPS = addSetting("autumn_temps", () -> new Double[]{0d, 0d, 0d}, holder -> holder.set(ssLoaded ? WorldSettingsConfig.getAutumnTemps() : new Double[3]));
        WINTER_TEMPS = addSetting("winter_temps", () -> new Double[]{0d, 0d, 0d}, holder -> holder.set(ssLoaded ? WorldSettingsConfig.getWinterTemps() : new Double[3]));
        SPRING_TEMPS = addSetting("spring_temps", () -> new Double[]{0d, 0d, 0d}, holder -> holder.set(ssLoaded ? WorldSettingsConfig.getSpringTemps() : new Double[3]));
    }

    public static String getKey(DynamicHolder<?> setting)
    {   return CONFIG_SETTINGS.inverse().get(setting);
    }

    public static DynamicHolder<?> getSetting(String key)
    {   return CONFIG_SETTINGS.get(key);
    }

    public enum Difficulty
    {
        SUPER_EASY(() -> Map.of(
            getKey(MIN_TEMP), () -> Temperature.convert(40, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(MAX_TEMP), () -> Temperature.convert(120, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(TEMP_RATE), () -> 0.5,
            getKey(REQUIRE_THERMOMETER), () -> false,
            getKey(FIRE_RESISTANCE_ENABLED), () -> true,
            getKey(ICE_RESISTANCE_ENABLED), () -> true
        )),

        EASY(() -> Map.of(
            getKey(MIN_TEMP), () -> Temperature.convert(45, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(MAX_TEMP), () -> Temperature.convert(110, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(TEMP_RATE), () -> 0.75,
            getKey(REQUIRE_THERMOMETER), () -> false,
            getKey(FIRE_RESISTANCE_ENABLED), () -> true,
            getKey(ICE_RESISTANCE_ENABLED), () -> true
        )),

        NORMAL(() -> Map.of(
            getKey(MIN_TEMP), () -> Temperature.convert(50, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(MAX_TEMP), () -> Temperature.convert(100, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(TEMP_RATE), () -> 1.0,
            getKey(REQUIRE_THERMOMETER), () -> true,
            getKey(FIRE_RESISTANCE_ENABLED), () -> true,
            getKey(ICE_RESISTANCE_ENABLED), () -> true
        )),

        HARD(() -> Map.of(
            getKey(MIN_TEMP), () -> Temperature.convert(55, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(MAX_TEMP), () -> Temperature.convert(90, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(TEMP_RATE), () -> 1.25,
            getKey(REQUIRE_THERMOMETER), () -> true,
            getKey(FIRE_RESISTANCE_ENABLED), () -> false,
            getKey(ICE_RESISTANCE_ENABLED), () -> false
        )),

        CUSTOM(() -> Map.of());

        private final Supplier<Map<String, Supplier<?>>> settingsSupplier;
        private Map<String, Supplier<?>> settings;

        Difficulty(Supplier<Map<String, Supplier<?>>> settings)
        {   this.settingsSupplier = settings;
        }

        private void ensureSettingsGenerated()
        {   if (settings == null) settings = settingsSupplier.get();
        }

        public <T> T getSetting(String id)
        {
            this.ensureSettingsGenerated();
            return (T) settings.get(id).get();
        }

        public <T> T getSetting(DynamicHolder<T> config)
        {
            this.ensureSettingsGenerated();
            return (T) settings.get(getKey(config)).get();
        }

        public <T> T getOrDefault(String id, T defaultValue)
        {
            this.ensureSettingsGenerated();
            return (T) settings.getOrDefault(id, () -> defaultValue).get();
        }

        public <T> T getOrDefault(DynamicHolder<T> config, T defaultValue)
        {
            this.ensureSettingsGenerated();
            return (T) settings.getOrDefault(getKey(config), () -> defaultValue).get();
        }

        public void load()
        {
            this.ensureSettingsGenerated();
            settings.forEach((id, loader) -> ConfigSettings.getSetting(id).setUnsafe(loader.get()));
        }

        public int getId()
        {   return this.ordinal();
        }

        public static Difficulty byId(int id)
        {   return values()[id];
        }

        public static Component getFormattedName(Difficulty difficulty)
        {
            return switch (difficulty)
            {   case SUPER_EASY  -> new TranslatableComponent("cold_sweat.config.difficulty.super_easy.name");
                case EASY  -> new TranslatableComponent("cold_sweat.config.difficulty.easy.name");
                case NORMAL  -> new TranslatableComponent("cold_sweat.config.difficulty.normal.name");
                case HARD  -> new TranslatableComponent("cold_sweat.config.difficulty.hard.name");
                default -> new TranslatableComponent("cold_sweat.config.difficulty.custom.name");
            };
        }
    }

    public static <T> DynamicHolder<T> addSetting(String id, Supplier<T> defaultVal, Consumer<DynamicHolder<T>> loader)
    {   DynamicHolder<T> holder = DynamicHolder.create(defaultVal, loader);
        CONFIG_SETTINGS.put(id, holder);
        return holder;
    }

    public static <T> DynamicHolder<T> addSettingWithRegistries(String id, Supplier<T> defaultVal, DynamicHolder.Loader<T> loader)
    {   DynamicHolder<T> holder = DynamicHolder.createWithRegistries(defaultVal, loader);
        CONFIG_SETTINGS.put(id, holder);
        return holder;
    }

    public static <T> DynamicHolder<T> addSyncedSetting(String id, Supplier<T> defaultVal, Consumer<DynamicHolder<T>> loader, Function<T, CompoundTag> writer, Function<CompoundTag, T> reader, Consumer<T> saver)
    {   DynamicHolder<T> holder = DynamicHolder.createSynced(defaultVal, loader, writer, reader, saver);
        CONFIG_SETTINGS.put(id, holder);
        return holder;
    }

    public static <T> DynamicHolder<T> addSyncedSettingWithRegistries(String id, Supplier<T> defaultVal, DynamicHolder.Loader<T> loader, DynamicHolder.Writer<T> writer, DynamicHolder.Reader<T> reader, DynamicHolder.Saver<T> saver)
    {   DynamicHolder<T> holder = DynamicHolder.createSyncedWithRegistries(defaultVal, loader, writer, reader, saver);
        CONFIG_SETTINGS.put(id, holder);
        return holder;
    }

    public static <T> DynamicHolder<T> addClientSetting(String id, Supplier<T> defaultVal, Consumer<DynamicHolder<T>> loader)
    {
        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            DynamicHolder<T> holder = DynamicHolder.create(defaultVal, loader);
            CLIENT_SETTINGS.put(id, holder);
            return holder;
        }
        else return DynamicHolder.create(() -> null, (value) -> {});
    }

    public static Map<String, CompoundTag> encode(RegistryAccess registryAccess)
    {
        Map<String, CompoundTag> map = new HashMap<>();
        CONFIG_SETTINGS.forEach((key, value) ->
        {   if (value.isSynced())
            {   map.put(key, value.encode(registryAccess));
            }
        });
        return map;
    }

    public static void decode(String key, CompoundTag tag, RegistryAccess registryAccess)
    {
        CONFIG_SETTINGS.computeIfPresent(key, (k, value) ->
        {   value.decode(tag, registryAccess);
            return value;
        });
    }

    public static void saveValues(RegistryAccess registryAccess)
    {
        CONFIG_SETTINGS.values().forEach(value ->
        {   if (value.isSynced())
            {   value.save(registryAccess);
            }
        });
    }

    public static void load(RegistryAccess registryAccess, boolean replace)
    {
        if (registryAccess != null)
        {   CONFIG_SETTINGS.values().forEach(dynamicHolder -> dynamicHolder.load(registryAccess, replace));
        }
        else
        {
            ColdSweat.LOGGER.warn("Loading Cold Sweat config settings without registry access. This is normal during startup.");
            CONFIG_SETTINGS.values().forEach(dynamicHolder ->
            {
                if (!dynamicHolder.requiresRegistries())
                {   dynamicHolder.load(replace);
                }
            });
        }
    }

    public static void clear()
    {
        for (Map.Entry<String, DynamicHolder<?>> entry : CONFIG_SETTINGS.entrySet())
        {   entry.getValue().reset();
        }
    }
}
