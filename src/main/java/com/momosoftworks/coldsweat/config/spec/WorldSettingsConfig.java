package com.momosoftworks.coldsweat.config.spec;

import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.serialization.ListBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class WorldSettingsConfig
{
    private static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> BIOME_TEMP_OFFSETS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> BIOME_TEMPERATURES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> DIMENSION_TEMP_OFFSETS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> DIMENSION_TEMPERATURES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> STRUCTURE_TEMP_OFFSETS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> STRUCTURE_TEMPERATURES;

    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> BLOCK_TEMPERATURES;
    public static final ForgeConfigSpec.IntValue MAX_BLOCK_TEMP_RANGE;

    public static final ForgeConfigSpec.ConfigValue<Boolean> IS_SOUL_FIRE_COLD;

    public static ForgeConfigSpec.ConfigValue<List<? extends Number>> SUMMER_TEMPERATURES;
    public static ForgeConfigSpec.ConfigValue<List<? extends Number>> AUTUMN_TEMPERATURES;
    public static ForgeConfigSpec.ConfigValue<List<? extends Number>> WINTER_TEMPERATURES;
    public static ForgeConfigSpec.ConfigValue<List<? extends Number>> SPRING_TEMPERATURES;

    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_SMART_HEARTH;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_SMART_BOILER;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_SMART_ICEBOX;
    public static final ForgeConfigSpec.ConfigValue<Double> SOURCE_EFFECT_STRENGTH;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SOURCE_SPREAD_WHITELIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SOURCE_SPREAD_BLACKLIST;

    public static final ForgeConfigSpec.ConfigValue<Integer> HEARTH_RANGE;
    public static final ForgeConfigSpec.ConfigValue<Integer> HEARTH_MAX_RANGE;
    public static final ForgeConfigSpec.ConfigValue<Integer> HEARTH_MAX_VOLUME;
    public static final ForgeConfigSpec.ConfigValue<Integer> HEARTH_WARM_UP_TIME;
    public static final ForgeConfigSpec.ConfigValue<Integer> HEARTH_MAX_INSULATION;

    public static final ForgeConfigSpec.ConfigValue<Integer> BOILER_RANGE;
    public static final ForgeConfigSpec.ConfigValue<Integer> BOILER_MAX_RANGE;
    public static final ForgeConfigSpec.ConfigValue<Integer> BOILER_MAX_VOLUME;
    public static final ForgeConfigSpec.ConfigValue<Integer> BOILER_WARM_UP_TIME;
    public static final ForgeConfigSpec.ConfigValue<Integer> BOILER_MAX_INSULATION;

    public static final ForgeConfigSpec.ConfigValue<Integer> ICEBOX_RANGE;
    public static final ForgeConfigSpec.ConfigValue<Integer> ICEBOX_MAX_RANGE;
    public static final ForgeConfigSpec.ConfigValue<Integer> ICEBOX_MAX_VOLUME;
    public static final ForgeConfigSpec.ConfigValue<Integer> ICEBOX_WARM_UP_TIME;
    public static final ForgeConfigSpec.ConfigValue<Integer> ICEBOX_MAX_INSULATION;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SLEEPING_OVERRIDE_BLOCKS;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOULD_CHECK_SLEEP;

    public static final ForgeConfigSpec.ConfigValue<Boolean> CUSTOM_WATER_FREEZE_BEHAVIOR;
    public static final ForgeConfigSpec.ConfigValue<Boolean> CUSTOM_ICE_DROPS;

    public static final WorldSettingsConfig INSTANCE = new WorldSettingsConfig();

    static
    {
        /*
         Dimensions
         */
        BUILDER.comment("Format: [[\"dimension_1\", temperature1, *units], [\"dimension_2\", temperature2, *units]... etc]",
                        "Common dimension IDs: minecraft:overworld, minecraft:the_nether, minecraft:the_end")
               .push("Dimensions");

        DIMENSION_TEMP_OFFSETS = BUILDER
                .comment("Applies an offset to the world's temperature across an entire dimension")
            .defineListAllowEmpty(List.of("Dimension Temperature Offsets"), () -> List.of(
                    List.of("minecraft:the_nether", 1.0),
                    List.of("minecraft:the_end", -0.1)
            ), it -> it instanceof List<?> list
                    && list.get(0) instanceof String
                    && list.get(1) instanceof Number
                    && (list.size() < 3 || list.get(2) instanceof String));

        DIMENSION_TEMPERATURES = BUILDER
            .comment("Defines the temperature of a dimension, overriding all other biome and dimension temperatures/settings")
            .defineListAllowEmpty(List.of("Dimension Temperatures"), () -> List.of(
                    // No default values
            ), it -> it instanceof List<?> list
                    && list.get(0) instanceof String
                    && list.get(1) instanceof Number
                    && (list.size() < 3 || list.get(2) instanceof String));

        BUILDER.pop();

        /*
         Biomes
         */
        BUILDER.comment("Format: [[\"biome_1\", tempLow, tempHigh, *units], [\"biome_2\", tempLow, tempHigh, *units]... etc]",
                       "temp-low: The temperature of the biome at midnight",
                       "temp-high: The temperature of the biome at noon",
                       "units: Optional. The units of the temperature (\"C\" or \"F\". Defaults to MC units)")
               .push("Biomes");

        BIOME_TEMP_OFFSETS = BUILDER
            .comment("Applies an offset to the temperature of a biome")
            .defineListAllowEmpty(List.of("Biome Temperature Offsets"), () -> List.of(),
                it -> it instanceof List<?> list
                      && list.get(0) instanceof String
                      && list.get(1) instanceof Number
                      && (list.size() < 3 || list.get(2) instanceof Number)
                      && (list.size() < 4 || list.get(3) instanceof String)
                );


        BIOME_TEMPERATURES = BUILDER
            .comment("Defines the temperature of a biome, overriding the biome's default temperature")
            .defineListAllowEmpty(List.of("Biome Temperatures"), () -> ListBuilder.begin(
                            List.of("minecraft:soul_sand_valley", 53, 53, "F"),
                            List.of("minecraft:old_growth_birch_forest", 58, 72, "F"),
                            List.of("minecraft:river", 60, 70, "F"),
                            List.of("minecraft:swamp", 72, 84, "F"),
                            List.of("minecraft:savanna", 70, 95, "F"),
                            List.of("minecraft:savanna_plateau", 76, 98, "F"),
                            List.of("minecraft:windswept_savanna", 67, 90, "F"),
                            List.of("minecraft:taiga", 44, 62, "F"),
                            List.of("minecraft:snowy_taiga", 19, 48, "F"),
                            List.of("minecraft:old_growth_pine_taiga", 48, 62, "F"),
                            List.of("minecraft:old_growth_spruce_taiga", 48, 62, "F"),
                            List.of("minecraft:desert", 48, 115, "F"),
                            List.of("minecraft:stony_shore", 50, 64, "F"),
                            List.of("minecraft:snowy_beach", 38, 52, "F"),
                            List.of("minecraft:snowy_slopes", 24, 38, "F"),
                            List.of("minecraft:windswept_forest", 48, 66, "F"),
                            List.of("minecraft:frozen_peaks", 15, 33, "F"),
                            List.of("minecraft:warm_ocean", 67, 76, "F"),
                            List.of("minecraft:deep_frozen_ocean", 56, 65, "F"),
                            List.of("minecraft:jungle", 76, 87, "F"),
                            List.of("minecraft:bamboo_jungle", 76, 87, "F"),
                            List.of("minecraft:badlands", 84, 120, "F"),
                            List.of("minecraft:wooded_badlands", 80, 108, "F"),
                            List.of("minecraft:eroded_badlands", 88, 120, "F"))
                     .addIf(CompatManager.isBiomesOPlentyLoaded(),
                            () -> List.of("biomesoplenty:bayou", 67, 78, "F"),
                            () -> List.of("biomesoplenty:bog", 62, 73, "F"),
                            () -> List.of("biomesoplenty:fir_clearing", 56, 68, "F"),
                            () -> List.of("biomesoplenty:marsh", 76, 87, "F"),
                            () -> List.of("biomesoplenty:wetland", 63, 74, "F"),
                            () -> List.of("biomesoplenty:field", 64, 85, "F"),
                            () -> List.of("biomesoplenty:ominous_woods", 65, 72, "F"),
                            () -> List.of("biomesoplenty:coniferous_forest", 44, 58, "F"),
                            () -> List.of("biomesoplenty:seasonal_forest", 52, 64, "F"),
                            () -> List.of("biomesoplenty:pumpkin_patch", 57, 78, "F"),
                            () -> List.of("biomesoplenty:woodland", 67, 80, "F"),
                            () -> List.of("biomesoplenty:mediterranean_forest", 64, 78, "F"),
                            () -> List.of("biomesoplenty:dune_beach", 67, 78, "F"),
                            () -> List.of("biomesoplenty:rocky_rainforest", 73, 86, "F"),
                            () -> List.of("biomesoplenty:old_growth_woodland", 65, 78, "F"),
                            () -> List.of("biomesoplenty:forested_field", 64, 78, "F"),
                            () -> List.of("biomesoplenty:fungal_jungle", 73, 86, "F"),
                            () -> List.of("biomesoplenty:highland", 57, 70, "F"),
                            () -> List.of("biomesoplenty:highland_moor", 54, 68, "F"),
                            () -> List.of("biomesoplenty:grassland", 58, 82, "F"),
                            () -> List.of("biomesoplenty:clover_patch", 56, 78, "F"),
                            () -> List.of("biomesoplenty:jade_cliffs", 57, 70, "F"),
                            () -> List.of("biomesoplenty:lush_desert", 72, 94, "F"),
                            () -> List.of("biomesoplenty:dryland", 67, 97, "F"),
                            () -> List.of("biomesoplenty:maple_woods", 58, 68, "F"),
                            () -> List.of("biomesoplenty:mystic_grove", 65, 72, "F"),
                            () -> List.of("biomesoplenty:orchard", 58, 78, "F"),
                            () -> List.of("biomesoplenty:prairie", 66, 82, "F"),
                            () -> List.of("biomesoplenty:origin_valley", 65, 80, "F"),
                            () -> List.of("biomesoplenty:snowy_coniferous_forest", 28, 48, "F"),
                            () -> List.of("biomesoplenty:snowy_fir_clearing", 32, 51, "F"),
                            () -> List.of("biomesoplenty:snowy_maple_woods", 32, 48, "F"),
                            () -> List.of("biomesoplenty:spider_nest", 75, 75, "F"),
                            () -> List.of("biomesoplenty:volcanic_plains", 82, 95, "F"),
                            () -> List.of("biomesoplenty:volcano", 94, 120, "F"),
                            () -> List.of("biomesoplenty:wooded_wasteland", 78, 95, "F"))
                    .addIf(CompatManager.isBiomesYoullGoLoaded(),
                            () -> List.of("byg:coniferous_forest", 52, 70, "F"),
                            () -> List.of("byg:autumnal_valley", 58, 67, "F"),
                            () -> List.of("byg:autumnal_forest", 60, 75, "F"),
                            () -> List.of("byg:autumnal_taiga", 56, 68, "F"),
                            () -> List.of("byg:baobab_savanna", 70, 95, "F"),
                            () -> List.of("byg:dacite_ridges", 40, 65, "F"),
                            () -> List.of("byg:firecracker_shrubland", 72, 105, "F"),
                            () -> List.of("byg:frosted_taiga", 22, 48, "F"),
                            () -> List.of("byg:cypress_swamplands", 68, 82, "F"),
                            () -> List.of("byg:dead_sea", 72, 82, "F"),
                            () -> List.of("byg:lush_stacks", 66, 75, "F"),
                            () -> List.of("byg:fragment_forest", 43, 64, "F"),
                            () -> List.of("byg:frosted_coniferous_forest", 8, 31, "F"),
                            () -> List.of("byg:maple_taiga", 53, 71, "F"),
                            () -> List.of("byg:skyris_vale", 42, 68, "F"),
                            () -> List.of("byg:twilight_meadow", 49, 66, "F"),
                            () -> List.of("byg:weeping_witch_forest", 56, 73, "F"),
                            () -> List.of("byg:subzero_hypogeal", -10, -10, "F"),
                            () -> List.of("byg:zelkova_forest", 44, 61, "F"))
                    .addIf(CompatManager.isAtmosphericLoaded(),
                            () -> List.of("atmospheric:dunes", 78, 115, "F"),
                            () -> List.of("atmospheric:flourishing_dunes", 68, 105, "F"),
                            () -> List.of("atmospheric:petrified_dunes", 58, 120, "F"),
                            () -> List.of("atmospheric:rocky_dunes", 55, 125, "F"),
                            () -> List.of("atmospheric:rainforest", 68, 90, "F"),
                            () -> List.of("atmospheric:rainforest_basin", 68, 90, "F"),
                            () -> List.of("atmospheric:sparse_rainforest", 62, 83, "F"),
                            () -> List.of("atmospheric:sparse_rainforest_basin", 62, 83, "F"))
                    .addIf(CompatManager.isEnvironmentalLoaded(),
                            () -> List.of("environmental:marsh", 60, 80, "F"))
                    .addIf(CompatManager.isTerralithLoaded(),
                            () -> List.of("terralith:moonlight_valley", 57, 76, "F"),
                            () -> List.of("terralith:rocky_mountains", 45, 73, "F"),
                            () -> List.of("terralith:blooming_plateau", 49, 78, "F"),
                            () -> List.of("terralith:alpine_grove", 16, 53, "F"),
                            () -> List.of("terralith:yellowstone", 47, 68, "F"),
                            () -> List.of("terralith:forested_highlands", 43, 70, "F"),
                            () -> List.of("terralith:temperate_highlands", 54, 80, "F"),
                            () -> List.of("terralith:amethyst_rainforest", 69, 84, "F"),
                            () -> List.of("terralith:sandstone_valley", 78, 114, "F"),
                            () -> List.of("terralith:ancient_sands", 83, 130, "F"),
                            () -> List.of("terralith:arid_highlands", 74, 102, "F"),
                            () -> List.of("terralith:volcanic_crater", 96, 162, "F"),
                            () -> List.of("terralith:volcanic_peaks", 76, 122, "F"),
                            () -> List.of("terralith:basalt_cliffs", 76, 122, "F"),
                            () -> List.of("terralith:birch_taiga", 40, 62, "F"),
                            () -> List.of("terralith:brushland", 64, 89, "F"),
                            () -> List.of("terralith:bryce_canyon", 72, 110, "F"),
                            () -> List.of("terralith:caldera", 58, 71, "F"),
                            () -> List.of("terralith:cloud_forest", 38, 58, "F"),
                            () -> List.of("terralith:desert_canyon", 73, 121, "F"),
                            () -> List.of("terralith:desert_spires", 60, 121, "F"),
                            () -> List.of("terralith:orchid_swamp", 62, 81, "F"),
                            () -> List.of("terralith:fractured_savanna", 65, 92, "F"),
                            () -> List.of("terralith:savanna_badlands", 68, 99, "F"),
                            () -> List.of("terralith:granite_cliffs", 65, 85, "F"),
                            () -> List.of("terralith:granite_cliffs", 65, 85, "F"),
                            () -> List.of("terralith:haze_mountain", 62, 74, "F"),
                            () -> List.of("terralith:highlands", 62, 74, "F"),
                            () -> List.of("terralith:lavender_valley", 59, 76, "F"),
                            () -> List.of("terralith:lavender_forest", 56, 75, "F"),
                            () -> List.of("terralith:red_oasis", 58, 94, "F"),
                            () -> List.of("terralith:shield", 48, 68, "F"),
                            () -> List.of("terralith:shield_clearing", 28, 80, "F"),
                            () -> List.of("terralith:steppe", 44, 78, "F"),
                            () -> List.of("terralith:warped_mesa", 66, 84, "F"))
                    .addIf(CompatManager.isWythersLoaded(),
                            () -> List.of("wythers:ancient_copper_beech_forest", 12, 16, "C"),
                            () -> List.of("wythers:ancient_emerald_beech_forest", 12, 15, "C"),
                            () -> List.of("wythers:ancient_golden_beech_forest", 12, 15, "C"),
                            () -> List.of("wythers:ancient_moss_forest", 12, 15, "C"),
                            () -> List.of("wythers:ancient_mossy_swamp", 10, 12, "C"),
                            () -> List.of("wythers:ancient_oak_swamp", 12, 15, "C"),
                            () -> List.of("wythers:ancient_taiga", 15, 18, "C"),
                            () -> List.of("wythers:andesite_crags", 20, 25, "C"),
                            () -> List.of("wythers:aspen_crags", 18, 20, "C"),
                            () -> List.of("wythers:autumnal_birch_forest", 15, 16, "C"),
                            () -> List.of("wythers:autumnal_crags", 15, 18, "C"),
                            () -> List.of("wythers:autumnal_flower_forest", 18, 20, "C"),
                            () -> List.of("wythers:autumnal_forest", 15, 16, "C"),
                            () -> List.of("wythers:autumnal_forest_edge", 15, 18, "C"),
                            () -> List.of("wythers:autumnal_plains", 18, 20, "C"),
                            () -> List.of("wythers:autumnal_swamp", 12, 15, "C"),
                            () -> List.of("wythers:ayers_rock", 32, 35, "C"),
                            () -> List.of("wythers:badlands_canyon", 30, 35, "C"),
                            () -> List.of("wythers:badlands_desert", 35, 40, "C"),
                            () -> List.of("wythers:badlands_river", 30, 34, "C"),
                            () -> List.of("wythers:bamboo_jungle_canyon", 12, 18, "C"),
                            () -> List.of("wythers:bamboo_jungle_highlands", 13, 15, "C"),
                            () -> List.of("wythers:bamboo_jungle_swamp", 12, 15, "C"),
                            () -> List.of("wythers:bamboo_swamp", 12, 15, "C"),
                            () -> List.of("wythers:bayou", 14, 18, "C"),
                            () -> List.of("wythers:berry_bog", 12, 16, "C"),
                            () -> List.of("wythers:billabong", 12, 16, "C"),
                            () -> List.of("wythers:birch_swamp", 12, 16, "C"),
                            () -> List.of("wythers:birch_taiga", 12, 16, "C"),
                            () -> List.of("wythers:black_beach", 18, 22, "C"),
                            () -> List.of("wythers:black_river", 15, 20, "C"),
                            () -> List.of("wythers:boreal_forest_red", 20, 23, "C"),
                            () -> List.of("wythers:boreal_forest_yellow", 20, 25, "C"),
                            () -> List.of("wythers:cactus_desert", 30, 45, "C"),
                            () -> List.of("wythers:calcite_caverns", 30, 35, "C"),
                            () -> List.of("wythers:calcite_coast", 30, 35, "C"),
                            () -> List.of("wythers:chaparral", 30, 35, "C"),
                            () -> List.of("wythers:coastal_mangroves", 15, 19, "C"),
                            () -> List.of("wythers:cold_island", -1, 3, "C"),
                            () -> List.of("wythers:cold_plains", -5, 2, "C"),
                            () -> List.of("wythers:cold_stony_shore", -10, 2, "C"),
                            () -> List.of("wythers:cool_forest", 5, 10, "C"),
                            () -> List.of("wythers:cool_forest_edge", 5, 10, "C"),
                            () -> List.of("wythers:cool_plains", 5, 10, "C"),
                            () -> List.of("wythers:cool_stony_canyons", 0, 1, "C"),
                            () -> List.of("wythers:cool_stony_peaks", -10, 0, "C"),
                            () -> List.of("wythers:crimson_tundra", 12, 15, "C"),
                            () -> List.of("wythers:danakil_desert", 30, 40, "C"),
                            () -> List.of("wythers:deep_dark_forest", 5, 6, "C"),
                            () -> List.of("wythers:deep_desert", 30, 40, "C"),
                            () -> List.of("wythers:deep_desert_river", 25, 30, "C"),
                            () -> List.of("wythers:deep_icy_ocean", -20, 0, "C"),
                            () -> List.of("wythers:deep_snowy_taiga", -10, 0, "C"),
                            () -> List.of("wythers:deep_underground", 0, 5, "C"),
                            () -> List.of("wythers:deepslate_shore", 10, 12, "C"),
                            () -> List.of("wythers:desert_beach", 30, 35, "C"),
                            () -> List.of("wythers:desert_island", 40, 45, "C"),
                            () -> List.of("wythers:desert_lakes", 30, 31, "C"),
                            () -> List.of("wythers:desert_pinnacles", 28, 30, "C"),
                            () -> List.of("wythers:desert_river", 30, 35, "C"),
                            () -> List.of("wythers:dripleaf_swamp", 20, 25, "C"),
                            () -> List.of("wythers:dry_savanna", 29, 35, "C"),
                            () -> List.of("wythers:dry_tropical_forest", 20, 26, "C"),
                            () -> List.of("wythers:dry_tropical_grassland", 28, 30, "C"),
                            () -> List.of("wythers:eucalyptus_deanei_forest", 20, 25, "C"),
                            () -> List.of("wythers:eucalyptus_jungle", 20, 25, "C"),
                            () -> List.of("wythers:eucalyptus_jungle_canyon", 20, 25, "C"),
                            () -> List.of("wythers:eucalyptus_salubris_woodland", 20, 25, "C"),
                            () -> List.of("wythers:eucalyptus_woodland", 22, 25, "C"),
                            () -> List.of("wythers:fen", 10, 15, "C"),
                            () -> List.of("wythers:flooded_jungle", 12, 18, "C"),
                            () -> List.of("wythers:flooded_rainforest", 16, 20, "C"),
                            () -> List.of("wythers:flooded_savanna", 16, 20, "C"),
                            () -> List.of("wythers:flooded_temperate_rainforest", 20, 22, "C"),
                            () -> List.of("wythers:flowering_pantanal", 25, 28, "C"),
                            () -> List.of("wythers:forbidden_forest", 12, 18, "C"),
                            () -> List.of("wythers:forest_edge", 15, 18, "C"),
                            () -> List.of("wythers:forested_highlands", 15, 18, "C"),
                            () -> List.of("wythers:frigid_island", 0, 5, "C"),
                            () -> List.of("wythers:frozen_island", -15, 0, "C"),
                            () -> List.of("wythers:fungous_dripstone_caves", 5, 10, "C"),
                            () -> List.of("wythers:giant_sequoia_forest", 10, 15, "C"),
                            () -> List.of("wythers:glacial_cliffs", 1, 10, "C"),
                            () -> List.of("wythers:granite_canyon", 15, 20, "C"),
                            () -> List.of("wythers:gravelly_beach", 20, 25, "C"),
                            () -> List.of("wythers:gravelly_river", 15, 20, "C"),
                            () -> List.of("wythers:guelta", 15, 18, "C"),
                            () -> List.of("wythers:harvest_fields", 20, 26, "C"),
                            () -> List.of("wythers:highland_plains", 20, 25, "C"),
                            () -> List.of("wythers:highland_tropical_rainforest", 15, 20, "C"),
                            () -> List.of("wythers:highlands", 20, 25, "C"),
                            () -> List.of("wythers:huangshan_highlands", 15, 25, "C"),
                            () -> List.of("wythers:humid_tropical_grassland", 20, 26, "C"),
                            () -> List.of("wythers:ice_cap", 0, 5, "C"),
                            () -> List.of("wythers:icy_crags", -5, 0, "C"),
                            () -> List.of("wythers:icy_ocean", -5, 0, "C"),
                            () -> List.of("wythers:icy_river", -5, 0, "C"),
                            () -> List.of("wythers:icy_shore", -5, 0, "C"),
                            () -> List.of("wythers:icy_volcano", 1, 2, "C"),
                            () -> List.of("wythers:jacaranda_savanna", 20, 25, "C"),
                            () -> List.of("wythers:jade_highlands", 20, 25, "C"),
                            () -> List.of("wythers:jungle_canyon", 20, 25, "C"),
                            () -> List.of("wythers:jungle_island", 20, 25, "C"),
                            () -> List.of("wythers:jungle_river", 20, 25, "C"),
                            () -> List.of("wythers:kwongan_heath", 30, 50, "C"),
                            () -> List.of("wythers:lantern_river", 15, 20, "C"),
                            () -> List.of("wythers:lapacho_plains", 20, 25, "C"),
                            () -> List.of("wythers:larch_taiga", 20, 22, "C"),
                            () -> List.of("wythers:lichenous_caves", 15, 20, "C"),
                            () -> List.of("wythers:lichenous_dripstone_caves", 12, 18, "C"),
                            () -> List.of("wythers:lush_dripstone_caves", 12, 18, "C"),
                            () -> List.of("wythers:lush_fungous_dripstone_caves", 10, 15, "C"),
                            () -> List.of("wythers:lush_shroom_caves", 15, 18, "C"),
                            () -> List.of("wythers:maple_mountains", 15, 18, "C"),
                            () -> List.of("wythers:marsh", 12, 15, "C"),
                            () -> List.of("wythers:mediterranean_island", 15, 18, "C"),
                            () -> List.of("wythers:mediterranean_island_thermal_springs", 25, 30, "C"),
                            () -> List.of("wythers:mossy_caves", 12, 15, "C"),
                            () -> List.of("wythers:mossy_dripstone_caves", 12, 15, "C"),
                            () -> List.of("wythers:mud_pools", 20, 25, "C"),
                            () -> List.of("wythers:mushroom_caves", 20, 25, "C"),
                            () -> List.of("wythers:mushroom_island", 25, 28, "C"),
                            () -> List.of("wythers:old_growth_taiga_crags", 18, 28, "C"),
                            () -> List.of("wythers:old_growth_taiga_swamp", 18, 22, "C"),
                            () -> List.of("wythers:outback", 30, 38, "C"),
                            () -> List.of("wythers:outback_desert", 40, 45, "C"),
                            () -> List.of("wythers:pantanal", 12, 18, "C"),
                            () -> List.of("wythers:phantasmal_forest", 15, 18, "C"),
                            () -> List.of("wythers:phantasmal_swamp", 15, 16, "C"),
                            () -> List.of("wythers:pine_barrens", 20, 26, "C"),
                            () -> List.of("wythers:red_desert", 30, 35, "C"),
                            () -> List.of("wythers:red_rock_canyon", 30, 32, "C"),
                            () -> List.of("wythers:sakura_forest", 26, 32, "C"),
                            () -> List.of("wythers:salt_lakes_pink", 16, 28, "C"),
                            () -> List.of("wythers:salt_lakes_turquoise", 16, 20, "C"),
                            () -> List.of("wythers:salt_lakes_white", 16, 29, "C"),
                            () -> List.of("wythers:sand_dunes", 30, 35, "C"),
                            () -> List.of("wythers:sandy_jungle", 30, 38, "C"),
                            () -> List.of("wythers:savanna_badlands", 35, 39, "C"),
                            () -> List.of("wythers:savanna_basaltic_incursions", 35, 40, "C"),
                            () -> List.of("wythers:savanna_river", 30, 35, "C"),
                            () -> List.of("wythers:scrub_forest", 20, 28, "C"),
                            () -> List.of("wythers:scrubland", 15, 18, "C"),
                            () -> List.of("wythers:snowy_bog", 0, 5, "C"),
                            () -> List.of("wythers:snowy_canyon", -5, 0, "C"),
                            () -> List.of("wythers:snowy_fen", -10, 0, "C"),
                            () -> List.of("wythers:snowy_peaks", -20, 0, "C"),
                            () -> List.of("wythers:snowy_thermal_taiga", 0, 5, "C"),
                            () -> List.of("wythers:snowy_tundra", 0, 2, "C"),
                            () -> List.of("wythers:sparse_bamboo_jungle", 15, 18, "C"),
                            () -> List.of("wythers:sparse_eucalyptus_jungle", 15, 18, "C"),
                            () -> List.of("wythers:sparse_eucalyptus_woodland", 20, 25, "C"),
                            () -> List.of("wythers:spring_flower_fields", 20, 28, "C"),
                            () -> List.of("wythers:spring_flower_forest", 20, 28, "C"),
                            () -> List.of("wythers:stony_canyon", 10, 15, "C"),
                            () -> List.of("wythers:subtropical_forest", 15, 18, "C"),
                            () -> List.of("wythers:subtropical_forest_edge", 15, 18, "C"),
                            () -> List.of("wythers:subtropical_grassland", 15, 20, "C"),
                            () -> List.of("wythers:taiga_crags", 12, 16, "C"),
                            () -> List.of("wythers:tangled_forest", 12, 16, "C"),
                            () -> List.of("wythers:temperate_island", 20, 25, "C"),
                            () -> List.of("wythers:temperate_rainforest", 20, 25, "C"),
                            () -> List.of("wythers:temperate_rainforest_crags", 20, 25, "C"),
                            () -> List.of("wythers:tepui", 10, 15, "C"),
                            () -> List.of("wythers:thermal_taiga", 20, 25, "C"),
                            () -> List.of("wythers:thermal_taiga_crags", 20, 25, "C"),
                            () -> List.of("wythers:tibesti_mountains", 30, 35, "C"),
                            () -> List.of("wythers:tropical_beach", 35, 38, "C"),
                            () -> List.of("wythers:tropical_forest", 22, 30, "C"),
                            () -> List.of("wythers:tropical_forest_canyon", 25, 30, "C"),
                            () -> List.of("wythers:tropical_forest_river", 22, 30, "C"),
                            () -> List.of("wythers:tropical_grassland", 20, 30, "C"),
                            () -> List.of("wythers:tropical_island", 24, 34, "C"),
                            () -> List.of("wythers:tropical_rainforest", 25, 32, "C"),
                            () -> List.of("wythers:tropical_volcano", 40, 50, "C"),
                            () -> List.of("wythers:tsingy_forest", 15, 18, "C"),
                            () -> List.of("wythers:tundra", 10, 15, "C"),
                            () -> List.of("wythers:underground", 15, 18, "C"),
                            () -> List.of("wythers:volcanic_chamber", 35, 45, "C"),
                            () -> List.of("wythers:volcanic_crater", 35, 45, "C"),
                            () -> List.of("wythers:volcano", 40, 50, "C"),
                            () -> List.of("wythers:warm_birch_forest", 30, 37, "C"),
                            () -> List.of("wythers:warm_stony_shore", 30, 37, "C"),
                            () -> List.of("wythers:waterlily_swamp", 15, 18, "C"),
                            () -> List.of("wythers:windswept_jungle", 18, 22, "C"),
                            () -> List.of("wythers:wistman_woods", 18, 20, "C"),
                            () -> List.of("wythers:wooded_desert", 22, 25, "C"),
                            () -> List.of("wythers:wooded_savanna", 22, 26, "C")
                    ).build(),
                it -> it instanceof List<?> list
                      && list.get(0) instanceof String
                      && list.get(1) instanceof Number
                      && list.get(2) instanceof Number
                      && (list.size() < 4 || list.get(3) instanceof String)
                );

        BUILDER.pop();


        BUILDER.push("Blocks");

        BLOCK_TEMPERATURES = BUILDER
                .comment("Allows for adding simple BlockTemps without the use of Java mods",
                         "Format (All temperatures are in Minecraft units):",
                         "[[\"block-ids\", <temperature>, <range>, <*max effect>, <*predicates>, <*nbt>, <*temperature-limit>], [etc...], [etc...]]",
                         "(* = optional) (1 \u00B0MC = 42 \u00B0F/ 23.33 \u00B0C)",
                         "",
                         "Arguments:",
                         "block-ids: Multiple IDs can be used by separating them with commas (i.e: \"minecraft:torch,minecraft:wall_torch\")",
                         "temperature: The temperature of the block, in Minecraft units",
                         "*falloff: The block is less effective as distance increases",
                         "*max effect: The maximum temperature change this block can cause to a player (even with multiple blocks)",
                         "*predicates: The state that the block has to be in for the temperature to be applied (i.e. lit=true).",
                         "- (Multiple predicates can be used by separating them with commas [i.e. \"lit=true,waterlogged=false\"])",
                         "*nbt: The NBT data that the block must have for the temperature to be applied.",
                         "*temperature-limit: The maximum world temperature at which this block temp will be effective.",
                         "- (Represents the minimum temp if the block temp is negative)")
                .defineListAllowEmpty(List.of("Block Temperatures"), () -> List.of(
                                            List.of("cold_sweat:boiler",         0.27, 7, 0.88, "lit=true", "", 4),
                                            List.of("cold_sweat:icebox",        -0.27, 7, 0.88, "frosted=true", "", 0),
                                            List.of("minecraft:fire",           0.476, 7, 0.9, "", "", 8),
                                            List.of("#minecraft:campfires",     0.476, 7, 0.9, "lit=true", " ", 8),
                                            List.of("minecraft:magma_block",     0.25, 3, 1.0),
                                            List.of("minecraft:lava_cauldron",    0.5, 7, 1.5),
                                            List.of("minecraft:ice",            -0.15, 4, 0.6, "", "", -0.7),
                                            List.of("minecraft:packed_ice",     -0.25, 4, 1.0, "", "", -0.7),
                                            List.of("minecraft:blue_ice",       -0.35, 4, 1.4, "", "", -0.7),
                                            List.of("#minecraft:ice",           -0.15, 4, 0.6, "", "", -0.7)
                                      ),
                            it -> it instanceof List<?> list
                                    && list.size() >= 3
                                    && list.get(0) instanceof String
                                    && list.get(1) instanceof Number
                                    && list.get(2) instanceof Number
                                    && (list.size() < 4 || list.get(3) instanceof Number)
                                    && (list.size() < 5 || list.get(4) instanceof String)
                                    && (list.size() < 6 || list.get(5) instanceof String)
                                    && (list.size() < 7 || list.get(6) instanceof Number));

        MAX_BLOCK_TEMP_RANGE = BUILDER
                .comment("The maximum range of blocks' area of effect",
                         "Note: This will not change anything unless blocks are configured to utilize the expanded range",
                          "This value is capped at 16 for performance reasons")
                .defineInRange("Block Range", 7, 1, 16);

        CUSTOM_WATER_FREEZE_BEHAVIOR = BUILDER
                .comment("When set to true, uses Cold Sweat's temperature system to determine water freezing behavior")
                .define("Custom Freezing Behavior", true);

        CUSTOM_ICE_DROPS = BUILDER
                .comment("When set to true, modifies ice blocks to be harvestable with a pickaxe")
                .define("Custom Ice Drops", true);

        BUILDER.pop();


        BUILDER.push("Misc");

        STRUCTURE_TEMPERATURES = BUILDER
                .comment("Overrides the world temperature when the player is within this structure",
                         "Format: [[\"structure_1\", temperature1, *units], [\"structure_2\", temperature2, *units]... etc]",
                         "(* = optional)")
                .defineListAllowEmpty(List.of("Structure Temperatures"), () -> List.of(
                        List.of("minecraft:igloo", 65, "F")
                ), it -> it instanceof List<?> list
                        && list.get(0) instanceof String
                        && list.get(1) instanceof Number
                        && (list.size() < 3 || list.get(2) instanceof String));

        STRUCTURE_TEMP_OFFSETS = BUILDER
                .comment("Offsets the world temperature when the player is within this structure",
                         "Format: [[\"structure_1\", offset1, *units], [\"structure_2\", offset2, *units]... etc]",
                         "(* = optional)")
                .defineListAllowEmpty(List.of("Structure Temperature Offsets"), () -> List.of(
                        // empty
                ), it -> it instanceof List<?> list
                        && list.get(0) instanceof String
                        && list.get(1) instanceof Number
                        && (list.size() < 3 || list.get(2) instanceof String));

        SLEEPING_OVERRIDE_BLOCKS = BUILDER
                .comment("List of blocks that will allow the player to sleep on them, regardless of the \"Prevent Sleep When in Danger\" setting",
                         "Use this list if the player is not getting the temperature effect from sleeping on particular blocks")
                .defineListAllowEmpty(List.of("Sleep Check Override Blocks"), () -> ListBuilder.<String>begin()
                        .addIf(CompatManager.modLoaded("comforts"),
                                () -> "#comforts:sleeping_bags")
                        .build(),
                it -> it instanceof String);

        SHOULD_CHECK_SLEEP = BUILDER
                .comment("When set to true, players cannot sleep if they are cold or hot enough to die")
                .define("Check Sleeping Conditions", true);

        IS_SOUL_FIRE_COLD = BUILDER
                .comment("Converts damage dealt by Soul Fire to cold damage (default: true)",
                         "Does not affect the block's temperature")
                .define("Cold Soul Fire", true);

        BUILDER.pop();


        BUILDER.push("Thermal Sources");

        SOURCE_EFFECT_STRENGTH = BUILDER
                .comment("How effective thermal sources are at normalizing temperature")
                .defineInRange("Thermal Source Strength", 0.75, 0, 1.0);

        SOURCE_SPREAD_WHITELIST = BUILDER
                .comment("List of additional blocks that thermal sources can spread through",
                         "Use this list if thermal sources aren't spreading through particular blocks that they should")
                .defineListAllowEmpty(List.of("Thermal Source Spread Whitelist"), () -> ListBuilder.begin(
                                              "minecraft:iron_bars",
                                              "#minecraft:leaves")
                                          .addIf(CompatManager.isCreateLoaded(),
                                              () -> "create:encased_fluid_pipe")
                                          .build(),
                                      o -> o instanceof String);

        SOURCE_SPREAD_BLACKLIST = BUILDER
                .comment("List of additional blocks that thermal sources spread through",
                         "Use this list if thermal sources are spreading through particular blocks that they shouldn't")
                .defineListAllowEmpty(List.of("Thermal Source Spread Blacklist"), () -> List.of(
                            ),
                            o -> o instanceof String);

        BUILDER.push("Hearth");

        ENABLE_SMART_HEARTH = BUILDER
                .comment("Allows the hearth to automatically turn on/off based on nearby players' temperature",
                         "If false, it turns on/off by redstone signal instead")
                .define("Automatic Hearth", false);
        HEARTH_RANGE = BUILDER
                .comment("The distance the hearth's air will travel from a source, like the hearth itself or the end of a pipe")
                .defineInRange("Hearth Range", 20, 0, Integer.MAX_VALUE);
        HEARTH_MAX_RANGE = BUILDER
                .comment("The maximum distance that air can be piped away from the hearth")
                .defineInRange("Max Hearth Range", 96, 0, Integer.MAX_VALUE);
        HEARTH_MAX_VOLUME = BUILDER
                .comment("The maximum volume of the hearth's area of effect")
                .defineInRange("Hearth Volume", 12000, 1, Integer.MAX_VALUE);
        HEARTH_WARM_UP_TIME = BUILDER
                .comment("The time it takes for the hearth to be fully functional after being placed")
                .defineInRange("Hearth Warm-Up Time", 1200, 0, Integer.MAX_VALUE);
        HEARTH_MAX_INSULATION = BUILDER
                .comment("The maximum amount of insulation that the hearth can provide")
                .defineInRange("Hearth Chill/Warmth Strength", 10, 0, 10);

        BUILDER.pop();

        BUILDER.push("Boiler");

        ENABLE_SMART_BOILER = BUILDER
                .comment("Allows the boiler to automatically turn on/off based on nearby players' temperature",
                         "If false, it turns on/off by redstone signal instead")
                .define("Automatic Boiler", false);
        BOILER_RANGE = BUILDER
                .comment("The distance the boiler's air will travel from a source, like the boiler itself or the end of a pipe")
                .defineInRange("Boiler Range", 16, 0, Integer.MAX_VALUE);
        BOILER_MAX_RANGE = BUILDER
                .comment("The maximum distance that air can be piped away from the boiler")
                .defineInRange("Max Boiler Range", 96, 0, Integer.MAX_VALUE);
        BOILER_MAX_VOLUME = BUILDER
                .comment("The maximum volume of the boiler's area of effect")
                .defineInRange("Boiler Volume", 2000, 1, Integer.MAX_VALUE);
        BOILER_WARM_UP_TIME = BUILDER
                .comment("The time it takes for the boiler to be fully functional after being placed")
                .defineInRange("Boiler Warm-Up Time", 1200, 0, Integer.MAX_VALUE);
        BOILER_MAX_INSULATION = BUILDER
                .comment("The maximum amount of insulation that the boiler can provide")
                .defineInRange("Boiler Warmth Strength", 5, 0, 10);

        BUILDER.pop();

        BUILDER.push("Icebox");

        ENABLE_SMART_ICEBOX = BUILDER
                .comment("Allows the icebox to automatically turn on/off based on nearby players' temperature",
                         "If false, it turns on/off by redstone signal instead")
                .define("Automatic Icebox", false);
        ICEBOX_RANGE = BUILDER
                .comment("The distance the icebox's air will travel from a source, like the icebox itself or the end of a pipe")
                .defineInRange("Icebox Range", 16, 0, Integer.MAX_VALUE);
        ICEBOX_MAX_RANGE = BUILDER
                .comment("The maximum distance that air can be piped away from the icebox")
                .defineInRange("Max Icebox Range", 96, 0, Integer.MAX_VALUE);
        ICEBOX_MAX_VOLUME = BUILDER
                .comment("The maximum volume of the icebox's area of effect")
                .defineInRange("Icebox Volume", 2000, 1, Integer.MAX_VALUE);
        ICEBOX_WARM_UP_TIME = BUILDER
                .comment("The time it takes for the icebox to be fully functional after being placed")
                .defineInRange("Icebox Warm-Up Time", 1200, 0, Integer.MAX_VALUE);
        ICEBOX_MAX_INSULATION = BUILDER
                .comment("The maximum amount of insulation that the icebox can provide")
                .defineInRange("Icebox Chill Strength", 5, 0, 10);

        BUILDER.pop();

        BUILDER.pop();


        /* Serene Seasons config */
        if (CompatManager.isSereneSeasonsLoaded())
        {
            BUILDER.comment("Format: [season-start, season-mid, season-end]",
                            "Applied as an offset to the world's temperature")
                   .push("Season Temperatures");

            SUMMER_TEMPERATURES = BUILDER
                    .defineList("Summer", Arrays.asList(
                            0.4, 0.6, 0.4
                    ), it -> it instanceof Number);

            AUTUMN_TEMPERATURES = BUILDER
                    .defineList("Autumn", Arrays.asList(
                            0.2, 0, -0.2
                    ), it -> it instanceof Number);

            WINTER_TEMPERATURES = BUILDER
                    .defineList("Winter", Arrays.asList(
                            -0.4, -0.6, -0.4
                    ), it -> it instanceof Number);

            SPRING_TEMPERATURES = BUILDER
                    .defineList("Spring", Arrays.asList(
                            -0.2, 0, 0.2
                    ), it -> it instanceof Number);

            BUILDER.pop();
        }

        SPEC = BUILDER.build();
    }

    public static void setup()
    {
        Path configPath = FMLPaths.CONFIGDIR.get();
        Path csConfigPath = Paths.get(configPath.toAbsolutePath().toString(), "coldsweat");

        // Create the config folder
        try
        {   Files.createDirectory(csConfigPath);
        }
        catch (Exception ignored) {}

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "coldsweat/world_settings.toml");
    }

    public static WorldSettingsConfig getInstance()
    {   return INSTANCE;
    }

    public void save()
    {   SPEC.save();
    }

    /* Getters */

    public List<? extends List<?>> getBiomeTempOffsets()
    {   return BIOME_TEMP_OFFSETS.get();
    }
    public List<? extends List<?>> getBiomeTemperatures()
    {   return BIOME_TEMPERATURES.get();
    }

    public List<? extends List<?>> getDimensionTempOffsets()
    {   return DIMENSION_TEMP_OFFSETS.get();
    }
    public List<? extends List<?>> getDimensionTemperatures()
    {   return DIMENSION_TEMPERATURES.get();
    }

    public List<? extends List<?>> getStructureTempOffsets()
    {   return STRUCTURE_TEMP_OFFSETS.get();
    }
    public List<? extends List<?>> getStructureTemperatures()
    {   return STRUCTURE_TEMPERATURES.get();
    }

    public List<? extends List<?>> getBlockTemps()
    {   return BLOCK_TEMPERATURES.get();
    }
    public int getBlockRange()
    {   return MAX_BLOCK_TEMP_RANGE.get();
    }

    public double getHearthStrength()
    {   return SOURCE_EFFECT_STRENGTH.get();
    }
    public boolean isSmartHearth()
    {   return ENABLE_SMART_HEARTH.get();
    }
    public List<String> getHearthSpreadWhitelist()
    {   return (List<String>) SOURCE_SPREAD_WHITELIST.get();
    }
    public List<String> getHearthSpreadBlacklist()
    {   return (List<String>) SOURCE_SPREAD_BLACKLIST.get();
    }

    public static Double[] getSummerTemps()
    {   return SUMMER_TEMPERATURES.get().stream().map(Number::doubleValue).toArray(Double[]::new);
    }
    public static Double[] getAutumnTemps()
    {   return AUTUMN_TEMPERATURES.get().stream().map(Number::doubleValue).toArray(Double[]::new);
    }
    public static Double[] getWinterTemps()
    {   return WINTER_TEMPERATURES.get().stream().map(Number::doubleValue).toArray(Double[]::new);
    }
    public static Double[] getSpringTemps()
    {   return SPRING_TEMPERATURES.get().stream().map(Number::doubleValue).toArray(Double[]::new);
    }

    public boolean isSoulFireCold()
    {   return IS_SOUL_FIRE_COLD.get();
    }

    public boolean isSleepChecked()
    {   return SHOULD_CHECK_SLEEP.get();
    }

    public synchronized List<? extends String> getSleepOverrideBlocks()
    {   return SLEEPING_OVERRIDE_BLOCKS.get();
    }

    /* Setters */

    public static synchronized void setBiomeTemperatures(List<? extends List<?>> temps)
    {   synchronized (BIOME_TEMPERATURES)
        {   BIOME_TEMPERATURES.set(temps);
        }
    }

    public static synchronized void setBiomeTempOffsets(List<? extends List<?>> offsets)
    {   synchronized (BIOME_TEMP_OFFSETS)
        {   BIOME_TEMP_OFFSETS.set(offsets);
        }
    }

    public static synchronized void setDimensionTemperatures(List<? extends List<?>> temps)
    {   synchronized (DIMENSION_TEMPERATURES)
        {   DIMENSION_TEMPERATURES.set(temps);
        }
    }

    public static synchronized void setDimensionTempOffsets(List<? extends List<?>> offsets)
    {   synchronized (DIMENSION_TEMP_OFFSETS)
        {   DIMENSION_TEMP_OFFSETS.set(offsets);
        }
    }

    public static synchronized void setStructureTemperatures(List<? extends List<?>> temps)
    {   synchronized (STRUCTURE_TEMPERATURES)
        {   STRUCTURE_TEMPERATURES.set(temps);
        }
    }

    public static synchronized void setStructureTempOffsets(List<? extends List<?>> offsets)
    {   synchronized (STRUCTURE_TEMP_OFFSETS)
        {   STRUCTURE_TEMP_OFFSETS.set(offsets);
        }
    }

    public static synchronized void setBlockTemps(List<? extends List<?>> temps)
    {   synchronized (BLOCK_TEMPERATURES)
        {   BLOCK_TEMPERATURES.set(temps);
        }
    }

    public static synchronized void setBlockRange(int range)
    {   synchronized (MAX_BLOCK_TEMP_RANGE)
        {   MAX_BLOCK_TEMP_RANGE.set(range);
        }
    }

    public static synchronized void setSourceSpreadWhitelist(List<ResourceLocation> whitelist)
    {   synchronized (SOURCE_SPREAD_WHITELIST)
        {   SOURCE_SPREAD_WHITELIST.set(whitelist.stream().map(ResourceLocation::toString).collect(Collectors.toList()));
        }
    }

    public static synchronized void setSourceSpreadBlacklist(List<ResourceLocation> blacklist)
    {   synchronized (SOURCE_SPREAD_BLACKLIST)
        {   SOURCE_SPREAD_BLACKLIST.set(blacklist.stream().map(ResourceLocation::toString).collect(Collectors.toList()));
        }
    }
}
