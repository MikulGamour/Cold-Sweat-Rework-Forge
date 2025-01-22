package com.momosoftworks.coldsweat.config.spec;

import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.joml.Vector2i;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ClientSettingsConfig
{
    private static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue USE_CELSIUS;
    public static final ForgeConfigSpec.IntValue TEMPERATURE_OFFSET;
    public static final ForgeConfigSpec.DoubleValue TEMPERATURE_SMOOTHING;

    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> BODY_TEMP_ICON_POS;
    public static final ForgeConfigSpec.BooleanValue SHOW_BODY_TEMP_ICON;
    public static final ForgeConfigSpec.BooleanValue MOVE_BODY_TEMP_ICON_ADVANCED;

    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> BODY_TEMP_READOUT_POS;
    public static final ForgeConfigSpec.BooleanValue SHOW_BODY_TEMP_READOUT;

    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> WORLD_TEMP_GAUGE_POS;
    public static final ForgeConfigSpec.BooleanValue SHOW_WORLD_TEMP_GAUGE;

    public static final ForgeConfigSpec.BooleanValue USE_CUSTOM_HOTBAR_LAYOUT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ICON_BOBBING;

    public static final ForgeConfigSpec.BooleanValue SHOW_HEARTH_DEBUG_VISUALS;

    public static final ForgeConfigSpec.BooleanValue SHOW_CONFIG_BUTTON;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> CONFIG_BUTTON_POS;
    public static final ForgeConfigSpec.BooleanValue SHOW_SCREEN_DISTORTIONS;

    public static final ForgeConfigSpec.BooleanValue HIGH_CONTRAST_MODE;

    public static final ForgeConfigSpec.BooleanValue ENABLE_CREATIVE_WARNING;

    public static final ForgeConfigSpec.BooleanValue HIDE_INSULATION_TOOLTIPS;
    public static final ForgeConfigSpec.BooleanValue EXPAND_TOOLTIPS;

    public static final ForgeConfigSpec.IntValue WATER_EFFECT_SETTING;

    static 
    {
        /*
         Temperature Display Preferences
         */
        BUILDER.push("Visual Preferences");
            USE_CELSIUS = BUILDER
                    .comment("Sets all temperatures to be displayed in Celsius")
                    .define("Celsius", false);
            TEMPERATURE_OFFSET = BUILDER
                    .comment("Visually offsets the world temperature to better match the user's definition of \"hot\" and \"cold\"")
                    .defineInRange("Temperature Offset", 0, -Integer.MAX_VALUE, Integer.MAX_VALUE);
            TEMPERATURE_SMOOTHING = BUILDER
                    .comment("The amount of smoothing applied to gauges in the UI",
                             "A value of 1 has no smoothing")
                    .defineInRange("Temperature Smoothing", 10, 1.0, Integer.MAX_VALUE);
            WATER_EFFECT_SETTING = BUILDER
                    .comment("Displays a dripping water effect on-screen and/or with particles when the player is wet",
                             "0: Off, 1: Particles, 2: On-Screen, 3: Both")
                    .defineInRange("Show Water Effect", 3, 0, 3);
        BUILDER.pop();

        BUILDER.push("UI Options");
            USE_CUSTOM_HOTBAR_LAYOUT = BUILDER
                    .define("Custom hotbar layout", true);
            ENABLE_ICON_BOBBING = BUILDER
                    .comment("Controls whether UI elements will shake when in critical conditions")
                    .define("Icon Bobbing", true);

            BODY_TEMP_ICON_POS = BUILDER
                    .comment("The position of the body temperature icon relative to default")
                    .defineList("Body Temperature Icon Offset", List.of(0, 0), it -> it instanceof Integer);
            SHOW_BODY_TEMP_ICON = BUILDER
                    .comment("Enables the body temperature icon above the hotbar")
                    .define("Show Body Temperature Icon", true);
            MOVE_BODY_TEMP_ICON_ADVANCED = BUILDER
                    .comment("Moves the body temperature icon to make way for the advanced readout when a thermometer is equipped")
                    .define("Move Body Icon For Advanced Info", true);

            BODY_TEMP_READOUT_POS = BUILDER
                    .comment("The position of the body temperature readout relative to default")
                    .defineList("Body Temperature Readout Offset", List.of(0, 0), it -> it instanceof Integer);
            SHOW_BODY_TEMP_READOUT = BUILDER
                    .comment("Enables the body temperature readout above the hotbar")
                    .define("Show Body Temperature Readout", true);

            WORLD_TEMP_GAUGE_POS = BUILDER
                    .comment("The position of the world temperature gauge relative to default")
                    .defineList("World Temperature UI Offset", List.of(0, 0), it -> it instanceof Integer);
            SHOW_WORLD_TEMP_GAUGE = BUILDER
                    .comment("Enables the world temperature gauge next to the hotbar")
                    .define("Show World Temperature Gauge", true);
        BUILDER.pop();

        BUILDER.push("Accessibility");
            SHOW_SCREEN_DISTORTIONS = BUILDER
                    .comment("Enables visual distortion effects when the player is too hot or cold")
                    .define("Distortion Effects", true);
            HIGH_CONTRAST_MODE = BUILDER
                    .comment("Enables high contrast mode for UI elements")
                    .define("High Contrast", false);
        BUILDER.pop();

        BUILDER.push("Misc");
            SHOW_CONFIG_BUTTON = BUILDER
                    .comment("Show the config menu button in the Options menu")
                    .define("Enable In-Game Config", true);
            CONFIG_BUTTON_POS = BUILDER
                    .comment("The position (offset) of the config button on the screen")
                    .defineList("Config Button Position", List.of(0, 0),
                                it -> it instanceof Integer);
            ENABLE_CREATIVE_WARNING = BUILDER
                    .comment("Warns the player about a bug that clears armor insulation when in creative mode")
                    .define("Enable Creative Mode Warning", true);
            SHOW_HEARTH_DEBUG_VISUALS = BUILDER
                    .comment("Displays areas that the Hearth is affecting when the F3 debug menu is open")
                    .define("Hearth Debug", true);
            HIDE_INSULATION_TOOLTIPS = BUILDER
                    .comment("Hides insulation tooltips for items, armor, and curios unless SHIFT is held")
                    .define("Hide Tooltips", false);
            EXPAND_TOOLTIPS = BUILDER
                    .comment("Automatically expands all collapsible tooltips")
                    .define("Expand Tooltips", false);
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

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC, "coldsweat/client.toml");
    }

    public static Vector2i getBodyIconPos()
    {   return new Vector2i(BODY_TEMP_ICON_POS.get().get(0), BODY_TEMP_ICON_POS.get().get(1));
    }

    public static Vector2i getBodyReadoutPos()
    {   return new Vector2i(BODY_TEMP_READOUT_POS.get().get(0), BODY_TEMP_READOUT_POS.get().get(1));
    }

    public static Vector2i getWorldGaugePos()
    {   return new Vector2i(WORLD_TEMP_GAUGE_POS.get().get(0), WORLD_TEMP_GAUGE_POS.get().get(1));
    }

    public static Vector2i getConfigButtonPos()
    {   return new Vector2i(CONFIG_BUTTON_POS.get().get(0), CONFIG_BUTTON_POS.get().get(1));
    }

    public static synchronized void writeAndSave()
    {
        USE_CELSIUS.set(ConfigSettings.CELSIUS.get());
        TEMPERATURE_OFFSET.set(ConfigSettings.TEMP_OFFSET.get());
        TEMPERATURE_SMOOTHING.set(ConfigSettings.TEMP_SMOOTHING.get());
        BODY_TEMP_ICON_POS.set(List.of(ConfigSettings.BODY_ICON_POS.get().x(), ConfigSettings.BODY_ICON_POS.get().y()));
        BODY_TEMP_READOUT_POS.set(List.of(ConfigSettings.BODY_READOUT_POS.get().x(), ConfigSettings.BODY_READOUT_POS.get().y()));
        WORLD_TEMP_GAUGE_POS.set(List.of(ConfigSettings.WORLD_GAUGE_POS.get().x(), ConfigSettings.WORLD_GAUGE_POS.get().y()));
        USE_CUSTOM_HOTBAR_LAYOUT.set(ConfigSettings.CUSTOM_HOTBAR_LAYOUT.get());
        ENABLE_ICON_BOBBING.set(ConfigSettings.ICON_BOBBING.get());
        SHOW_HEARTH_DEBUG_VISUALS.set(ConfigSettings.HEARTH_DEBUG.get());
        ENABLE_CREATIVE_WARNING.set(ConfigSettings.SHOW_CREATIVE_WARNING.get());
        SHOW_BODY_TEMP_ICON.set(ConfigSettings.BODY_ICON_ENABLED.get());
        SHOW_BODY_TEMP_READOUT.set(ConfigSettings.BODY_READOUT_ENABLED.get());
        SHOW_WORLD_TEMP_GAUGE.set(ConfigSettings.WORLD_GAUGE_ENABLED.get());
        SHOW_SCREEN_DISTORTIONS.set(ConfigSettings.DISTORTION_EFFECTS.get());
        HIGH_CONTRAST_MODE.set(ConfigSettings.HIGH_CONTRAST.get());
        CONFIG_BUTTON_POS.set(List.of(ConfigSettings.CONFIG_BUTTON_POS.get().x(),
                                        ConfigSettings.CONFIG_BUTTON_POS.get().y()));
        MOVE_BODY_TEMP_ICON_ADVANCED.set(ConfigSettings.MOVE_BODY_ICON_WHEN_ADVANCED.get());
        WATER_EFFECT_SETTING.set(ConfigSettings.WATER_EFFECT_SETTING.get().ordinal());
        save();
    }

    public static synchronized void save()
    {   SPEC.save();
    }
}
