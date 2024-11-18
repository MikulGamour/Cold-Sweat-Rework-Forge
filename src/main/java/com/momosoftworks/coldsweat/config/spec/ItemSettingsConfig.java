package com.momosoftworks.coldsweat.config.spec;

import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.ListBuilder;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class ItemSettingsConfig
{
    private static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> BOILER_FUELS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> ICEBOX_FUELS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> HEARTH_FUELS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> HEARTH_POTION_BLACKLIST;
    public static final ForgeConfigSpec.BooleanValue ALLOW_POTIONS_IN_HEARTH;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> SOULSPRING_LAMP_FUELS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SOULSPRING_LAMP_DIMENSIONS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> FOOD_TEMPERATURES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> CARRIED_ITEM_TEMPERATURES;

    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> INSULATION_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> INSULATION_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> INSULATING_ARMOR;
    public static final ForgeConfigSpec.ConfigValue<List<?>> INSULATION_SLOTS;
    public static final ForgeConfigSpec.DoubleValue INSULATION_STRENGTH;

    public static final ForgeConfigSpec.IntValue WATERSKIN_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue SOULSPRING_LAMP_STRENGTH;

    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> INSULATING_CURIOS;

    private static final ItemSettingsConfig INSTANCE = new ItemSettingsConfig();

    static
    {
        /*
          Fuel Items
         */
        BUILDER.push("Fuel Items")
                .comment("Defines items that can be used as fuel",
                         "Format: [[\"item-id-1\", amount-1], [\"item-id-2\", amount-2], ...etc]");
        BOILER_FUELS = BUILDER
                .defineListAllowEmpty(List.of("Boiler"), () -> ListBuilder.begin(
                                List.of("#minecraft:planks",         10),
                                List.of("#minecraft:coals",          37),
                                List.of("#minecraft:logs_that_burn", 37),
                                List.of("minecraft:coal_block",      333),
                                List.of("minecraft:magma_block",     333),
                                List.of("minecraft:lava_bucket",     1000)
                        ).build(),
                        it -> it instanceof List<?> list && list.size() == 2 && list.get(0) instanceof String && list.get(1) instanceof Number);

        ICEBOX_FUELS = BUILDER
                .defineListAllowEmpty(List.of("Icebox"), () -> ListBuilder.begin(
                                List.of("minecraft:snowball",           10),
                                List.of("minecraft:clay_ball",          37),
                                List.of("minecraft:snow_block",         100),
                                List.of("minecraft:ice",                250),
                                List.of("minecraft:clay",               333),
                                List.of("minecraft:powder_snow_bucket", 100),
                                //List.of("minecraft:water_bucket",       1000),
                                List.of("minecraft:packed_ice",         1000)
                        ).build(),
                        it -> it instanceof List<?> list && list.size() == 2 && list.get(0) instanceof String && list.get(1) instanceof Number);

        HEARTH_FUELS = BUILDER
                .comment("Negative values indicate cold fuel")
                .defineListAllowEmpty(List.of("Hearth"), () -> ListBuilder.begin(
                                // Hot
                                List.of("#minecraft:planks",         10),
                                List.of("#minecraft:coals",          37),
                                List.of("#minecraft:logs_that_burn", 37),
                                List.of("minecraft:coal_block",      333),
                                List.of("minecraft:magma_block",     333),
                                List.of("minecraft:lava_bucket",     1000),
                                // Cold
                                List.of("minecraft:snowball",           -10),
                                List.of("minecraft:clay_ball",          -37),
                                List.of("minecraft:snow_block",         -100),
                                List.of("minecraft:ice",                -250),
                                List.of("minecraft:clay",               -333),
                                List.of("minecraft:powder_snow_bucket", -100),
                                List.of("minecraft:packed_ice",         -1000)
                        ).build(),
                        it -> it instanceof List<?> list && list.size() == 2 && list.get(0) instanceof String && list.get(1) instanceof Number);
        HEARTH_POTION_BLACKLIST = BUILDER
                .comment("Potions containing any of these effects will not be allowed in the hearth",
                         "Format: [\"effect_id\", \"effect_id\", ...etc]")
                .defineListAllowEmpty(List.of("Blacklisted Hearth Potions"), () -> ListBuilder.begin(
                                "minecraft:instant_damage",
                                "minecraft:poison",
                                "minecraft:wither",
                                "minecraft:weakness",
                                "minecraft:mining_fatigue",
                                "minecraft:slowness"
                        ).build(),
                        it -> it instanceof String);
        ALLOW_POTIONS_IN_HEARTH = BUILDER
                .comment("If true, potions can be used as fuel in the hearth",
                         "This gives all players in range the potion effect")
                .define("Allow Potions in Hearth", true);
        BUILDER.pop();

        /*
          Soulspring Lamp Items
         */
        BUILDER.push("Soulspring Lamp");
        SOULSPRING_LAMP_FUELS = BUILDER
                .comment("Defines items that the Soulspring Lamp can use as fuel",
                        "Format: [[\"item-id-1\", amount-1], [\"item-id-2\", amount-2], ...etc]")
                .defineListAllowEmpty(List.of("Fuel Items"), () -> ListBuilder.<List<?>>begin(
                                    List.of("cold_sweat:soul_sprout", 4)
                        ).build(),
                        it -> it instanceof List<?> list && list.size() == 2 && list.get(0) instanceof String && list.get(1) instanceof Number);

        SOULSPRING_LAMP_DIMENSIONS = BUILDER
                .comment("Defines the dimensions that the Soulspring Lamp can be used in",
                        "Format: [\"dimension-id-1\", \"dimension-id-2\", ...etc]")
                .defineListAllowEmpty(List.of("Valid Dimensions"), () -> ListBuilder.begin(
                                "minecraft:the_nether"
                        ).build(),
                        it -> it instanceof String);
        BUILDER.pop();

        /*
         Insulation
         */
        BUILDER.push("Insulation");
        INSULATION_ITEMS = BUILDER
                .comment("Defines the items that can be used for insulating armor in the Sewing Table",
                         "Format: [[\"item_id\", cold, hot, \"static\", *nbt], [\"item_id\", amount, adapt-speed, \"adaptive\", *nbt], ...etc]",
                         "\"item_id\": The item's ID (i.e. \"minecraft:iron_ingot\"). Accepts tags with \"#\" (i.e. \"#minecraft:wool\").",
                         "",
                         "Adaptive Insulation: ",
                         "\"amount\": The amount of insulation the item provides.",
                         "\"adapt-speed\": The speed at which the insulation adapts to the environment.",
                         "*\"type\": Optional. Either \"static\" or \"adaptive\". Defines the insulation type. Defaults to static.",
                         "*\"nbt\": Optional. If set, the item will only provide insulation if it has the specified NBT tag.",
                         "",
                         "Static Insulation: ",
                         "\"cold\": The amount of cold insulation the item provides.",
                         "\"hot\": The amount of heat insulation the item provides.",
                         "*\"type\": Optional. Either \"static\" or \"adaptive\". Defines the insulation type. Defaults to static.",
                         "*\"nbt\": Optional. If set, the item will only provide insulation if it has the specified NBT tag."
                )
                .defineListAllowEmpty(List.of("Insulation Ingredients"), () -> ListBuilder.begin(
                                List.of("minecraft:leather_helmet",     4,  4),
                                List.of("minecraft:leather_chestplate", 6,  6),
                                List.of("minecraft:leather_leggings",   5,  5),
                                List.of("minecraft:leather_boots",      4,  4),
                                List.of("minecraft:leather",            1,  1),
                                List.of("cold_sweat:chameleon_molt",    2, 0.0085, "adaptive"),
                                List.of("cold_sweat:hoglin_hide",       0,  2),
                                List.of("cold_sweat:goat_fur",          2,  0),
                                List.of("#minecraft:wool",              1.5, 0),
                                List.of("minecraft:rabbit_hide",        0,  1.5),
                                List.of("cold_sweat:hoglin_headpiece",  0,  8),
                                List.of("cold_sweat:hoglin_tunic",      0,  12),
                                List.of("cold_sweat:hoglin_trousers",   0,  10),
                                List.of("cold_sweat:hoglin_hooves",     0,  8),
                                List.of("cold_sweat:goat_fur_cap",      8,  0),
                                List.of("cold_sweat:goat_fur_parka",    12, 0),
                                List.of("cold_sweat:goat_fur_pants",    10, 0),
                                List.of("cold_sweat:goat_fur_boots",    8,  0))
                            .addIf(CompatManager.isEnvironmentalLoaded(),
                                () -> List.of("environmental:yak_hair", 1.5, -1)
                        ).build(),
                        it -> it instanceof List<?> list && list.size() >= 3
                                && list.get(0) instanceof String
                                && list.get(1) instanceof Number
                                && list.get(2) instanceof Number
                                && (list.size() < 4 || list.get(3) instanceof String)
                                && (list.size() < 5 || list.get(4) instanceof String));

        INSULATING_ARMOR = BUILDER
                .comment("Defines the items that provide insulation when worn",
                        "See Insulation Ingredients for formatting")
                .defineListAllowEmpty(List.of("Insulating Armor"), () -> ListBuilder.begin(
                                List.of("minecraft:leather_helmet",      4,  4),
                                List.of("minecraft:leather_chestplate",  6,  6),
                                List.of("minecraft:leather_leggings",    5,  5),
                                List.of("minecraft:leather_boots",       4,  4),
                                List.of("cold_sweat:hoglin_headpiece",   0,  8),
                                List.of("cold_sweat:hoglin_tunic",       0,  12),
                                List.of("cold_sweat:hoglin_trousers",    0,  10),
                                List.of("cold_sweat:hoglin_hooves",      0,  8),
                                List.of("cold_sweat:goat_fur_cap",       8,  0),
                                List.of("cold_sweat:goat_fur_parka",     12, 0),
                                List.of("cold_sweat:goat_fur_pants",     10, 0),
                                List.of("cold_sweat:goat_fur_boots",     8,  0))
                            .addIf(CompatManager.isEnvironmentalLoaded(),
                                () -> List.of("environmental:yak_pants", 7.5, -5)
                        ).build(),
                        it -> it instanceof List<?> list && list.size() >= 3
                                && list.get(0) instanceof String
                                && list.get(1) instanceof Number
                                && list.get(2) instanceof Number
                                && (list.size() < 4 || list.get(3) instanceof String)
                                && (list.size() < 5 || list.get(4) instanceof String));

        if (CompatManager.isCuriosLoaded())
        {
            INSULATING_CURIOS = BUILDER
                    .comment("Defines the items that provide insulation when worn in a curio slot",
                             "See Insulation Ingredients for formatting")
                    .defineListAllowEmpty(List.of("Insulating Curios"), () -> List.of(
                            // Nothing defined
                        ),
                        it -> it instanceof List<?> list && list.size() >= 3
                                && list.get(0) instanceof String
                                && list.get(1) instanceof Number
                                && list.get(2) instanceof Number
                                && (list.size() < 4 || list.get(3) instanceof String)
                                && (list.size() < 5 || list.get(4) instanceof String));
        }
        else INSULATING_CURIOS = null;

        INSULATION_SLOTS = BUILDER
                .comment("Defines how many insulation slots armor pieces have",
                         "There are 4 modes for this setting:",
                         "Static: Each armor slot (head, body, legs, feet) has a fixed number of insulation slots",
                         "- Format: [head, body, legs, feet] (a list of integers; insulation slot count for each armor slot)",
                         " ",
                         "Linear: Number of slots increases steadily with protection",
                         "Exponential: Number of slots increases rapidly with protection",
                         "Logarithmic: Number of slots increases with protection, with diminishing returns",
                         "- Format: [number, max-slots] (a positive integer or decimal; the rate of increase)")
                .defineList("Insulation Slots", List.of("static", 4, 6, 5, 4),
                        it -> it instanceof Number || it instanceof String);

        INSULATION_STRENGTH = BUILDER
                .comment("Defines the effectiveness of insulating items in protecting against temperature")
                .defineInRange("Insulation Strength", 1.0, 0, Double.MAX_VALUE);

        INSULATION_BLACKLIST = BUILDER
                .comment("Defines wearable items that cannot be insulated",
                        "Format: [\"item_id\", \"item_id\", ...etc]")
                .defineListAllowEmpty(List.of("Insulation Blacklist"), () -> List.of(
                ),
                it -> it instanceof String);

        BUILDER.pop();

        /*
         Consumables
         */
        BUILDER.push("Consumables");

        FOOD_TEMPERATURES = BUILDER
                .comment("Defines items that affect the player's temperature when consumed",
                        "Format: [[\"item_id\", amount, *nbt, *duration], [\"item_id\", amount, *nbt, *duration], ...etc]",
                        "Negative values are cold foods, positive values are hot foods",
                        "nbt: Optional. If set, the item will only affect the player's temperature if it has the specified NBT tag.",
                        "duration: Optional. If set, the player's temperature will remain increased/decreased for this amount of time (in ticks).")
                .defineListAllowEmpty(List.of("Temperature-Affecting Foods"), () -> Arrays.asList(
                        List.of("cold_sweat:soul_sprout", -20, "{}", 1200)
                ),
                it -> it instanceof List<?> list && list.size() >= 2
                        && list.get(0) instanceof String
                        && list.get(1) instanceof Number
                        && (list.size() < 3 || list.get(2) instanceof String)
                        && (list.size() < 4 || list.get(3) instanceof Number));

        WATERSKIN_STRENGTH = BUILDER
                .comment("Defines how much a waterskin will change the player's body temperature by when used")
                .defineInRange("Waterskin Strength", 50, 0, Integer.MAX_VALUE);

        SOULSPRING_LAMP_STRENGTH = BUILDER
                .comment("Determines the strength of the Soulspring Lamp's effect before it is overwhelmed",
                         "A value of 1 means it will never be overwhelmed")
                .defineInRange("Soulspring Lamp Strength", 0.6, 0, 1);

        BUILDER.pop();

        /*
         Misc
         */
        BUILDER.push("Misc");

        CARRIED_ITEM_TEMPERATURES = BUILDER
                .comment("Defines items that affect the player's temperature when in the inventory",
                         "Format: [[\"item_id\", temperature, strict_type, trait, *nbt, *max_effect], [\"item_id\", temperature, strict_type, trait, *nbt, *max_effect], ...etc]",
                         "temperature: The temperature change the item will apply to the entity. For core temperature, this is applied every tick",
                         "strict_type: Either \"inventory\", \"hotbar\", or \"hand\". Defines what slots the item must be in to apply to the entity",
                         "trait: The temperature trait to apply the effect to. Typical values are \"core\" for body temperature or \"world\" for ambient temperature. More on the mod documentation page.",
                         "nbt: Optional. The NBT data the item must have to apply to the entity.",
                         "max_effect: Optional. The maximum temperature effect the item can apply to the entity.")
                .defineListAllowEmpty(List.of("Carried Item Temperatures"), () -> List.of(
                ),
                it -> it instanceof List<?> list && CSMath.betweenInclusive(list.size(), 4, 6)
                        && list.get(0) instanceof String
                        && list.get(1) instanceof Number
                        && list.get(2) instanceof String
                        && list.get(3) instanceof String
                        && (list.size() < 5 || list.get(4) instanceof String)
                        && (list.size() < 6 || list.get(5) instanceof Number));

        BUILDER.pop();

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

        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, SPEC, "coldsweat/item.toml");
    }

    public static void save()
    {   SPEC.save();
    }

    public static ItemSettingsConfig getInstance()
    {   return INSTANCE;
    }
}
