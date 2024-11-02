package com.momosoftworks.coldsweat.config.spec;

import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ClientSettingsConfig
{
    private static final ModConfigSpec SPEC;
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue USE_CELSIUS;
    public static final ModConfigSpec.IntValue TEMPERATURE_OFFSET;
    public static final ModConfigSpec.DoubleValue TEMPERATURE_SMOOTHING;

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> BODY_TEMP_ICON_POS;
    public static final ModConfigSpec.BooleanValue SHOW_BODY_TEMP_ICON;
    public static final ModConfigSpec.BooleanValue MOVE_BODY_TEMP_ICON_ADVANCED;

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> BODY_TEMP_READOUT_POS;
    public static final ModConfigSpec.BooleanValue SHOW_BODY_TEMP_READOUT;

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> WORLD_TEMP_GAUGE_POS;
    public static final ModConfigSpec.BooleanValue SHOW_WORLD_TEMP_GAUGE;

    public static final ModConfigSpec.BooleanValue USE_CUSTOM_HOTBAR_LAYOUT;
    public static final ModConfigSpec.BooleanValue ENABLE_ICON_BOBBING;

    public static final ModConfigSpec.BooleanValue SHOW_HEARTH_DEBUG_VISUALS;

    public static final ModConfigSpec.BooleanValue SHOW_CONFIG_BUTTON;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> CONFIG_BUTTON_POS;
    public static final ModConfigSpec.BooleanValue SHOW_SCREEN_DISTORTIONS;

    public static final ModConfigSpec.BooleanValue HIGH_CONTRAST_MODE;

    public static final ModConfigSpec.BooleanValue ENABLE_CREATIVE_WARNING;

    public static final ModConfigSpec.BooleanValue HIDE_INSULATION_TOOLTIPS;
    public static final ModConfigSpec.BooleanValue EXPAND_TOOLTIPS;

    public static final ModConfigSpec.BooleanValue SHOW_WATER_EFFECT;

    private static final ClientSettingsConfig INSTANCE = new ClientSettingsConfig();

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
            SHOW_WATER_EFFECT = BUILDER
                    .comment("Displays a dripping water effect on-screen when the player is wet")
                    .define("Show Water Effect", true);
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

    public static void setup(ModContainer modContainer)
    {
        Path configPath = FMLPaths.CONFIGDIR.get();
        Path csConfigPath = Paths.get(configPath.toAbsolutePath().toString(), "coldsweat");

        // Create the config folder
        try
        {   Files.createDirectory(csConfigPath);
        }
        catch (Exception ignored) {}

        modContainer.registerConfig(ModConfig.Type.CLIENT, SPEC, "coldsweat/client.toml");
    }

    public static ClientSettingsConfig getInstance()
    {   return INSTANCE;
    }

    /*
     * Non-private values for use elsewhere
     */

    public boolean isHighContrast()
    {   return HIGH_CONTRAST_MODE.get();
    }
    public void setHighContrast(boolean enabled)
    {   HIGH_CONTRAST_MODE.set(enabled);
    }

    public boolean isCelsius()
    {   return USE_CELSIUS.get();
    }

    public int getTempOffset()
    {   return TEMPERATURE_OFFSET.get();
    }

    public int getBodyIconX()
    {   return BODY_TEMP_ICON_POS.get().get(0);
    }
    public int getBodyIconY()
    {   return BODY_TEMP_ICON_POS.get().get(1);
    }

    public int getBodyReadoutX()
    {   return BODY_TEMP_READOUT_POS.get().get(0);
    }
    public int getBodyReadoutY()
    {   return BODY_TEMP_READOUT_POS.get().get(1);
    }

    public int getWorldGaugeX()
    {   return WORLD_TEMP_GAUGE_POS.get().get(0);
    }
    public int getWorldGaugeY()
    {   return WORLD_TEMP_GAUGE_POS.get().get(1);
    }

    public double getTempSmoothing()
    {   return TEMPERATURE_SMOOTHING.get();
    }

    public boolean isCustomHotbarLayout()
    {   return USE_CUSTOM_HOTBAR_LAYOUT.get();
    }

    public boolean isIconBobbing()
    {   return ENABLE_ICON_BOBBING.get();
    }

    public boolean isHearthDebug()
    {   return SHOW_HEARTH_DEBUG_VISUALS.get();
    }

    public boolean showCreativeWarning()
    {   return ENABLE_CREATIVE_WARNING.get();
    }

    public boolean isBodyIconEnabled()
    {   return SHOW_BODY_TEMP_ICON.get();
    }
    public boolean isBodyReadoutEnabled()
    {   return SHOW_BODY_TEMP_READOUT.get();
    }
    public boolean isWorldGaugeEnabled()
    {   return SHOW_WORLD_TEMP_GAUGE.get();
    }

    public boolean moveBodyIconWhenAdvanced()
    {   return MOVE_BODY_TEMP_ICON_ADVANCED.get();
    }

    public boolean hideTooltips()
    {   return HIDE_INSULATION_TOOLTIPS.get();
    }

    public boolean isWaterEffectEnabled()
    {   return SHOW_WATER_EFFECT.get();
    }

    /*
     * Safe set methods for config values
     */

    public void setCelsius(boolean enabled)
    {   USE_CELSIUS.set(enabled);
    }

    public void setTempOffset(int offset)
    {   TEMPERATURE_OFFSET.set(offset);
    }

    public void setBodyIconX(int pos)
    {   BODY_TEMP_ICON_POS.set(List.of(pos, getBodyIconY()));
    }
    public void setBodyIconY(int pos)
    {   BODY_TEMP_ICON_POS.set(List.of(getBodyIconX(), pos));
    }

    public void setBodyReadoutX(int pos)
    {   BODY_TEMP_READOUT_POS.set(List.of(pos, getBodyReadoutY()));
    }
    public void setBodyReadoutY(int pos)
    {   BODY_TEMP_READOUT_POS.set(List.of(getBodyReadoutX(), pos));
    }

    public void setWorldGaugeX(int pos)
    {   WORLD_TEMP_GAUGE_POS.set(List.of(pos, getWorldGaugeY()));
    }
    public void setWorldGaugeY(int pos)
    {   WORLD_TEMP_GAUGE_POS.set(List.of(getWorldGaugeX(), pos));
    }

    public void setCustomHotbar(boolean enabled)
    {   USE_CUSTOM_HOTBAR_LAYOUT.set(enabled);
    }

    public void setIconBobbing(boolean enabled)
    {   ENABLE_ICON_BOBBING.set(enabled);
    }

    public void setHearthDebug(boolean enabled)
    {   SHOW_HEARTH_DEBUG_VISUALS.set(enabled);
    }

    public boolean showConfigButton()
    {   return SHOW_CONFIG_BUTTON.get();
    }
    public List<? extends Integer> getConfigButtonPos()
    {   return CONFIG_BUTTON_POS.get();
    }
    public void setConfigButtonPos(List<Integer> pos)
    {   CONFIG_BUTTON_POS.set(pos);
    }

    public boolean areDistortionsEnabled()
    {   return SHOW_SCREEN_DISTORTIONS.get();
    }
    public void setDistortionsEnabled(boolean enabled)
    {   SHOW_SCREEN_DISTORTIONS.set(enabled);
    }

    public void setCreativeWarningEnabled(boolean enabled)
    {   ENABLE_CREATIVE_WARNING.set(enabled);
    }

    public void setBodyIconEnabled(boolean enabled)
    {   SHOW_BODY_TEMP_ICON.set(enabled);
    }
    public void setBodyReadoutEnabled(boolean enabled)
    {   SHOW_BODY_TEMP_READOUT.set(enabled);
    }
    public void setWorldGaugeEnabled(boolean enabled)
    {   SHOW_WORLD_TEMP_GAUGE.set(enabled);
    }

    public void setTempSmoothing(double smoothing)
    {   TEMPERATURE_SMOOTHING.set(smoothing);
    }

    public void setMoveBodyIconWhenAdvanced(boolean enabled)
    {   MOVE_BODY_TEMP_ICON_ADVANCED.set(enabled);
    }

    public void setHideTooltips(boolean hide)
    {   HIDE_INSULATION_TOOLTIPS.set(hide);
    }

    public void setWaterEffectEnabled(boolean enabled)
    {   SHOW_WATER_EFFECT.set(enabled);
    }

    public synchronized void writeAndSave()
    {   this.setCelsius(ConfigSettings.CELSIUS.get());
        this.setTempOffset(ConfigSettings.TEMP_OFFSET.get());
        this.setTempSmoothing(ConfigSettings.TEMP_SMOOTHING.get());
        this.setBodyIconX(ConfigSettings.BODY_ICON_POS.get().x());
        this.setBodyIconY(ConfigSettings.BODY_ICON_POS.get().y());
        this.setBodyReadoutX(ConfigSettings.BODY_READOUT_POS.get().x());
        this.setBodyReadoutY(ConfigSettings.BODY_READOUT_POS.get().y());
        this.setWorldGaugeX(ConfigSettings.WORLD_GAUGE_POS.get().x());
        this.setWorldGaugeY(ConfigSettings.WORLD_GAUGE_POS.get().y());
        this.setCustomHotbar(ConfigSettings.CUSTOM_HOTBAR_LAYOUT.get());
        this.setIconBobbing(ConfigSettings.ICON_BOBBING.get());
        this.setHearthDebug(ConfigSettings.HEARTH_DEBUG.get());
        this.setCreativeWarningEnabled(ConfigSettings.SHOW_CREATIVE_WARNING.get());
        this.setBodyIconEnabled(ConfigSettings.BODY_ICON_ENABLED.get());
        this.setBodyReadoutEnabled(ConfigSettings.BODY_READOUT_ENABLED.get());
        this.setWorldGaugeEnabled(ConfigSettings.WORLD_GAUGE_ENABLED.get());
        this.setDistortionsEnabled(ConfigSettings.DISTORTION_EFFECTS.get());
        this.setHighContrast(ConfigSettings.HIGH_CONTRAST.get());
        this.setConfigButtonPos(List.of(ConfigSettings.CONFIG_BUTTON_POS.get().x(),
                                        ConfigSettings.CONFIG_BUTTON_POS.get().y()));
        this.setMoveBodyIconWhenAdvanced(ConfigSettings.MOVE_BODY_ICON_WHEN_ADVANCED.get());
        this.setWaterEffectEnabled(ConfigSettings.SHOW_WATER_EFFECT.get());
        this.save();
    }

    public synchronized void save()
    {   SPEC.save();
    }
}
