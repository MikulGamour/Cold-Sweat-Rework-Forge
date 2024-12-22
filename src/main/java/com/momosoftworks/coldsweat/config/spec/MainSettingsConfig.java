package com.momosoftworks.coldsweat.config.spec;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class MainSettingsConfig
{
    private static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<Integer> DIFFICULTY;
    public static final ForgeConfigSpec.ConfigValue<String> VERSION;

    public static final ForgeConfigSpec.ConfigValue<Double> MAX_HABITABLE_TEMPERATURE;
    public static final ForgeConfigSpec.ConfigValue<Double> MIN_HABITABLE_TEMPERATURE;
    public static final ForgeConfigSpec.ConfigValue<Double> TEMP_RATE_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<Double> TEMP_DAMAGE;

    public static final ForgeConfigSpec.ConfigValue<Boolean> FIRE_RESISTANCE_BLOCKS_OVERHEATING;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ICE_RESISTANCE_BLOCKS_FREEZING;

    public static final ForgeConfigSpec.ConfigValue<Boolean> NULLIFY_IN_PEACEFUL;
    public static final ForgeConfigSpec.ConfigValue<Boolean> REQUIRE_THERMOMETER;

    public static final ForgeConfigSpec.ConfigValue<Integer> GRACE_PERIOD_LENGTH;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_GRACE_PERIOD;

    public static final ForgeConfigSpec.ConfigValue<Double> HEATSTROKE_FOG;
    public static final ForgeConfigSpec.ConfigValue<Double> FREEZING_HEARTS;
    public static final ForgeConfigSpec.ConfigValue<Double> COLD_KNOCKBACK;
    public static final ForgeConfigSpec.ConfigValue<Double> COLD_MINING;
    public static final ForgeConfigSpec.ConfigValue<Double> COLD_MOVEMENT;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_TEMP_MODIFIERS;
    public static final ForgeConfigSpec.DoubleValue MODIFIER_TICK_RATE;

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
                .defineListAllowEmpty(List.of("Disabled Temperature Modifiers"), () -> List.of(), o -> o instanceof String);

        MODIFIER_TICK_RATE = BUILDER
                .comment("Changes the update rate for temperature modifiers on entities",
                         "Temperature modifiers control most of Cold Sweat's behavior, so lowering this value will improve performance at the cost of responsiveness")
                .defineInRange("Modifier Tick Rate", 1.0, 0.1, 1.0);

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

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "coldsweat/main.toml");
    }

    public static void save()
    {   SPEC.save();
    }
}
