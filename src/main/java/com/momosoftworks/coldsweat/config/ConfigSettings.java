package com.momosoftworks.coldsweat.config;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Either;

import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.insulation.slot.ScalingFormula;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.spec.*;
import com.momosoftworks.coldsweat.config.type.CarriedItemTemperature;
import com.momosoftworks.coldsweat.config.type.InsulatingMount;
import com.momosoftworks.coldsweat.config.type.Insulator;
import com.momosoftworks.coldsweat.config.type.PredicateItem;
import com.momosoftworks.coldsweat.data.codec.configuration.DepthTempData;
import com.momosoftworks.coldsweat.data.codec.configuration.EntityTempData;
import com.momosoftworks.coldsweat.data.codec.requirement.NbtRequirement;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import com.momosoftworks.coldsweat.util.math.FastMap;
import com.momosoftworks.coldsweat.util.serialization.*;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.configuration.SpawnBiomeData;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.math.Vec2i;
import com.momosoftworks.coldsweat.util.registries.ModEntities;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import com.momosoftworks.coldsweat.util.serialization.Triplet;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.nbt.*;
import net.minecraft.potion.Effect;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.function.*;
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
    public static final DynamicHolder<Map<Biome, Triplet<Double, Double, Temperature.Units>>> BIOME_TEMPS;
    public static final DynamicHolder<Map<Biome, Triplet<Double, Double, Temperature.Units>>> BIOME_OFFSETS;
    public static final DynamicHolder<Map<DimensionType, Pair<Double, Temperature.Units>>> DIMENSION_TEMPS;
    public static final DynamicHolder<Map<DimensionType, Pair<Double, Temperature.Units>>> DIMENSION_OFFSETS;
    public static final DynamicHolder<Map<Structure<?>, Pair<Double, Temperature.Units>>> STRUCTURE_TEMPS;
    public static final DynamicHolder<Map<Structure<?>, Pair<Double, Temperature.Units>>> STRUCTURE_OFFSETS;
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
    public static final DynamicHolder<Multimap<Item, Insulator>> INSULATION_ITEMS;
    public static final DynamicHolder<Multimap<Item, Insulator>> INSULATING_ARMORS;
    public static final DynamicHolder<Multimap<Item, Insulator>> INSULATING_CURIOS;
    public static final DynamicHolder<ScalingFormula> INSULATION_SLOTS;
    public static final DynamicHolder<List<Item>> INSULATION_BLACKLIST;

    public static final DynamicHolder<Multimap<Item, PredicateItem>> FOOD_TEMPERATURES;

    public static final DynamicHolder<Multimap<Item, CarriedItemTemperature>> CARRIED_ITEM_TEMPERATURES;

    public static final DynamicHolder<Integer> WATERSKIN_STRENGTH;
    public static final DynamicHolder<Double> SOULSPRING_LAMP_STRENGTH;

    public static final DynamicHolder<List<DimensionType>> LAMP_DIMENSIONS;

    public static final DynamicHolder<Multimap<Item, PredicateItem>> BOILER_FUEL;
    public static final DynamicHolder<Multimap<Item, PredicateItem>> ICEBOX_FUEL;
    public static final DynamicHolder<Multimap<Item, PredicateItem>> HEARTH_FUEL;
    public static final DynamicHolder<Multimap<Item, PredicateItem>> SOULSPRING_LAMP_FUEL;

    public static final DynamicHolder<Boolean> HEARTH_POTIONS_ENABLED;
    public static final DynamicHolder<List<Effect>> HEARTH_POTION_BLACKLIST;

    // Entity Settings
    public static final DynamicHolder<Triplet<Integer, Integer, Double>> FUR_TIMINGS;
    public static final DynamicHolder<Triplet<Integer, Integer, Double>> SHED_TIMINGS;
    public static final DynamicHolder<Multimap<Biome, SpawnBiomeData>> ENTITY_SPAWN_BIOMES;
    public static final DynamicHolder<Multimap<EntityType<?>, InsulatingMount>> INSULATED_ENTITIES;
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


        BIOME_TEMPS = addSyncedSettingWithRegistries("biome_temps", FastMap::new, (holder, registryAccess) -> holder.get(registryAccess).putAll(ConfigHelper.getBiomesWithValues(WorldSettingsConfig.getInstance().getBiomeTemperatures(), true, registryAccess)),
        (encoder, registryAccess) -> ConfigHelper.serializeBiomeTemps(encoder, "BiomeTemps", registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeBiomeTemps(decoder, "BiomeTemps", registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.getInstance().setBiomeTemperatures(saver.entrySet().stream()
                                                            .map(entry ->
                                                            {
                                                                ResourceLocation biome = RegistryHelper.getBiomeId(entry.getKey(), registryAccess);
                                                                if (biome == null) return null;

                                                                Temperature.Units units = entry.getValue().getC();
                                                                double min = Temperature.convert(entry.getValue().getA(), Temperature.Units.MC, units, true);
                                                                double max = Temperature.convert(entry.getValue().getB(), Temperature.Units.MC, units, true);

                                                                return Arrays.asList(biome.toString(), min, max, units.toString());
                                                            })
                                                            .filter(Objects::nonNull)
                                                            .collect(Collectors.toList())));

        BIOME_OFFSETS = addSyncedSettingWithRegistries("biome_offsets", FastMap::new, (holder, registryAccess) -> holder.get(registryAccess).putAll(ConfigHelper.getBiomesWithValues(WorldSettingsConfig.getInstance().getBiomeTempOffsets(), false, registryAccess)),
        (encoder, registryAccess) -> ConfigHelper.serializeBiomeTemps(encoder, "BiomeOffsets", registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeBiomeTemps(decoder, "BiomeOffsets", registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.getInstance().setBiomeTempOffsets(saver.entrySet().stream()
                                                            .map(entry ->
                                                            {
                                                                ResourceLocation biome = RegistryHelper.getBiomeId(entry.getKey(), registryAccess);
                                                                if (biome == null) return null;

                                                                Temperature.Units units = entry.getValue().getC();
                                                                double min = Temperature.convert(entry.getValue().getA(), Temperature.Units.MC, units, false);
                                                                double max = Temperature.convert(entry.getValue().getB(), Temperature.Units.MC, units, false);

                                                                return Arrays.asList(biome.toString(), min, max, units.toString());
                                                            })
                                                            .filter(Objects::nonNull)
                                                            .collect(Collectors.toList())));

        DIMENSION_TEMPS = addSyncedSettingWithRegistries("dimension_temps", FastMap::new, (holder, registryAccess) -> holder.get(registryAccess).putAll(ConfigHelper.getDimensionsWithValues(WorldSettingsConfig.getInstance().getDimensionTemperatures(), true, registryAccess)),
        (encoder, registryAccess) -> ConfigHelper.serializeDimensionTemps(encoder, "DimensionTemps", registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeDimensionTemps(decoder, "DimensionTemps", registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.getInstance().setDimensionTemperatures(saver.entrySet().stream()
                                                     .map(entry ->
                                                     {
                                                         ResourceLocation dim = RegistryHelper.getDimensionId(entry.getKey(), registryAccess);
                                                         if (dim == null) return null;

                                                         Temperature.Units units = entry.getValue().getSecond();
                                                         double temp = Temperature.convert(entry.getValue().getFirst(), Temperature.Units.MC, units, true);

                                                         return Arrays.asList(dim.toString(), temp, units.toString());
                                                     })
                                                     .filter(Objects::nonNull)
                                                     .collect(Collectors.toList())));

        DIMENSION_OFFSETS = addSyncedSettingWithRegistries("dimension_offsets", FastMap::new, (holder, registryAccess) -> holder.get(registryAccess).putAll(ConfigHelper.getDimensionsWithValues(WorldSettingsConfig.getInstance().getDimensionTempOffsets(), false, registryAccess)),
        (encoder, registryAccess) -> ConfigHelper.serializeDimensionTemps(encoder, "DimensionOffsets", registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeDimensionTemps(decoder, "DimensionOffsets", registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.getInstance().setDimensionTempOffsets(saver.entrySet().stream()
                                                     .map(entry ->
                                                     {
                                                         ResourceLocation dim = RegistryHelper.getDimensionId(entry.getKey(), registryAccess);
                                                         if (dim == null) return null;

                                                         Temperature.Units units = entry.getValue().getSecond();
                                                         double temp = Temperature.convert(entry.getValue().getFirst(), Temperature.Units.MC, units, false);

                                                         return Arrays.asList(dim.toString(), temp, units.toString());
                                                     })
                                                     .filter(Objects::nonNull)
                                                     .collect(Collectors.toList())));

        STRUCTURE_TEMPS = addSyncedSettingWithRegistries("structure_temperatures", FastMap::new, (holder, registryAccess) -> holder.get(registryAccess).putAll(ConfigHelper.getStructuresWithValues(WorldSettingsConfig.getInstance().getStructureTemperatures(), true, registryAccess)),
        (encoder, registryAccess) -> ConfigHelper.serializeStructureTemps(encoder, "StructureTemperatures", registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeStructureTemps(decoder, "StructureTemperatures", registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.getInstance().setStructureTemperatures(saver.entrySet().stream()
                                                     .map(entry ->
                                                     {
                                                         ResourceLocation struct = RegistryHelper.getStructureId(entry.getKey(), registryAccess);
                                                         if (struct == null) return null;

                                                         Temperature.Units units = entry.getValue().getSecond();
                                                         double temp = Temperature.convert(entry.getValue().getFirst(), Temperature.Units.MC, units, true);

                                                         return Arrays.asList(struct.toString(), temp, units.toString());
                                                     })
                                                     .filter(Objects::nonNull)
                                                     .collect(Collectors.toList())));

        STRUCTURE_OFFSETS = addSyncedSettingWithRegistries("structure_offsets", FastMap::new, (holder, registryAccess) -> holder.get(registryAccess).putAll(ConfigHelper.getStructuresWithValues(WorldSettingsConfig.getInstance().getStructureTempOffsets(), false, registryAccess)),
        (encoder, registryAccess) -> ConfigHelper.serializeStructureTemps(encoder, "StructureOffsets", registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeStructureTemps(decoder, "StructureOffsets", registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.getInstance().setStructureTempOffsets(saver.entrySet().stream()
                                                     .map(entry ->
                                                     {
                                                         ResourceLocation struct = RegistryHelper.getStructureId(entry.getKey(), registryAccess);
                                                         if (struct == null) return null;

                                                         Temperature.Units units = entry.getValue().getSecond();
                                                         double temp = Temperature.convert(entry.getValue().getFirst(), Temperature.Units.MC, units, false);

                                                         return Arrays.asList(struct.toString(), temp, units.toString());
                                                     })
                                                     .filter(Objects::nonNull)
                                                     .collect(Collectors.toList())));

        DEPTH_REGIONS = addSetting("depth_regions", ArrayList::new, holder -> {});

        BiFunction<Item, List<?>, PredicateItem> fuelMapper = (item, args) ->
        {
            double fuel = ((Number) args.get(0)).doubleValue();
            NbtRequirement nbtRequirement;
            if (args.size() > 2)
            {   nbtRequirement = new NbtRequirement(NBTHelper.parseCompoundNbt((String) args.get(2)));
            }
            else nbtRequirement = new NbtRequirement(new CompoundNBT());
            return new PredicateItem(fuel, new ItemRequirement(Optional.of(Arrays.asList(Either.right(item))),
                                                               Optional.empty(), Optional.empty(),
                                                               Optional.empty(), Optional.empty(),
                                                               Optional.empty(), Optional.empty(),
                                                               nbtRequirement),
                                     EntityRequirement.NONE);
        };
        BOILER_FUEL = addSetting("boiler_fuel_items", FastMultiMap::new, holder -> holder.get().putAll(ConfigHelper.readItemMultimap(ItemSettingsConfig.getInstance().getBoilerFuelItems(), fuelMapper)));
        ICEBOX_FUEL = addSetting("icebox_fuel_items", FastMultiMap::new, holder -> holder.get().putAll(ConfigHelper.readItemMultimap(ItemSettingsConfig.getInstance().getIceboxFuelItems(), fuelMapper)));
        HEARTH_FUEL = addSetting("hearth_fuel_items", FastMultiMap::new, holder -> holder.get().putAll(ConfigHelper.readItemMultimap(ItemSettingsConfig.getInstance().getHearthFuelItems(), fuelMapper)));

        SOULSPRING_LAMP_FUEL = addSyncedSetting("lamp_fuel_items", FastMultiMap::new, holder -> holder.get().putAll(ConfigHelper.readItemMultimap(ItemSettingsConfig.getInstance().getSoulLampFuelItems(), fuelMapper)),
        (encoder) -> ConfigHelper.serializeItemMultimap(encoder, "LampFuelItems", fuel -> fuel.serialize()),
        (decoder) -> ConfigHelper.deserializeItemMultimap(decoder, "LampFuelItems", nbt -> PredicateItem.deserialize(nbt)),
        (saver) -> ConfigHelper.writeItemMultimap(saver,
                                             list -> ItemSettingsConfig.getInstance().setSoulLampFuelItems(list),
                                             fuel -> Arrays.asList(fuel.value, fuel.data.nbt.tag.toString())));

        HEARTH_POTIONS_ENABLED = addSetting("hearth_potions_enabled", () -> true, holder -> holder.set(ItemSettingsConfig.getInstance().arePotionsEnabled()));
        HEARTH_POTION_BLACKLIST = addSetting("hearth_potion_blacklist", ArrayList::new,
                                             holder -> holder.get().addAll(ItemSettingsConfig.getInstance().getPotionBlacklist()
                                                       .stream()
                                                       .map(entry -> ForgeRegistries.POTIONS.getValue(new ResourceLocation(entry)))
                                                       .collect(ArrayList::new, List::add, List::addAll)));

        INSULATION_ITEMS = addSyncedSetting("insulation_items", FastMultiMap::new, holder -> holder.get().putAll(ConfigHelper.readItemInsulations(ItemSettingsConfig.getInstance().getInsulationItems(), Insulation.Slot.ITEM)),
        (encoder) -> ConfigHelper.serializeItemInsulations(encoder, "InsulationItems"),
        (decoder) -> ConfigHelper.deserializeItemInsulations(decoder, "InsulationItems"),
        (saver) -> ConfigHelper.writeItemInsulations(saver, list -> ItemSettingsConfig.getInstance().setInsulationItems(list)));

        INSULATING_ARMORS = addSyncedSetting("insulating_armors", FastMultiMap::new, holder -> holder.get().putAll(ConfigHelper.readItemInsulations(ItemSettingsConfig.getInstance().getInsulatingArmorItems(), Insulation.Slot.ARMOR)),
        (encoder) -> ConfigHelper.serializeItemInsulations(encoder, "InsulatingArmors"),
        (decoder) -> ConfigHelper.deserializeItemInsulations(decoder, "InsulatingArmors"),
        (saver) -> ConfigHelper.writeItemInsulations(saver, list -> ItemSettingsConfig.getInstance().setInsulatingArmorItems(list)));

        INSULATING_CURIOS = addSyncedSetting("insulating_curios", FastMultiMap::new, holder ->
        {
            if (CompatManager.isCuriosLoaded())
            {   holder.get().putAll(ConfigHelper.readItemInsulations(ItemSettingsConfig.getInstance().getInsulatingCurios(), Insulation.Slot.CURIO));
            }
        },
        (encoder) -> ConfigHelper.serializeItemInsulations(encoder, "InsulatingCurios"),
        (decoder) -> ConfigHelper.deserializeItemInsulations(decoder, "InsulatingCurios"),
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
        (encoder) ->
        {
            CompoundNBT tag = new CompoundNBT();
            tag.putString("Mode", encoder.getType().getSerializedName());

            ListNBT values = new ListNBT();
            encoder.getValues().forEach(value -> values.add(DoubleNBT.valueOf(value.doubleValue())));
            tag.put("Values", values);

            return tag;
        },
        (decoder) ->
        {
            ScalingFormula.Type scalingType = ScalingFormula.Type.byName(decoder.getString("Mode"));
            List<? extends Number> values = decoder.getList("Values", 6)
                    .stream()
                    .map(tag -> ((DoubleNBT) tag).getAsNumber()).collect(Collectors.toList());

            return scalingType == ScalingFormula.Type.STATIC
                                  ? new ScalingFormula.Static(values.get(0).intValue(),
                                                              values.get(1).intValue(),
                                                              values.get(2).intValue(),
                                                              values.get(3).intValue())
                                  : new ScalingFormula.Dynamic(scalingType,
                                                               values.get(0).doubleValue(),
                                                               values.size() > 2 ? values.get(2).doubleValue() : Double.MAX_VALUE);
        },
        (saver) ->
        {
            List list = new ArrayList<>();
            list.add(saver.getType().getSerializedName());
            list.addAll(saver.getValues());
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

        FOOD_TEMPERATURES = addSyncedSetting("food_temperatures", FastMultiMap::new, holder -> holder.get().putAll(ConfigHelper.readItemMultimap(ItemSettingsConfig.getInstance().getFoodTemperatures(), (item, args) ->
        {
            double value = ((Number) args.get(0)).doubleValue();
            NbtRequirement nbtRequirement = args.size() > 1
                                            ? new NbtRequirement(NBTHelper.parseCompoundNbt((String) args.get(1)))
                                            : new NbtRequirement(new CompoundNBT());
            Integer duration = args.size() > 2 ? ((Number) args.get(2)).intValue() : null;
            ItemRequirement itemRequirement = new ItemRequirement(Optional.of(Arrays.asList(Either.right(item))),
                                                                  Optional.empty(), Optional.empty(),
                                                                  Optional.empty(), Optional.empty(),
                                                                  Optional.empty(), Optional.empty(),
                                                                  nbtRequirement);
            CompoundNBT tag = new CompoundNBT();
            if (duration != null)
            {   tag.putInt("duration", duration);
            }
            return new PredicateItem(value, itemRequirement, EntityRequirement.NONE, tag);
        })),
        (encoder) -> ConfigHelper.serializeItemMultimap(encoder, "FoodTemperatures", PredicateItem::serialize),
        (decoder) -> ConfigHelper.deserializeItemMultimap(decoder, "FoodTemperatures", PredicateItem::deserialize),
        (saver) -> ConfigHelper.writeItemMultimap(saver,
                                             list -> ItemSettingsConfig.getInstance().setFoodTemperatures(list),
                                             food ->
                                             {
                                                 List<Object> foodData = new ArrayList<>(Arrays.asList(food.value, food.data.nbt.tag.toString()));
                                                 if (food.extraData.contains("duration"))
                                                 {   foodData.add(food.extraData.getInt("duration"));
                                                 }
                                                 return foodData;
                                             }));

        CARRIED_ITEM_TEMPERATURES = addSyncedSetting("carried_item_temps", FastMultiMap::new, holder ->
        {
            List<?> list = ItemSettingsConfig.getInstance().getCarriedTemps();
            Multimap<Item, CarriedItemTemperature> map = new FastMultiMap<>();

            for (Object entry : list)
            {
                List<?> entryList = (List<?>) entry;
                // item ID
                String itemID = (String) entryList.get(0);
                Either<ITag<Item>, Item> item = itemID.startsWith("#")
                                                  ? Either.left(ItemTags.getAllTags().getTag(new ResourceLocation(itemID.substring(1))))
                                                  : Either.right(ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemID)));
                //temp
                double temp = ((Number) entryList.get(1)).doubleValue();
                // slots
                List<Either<IntegerBounds, EquipmentSlotType>> slots;
                switch ((String) entryList.get(2))
                {
                    case "inventory" : slots = Arrays.asList(Either.left(IntegerBounds.NONE)); break;
                    case "hotbar"    : slots = Arrays.asList(Either.left(new IntegerBounds(36, 44))); break;
                    case "hand" : slots = Arrays.asList(Either.right(EquipmentSlotType.MAINHAND), Either.right(EquipmentSlotType.OFFHAND)); break;
                    default : slots = Arrays.asList(Either.left(new IntegerBounds(-1, -1))); break;
                };
                // trait
                Temperature.Trait trait = Temperature.Trait.fromID((String) entryList.get(3));
                // nbt
                NbtRequirement nbtRequirement = entryList.size() > 4
                                                ? new NbtRequirement(NBTHelper.parseCompoundNbt((String) entryList.get(4)))
                                                : new NbtRequirement(new CompoundNBT());
                // max effect
                double maxEffect = entryList.size() > 5 ? ((Number) entryList.get(5)).doubleValue() : Double.MAX_VALUE;
                // compile item requirement
                ItemRequirement itemRequirement = new ItemRequirement(Optional.of(Arrays.asList(item)),
                                                                      Optional.empty(), Optional.empty(),
                                                                      Optional.empty(), Optional.empty(),
                                                                      Optional.empty(), Optional.empty(),
                                                                      nbtRequirement);
                // final carried temp
                CarriedItemTemperature carriedTemp = new CarriedItemTemperature(itemRequirement, slots, temp, trait, maxEffect, EntityRequirement.NONE);

                // add carried temp to map
                if (item.left().isPresent())
                {   item.left().get().getValues().forEach(i -> map.put(i, carriedTemp));
                }
                else
                {   map.put(item.right().get(), carriedTemp);
                }
            }
            holder.get().putAll(map);
        },
        (encoder) -> ConfigHelper.serializeItemMultimap(encoder, "CarriedItemTemps", CarriedItemTemperature::serialize),
        (decoder) -> ConfigHelper.deserializeItemMultimap(decoder, "CarriedItemTemps", CarriedItemTemperature::deserialize),
        (saver) ->
        {
            ConfigHelper.writeItemMultimap(saver,
            list -> ItemSettingsConfig.getInstance().setCarriedTemps(list),
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
                if (!temp.item.nbt.tag.isEmpty())
                {   entry.add(temp.item.nbt.tag.toString());
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
            EntitySettingsConfig.getInstance().setGoatFurStats(list);
        });

        SHED_TIMINGS = addSyncedSetting("shed_timings", () -> new Triplet<>(0, 0, 0d), holder ->
        {
            List<?> entry = EntitySettingsConfig.getInstance().getChameleonShedStats();
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
            EntitySettingsConfig.getInstance().setChameleonShedStats(list);
        });

        ENTITY_SPAWN_BIOMES = addSettingWithRegistries("entity_spawn_biomes", FastMultiMap::new, (holder, registryAccess) ->
        {
            Multimap<Biome, SpawnBiomeData> map = HashMultimap.create();
            // Function to read biomes from configs and put them in the config settings
            BiConsumer<EntityType<?>, List<? extends List<?>>> configReader = (entityType, configBiomes) ->
            {
                for (List<?> entry : configBiomes)
                {
                    String biomeId = ((String) entry.get(0));
                    List<Biome> biomes = ConfigHelper.parseRegistryItems(Registry.BIOME_REGISTRY, registryAccess, biomeId);
                    for (Biome biome : biomes)
                    {
                        SpawnBiomeData spawnData = new SpawnBiomeData(biomes, EntityClassification.CREATURE, ((Number) entry.get(1)).intValue(),
                                                                      Arrays.asList(Either.right(entityType)),
                                                                      Optional.empty());
                        map.put(biome, spawnData);
                    }
                }
            };

            // Parse goat and chameleon biomes
            configReader.accept(ModEntities.CHAMELEON, EntitySettingsConfig.getInstance().getChameleonSpawnBiomes());
            configReader.accept(ModEntities.GOAT, EntitySettingsConfig.getInstance().getGoatSpawnBiomes());

            holder.get(registryAccess).putAll(map);
        });

        INSULATED_ENTITIES = addSetting("insulated_entities", FastMultiMap::new, holder -> holder.get().putAll(
        EntitySettingsConfig.getInstance().getInsulatedEntities().stream().map(entry ->
        {
            List<Map.Entry<EntityType<?>, InsulatingMount>> entries = new ArrayList<>();
            String entityID = (String) entry.get(0);
            double coldInsul = ((Number) entry.get(1)).doubleValue();
            double hotInsul = entry.size() < 3
                              ? coldInsul
                              : ((Number) entry.get(2)).doubleValue();

            for (EntityType<?> entityType : ConfigHelper.getEntityTypes(entityID))
            {   entries.add(new AbstractMap.SimpleEntry<>(entityType, new InsulatingMount(entityType, coldInsul, hotInsul, EntityRequirement.NONE)));
            }

            return entries;
        })
        .flatMap(List::stream)
        .distinct()
        .filter(entry -> entry.getKey() != null)
        .collect(FastMultiMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), FastMultiMap::putAll)));

        ENTITY_TEMPERATURES = addSetting("entity_temperatures", FastMultiMap::new, holder ->
        {
            List<?> list = EntitySettingsConfig.ENTITY_TEMPERATURES.get();
            Multimap<EntityType<?>, EntityTempData> map = new FastMultiMap<>();
            for (Object entry : list)
            {
                List<?> entryList = (List<?>) entry;
                String entityID = (String) entryList.get(0);
                double temp = ((Number) entryList.get(1)).doubleValue();
                double range = ((Number) entryList.get(2)).doubleValue();
                Temperature.Units units = entryList.size() > 3
                                          ? Temperature.Units.fromID((String) entryList.get(3))
                                          : Temperature.Units.MC;

                for (EntityType<?> entityType : ConfigHelper.getEntityTypes(entityID))
                {
                    EntityRequirement requirement = new EntityRequirement(Optional.of(entityType), Optional.empty(), Optional.empty(), Optional.empty(),
                                                                          Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                                                                          Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
                    map.put(entityType, new EntityTempData(requirement, temp, range, units, Optional.empty(), Optional.empty()));
                }
            }
            holder.get().putAll(map);
        });

        BLOCK_RANGE = addSyncedSetting("block_range", () -> 7, holder -> holder.set(WorldSettingsConfig.getInstance().getBlockRange()),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "BlockRange"),
        (decoder) -> decoder.getInt("BlockRange"),
        (saver) -> WorldSettingsConfig.getInstance().setBlockRange(saver));

        COLD_SOUL_FIRE = addSetting("cold_soul_fire", () -> true, holder -> holder.set(WorldSettingsConfig.getInstance().isSoulFireCold()));

        HEARTH_SPREAD_WHITELIST = addSyncedSetting("hearth_spread_whitelist", ArrayList::new, holder -> holder.get().addAll(ConfigHelper.getBlocks(WorldSettingsConfig.getInstance().getHearthSpreadWhitelist().toArray(new String[0]))),
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
        saver -> WorldSettingsConfig.getInstance().setHearthSpreadWhitelist(saver.stream().map(ForgeRegistries.BLOCKS::getKey).collect(Collectors.toList())));

        HEARTH_SPREAD_BLACKLIST = addSyncedSetting("hearth_spread_blacklist", ArrayList::new, holder -> holder.get().addAll(ConfigHelper.getBlocks(WorldSettingsConfig.getInstance().getHearthSpreadBlacklist().toArray(new String[0]))),
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
        saver -> WorldSettingsConfig.getInstance().setHearthSpreadBlacklist(saver.stream().map(ForgeRegistries.BLOCKS::getKey).collect(Collectors.toList())));

        HEARTH_STRENGTH = addSetting("hearth_effect", () -> 0.75, holder -> holder.set(WorldSettingsConfig.getInstance().getHearthStrength()));

        SMART_HEARTH = addSetting("smart_hearth", () -> false, holder -> holder.set(WorldSettingsConfig.getInstance().isSmartHearth()));

        INSULATION_STRENGTH = addSetting("insulation_strength", () -> 1d, holder -> holder.set(ItemSettingsConfig.getInstance().getInsulationStrength()));

        DISABLED_MODIFIERS = addSetting("disabled_modifiers", ArrayList::new, holder -> holder.get().addAll(MainSettingsConfig.DISABLED_TEMP_MODIFIERS.get().stream().map(ResourceLocation::new).collect(Collectors.toList())));

        // Client

        CELSIUS = addClientSetting("celsius", () -> false, holder -> holder.set(ClientSettingsConfig.getInstance().isCelsius()));

        TEMP_OFFSET = addClientSetting("temp_offset", () -> 0, holder -> holder.set(ClientSettingsConfig.getInstance().getTempOffset()));

        TEMP_SMOOTHING = addClientSetting("temp_smoothing", () -> 10d, holder -> holder.set(ClientSettingsConfig.getInstance().getTempSmoothing()));

        BODY_ICON_POS = addClientSetting("body_icon_pos", Vec2i::new, holder -> holder.set(new Vec2i(ClientSettingsConfig.getInstance().getBodyIconX(),
                                                                  ClientSettingsConfig.getInstance().getBodyIconY())));
        BODY_ICON_ENABLED = addClientSetting("body_icon_enabled", () -> true, holder -> holder.set(ClientSettingsConfig.getInstance().isBodyIconEnabled()));

        MOVE_BODY_ICON_WHEN_ADVANCED = addClientSetting("move_body_icon_for_advanced", () -> true, holder -> holder.set(ClientSettingsConfig.getInstance().moveBodyIconWhenAdvanced()));

        BODY_READOUT_POS = addClientSetting("body_readout_pos", Vec2i::new, holder -> holder.set(new Vec2i(ClientSettingsConfig.getInstance().getBodyReadoutX(),
                                                                                ClientSettingsConfig.getInstance().getBodyReadoutY())));
        BODY_READOUT_ENABLED = addClientSetting("body_readout_enabled", () -> true, holder -> holder.set(ClientSettingsConfig.getInstance().isBodyReadoutEnabled()));

        WORLD_GAUGE_POS = addClientSetting("world_gauge_pos", Vec2i::new, holder -> holder.set(new Vec2i(ClientSettingsConfig.getInstance().getWorldGaugeX(),
                                                                    ClientSettingsConfig.getInstance().getWorldGaugeY())));
        WORLD_GAUGE_ENABLED = addClientSetting("world_gauge_enabled", () -> true, holder -> holder.set(ClientSettingsConfig.getInstance().isWorldGaugeEnabled()));

        CUSTOM_HOTBAR_LAYOUT = addClientSetting("custom_hotbar_layout", () -> true, holder -> holder.set(ClientSettingsConfig.getInstance().customHotbarEnabled()));
        ICON_BOBBING = addClientSetting("icon_bobbing", () -> true, holder -> holder.set(ClientSettingsConfig.getInstance().isIconBobbingEnabled()));

        HEARTH_DEBUG = addClientSetting("hearth_debug", () -> true, holder -> holder.set(ClientSettingsConfig.getInstance().isHearthDebugEnabled()));

        SHOW_CONFIG_BUTTON = addClientSetting("show_config_button", () -> true, holder -> holder.set(ClientSettingsConfig.getInstance().isConfigButtonEnabled()));
        CONFIG_BUTTON_POS = addClientSetting("config_button_pos", Vec2i::new, holder -> holder.set(new Vec2i(ClientSettingsConfig.getInstance().getConfigButtonPos().get(0),
                                                                          ClientSettingsConfig.getInstance().getConfigButtonPos().get(1))));

        DISTORTION_EFFECTS = addClientSetting("distortion_effects", () -> true, holder -> holder.set(ClientSettingsConfig.getInstance().areDistortionsEnabled()));

        HIGH_CONTRAST = addClientSetting("high_contrast", () -> false, holder -> holder.set(ClientSettingsConfig.getInstance().isHighContrast()));

        SHOW_CREATIVE_WARNING = addClientSetting("show_creative_warning", () -> true, holder -> holder.set(ClientSettingsConfig.getInstance().isCreativeWarningEnabled()));

        HIDE_TOOLTIPS = addClientSetting("hide_tooltips", () -> false, holder -> holder.set(ClientSettingsConfig.getInstance().hideTooltips()));
        EXPAND_TOOLTIPS = addClientSetting("expand_tooltips", () -> true, holder -> holder.set(ClientSettingsConfig.EXPAND_TOOLTIPS.get()));

        SHOW_WATER_EFFECT = addClientSetting("show_water_effect", () -> true, holder -> holder.set(ClientSettingsConfig.getInstance().isWaterEffectEnabled()));

        boolean ssLoaded = CompatManager.isSereneSeasonsLoaded();
        SUMMER_TEMPS = addSetting("summer_temps", () -> new Double[]{0d, 0d, 0d}, holder -> holder.set(ssLoaded ? WorldSettingsConfig.getInstance().getSummerTemps() : new Double[3]));
        AUTUMN_TEMPS = addSetting("autumn_temps", () -> new Double[]{0d, 0d, 0d}, holder -> holder.set(ssLoaded ? WorldSettingsConfig.getInstance().getAutumnTemps() : new Double[3]));
        WINTER_TEMPS = addSetting("winter_temps", () -> new Double[]{0d, 0d, 0d}, holder -> holder.set(ssLoaded ? WorldSettingsConfig.getInstance().getWinterTemps() : new Double[3]));
        SPRING_TEMPS = addSetting("spring_temps", () -> new Double[]{0d, 0d, 0d}, holder -> holder.set(ssLoaded ? WorldSettingsConfig.getInstance().getSpringTemps() : new Double[3]));
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

    public static <T> DynamicHolder<T> addSyncedSetting(String id, Supplier<T> defaultVal, Consumer<DynamicHolder<T>> loader, Function<T, CompoundNBT> writer, Function<CompoundNBT, T> reader, Consumer<T> saver)
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

    public static Map<String, CompoundNBT> encode(DynamicRegistries registryAccess)
    {
        Map<String, CompoundNBT> map = new HashMap<>();
        CONFIG_SETTINGS.forEach((key, value) ->
        {   if (value.isSynced())
            {   map.put(key, value.encode(registryAccess));
            }
        });
        return map;
    }

    public static void decode(String key, CompoundNBT tag, DynamicRegistries registryAccess)
    {
        CONFIG_SETTINGS.computeIfPresent(key, (k, value) ->
        {   value.decode(tag, registryAccess);
            return value;
        });
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
    }
}
