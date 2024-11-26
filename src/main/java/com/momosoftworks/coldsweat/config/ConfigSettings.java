package com.momosoftworks.coldsweat.config;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Multimap;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.insulation.slot.ScalingFormula;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.config.spec.*;
import com.momosoftworks.coldsweat.data.ModRegistries;
import com.momosoftworks.coldsweat.data.codec.configuration.*;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.FastMap;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import com.momosoftworks.coldsweat.util.math.Vec2i;
import com.momosoftworks.coldsweat.util.registries.ModEntities;
import com.momosoftworks.coldsweat.util.serialization.*;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.nbt.*;
import net.minecraft.potion.Effect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.thread.EffectiveSide;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.util.TriConsumer;

import javax.xml.ws.Holder;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.momosoftworks.coldsweat.util.serialization.DynamicHolder.SyncType;

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
    public static final DynamicHolder<Map<Structure<?>, StructureTempData>> STRUCTURE_TEMPS;
    public static final DynamicHolder<Map<Structure<?>, StructureTempData>> STRUCTURE_OFFSETS;
    public static final DynamicHolder<List<DepthTempData>> DEPTH_REGIONS;
    public static final DynamicHolder<Boolean> CHECK_SLEEP_CONDITIONS;
    public static final DynamicHolder<Double[]> SUMMER_TEMPS;
    public static final DynamicHolder<Double[]> AUTUMN_TEMPS;
    public static final DynamicHolder<Double[]> WINTER_TEMPS;
    public static final DynamicHolder<Double[]> SPRING_TEMPS;

    // Block settings
    public static final DynamicHolder<Integer> BLOCK_RANGE;
    public static final DynamicHolder<Boolean> COLD_SOUL_FIRE;
    public static final DynamicHolder<List<Block>> THERMAL_SOURCE_SPREAD_WHITELIST;
    public static final DynamicHolder<List<Block>> THERMAL_SOURCE_SPREAD_BLACKLIST;
    public static final DynamicHolder<Double> THERMAL_SOURCE_STRENGTH;

    public static final DynamicHolder<Boolean> SMART_HEARTH;
    public static final DynamicHolder<Integer> HEARTH_MAX_RANGE;
    public static final DynamicHolder<Integer> HEARTH_RANGE;
    public static final DynamicHolder<Integer> HEARTH_MAX_VOLUME;
    public static final DynamicHolder<Integer> HEARTH_WARM_UP_TIME;
    public static final DynamicHolder<Integer> HEARTH_MAX_INSULATION;

    public static final DynamicHolder<Boolean> SMART_BOILER;
    public static final DynamicHolder<Integer> BOILER_MAX_RANGE;
    public static final DynamicHolder<Integer> BOILER_RANGE;
    public static final DynamicHolder<Integer> BOILER_MAX_VOLUME;
    public static final DynamicHolder<Integer> BOILER_WARM_UP_TIME;
    public static final DynamicHolder<Integer> BOILER_MAX_INSULATION;

    public static final DynamicHolder<Boolean> SMART_ICEBOX;
    public static final DynamicHolder<Integer> ICEBOX_MAX_RANGE;
    public static final DynamicHolder<Integer> ICEBOX_RANGE;
    public static final DynamicHolder<Integer> ICEBOX_MAX_VOLUME;
    public static final DynamicHolder<Integer> ICEBOX_WARM_UP_TIME;
    public static final DynamicHolder<Integer> ICEBOX_MAX_INSULATION;

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
    public static final DynamicHolder<List<Effect>> HEARTH_POTION_BLACKLIST;

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
        DIFFICULTY = addSyncedSetting("difficulty", () -> Difficulty.NORMAL, holder -> holder.set(Difficulty.byId(MainSettingsConfig.DIFFICULTY.get())),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder.getId(), "Difficulty"),
        (decoder) -> Difficulty.byId(decoder.getInt("Difficulty")),
        (saver) -> MainSettingsConfig.DIFFICULTY.set(saver.getId()),
        SyncType.BOTH_WAYS);

        MAX_TEMP = addSyncedSetting("max_temp", () -> 1.7, holder -> holder.set(MainSettingsConfig.MAX_HABITABLE_TEMPERATURE.get()),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "MaxTemp"),
        (decoder) -> decoder.getDouble("MaxTemp"),
        (saver) -> MainSettingsConfig.MAX_HABITABLE_TEMPERATURE.set(saver),
        SyncType.BOTH_WAYS);

        MIN_TEMP = addSyncedSetting("min_temp", () -> 0.5, holder -> holder.set(MainSettingsConfig.MIN_HABITABLE_TEMPERATURE.get()),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "MinTemp"),
        (decoder) -> decoder.getDouble("MinTemp"),
        (saver) -> MainSettingsConfig.MIN_HABITABLE_TEMPERATURE.set(saver),
        SyncType.BOTH_WAYS);

        TEMP_RATE = addSyncedSetting("temp_rate", () -> 1d, holder -> holder.set(MainSettingsConfig.TEMP_RATE_MULTIPLIER.get()),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "TempRate"),
        (decoder) -> decoder.getDouble("TempRate"),
        (saver) -> MainSettingsConfig.TEMP_RATE_MULTIPLIER.set(saver),
        SyncType.BOTH_WAYS);

        TEMP_DAMAGE = addSyncedSetting("temp_damage", () -> 2d, holder -> holder.set(MainSettingsConfig.TEMP_DAMAGE.get()),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "TempDamage"),
        (decoder) -> decoder.getDouble("TempDamage"),
        (saver) -> MainSettingsConfig.TEMP_DAMAGE.set(saver),
        SyncType.BOTH_WAYS);

        FIRE_RESISTANCE_ENABLED = addSyncedSetting("fire_resistance_enabled", () -> true, holder -> holder.set(MainSettingsConfig.FIRE_RESISTANCE_BLOCKS_OVERHEATING.get()),
        (encoder) -> ConfigHelper.serializeNbtBool(encoder, "FireResistanceEnabled"),
        (decoder) -> decoder.getBoolean("FireResistanceEnabled"),
        (saver) -> MainSettingsConfig.FIRE_RESISTANCE_BLOCKS_OVERHEATING.set(saver),
        SyncType.BOTH_WAYS);

        ICE_RESISTANCE_ENABLED = addSyncedSetting("ice_resistance_enabled", () -> true, holder -> holder.set(MainSettingsConfig.ICE_RESISTANCE_BLOCKS_FREEZING.get()),
        (encoder) -> ConfigHelper.serializeNbtBool(encoder, "IceResistanceEnabled"),
        (decoder) -> decoder.getBoolean("IceResistanceEnabled"),
        (saver) -> MainSettingsConfig.ICE_RESISTANCE_BLOCKS_FREEZING.set(saver),
        SyncType.BOTH_WAYS);

        USE_PEACEFUL_MODE = addSyncedSetting("use_peaceful", () -> true, holder -> holder.set(MainSettingsConfig.NULLIFY_IN_PEACEFUL.get()),
        (encoder) -> ConfigHelper.serializeNbtBool(encoder, "UsePeaceful"),
        (decoder) -> decoder.getBoolean("UsePeaceful"),
        (saver) -> MainSettingsConfig.NULLIFY_IN_PEACEFUL.set(saver),
        SyncType.BOTH_WAYS);

        REQUIRE_THERMOMETER = addSyncedSetting("require_thermometer", () -> true, holder -> holder.set(MainSettingsConfig.REQUIRE_THERMOMETER.get()),
        (encoder) -> ConfigHelper.serializeNbtBool(encoder, "RequireThermometer"),
        (decoder) -> decoder.getBoolean("RequireThermometer"),
        (saver) -> MainSettingsConfig.REQUIRE_THERMOMETER.set(saver),
        SyncType.BOTH_WAYS);

        GRACE_LENGTH = addSyncedSetting("grace_length", () -> 6000, holder -> holder.set(MainSettingsConfig.GRACE_PERIOD_LENGTH.get()),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "GraceLength"),
        (decoder) -> decoder.getInt("GraceLength"),
        (saver) -> MainSettingsConfig.GRACE_PERIOD_LENGTH.set(saver),
        SyncType.BOTH_WAYS);

        GRACE_ENABLED = addSyncedSetting("grace_enabled", () -> true, holder -> holder.set(MainSettingsConfig.ENABLE_GRACE_PERIOD.get()),
        (encoder) -> ConfigHelper.serializeNbtBool(encoder, "GraceEnabled"),
        (decoder) -> decoder.getBoolean("GraceEnabled"),
        (saver) -> MainSettingsConfig.ENABLE_GRACE_PERIOD.set(saver),
        SyncType.BOTH_WAYS);


        HEARTS_FREEZING_PERCENTAGE = addSyncedSetting("hearts_freezing_percentage", () -> 0.5, holder -> holder.set(MainSettingsConfig.FREEZING_HEARTS.get()),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "HeartsFreezingPercentage"),
        (decoder) -> decoder.getDouble("HeartsFreezingPercentage"),
        (saver) -> MainSettingsConfig.FREEZING_HEARTS.set(saver),
        SyncType.BOTH_WAYS);

        COLD_MINING_IMPAIRMENT = addSyncedSetting("cold_mining_slowdown", () -> 0.5, holder -> holder.set(MainSettingsConfig.COLD_MINING.get()),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "ColdMiningImpairment"),
        (decoder) -> decoder.getDouble("ColdMiningImpairment"),
        (saver) -> MainSettingsConfig.COLD_MINING.set(saver),
        SyncType.BOTH_WAYS);

        COLD_MOVEMENT_SLOWDOWN = addSyncedSetting("cold_movement_slowdown", () -> 0.5, holder -> holder.set(MainSettingsConfig.COLD_MOVEMENT.get()),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "ColdMovementSlowdown"),
        (decoder) -> decoder.getDouble("ColdMovementSlowdown"),
        (saver) -> MainSettingsConfig.COLD_MOVEMENT.set(saver),
        SyncType.BOTH_WAYS);

        COLD_KNOCKBACK_REDUCTION = addSyncedSetting("cold_knockback_reduction", () -> 0.5, holder -> holder.set(MainSettingsConfig.COLD_KNOCKBACK.get()),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "ColdKnockbackReduction"),
        (decoder) -> decoder.getDouble("ColdKnockbackReduction"),
        (saver) -> MainSettingsConfig.COLD_KNOCKBACK.set(saver),
        SyncType.BOTH_WAYS);

        HEATSTROKE_FOG_DISTANCE = addSyncedSetting("heatstroke_fog_distance", () -> 6d, holder -> holder.set(MainSettingsConfig.HEATSTROKE_FOG.get()),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "HeatstrokeFogDistance"),
        (decoder) -> decoder.getDouble("HeatstrokeFogDistance"),
        (saver) -> MainSettingsConfig.HEATSTROKE_FOG.set(saver),
        SyncType.BOTH_WAYS);


        BIOME_TEMPS = addSyncedSettingWithRegistries("biome_temps", FastMap::new, (holder, registryAccess) ->
        {
            Map<Biome, BiomeTempData> dataMap = ConfigHelper.getRegistryMap(WorldSettingsConfig.BIOME_TEMPERATURES.get(), Registry.BIOME_REGISTRY,
                                                                            toml -> BiomeTempData.fromToml(toml, false, registryAccess), data -> data.biomes);
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.BIOME_TEMP_DATA);

            holder.get(registryAccess).putAll(dataMap);
        },
        (encoder, registryAccess) -> ConfigHelper.serializeHolderRegistry(encoder, "BiomeTemps", Registry.BIOME_REGISTRY, ModRegistries.BIOME_TEMP_DATA, registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeRegistry(decoder, "BiomeTemps", Registry.BIOME_REGISTRY, ModRegistries.BIOME_TEMP_DATA, registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.BIOME_TEMPERATURES.set(saver.entrySet().stream()
                                                            .map(entry ->
                                                            {
                                                                ResourceLocation biome = RegistryHelper.getKey(entry.getKey(), registryAccess);
                                                                if (biome == null) return null;

                                                                Temperature.Units units = entry.getValue().units;
                                                                double min = Temperature.convert(entry.getValue().minTemp(), Temperature.Units.MC, units, true);
                                                                double max = Temperature.convert(entry.getValue().maxTemp(), Temperature.Units.MC, units, true);

                                                                return Arrays.asList(biome.toString(), min, max, units.toString());
                                                            })
                                                            .filter(Objects::nonNull)
                                                            .collect(Collectors.toList())),
        SyncType.ONE_WAY);

        BIOME_OFFSETS = addSyncedSettingWithRegistries("biome_offsets", FastMap::new, (holder, registryAccess) ->
        {
            Map<Biome, BiomeTempData> dataMap = ConfigHelper.getRegistryMap(WorldSettingsConfig.BIOME_TEMP_OFFSETS.get(), Registry.BIOME_REGISTRY,
                                                                            toml -> BiomeTempData.fromToml(toml, true, registryAccess), data -> data.biomes);
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.BIOME_TEMP_DATA);

            holder.get(registryAccess).putAll(dataMap);
        },
        (encoder, registryAccess) -> ConfigHelper.serializeHolderRegistry(encoder, "BiomeOffsets", Registry.BIOME_REGISTRY, ModRegistries.BIOME_TEMP_DATA, registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeRegistry(decoder, "BiomeOffsets", Registry.BIOME_REGISTRY, ModRegistries.BIOME_TEMP_DATA, registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.BIOME_TEMP_OFFSETS.set(saver.entrySet().stream()
                                                            .map(entry ->
                                                            {
                                                                BiomeTempData data = entry.getValue();
                                                                ResourceLocation biome = RegistryHelper.getKey(entry.getKey(), registryAccess);
                                                                if (biome == null) return null;

                                                                Temperature.Units units = data.units;
                                                                double min = Temperature.convert(data.min, Temperature.Units.MC, units, false);
                                                                double max = Temperature.convert(data.max, Temperature.Units.MC, units, false);

                                                                return Arrays.asList(biome.toString(), min, max, units.toString());
                                                            })
                                                            .filter(Objects::nonNull)
                                                            .collect(Collectors.toList())),
        SyncType.ONE_WAY);

        DIMENSION_TEMPS = addSyncedSettingWithRegistries("dimension_temps", FastMap::new, (holder, registryAccess) ->
        {
            Map<DimensionType, DimensionTempData> dataMap = ConfigHelper.getRegistryMap(WorldSettingsConfig.DIMENSION_TEMPERATURES.get(), Registry.DIMENSION_TYPE_REGISTRY,
                                                                                       toml -> DimensionTempData.fromToml(toml, false, registryAccess), data -> data.dimensions);
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.DIMENSION_TEMP_DATA);

            holder.get(registryAccess).putAll(dataMap);
        },
        (encoder, registryAccess) -> ConfigHelper.serializeHolderRegistry(encoder, "DimensionTemps", Registry.DIMENSION_TYPE_REGISTRY, ModRegistries.DIMENSION_TEMP_DATA, registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeRegistry(decoder, "DimensionTemps", Registry.DIMENSION_TYPE_REGISTRY, ModRegistries.DIMENSION_TEMP_DATA, registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.DIMENSION_TEMPERATURES.set(saver.entrySet().stream()
                                                     .map(entry ->
                                                     {
                                                         ResourceLocation dim = RegistryHelper.getKey(entry.getKey(), registryAccess);
                                                         if (dim == null) return null;

                                                         Temperature.Units units = entry.getValue().units;
                                                         double temp = Temperature.convert(entry.getValue().temperature, Temperature.Units.MC, units, true);

                                                         return Arrays.asList(dim.toString(), temp, units.toString());
                                                     })
                                                     .filter(Objects::nonNull)
                                                     .collect(Collectors.toList())),
        SyncType.ONE_WAY);

        DIMENSION_OFFSETS = addSyncedSettingWithRegistries("dimension_offsets", FastMap::new, (holder, registryAccess) ->
        {
            Map<DimensionType, DimensionTempData> dataMap = ConfigHelper.getRegistryMap(WorldSettingsConfig.DIMENSION_TEMP_OFFSETS.get(), Registry.DIMENSION_TYPE_REGISTRY,
                                                                                       toml -> DimensionTempData.fromToml(toml, true, registryAccess), data -> data.dimensions);
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.DIMENSION_TEMP_DATA);

            holder.get(registryAccess).putAll(dataMap);
        },
        (encoder, registryAccess) -> ConfigHelper.serializeHolderRegistry(encoder, "DimensionOffsets", Registry.DIMENSION_TYPE_REGISTRY, ModRegistries.DIMENSION_TEMP_DATA, registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeRegistry(decoder, "DimensionOffsets", Registry.DIMENSION_TYPE_REGISTRY, ModRegistries.DIMENSION_TEMP_DATA, registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.DIMENSION_TEMP_OFFSETS.set(saver.entrySet().stream()
                                                     .map(entry ->
                                                     {
                                                         ResourceLocation dim = RegistryHelper.getKey(entry.getKey(), registryAccess);
                                                         if (dim == null) return null;

                                                         Temperature.Units units = entry.getValue().units;
                                                         double temp = Temperature.convert(entry.getValue().temperature, Temperature.Units.MC, units, false);

                                                         return Arrays.asList(dim.toString(), temp, units.toString());
                                                     })
                                                     .filter(Objects::nonNull)
                                                     .collect(Collectors.toList())),
        SyncType.ONE_WAY);

        STRUCTURE_TEMPS = addSyncedSettingWithRegistries("structure_temperatures", FastMap::new, (holder, registryAccess) ->
        {
            Map<Structure<?>, StructureTempData> dataMap = ConfigHelper.getRegistryMap(WorldSettingsConfig.STRUCTURE_TEMPERATURES.get(), Registry.STRUCTURE_FEATURE_REGISTRY,
                                                                                   toml -> StructureTempData.fromToml(toml, true, registryAccess), data -> data.structures);
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.STRUCTURE_TEMP_DATA);

            holder.get(registryAccess).putAll(dataMap);
        },
        (encoder, registryAccess) -> ConfigHelper.serializeRegistry(encoder, "StructureTemperatures", Registry.STRUCTURE_FEATURE_REGISTRY, ModRegistries.STRUCTURE_TEMP_DATA, registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeRegistry(decoder, "StructureTemperatures", Registry.STRUCTURE_FEATURE_REGISTRY, ModRegistries.STRUCTURE_TEMP_DATA, registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.STRUCTURE_TEMPERATURES.set(saver.entrySet().stream()
                                                     .map(entry ->
                                                     {
                                                         ResourceLocation struct = RegistryHelper.getKey(entry.getKey(), registryAccess);
                                                         if (struct == null) return null;

                                                         Temperature.Units units = entry.getValue().units;
                                                         double temp = Temperature.convert(entry.getValue().temperature, Temperature.Units.MC, units, true);

                                                         return Arrays.asList(struct.toString(), temp, units.toString());
                                                     })
                                                     .filter(Objects::nonNull)
                                                     .collect(Collectors.toList())),
        SyncType.ONE_WAY);

        STRUCTURE_OFFSETS = addSyncedSettingWithRegistries("structure_offsets", FastMap::new, (holder, registryAccess) ->
        {
            Map<Structure<?>, StructureTempData> dataMap = ConfigHelper.getRegistryMap(WorldSettingsConfig.STRUCTURE_TEMP_OFFSETS.get(), Registry.STRUCTURE_FEATURE_REGISTRY,
                                                                                   toml -> StructureTempData.fromToml(toml, false, registryAccess), data -> data.structures);
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.STRUCTURE_TEMP_DATA);

            holder.get(registryAccess).putAll(dataMap);
        },
        (encoder, registryAccess) -> ConfigHelper.serializeRegistry(encoder, "StructureOffsets", Registry.STRUCTURE_FEATURE_REGISTRY, ModRegistries.STRUCTURE_TEMP_DATA, registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeRegistry(decoder, "StructureOffsets", Registry.STRUCTURE_FEATURE_REGISTRY, ModRegistries.STRUCTURE_TEMP_DATA, registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.STRUCTURE_TEMP_OFFSETS.set(saver.entrySet().stream()
                                                     .map(entry ->
                                                     {
                                                         ResourceLocation struct = RegistryHelper.getKey(entry.getKey(), registryAccess);
                                                         if (struct == null) return null;

                                                         Temperature.Units units = entry.getValue().units;
                                                         double temp = Temperature.convert(entry.getValue().temperature, Temperature.Units.MC, units, false);

                                                         return Arrays.asList(struct.toString(), temp, units.toString());
                                                     })
                                                     .filter(Objects::nonNull)
                                                     .collect(Collectors.toList())),
        SyncType.ONE_WAY);

        DEPTH_REGIONS = addSetting("depth_regions", ArrayList::new, holder -> {});

        TriConsumer<FuelData.FuelType, ForgeConfigSpec.ConfigValue<List<? extends List<?>>>, DynamicHolder<Multimap<Item, FuelData>>> fuelAdder =
        (fuelType, configValue, holder) ->
        {
            Multimap<Item, FuelData> dataMap = new FastMultiMap<>();
            for (List<?> list : configValue.get())
            {
                FuelData data = FuelData.fromToml(list, fuelType);
                if (data == null) continue;

                for (Item item : RegistryHelper.mapTaggableList(data.data.items.get()))
                {   dataMap.put(item, data);
                }
            }
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.FUEL_DATA);
            holder.get().putAll(dataMap);
        };
        BOILER_FUEL = addSetting("boiler_fuel_items", FastMultiMap::new, holder -> fuelAdder.accept(FuelData.FuelType.BOILER, ItemSettingsConfig.BOILER_FUELS, holder));
        ICEBOX_FUEL = addSetting("icebox_fuel_items", FastMultiMap::new, holder -> fuelAdder.accept(FuelData.FuelType.ICEBOX, ItemSettingsConfig.ICEBOX_FUELS, holder));
        HEARTH_FUEL = addSetting("hearth_fuel_items", FastMultiMap::new, holder -> fuelAdder.accept(FuelData.FuelType.HEARTH, ItemSettingsConfig.HEARTH_FUELS, holder));

        SOULSPRING_LAMP_FUEL = addSyncedSetting("lamp_fuel_items", FastMultiMap::new, holder -> fuelAdder.accept(FuelData.FuelType.SOUL_LAMP, ItemSettingsConfig.SOULSPRING_LAMP_FUELS, holder),
        (encoder) -> ConfigHelper.serializeMultimapRegistry(encoder, "LampFuelItems", Registry.ITEM_REGISTRY, ModRegistries.FUEL_DATA, ForgeRegistries.ITEMS::getKey),
        (decoder) -> ConfigHelper.deserializeMultimapRegistry(decoder, "LampFuelItems", ModRegistries.FUEL_DATA, ForgeRegistries.ITEMS::getValue),
        (saver) -> ConfigHelper.writeRegistryMultimap(saver,
                                                      fuel -> ConfigHelper.getTaggableListStrings(fuel.data.items.get(), Registry.ITEM_REGISTRY),
                                                      fuel -> Arrays.asList(fuel.fuel, fuel.data.nbt.tag.toString()),
                                                      list -> ItemSettingsConfig.SOULSPRING_LAMP_FUELS.set(list)),
        SyncType.ONE_WAY);

        HEARTH_POTIONS_ENABLED = addSetting("hearth_potions_enabled", () -> true, holder -> holder.set(ItemSettingsConfig.ALLOW_POTIONS_IN_HEARTH.get()));
        HEARTH_POTION_BLACKLIST = addSetting("hearth_potion_blacklist", ArrayList::new,
                                             holder -> holder.get().addAll(ItemSettingsConfig.HEARTH_POTION_BLACKLIST.get()
                                                       .stream()
                                                       .map(entry -> ForgeRegistries.POTIONS.getValue(new ResourceLocation(entry)))
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

                for (Item item : RegistryHelper.mapTaggableList(data.data.items.get()))
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
        (encoder) -> ConfigHelper.serializeMultimapRegistry(encoder, "InsulationItems", Registry.ITEM_REGISTRY, ModRegistries.INSULATOR_DATA, item -> ForgeRegistries.ITEMS.getKey(item)),
        (decoder) -> ConfigHelper.deserializeMultimapRegistry(decoder, "InsulationItems", ModRegistries.INSULATOR_DATA, rl -> ForgeRegistries.ITEMS.getValue(rl)),
        (saver) -> ConfigHelper.writeItemInsulations(saver, list -> ItemSettingsConfig.INSULATION_ITEMS.set(list)),
        SyncType.ONE_WAY);

        INSULATING_ARMORS = addSyncedSetting("insulating_armors", FastMultiMap::new, holder ->
        {   insulatorAdder.accept(ItemSettingsConfig.INSULATING_ARMOR, holder, Insulation.Slot.ARMOR);
        },
        (encoder) -> ConfigHelper.serializeMultimapRegistry(encoder, "InsulatingArmors", Registry.ITEM_REGISTRY, ModRegistries.INSULATOR_DATA, item -> ForgeRegistries.ITEMS.getKey(item)),
        (decoder) -> ConfigHelper.deserializeMultimapRegistry(decoder, "InsulatingArmors", ModRegistries.INSULATOR_DATA, rl -> ForgeRegistries.ITEMS.getValue(rl)),
        (saver) -> ConfigHelper.writeItemInsulations(saver, list -> ItemSettingsConfig.INSULATING_ARMOR.set(list)),
        SyncType.ONE_WAY);

        INSULATING_CURIOS = addSyncedSetting("insulating_curios", FastMultiMap::new, holder ->
        {
            if (CompatManager.isCuriosLoaded())
            {   insulatorAdder.accept(ItemSettingsConfig.INSULATING_CURIOS, holder, Insulation.Slot.CURIO);
            }
        },
        (encoder) -> ConfigHelper.serializeMultimapRegistry(encoder, "InsulatingCurios", Registry.ITEM_REGISTRY, ModRegistries.INSULATOR_DATA, item -> ForgeRegistries.ITEMS.getKey(item)),
        (decoder) -> ConfigHelper.deserializeMultimapRegistry(decoder, "InsulatingCurios", ModRegistries.INSULATOR_DATA, rl -> ForgeRegistries.ITEMS.getValue(rl)),
        (saver) ->
        {   if (CompatManager.isCuriosLoaded())
            {   ConfigHelper.writeItemInsulations(saver, list -> ItemSettingsConfig.INSULATING_CURIOS.set(list));
            }
        },
        SyncType.ONE_WAY);

        INSULATION_SLOTS = addSyncedSetting("insulation_slots", () -> new ScalingFormula.Static(0, 0, 0, 0), holder ->
        {
            List<?> list = ItemSettingsConfig.INSULATION_SLOTS.get();
            // Handle legacy insulation notation
            if (list.size() == 4 && list.stream().allMatch(el -> el instanceof Integer))
            {   list = Arrays.asList("static", list.get(0), list.get(1), list.get(2), list.get(3));
            }
            String mode = ((String) list.get(0));

            ScalingFormula.Type scalingType = ScalingFormula.Type.byName(mode);
            List<? extends Number> values = list.subList(1, list.size()).stream().map(o -> (Number) o).collect(Collectors.toList());

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
            ItemSettingsConfig.INSULATION_SLOTS.set(list);
        },
        SyncType.BOTH_WAYS);

        INSULATION_BLACKLIST = addSetting("insulation_blacklist", ArrayList::new,
                                          holder -> holder.get().addAll(ItemSettingsConfig.INSULATION_BLACKLIST.get()
                                                    .stream()
                                                    .map(entry -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(entry)))
                                                    .collect(ArrayList::new, List::add, List::addAll)));

        CHECK_SLEEP_CONDITIONS = addSetting("check_sleep_conditions", () -> true, holder -> holder.set(WorldSettingsConfig.SHOULD_CHECK_SLEEP.get()));

        SLEEP_CHECK_IGNORE_BLOCKS = addSetting("sleep_check_override_blocks", ArrayList::new, holder -> holder.get().addAll(ConfigHelper.getBlocks(WorldSettingsConfig.SLEEPING_OVERRIDE_BLOCKS.get().toArray(new String[0]))));

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

                for (Item item : RegistryHelper.mapTaggableList(data.data.items.get()))
                {   dataMap.put(item, data);
                }
            }
            // Handle registry removals
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.FOOD_DATA);
            // Add entries
            holder.get().putAll(dataMap);
        },
        (encoder) -> ConfigHelper.serializeMultimapRegistry(encoder, "FoodTemperatures", Registry.ITEM_REGISTRY, ModRegistries.FOOD_DATA, item -> ForgeRegistries.ITEMS.getKey(item)),
        (decoder) -> ConfigHelper.deserializeMultimapRegistry(decoder, "FoodTemperatures", ModRegistries.FOOD_DATA, rl -> ForgeRegistries.ITEMS.getValue(rl)),
        (saver) -> ConfigHelper.writeRegistryMultimap(saver,
                                                      food -> ConfigHelper.getTaggableListStrings(food.data.items.get(), Registry.ITEM_REGISTRY),
                                                      food -> ListBuilder.begin(food.temperature, food.data.nbt.tag.toString())
                                                              .addIf(food.duration > 0, () -> food.duration).build(),
                                                      list -> ItemSettingsConfig.FOOD_TEMPERATURES.set(list)),
        SyncType.ONE_WAY);

        CARRIED_ITEM_TEMPERATURES = addSyncedSetting("carried_item_temps", FastMultiMap::new, holder ->
        {
            // Read the insulation items from the config
            Multimap<Item, ItemCarryTempData> dataMap = new FastMultiMap<>();
            for (List<?> list : ItemSettingsConfig.CARRIED_ITEM_TEMPERATURES.get())
            {
                ItemCarryTempData data = ItemCarryTempData.fromToml(list);
                if (data == null) continue;

                for (Item item : RegistryHelper.mapTaggableList(data.data.items.get()))
                {   dataMap.put(item, data);
                }
            }
            // Handle registry removals
            ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.CARRY_TEMP_DATA);
            // Add entries
            holder.get().putAll(dataMap);
        },
        (encoder) -> ConfigHelper.serializeMultimapRegistry(encoder, "CarriedItemTemps", Registry.ITEM_REGISTRY, ModRegistries.CARRY_TEMP_DATA, item -> ForgeRegistries.ITEMS.getKey(item)),
        (decoder) -> ConfigHelper.deserializeMultimapRegistry(decoder, "CarriedItemTemps", ModRegistries.CARRY_TEMP_DATA, rl -> ForgeRegistries.ITEMS.getValue(rl)),
        (saver) ->
        {
            ConfigHelper.writeRegistryMultimap(saver,
            temp -> ConfigHelper.getTaggableListStrings(temp.data.items.get(), Registry.ITEM_REGISTRY),
            temp ->
            {
                List<Object> entry = new ArrayList<>();
                // Temperature
                entry.add(temp.temperature);
                // Slot types
                String strictType = temp.getSlotRangeName();
                if (strictType.isEmpty()) return null;
                entry.add(strictType);
                // Trait
                entry.add(temp.trait.getSerializedName());
                // NBT data
                if (!temp.data.nbt.tag.isEmpty())
                {   entry.add(temp.data.nbt.tag.toString());
                }
                return entry;
            },
            list -> ItemSettingsConfig.CARRIED_ITEM_TEMPERATURES.set(list));
        },
        SyncType.ONE_WAY);

        WATERSKIN_STRENGTH = addSetting("waterskin_strength", () -> 50, holder -> holder.set(ItemSettingsConfig.WATERSKIN_STRENGTH.get()));

        SOULSPRING_LAMP_STRENGTH = addSetting("soulspring_lamp_strength", () -> 0.6d, holder -> holder.set(ItemSettingsConfig.SOULSPRING_LAMP_STRENGTH.get()));

        LAMP_DIMENSIONS = addSettingWithRegistries("valid_lamp_dimensions", ArrayList::new,
                                                   (holder, registryAccess) -> holder.get(registryAccess).addAll(new ArrayList<>(ItemSettingsConfig.SOULSPRING_LAMP_DIMENSIONS.get()
                                                                           .stream()
                                                                           .map(entry -> registryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).get(new ResourceLocation(entry)))
                                                                           .collect(ArrayList::new, List::add, List::addAll))));

        FUR_TIMINGS = addSyncedSetting("fur_timings", () -> new Triplet<>(0, 0, 0d), holder ->
        {   List<?> entry = EntitySettingsConfig.GOAT_FUR_GROWTH_STATS.get();
            holder.set(new Triplet<>(((Number) entry.get(0)).intValue(), ((Number) entry.get(1)).intValue(), ((Number) entry.get(2)).doubleValue()));
        },
        encoder ->
        {   CompoundNBT tag = new CompoundNBT();
            tag.put("Interval", IntNBT.valueOf(encoder.getA()));
            tag.put("Cooldown", IntNBT.valueOf(encoder.getB()));
            tag.put("Chance", DoubleNBT.valueOf(encoder.getC()));
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
            EntitySettingsConfig.GOAT_FUR_GROWTH_STATS.set(list);
        },
        SyncType.BOTH_WAYS);

        SHED_TIMINGS = addSyncedSetting("shed_timings", () -> new Triplet<>(0, 0, 0d), holder ->
        {
            List<?> entry = EntitySettingsConfig.CHAMELEON_SHED_STATS.get();
            holder.set(new Triplet<>(((Number) entry.get(0)).intValue(), ((Number) entry.get(1)).intValue(), ((Number) entry.get(2)).doubleValue()));
        },
        encoder ->
        {   CompoundNBT tag = new CompoundNBT();
            tag.put("Interval", IntNBT.valueOf(encoder.getA()));
            tag.put("Cooldown", IntNBT.valueOf(encoder.getB()));
            tag.put("Chance", DoubleNBT.valueOf(encoder.getC()));
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
            EntitySettingsConfig.CHAMELEON_SHED_STATS.set(list);
        },
        SyncType.BOTH_WAYS);

        ENTITY_SPAWN_BIOMES = addSettingWithRegistries("entity_spawn_biomes", FastMultiMap::new, (holder, registryAccess) ->
        {
            // Function to read biomes from configs and put them in the config settings
            BiConsumer<EntityType<?>, List<? extends List<?>>> configReader = (entityType, configBiomes) ->
            {
                Multimap<Biome, SpawnBiomeData> dataMap = ConfigHelper.getRegistryMultimap(configBiomes, Registry.BIOME_REGISTRY,
                                                                                                   toml -> SpawnBiomeData.fromToml(toml, entityType, registryAccess), data -> data.biomes);
                ConfigLoadingHandler.removeEntries(dataMap.values(), ModRegistries.ENTITY_SPAWN_BIOME_DATA);

                holder.get(registryAccess).putAll(dataMap);
            };

            // Parse goat and chameleon biomes
            configReader.accept(ModEntities.CHAMELEON, EntitySettingsConfig.CHAMELEON_SPAWN_BIOMES.get());
            configReader.accept(ModEntities.GOAT, EntitySettingsConfig.GOAT_SPAWN_BIOMES.get());
        });

        INSULATED_MOUNTS = addSetting("insulated_entities", FastMultiMap::new, holder ->
        {
            // Read the insulation items from the config
            Multimap<EntityType<?>, MountData> dataMap = new FastMultiMap<>();
            for (List<?> list : EntitySettingsConfig.INSULATED_MOUNTS.get())
            {
                MountData data = MountData.fromToml(list);
                for (EntityType<?> entityType : RegistryHelper.mapTaggableList(data.entities))
                {   dataMap.put(entityType, data);
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

                for (EntityType<?> entityType : RegistryHelper.mapTaggableList(data.entity.entities.get()))
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
        (saver) -> WorldSettingsConfig.MAX_BLOCK_TEMP_RANGE.set(saver),
        SyncType.BOTH_WAYS);

        COLD_SOUL_FIRE = addSetting("cold_soul_fire", () -> true, holder -> holder.set(WorldSettingsConfig.IS_SOUL_FIRE_COLD.get()));

        THERMAL_SOURCE_SPREAD_WHITELIST = addSyncedSetting("hearth_spread_whitelist", ArrayList::new, holder -> holder.get().addAll(ConfigHelper.getBlocks(WorldSettingsConfig.SOURCE_SPREAD_WHITELIST.get().toArray(new String[0]))),
        (encoder) ->
        {
            CompoundNBT tag = new CompoundNBT();
            ListNBT list = new ListNBT();
            for (Block entry : encoder)
            {   list.add(StringNBT.valueOf(ForgeRegistries.BLOCKS.getKey(entry).toString()));
            }
            tag.put("HearthWhitelist", list);
            return tag;
        },
        (decoder) ->
        {
            List<Block> list = new ArrayList<>();
            for (INBT entry : decoder.getList("HearthWhitelist", 8))
            {   list.add(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.getAsString())));
            }
            return list;
        },
        saver -> WorldSettingsConfig.SOURCE_SPREAD_WHITELIST.set(saver.stream().map(block -> ForgeRegistries.BLOCKS.getKey(block).toString()).collect(Collectors.toList())),
        SyncType.BOTH_WAYS);

        THERMAL_SOURCE_SPREAD_BLACKLIST = addSyncedSetting("hearth_spread_blacklist", ArrayList::new, holder -> holder.get().addAll(ConfigHelper.getBlocks(WorldSettingsConfig.SOURCE_SPREAD_BLACKLIST.get().toArray(new String[0]))),
        (encoder) ->
        {
            CompoundNBT tag = new CompoundNBT();
            ListNBT list = new ListNBT();
            for (Block entry : encoder)
            {   list.add(StringNBT.valueOf(ForgeRegistries.BLOCKS.getKey(entry).toString()));
            }
            tag.put("HearthBlacklist", list);
            return tag;
        },
        (decoder) ->
        {
            List<Block> list = new ArrayList<>();
            for (INBT entry : decoder.getList("HearthBlacklist", 8))
            {   list.add(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.getAsString())));
            }
            return list;
        },
        saver -> WorldSettingsConfig.SOURCE_SPREAD_WHITELIST.set(saver.stream().map(block -> ForgeRegistries.BLOCKS.getKey(block).toString()).collect(Collectors.toList())),
        SyncType.BOTH_WAYS);

        THERMAL_SOURCE_STRENGTH = addSetting("hearth_effect", () -> 0.75, holder -> holder.set(WorldSettingsConfig.SOURCE_EFFECT_STRENGTH.get()));

        SMART_HEARTH = addSyncedSetting("smart_hearth", () -> false, holder -> holder.set(WorldSettingsConfig.ENABLE_SMART_HEARTH.get()),
        (encoder) -> ConfigHelper.serializeNbtBool(encoder, "SmartHearth"),
        (decoder) -> decoder.getBoolean("SmartHearth"),
        (saver) -> WorldSettingsConfig.ENABLE_SMART_HEARTH.set(saver),
        SyncType.BOTH_WAYS);

        SMART_BOILER = addSyncedSetting("smart_boiler", () -> false, holder -> holder.set(WorldSettingsConfig.ENABLE_SMART_BOILER.get()),
        (encoder) -> ConfigHelper.serializeNbtBool(encoder, "SmartBoiler"),
        (decoder) -> decoder.getBoolean("SmartBoiler"),
        (saver) -> WorldSettingsConfig.ENABLE_SMART_BOILER.set(saver),
        SyncType.BOTH_WAYS);

        SMART_ICEBOX = addSyncedSetting("smart_icebox", () -> false, holder -> holder.set(WorldSettingsConfig.ENABLE_SMART_ICEBOX.get()),
        (encoder) -> ConfigHelper.serializeNbtBool(encoder, "SmartIcebox"),
        (decoder) -> decoder.getBoolean("SmartIcebox"),
        (saver) -> WorldSettingsConfig.ENABLE_SMART_ICEBOX.set(saver),
        SyncType.BOTH_WAYS);

        HEARTH_MAX_RANGE = addSyncedSetting("hearth_max_range", () -> 16, holder -> holder.set(WorldSettingsConfig.HEARTH_MAX_RANGE.get()),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "HearthMaxRange"),
        (decoder) -> decoder.getInt("HearthMaxRange"),
        (saver) -> WorldSettingsConfig.HEARTH_MAX_RANGE.set(saver),
        SyncType.BOTH_WAYS);

        HEARTH_RANGE = addSyncedSetting("hearth_range", () -> 8, holder -> holder.set(WorldSettingsConfig.HEARTH_RANGE.get()),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "HearthRange"),
        (decoder) -> decoder.getInt("HearthRange"),
        (saver) -> WorldSettingsConfig.HEARTH_RANGE.set(saver),
        SyncType.BOTH_WAYS);

        HEARTH_MAX_VOLUME = addSyncedSetting("hearth_max_volume", () -> 1000, holder -> holder.set(WorldSettingsConfig.HEARTH_MAX_VOLUME.get()),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "HearthMaxVolume"),
        (decoder) -> decoder.getInt("HearthMaxVolume"),
        (saver) -> WorldSettingsConfig.HEARTH_MAX_VOLUME.set(saver),
        SyncType.BOTH_WAYS);

        HEARTH_WARM_UP_TIME = addSyncedSetting("hearth_warm_up_time", () -> 20, holder -> holder.set(WorldSettingsConfig.HEARTH_WARM_UP_TIME.get()),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "HearthWarmUpTime"),
        (decoder) -> decoder.getInt("HearthWarmUpTime"),
        (saver) -> WorldSettingsConfig.HEARTH_WARM_UP_TIME.set(saver),
        SyncType.BOTH_WAYS);

        HEARTH_MAX_INSULATION = addSyncedSetting("hearth_max_insulation", () -> 1, holder -> holder.set(WorldSettingsConfig.HEARTH_MAX_INSULATION.get()),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "HearthMaxInsulation"),
        (decoder) -> decoder.getInt("HearthMaxInsulation"),
        (saver) -> WorldSettingsConfig.HEARTH_MAX_INSULATION.set(saver),
        SyncType.BOTH_WAYS);

        BOILER_MAX_RANGE = addSyncedSetting("boiler_max_range", () -> 16, holder -> holder.set(WorldSettingsConfig.BOILER_MAX_RANGE.get()),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "BoilerMaxRange"),
        (decoder) -> decoder.getInt("BoilerMaxRange"),
        (saver) -> WorldSettingsConfig.BOILER_MAX_RANGE.set(saver),
        SyncType.BOTH_WAYS);

        BOILER_RANGE = addSyncedSetting("boiler_range", () -> 8, holder -> holder.set(WorldSettingsConfig.BOILER_RANGE.get()),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "BoilerRange"),
        (decoder) -> decoder.getInt("BoilerRange"),
        (saver) -> WorldSettingsConfig.BOILER_RANGE.set(saver),
        SyncType.BOTH_WAYS);

        BOILER_MAX_VOLUME = addSyncedSetting("boiler_max_volume", () -> 1000, holder -> holder.set(WorldSettingsConfig.BOILER_MAX_VOLUME.get()),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "BoilerMaxVolume"),
        (decoder) -> decoder.getInt("BoilerMaxVolume"),
        (saver) -> WorldSettingsConfig.BOILER_MAX_VOLUME.set(saver),
        SyncType.BOTH_WAYS);

        BOILER_WARM_UP_TIME = addSyncedSetting("boiler_warm_up_time", () -> 20, holder -> holder.set(WorldSettingsConfig.BOILER_WARM_UP_TIME.get()),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "BoilerWarmUpTime"),
        (decoder) -> decoder.getInt("BoilerWarmUpTime"),
        (saver) -> WorldSettingsConfig.BOILER_WARM_UP_TIME.set(saver),
        SyncType.BOTH_WAYS);

        BOILER_MAX_INSULATION = addSyncedSetting("boiler_max_insulation", () -> 1, holder -> holder.set(WorldSettingsConfig.BOILER_MAX_INSULATION.get()),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "BoilerMaxInsulation"),
        (decoder) -> decoder.getInt("BoilerMaxInsulation"),
        (saver) -> WorldSettingsConfig.BOILER_MAX_INSULATION.set(saver),
        SyncType.BOTH_WAYS);

        ICEBOX_MAX_RANGE = addSyncedSetting("icebox_max_range", () -> 16, holder -> holder.set(WorldSettingsConfig.ICEBOX_MAX_RANGE.get()),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "IceboxMaxRange"),
        (decoder) -> decoder.getInt("IceboxMaxRange"),
        (saver) -> WorldSettingsConfig.ICEBOX_MAX_RANGE.set(saver),
        SyncType.BOTH_WAYS);

        ICEBOX_RANGE = addSyncedSetting("icebox_range", () -> 8, holder -> holder.set(WorldSettingsConfig.ICEBOX_RANGE.get()),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "IceboxRange"),
        (decoder) -> decoder.getInt("IceboxRange"),
        (saver) -> WorldSettingsConfig.ICEBOX_RANGE.set(saver),
        SyncType.BOTH_WAYS);

        ICEBOX_MAX_VOLUME = addSyncedSetting("icebox_max_volume", () -> 1000, holder -> holder.set(WorldSettingsConfig.ICEBOX_MAX_VOLUME.get()),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "IceboxMaxVolume"),
        (decoder) -> decoder.getInt("IceboxMaxVolume"),
        (saver) -> WorldSettingsConfig.ICEBOX_MAX_VOLUME.set(saver),
        SyncType.BOTH_WAYS);

        ICEBOX_WARM_UP_TIME = addSyncedSetting("icebox_warm_up_time", () -> 20, holder -> holder.set(WorldSettingsConfig.ICEBOX_WARM_UP_TIME.get()),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "IceboxWarmUpTime"),
        (decoder) -> decoder.getInt("IceboxWarmUpTime"),
        (saver) -> WorldSettingsConfig.ICEBOX_WARM_UP_TIME.set(saver),
        SyncType.BOTH_WAYS);

        ICEBOX_MAX_INSULATION = addSyncedSetting("icebox_max_insulation", () -> 1, holder -> holder.set(WorldSettingsConfig.ICEBOX_MAX_INSULATION.get()),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "IceboxMaxInsulation"),
        (decoder) -> decoder.getInt("IceboxMaxInsulation"),
        (saver) -> WorldSettingsConfig.ICEBOX_MAX_INSULATION.set(saver),
        SyncType.BOTH_WAYS);

        INSULATION_STRENGTH = addSyncedSetting("insulation_strength", () -> 1d, holder -> holder.set(ItemSettingsConfig.INSULATION_STRENGTH.get()),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "InsulationStrength"),
        (decoder) -> decoder.getDouble("InsulationStrength"),
        (saver) -> ItemSettingsConfig.INSULATION_STRENGTH.set(saver),
        SyncType.BOTH_WAYS);

        DISABLED_MODIFIERS = addSetting("disabled_modifiers", ArrayList::new, holder -> holder.get().addAll(MainSettingsConfig.DISABLED_TEMP_MODIFIERS.get().stream().map(ResourceLocation::new).collect(Collectors.toList())));

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
        SUPER_EASY(() -> CSMath.mapOf(
            getKey(MIN_TEMP), () -> Temperature.convert(40, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(MAX_TEMP), () -> Temperature.convert(120, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(TEMP_RATE), () -> 0.5,
            getKey(REQUIRE_THERMOMETER), () -> false,
            getKey(FIRE_RESISTANCE_ENABLED), () -> true,
            getKey(ICE_RESISTANCE_ENABLED), () -> true
        )),

        EASY(() -> CSMath.mapOf(
            getKey(MIN_TEMP), () -> Temperature.convert(45, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(MAX_TEMP), () -> Temperature.convert(110, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(TEMP_RATE), () -> 0.75,
            getKey(REQUIRE_THERMOMETER), () -> false,
            getKey(FIRE_RESISTANCE_ENABLED), () -> true,
            getKey(ICE_RESISTANCE_ENABLED), () -> true
        )),

        NORMAL(() -> CSMath.mapOf(
            getKey(MIN_TEMP), () -> Temperature.convert(50, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(MAX_TEMP), () -> Temperature.convert(100, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(TEMP_RATE), () -> 1.0,
            getKey(REQUIRE_THERMOMETER), () -> true,
            getKey(FIRE_RESISTANCE_ENABLED), () -> true,
            getKey(ICE_RESISTANCE_ENABLED), () -> true
        )),

        HARD(() -> CSMath.mapOf(
            getKey(MIN_TEMP), () -> Temperature.convert(55, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(MAX_TEMP), () -> Temperature.convert(90, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(TEMP_RATE), () -> 1.25,
            getKey(REQUIRE_THERMOMETER), () -> true,
            getKey(FIRE_RESISTANCE_ENABLED), () -> false,
            getKey(ICE_RESISTANCE_ENABLED), () -> false
        )),

        CUSTOM(() -> CSMath.mapOf());

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

        public static IFormattableTextComponent getFormattedName(Difficulty difficulty)
        {
            switch (difficulty)
            {   case SUPER_EASY : return new TranslationTextComponent("cold_sweat.config.difficulty.super_easy.name");
                case EASY : return new TranslationTextComponent("cold_sweat.config.difficulty.easy.name");
                case NORMAL : return new TranslationTextComponent("cold_sweat.config.difficulty.normal.name");
                case HARD : return new TranslationTextComponent("cold_sweat.config.difficulty.hard.name");
                default: return new TranslationTextComponent("cold_sweat.config.difficulty.custom.name");
            }
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

    public static <T> DynamicHolder<T> addSyncedSetting(String id, Supplier<T> defaultVal, Consumer<DynamicHolder<T>> loader, Function<T, CompoundNBT> writer, Function<CompoundNBT, T> reader,
                                                        Consumer<T> saver, DynamicHolder.SyncType syncType)
    {   DynamicHolder<T> holder = DynamicHolder.createSynced(defaultVal, loader, writer, reader, saver, syncType);
        CONFIG_SETTINGS.put(id, holder);
        return holder;
    }

    public static <T> DynamicHolder<T> addSyncedSettingWithRegistries(String id, Supplier<T> defaultVal, DynamicHolder.Loader<T> loader, DynamicHolder.Writer<T> writer, DynamicHolder.Reader<T> reader,
                                                                      DynamicHolder.Saver<T> saver, DynamicHolder.SyncType syncType)
    {   DynamicHolder<T> holder = DynamicHolder.createSyncedWithRegistries(defaultVal, loader, writer, reader, saver, syncType);
        CONFIG_SETTINGS.put(id, holder);
        return holder;
    }

    public static <T> DynamicHolder<T> addClientSetting(String id, Supplier<T> defaultVal, Consumer<DynamicHolder<T>> loader)
    {
        if (EffectiveSide.get().isClient())
        {
            DynamicHolder<T> holder = DynamicHolder.create(defaultVal, loader);
            CLIENT_SETTINGS.put(id, holder);
            return holder;
        }
        else return DynamicHolder.create(() -> null, (value) -> {});
    }

    public static CompoundNBT encode(DynamicRegistries registryAccess)
    {
        CompoundNBT map = new CompoundNBT();
        CONFIG_SETTINGS.forEach((key, value) ->
        {
            if (value.getSyncType().canSend())
            {   CompoundNBT encoded = value.encode(registryAccess);
                map.merge(encoded);
            }
        });
        return map;
    }

    public static void decode(CompoundNBT tag, DynamicRegistries registryAccess)
    {
        ConfigData.IDENTIFIABLES.clear();
        for (DynamicHolder<?> config : CONFIG_SETTINGS.values())
        {
            if (config.getSyncType().canReceive())
            {   config.decode(tag, registryAccess);
            }
        }
    }

    public static void saveValues(DynamicRegistries registryAccess)
    {
        CONFIG_SETTINGS.values().forEach(value ->
        {   if (value.isSynced())
            {   value.save(registryAccess);
            }
        });
    }

    public static void load(DynamicRegistries registryAccess, boolean replace)
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
        ConfigData.IDENTIFIABLES.clear();
    }
}
