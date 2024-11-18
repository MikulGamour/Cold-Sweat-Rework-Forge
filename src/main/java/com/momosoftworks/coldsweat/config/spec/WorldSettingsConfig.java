package com.momosoftworks.coldsweat.config.spec;

import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.serialization.ListBuilder;
import net.minecraft.util.ResourceLocation;
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
            .defineListAllowEmpty(Arrays.asList("Dimension Temperature Offsets"), () -> Arrays.asList(
                    Arrays.asList("minecraft:the_nether", 1.0),
                    Arrays.asList("minecraft:the_end", -0.1)
            ), it ->
            {
                if (!(it instanceof List<?>)) return false;
                List<?> list = (List<?>) it;
                return list.get(0) instanceof String
                    && list.get(1) instanceof Number
                    && (list.size() < 3 || list.get(2) instanceof String);
            });

        DIMENSION_TEMPERATURES = BUILDER
            .comment("Defines the temperature of a dimension, overriding all other biome and dimension temperatures/settings")
            .defineListAllowEmpty(Arrays.asList("Dimension Temperatures"), () -> Arrays.asList(
                    // No default values
            ), it ->
            {
                if (!(it instanceof List<?>)) return false;
                List<?> list = (List<?>) it;
                return list.get(0) instanceof String
                    && list.get(1) instanceof Number
                    && (list.size() < 3 || ((List<?>) it).get(2) instanceof String);
            });

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
            .defineListAllowEmpty(Arrays.asList("Biome Temperature Offsets"), () -> Arrays.asList(),
                it ->
                {
                    if (!(it instanceof List<?>)) return false;
                    List<?> list = (List<?>) it;
                    return list.get(0) instanceof String
                        && list.get(1) instanceof Number
                        && (list.size() < 3 || list.get(2) instanceof Number)
                        && (list.size() < 4 || list.get(3) instanceof String);
                });


        BIOME_TEMPERATURES = BUILDER
            .comment("Defines the temperature of a biome, overriding the biome's default temperature")
            .defineListAllowEmpty(Arrays.asList("Biome Temperatures"), () -> ListBuilder.begin(
                            Arrays.asList("minecraft:soul_sand_valley", 53, 53, "F"),
                            Arrays.asList("minecraft:tall_birch_forest", 58, 72, "F"),
                            Arrays.asList("minecraft:tall_birch_hills", 58, 72, "F"),
                            Arrays.asList("minecraft:river", 60, 70, "F"),
                            Arrays.asList("minecraft:swamp", 72, 84, "F"),
                            Arrays.asList("minecraft:savanna", 70, 95, "F"),
                            Arrays.asList("minecraft:savanna_plateau", 76, 98, "F"),
                            Arrays.asList("minecraft:shattered_savanna", 67, 90, "F"),
                            Arrays.asList("minecraft:shattered_savanna_plateau", 67, 90, "F"),
                            Arrays.asList("minecraft:taiga", 44, 62, "F"),
                            Arrays.asList("minecraft:snowy_taiga", 19, 48, "F"),
                            Arrays.asList("minecraft:old_growth_pine_taiga", 48, 62, "F"),
                            Arrays.asList("minecraft:old_growth_spruce_taiga", 48, 62, "F"),
                            Arrays.asList("minecraft:desert", 48, 115, "F"),
                            Arrays.asList("minecraft:stone_shore", 50, 64, "F"),
                            Arrays.asList("minecraft:snowy_beach", 38, 52, "F"),
                            Arrays.asList("minecraft:snowy_slopes", 24, 38, "F"),
                            Arrays.asList("minecraft:windswept_forest", 48, 66, "F"),
                            Arrays.asList("minecraft:frozen_peaks", 15, 33, "F"),
                            Arrays.asList("minecraft:warm_ocean", 67, 76, "F"),
                            Arrays.asList("minecraft:deep_frozen_ocean", 56, 65, "F"),
                            Arrays.asList("minecraft:jungle", 76, 87, "F"),
                            Arrays.asList("minecraft:bamboo_jungle", 76, 87, "F"),
                            Arrays.asList("minecraft:badlands", 84, 120, "F"),
                            Arrays.asList("minecraft:wooded_badlands_plateau", 80, 108, "F"),
                            Arrays.asList("minecraft:eroded_badlands", 88, 120, "F"))
                     .addIf(CompatManager.isBiomesOPlentyLoaded(),
                            () -> Arrays.asList("biomesoplenty:bayou", 67, 78, "F"),
                            () -> Arrays.asList("biomesoplenty:fir_clearing", 56, 68, "F"),
                            () -> Arrays.asList("biomesoplenty:marsh", 76, 87, "F"),
                            () -> Arrays.asList("biomesoplenty:grassland_clover_patch", 56, 78, "F"),
                            () -> Arrays.asList("biomesoplenty:grassland", 56, 78, "F"),
                            () -> Arrays.asList("biomesoplenty:wetland", 63, 74, "F"),
                            () -> Arrays.asList("biomesoplenty:ominous_woods", 65, 72, "F"),
                            () -> Arrays.asList("biomesoplenty:coniferous_forest", 44, 58, "F"),
                            () -> Arrays.asList("biomesoplenty:seasonal_forest", 52, 64, "F"),
                            () -> Arrays.asList("biomesoplenty:woodland", 67, 80, "F"),
                            () -> Arrays.asList("biomesoplenty:mediterranean_forest", 64, 78, "F"),
                            () -> Arrays.asList("biomesoplenty:dune_beach", 67, 78, "F"),
                            () -> Arrays.asList("biomesoplenty:rainforest_cliffs", 73, 86, "F"),
                            () -> Arrays.asList("biomesoplenty:fungal_jungle", 73, 86, "F"),
                            () -> Arrays.asList("biomesoplenty:highland", 57, 70, "F"),
                            () -> Arrays.asList("biomesoplenty:highland_moor", 54, 68, "F"),
                            () -> Arrays.asList("biomesoplenty:grassland", 58, 82, "F"),
                            () -> Arrays.asList("biomesoplenty:meadow", 56, 78, "F"),
                            () -> Arrays.asList("biomesoplenty:meadow_forest", 56, 78, "F"),
                            () -> Arrays.asList("biomesoplenty:jade_cliffs", 57, 70, "F"),
                            () -> Arrays.asList("biomesoplenty:lush_desert", 72, 94, "F"),
                            () -> Arrays.asList("biomesoplenty:dryland", 67, 97, "F"),
                            () -> Arrays.asList("biomesoplenty:mystic_grove", 65, 72, "F"),
                            () -> Arrays.asList("biomesoplenty:orchard", 58, 78, "F"),
                            () -> Arrays.asList("biomesoplenty:prairie", 66, 82, "F"),
                            () -> Arrays.asList("biomesoplenty:origin_valley", 65, 80, "F"),
                            () -> Arrays.asList("biomesoplenty:snowy_coniferous_forest", 28, 48, "F"),
                            () -> Arrays.asList("biomesoplenty:snowy_fir_clearing", 32, 51, "F"),
                            () -> Arrays.asList("biomesoplenty:snowy_maple_forest", 32, 48, "F"),
                            () -> Arrays.asList("biomesoplenty:volcanic_plains", 82, 95, "F"),
                            () -> Arrays.asList("biomesoplenty:volcano", 94, 120, "F"))
                    .addIf(CompatManager.isBiomesYoullGoLoaded(),
                            () -> Arrays.asList("byg:coniferous_forest", 52, 70, "F"),
                            () -> Arrays.asList("byg:autumnal_valley", 58, 67, "F"),
                            () -> Arrays.asList("byg:seasonal_forest", 60, 75, "F"),
                            () -> Arrays.asList("byg:seasonal_taiga", 56, 68, "F"),
                            () -> Arrays.asList("byg:baobab_savanna", 70, 95, "F"),
                            () -> Arrays.asList("byg:dover_mountains", 40, 65, "F"),
                            () -> Arrays.asList("byg:cypress_swamplands", 68, 82, "F"),
                            () -> Arrays.asList("byg:dead_sea", 72, 82, "F"),
                            () -> Arrays.asList("byg:stone_forest", 43, 64, "F"),
                            () -> Arrays.asList("byg:snowy_coniferous_forest", 8, 31, "F"),
                            () -> Arrays.asList("byg:snowy_coniferous_forest_hills", 8, 31, "F"),
                            () -> Arrays.asList("byg:maple_taiga", 53, 71, "F"),
                            () -> Arrays.asList("byg:skyris_steeps", 42, 68, "F"),
                            () -> Arrays.asList("byg:skyris_peaks", 42, 68, "F"),
                            () -> Arrays.asList("byg:skyris_highlands", 42, 68, "F"),
                            () -> Arrays.asList("byg:skyris_highlands_clearing", 42, 68, "F"),
                            () -> Arrays.asList("byg:weeping_witch_forest", 56, 73, "F"),
                            () -> Arrays.asList("byg:subzero_hypogeal", -10, -10, "F"),
                            () -> Arrays.asList("byg:zelkova_forest", 44, 61, "F"))
                    .addIf(CompatManager.isAtmosphericLoaded(),
                            () -> Arrays.asList("atmospheric:dunes", 78, 115, "F"),
                            () -> Arrays.asList("atmospheric:dunes_hills", 78, 115, "F"),
                            () -> Arrays.asList("atmospheric:flourishing_dunes", 68, 105, "F"),
                            () -> Arrays.asList("atmospheric:petrified_dunes", 58, 120, "F"),
                            () -> Arrays.asList("atmospheric:rocky_dunes", 55, 125, "F"),
                            () -> Arrays.asList("atmospheric:rainforest", 68, 90, "F"),
                            () -> Arrays.asList("atmospheric:rainforest_mountains", 68, 90, "F"),
                            () -> Arrays.asList("atmospheric:rainforest_plateau", 68, 90, "F"),
                            () -> Arrays.asList("atmospheric:rainforest_mountains", 68, 90, "F"),
                            () -> Arrays.asList("atmospheric:rainforest_basin", 68, 90, "F"),
                            () -> Arrays.asList("atmospheric:sparse_rainforest_plateau", 62, 83, "F"),
                            () -> Arrays.asList("atmospheric:sparse_rainforest_basin", 62, 83, "F"))
                    .addIf(CompatManager.isEnvironmentalLoaded(),
                            () -> Arrays.asList("environmental:marsh", 60, 80, "F")
                    ).build(),
                it ->
                {
                    if (!(it instanceof List<?>)) return false;
                    List<?> list = (List<?>) it;
                    return list.get(0) instanceof String
                        && list.get(1) instanceof Number
                        && list.get(2) instanceof Number
                        && (list.size() < 4 || list.get(3) instanceof String);
                });

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
                .defineListAllowEmpty(Arrays.asList("Block Temperatures"), () -> ListBuilder.<List<Object>>begin(
                                            Arrays.asList("cold_sweat:boiler",         0.27, 7, 0.88, "lit=true", "", 4),
                                            Arrays.asList("cold_sweat:icebox",        -0.27, 7, 0.88, "frosted=true", "", 0),
                                            Arrays.asList("minecraft:fire",           0.476, 7, 0.9, "", "", 8),
                                            Arrays.asList("#minecraft:campfires",     0.476, 7, 0.9, "lit=true", " ", 8),
                                            Arrays.asList("minecraft:magma_block",     0.25, 3, 1.0),
                                            Arrays.asList("minecraft:ice",            -0.15, 4, 0.6, "", "", -0.7),
                                            Arrays.asList("minecraft:packed_ice",     -0.25, 4, 1.0, "", "", -0.7),
                                            Arrays.asList("minecraft:blue_ice",       -0.35, 4, 1.4, "", "", -0.7),
                                            Arrays.asList("#minecraft:ice",           -0.15, 4, 0.6, "", "", -0.7)
                                            ).addIf(CompatManager.isCavesAndCliffsLoaded(),
                                                    () -> Arrays.asList("cavesandcliffs:lava_cauldron",    0.5, 7, 1.5)
                                            ).build(),
                            it -> {
                                    if (!(it instanceof List<?>)) return false;
                                    List<?> list = (List<?>) it;
                                    return list.size() >= 3
                                    && list.get(0) instanceof String
                                    && list.get(1) instanceof Number
                                    && list.get(2) instanceof Number
                                    && (list.size() < 4 || list.get(3) instanceof Number)
                                    && (list.size() < 5 || list.get(4) instanceof String)
                                    && (list.size() < 6 || list.get(5) instanceof String)
                                    && (list.size() < 7 || list.get(6) instanceof Number);
                            });

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
                .defineListAllowEmpty(Arrays.asList("Structure Temperatures"), () -> Arrays.asList(
                        Arrays.asList("minecraft:igloo", 65, "F")
                ), it ->
                {
                    if (!(it instanceof List<?>)) return false;
                    List<?> list = (List<?>) it;
                    return list.get(0) instanceof String
                        && list.get(1) instanceof Number
                        && (list.size() < 3 || list.get(2) instanceof String);
                });

        STRUCTURE_TEMP_OFFSETS = BUILDER
                .comment("Offsets the world temperature when the player is within this structure",
                         "Format: [[\"structure_1\", offset1, *units], [\"structure_2\", offset2, *units]... etc]",
                         "(* = optional)")
                .defineListAllowEmpty(Arrays.asList("Structure Temperature Offsets"), () -> Arrays.asList(
                        // empty
                ),
                it ->
                {
                    if (!(it instanceof List<?>)) return false;
                    List<?> list = ((List<?>) it);
                    return list.get(0) instanceof String
                        && list.get(1) instanceof Number
                        && list.size() < 3 || list.get(2) instanceof String;
                });

        SLEEPING_OVERRIDE_BLOCKS = BUILDER
                .comment("List of blocks that will allow the player to sleep on them, regardless of the \"Prevent Sleep When in Danger\" setting",
                         "Use this list if the player is not getting the temperature effect from sleeping on particular blocks")
                .defineListAllowEmpty(Arrays.asList("Sleep Check Override Blocks"), () -> ListBuilder.<String>begin()
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
                .defineListAllowEmpty(Arrays.asList("Thermal Source Spread Whitelist"), () -> ListBuilder.begin(
                                              "minecraft:iron_bars",
                                              "#minecraft:leaves")
                                          .addIf(CompatManager.isCreateLoaded(),
                                              () -> "create:encased_fluid_pipe")
                                          .build(),
                                      o -> o instanceof String);

        SOURCE_SPREAD_BLACKLIST = BUILDER
                .comment("List of additional blocks that thermal sources spread through",
                         "Use this list if thermal sources are spreading through particular blocks that they shouldn't")
                .defineListAllowEmpty(Arrays.asList("Thermal Source Spread Blacklist"), () -> Arrays.asList(
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

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "coldsweat/world.toml");
    }

    public static void save()
    {   SPEC.save();
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
