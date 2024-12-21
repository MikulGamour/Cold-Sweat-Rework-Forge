package com.momosoftworks.coldsweat.config.spec;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class MainSettingsConfig
{
    private static final ModConfigSpec SPEC;
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<Integer> DIFFICULTY;
    public static final ModConfigSpec.ConfigValue<String> VERSION;

    public static final ModConfigSpec.ConfigValue<Double> MAX_HABITABLE_TEMPERATURE;
    public static final ModConfigSpec.ConfigValue<Double> MIN_HABITABLE_TEMPERATURE;
    public static final ModConfigSpec.ConfigValue<Double> TEMP_RATE_MULTIPLIER;
    public static final ModConfigSpec.ConfigValue<Double> TEMP_DAMAGE;

    public static final ModConfigSpec.ConfigValue<Boolean> FIRE_RESISTANCE_BLOCKS_OVERHEATING;
    public static final ModConfigSpec.ConfigValue<Boolean> ICE_RESISTANCE_BLOCKS_FREEZING;

    public static final ModConfigSpec.ConfigValue<Boolean> NULLIFY_IN_PEACEFUL;
    public static final ModConfigSpec.ConfigValue<Boolean> REQUIRE_THERMOMETER;

    public static final ModConfigSpec.ConfigValue<Integer> GRACE_PERIOD_LENGTH;
    public static final ModConfigSpec.ConfigValue<Boolean> ENABLE_GRACE_PERIOD;

    public static final ModConfigSpec.ConfigValue<Double> HEATSTROKE_FOG;
    public static final ModConfigSpec.ConfigValue<Double> FREEZING_HEARTS;
    public static final ModConfigSpec.ConfigValue<Double> COLD_KNOCKBACK;
    public static final ModConfigSpec.ConfigValue<Double> COLD_MINING;
    public static final ModConfigSpec.ConfigValue<Double> COLD_MOVEMENT;

    public static final ModConfigSpec.ConfigValue<List<? extends String>> DISABLED_TEMP_MODIFIERS;

    static 
    {
        ConfigSettings.Difficulty defaultDiff = ConfigSettings.DEFAULT_DIFFICULTY;

        BUILDER.comment("DO NOT EDIT THE SETTINGS IN THIS SECTION")
               .push("Builtin");

        DIFFICULTY = BUILDER
                .defineInRange("Difficulty", defaultDiff.ordinal(), 0, ConfigSettings.Difficulty.values().length - 1);

        VERSION = BUILDER
                .define("Version", ColdSweat.getVersion());

        BUILDER.pop();

        /*
         Details about how the player is affected by temperature
         */
        BUILDER.push("Difficulty");

        MIN_HABITABLE_TEMPERATURE = BUILDER
                .comment("Defines the minimum habitable temperature")
                .defineInRange("Minimum Habitable Temperature", defaultDiff.getOrDefault(ConfigSettings.MIN_TEMP, Temperature.convert(50, Temperature.Units.F, Temperature.Units.MC, true)),
                               Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        MAX_HABITABLE_TEMPERATURE = BUILDER
                .comment("Defines the maximum habitable temperature")
                .defineInRange("Maximum Habitable Temperature", defaultDiff.getOrDefault(ConfigSettings.MAX_TEMP, Temperature.convert(100, Temperature.Units.F, Temperature.Units.MC, true)),
                               Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        TEMP_RATE_MULTIPLIER = BUILDER
                .comment("Rate at which the player's body temperature changes (default: 1.0 (100%))")
                .defineInRange("Rate Multiplier", defaultDiff.getOrDefault(ConfigSettings.TEMP_RATE, 1d), 0d, Double.POSITIVE_INFINITY);

        TEMP_DAMAGE = BUILDER
                .comment("Damage dealt to the player when they are too hot or too cold")
                .defineInRange("Temperature Damage", defaultDiff.getOrDefault(ConfigSettings.TEMP_DAMAGE, 2d), 0d, Double.POSITIVE_INFINITY);

        NULLIFY_IN_PEACEFUL = BUILDER
                .comment("Sets whether damage scales with difficulty")
                .define("Damage Scaling", defaultDiff.getOrDefault("damage_scaling", true));

        BUILDER.pop();


        /*
         Potion effects affecting the player's temperature
         */
        BUILDER.push("Items");

        FIRE_RESISTANCE_BLOCKS_OVERHEATING = BUILDER
                .comment("Allow fire resistance to block overheating damage")
                .define("Fire Resistance Immunity", defaultDiff.getOrDefault(ConfigSettings.FIRE_RESISTANCE_ENABLED, true));

        ICE_RESISTANCE_BLOCKS_FREEZING = BUILDER
                .comment("Allow ice resistance to block freezing damage")
                .define("Ice Resistance Immunity", defaultDiff.getOrDefault(ConfigSettings.ICE_RESISTANCE_ENABLED, true));

        REQUIRE_THERMOMETER = BUILDER
            .comment("Thermometer item is required to see detailed world temperature")
            .define("Require Thermometer", defaultDiff.getOrDefault(ConfigSettings.REQUIRE_THERMOMETER, true));

        BUILDER.pop();


        /*
         Temperature effects
         */
        BUILDER.push("Temperature Effects");
            BUILDER.push("Hot");

            HEATSTROKE_FOG = BUILDER
                .comment("Defines the distance at which the player's vision is obscured by heatstroke fog",
                         "Set to a value above 64 to disable the effect")
                .defineInRange("Heatstroke Fog", defaultDiff.getOrDefault(ConfigSettings.HEATSTROKE_FOG_DISTANCE, 6.0), 0, Double.POSITIVE_INFINITY);

            BUILDER.pop();

            BUILDER.push("Cold");

            FREEZING_HEARTS = BUILDER
                .comment("Up to a certain portion of the player's hearts will freeze over when they are too cold, preventing regeneration",
                         "Represented as a percentage in decimal form")
                .defineInRange("Max Frozen Health Percentage", defaultDiff.getOrDefault(ConfigSettings.HEARTS_FREEZING_PERCENTAGE, 0.5), 0, 1);

            COLD_KNOCKBACK = BUILDER
                .comment("The player's attack knockback will be reduced by this amount when they are too cold",
                         "Represented as a percentage in decimal form")
                .defineInRange("Freezing Knockback Reduction", defaultDiff.getOrDefault(ConfigSettings.COLD_KNOCKBACK_REDUCTION, 0.5), 0, 1);

            COLD_MOVEMENT = BUILDER
                .comment("The player's movement speed will be reduced by this amount when they are too cold",
                         "Represented as a percentage in decimal form")
                .defineInRange("Freezing Sluggishness", defaultDiff.getOrDefault(ConfigSettings.COLD_MOVEMENT_SLOWDOWN, 0.5), 0, 1);

            COLD_MINING = BUILDER
                .comment("The player's mining speed will be reduced by this amount when they are too cold",
                         "Represented as a percentage in decimal form")
                .defineInRange("Freezing Mining Impairment", defaultDiff.getOrDefault(ConfigSettings.COLD_MINING_IMPAIRMENT, 0.5), 0, 1);

            BUILDER.pop();
        BUILDER.pop();


        BUILDER.push("Grace Period");

                GRACE_PERIOD_LENGTH = BUILDER
                .comment("The amount of time (in ticks) after the player spawns during which they are immune to temperature effects")
                .defineInRange("Grace Period Length", defaultDiff.getOrDefault(ConfigSettings.GRACE_LENGTH, 6000), 0, Integer.MAX_VALUE);

                ENABLE_GRACE_PERIOD = BUILDER
                .comment("Enables the grace period")
                .define("Grace Period Enabled", defaultDiff.getOrDefault(ConfigSettings.GRACE_ENABLED, true));

        BUILDER.pop();

        BUILDER.push("Misc");

        DISABLED_TEMP_MODIFIERS = BUILDER
                .comment("Add TempModifier IDs to this list to disable them",
                         "Allows for more granular control of Cold Sweat's features",
                         " Run \"/temp debug @s <trait>\" to see IDs of all modifiers affecting the player",
                         "See the Cold Sweat documentation for a list of default TempModifiers")
                .defineListAllowEmpty("Disabled Temperature Modifiers", List.of(), o -> o instanceof String);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void setup(ModContainer modContainer)
    {
        Path configPath = FMLPaths.CONFIGDIR.get();
        Path csConfigPath = Paths.get(configPath.toAbsolutePath().toString(), "coldsweat");

        // Create the config folder
        try
        {   Files.createDirectory(csConfigPath);
        }
        catch (Exception ignored) {}

        modContainer.registerConfig(ModConfig.Type.COMMON, SPEC, "coldsweat/main.toml");
    }

    public static void save()
    {   SPEC.save();
    }
}
