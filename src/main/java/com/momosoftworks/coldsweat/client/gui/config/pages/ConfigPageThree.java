package com.momosoftworks.coldsweat.client.gui.config.pages;

import com.momosoftworks.coldsweat.client.gui.config.AbstractConfigPage;
import com.momosoftworks.coldsweat.client.gui.config.ConfigScreen;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public class ConfigPageThree extends AbstractConfigPage
{
    public ConfigPageThree(Screen parentScreen)
    {   super(parentScreen);
    }

    @Override
    public Component sectionOneTitle()
    {   return Component.translatable("cold_sweat.config.section.other");
    }

    @Nullable
    @Override
    public Component sectionTwoTitle()
    {   return Component.translatable("cold_sweat.config.section.difficulty");
    }

    @Override
    protected void init()
    {
        super.init();

        // Check sleep conditions
        this.addButton("check_sleep_conditions", Side.LEFT,
                       () -> getToggleButtonText(Component.translatable("cold_sweat.config.check_sleep_conditions.name"), ConfigSettings.CHECK_SLEEP_CONDITIONS.get()),
                       button -> ConfigSettings.CHECK_SLEEP_CONDITIONS.set(!ConfigSettings.CHECK_SLEEP_CONDITIONS.get()),
                       true, false, false, Component.translatable("cold_sweat.config.check_sleep_conditions.desc"));

        // Enable Grace Period
        this.addButton("grace_toggle", Side.LEFT,
                       () -> getToggleButtonText(Component.translatable("cold_sweat.config.grace_period.name"), ConfigSettings.GRACE_ENABLED.get()),
                       button -> ConfigSettings.GRACE_ENABLED.set(!ConfigSettings.GRACE_ENABLED.get()),
                       true, false, false, Component.translatable("cold_sweat.config.grace_period.desc"));

        // Grace Period Length
        this.addDecimalInput("grace_length", Side.LEFT, Component.translatable("cold_sweat.config.grace_period_length.name"),
                             value -> ConfigSettings.GRACE_LENGTH.set(value.intValue()),
                             input -> input.setValue(ConfigSettings.GRACE_LENGTH.get() + ""),
                             true, false, false, Component.translatable("cold_sweat.config.grace_period_length.desc_1"),
                             Component.translatable("cold_sweat.config.grace_period_length.desc_2").withStyle(ChatFormatting.DARK_GRAY));

        // Insulation Strength
        this.addDecimalInput("insulation_strength", Side.LEFT, Component.translatable("cold_sweat.config.insulation_strength.name"),
                             value -> ConfigSettings.INSULATION_STRENGTH.set(value),
                             input -> input.setValue(ConfigSettings.INSULATION_STRENGTH.get() + ""),
                             true, false, false,
                             Component.translatable("cold_sweat.config.insulation_strength.desc"));

        // Modifier Tick Rate
        this.addSliderButton("modifier_tick_rate", Side.LEFT,
                             () -> getSliderPercentageText(Component.translatable("cold_sweat.config.modifier_tick_rate.name"), ConfigSettings.MODIFIER_TICK_RATE.get(), 10),
                             0.1, 1,
                             (value, button) -> ConfigSettings.MODIFIER_TICK_RATE.set(value),
                             (button) -> button.setValue(CSMath.blend(0, 1, ConfigSettings.MODIFIER_TICK_RATE.get(), 0.1, 1)),
                             true, false,
                             Component.translatable("cold_sweat.config.modifier_tick_rate.desc"));

        // Freezing Hearts Percentage
        this.addSliderButton("freezing_hearts", Side.RIGHT,
                             () -> getSliderPercentageText(Component.translatable("cold_sweat.config.cold_freezing_hearts.name"), ConfigSettings.HEARTS_FREEZING_PERCENTAGE.get(), 0),
                             0, 1,
                             (value, button) -> ConfigSettings.HEARTS_FREEZING_PERCENTAGE.set(value),
                             (button) -> button.setValue(ConfigSettings.HEARTS_FREEZING_PERCENTAGE.get()),
                             true, false,
                             Component.translatable("cold_sweat.config.cold_freezing_hearts.desc"));

        // Cold Mining Speed
        this.addSliderButton("cold_mining_speed", Side.RIGHT,
                             () -> getSliderPercentageText(Component.translatable("cold_sweat.config.cold_mining_impairment.name"), ConfigSettings.COLD_MINING_IMPAIRMENT.get(), 0),
                             0, 1,
                             (value, button) -> ConfigSettings.COLD_MINING_IMPAIRMENT.set(value),
                             (button) -> button.setValue(ConfigSettings.COLD_MINING_IMPAIRMENT.get()),
                             true, false,
                             Component.translatable("cold_sweat.config.cold_mining_impairment.desc"));

        // Cold Movement Speed
        this.addSliderButton("cold_movement_speed", Side.RIGHT,
                             () -> getSliderPercentageText(Component.translatable("cold_sweat.config.cold_movement_slowdown.name"), ConfigSettings.COLD_MOVEMENT_SLOWDOWN.get(), 0),
                             0, 1,
                             (value, button) -> ConfigSettings.COLD_MOVEMENT_SLOWDOWN.set(value),
                             (button) -> button.setValue(ConfigSettings.COLD_MOVEMENT_SLOWDOWN.get()),
                             true, false,
                             Component.translatable("cold_sweat.config.cold_movement_slowdown.desc"));

        // Cold Knockback Reduction
        this.addSliderButton("cold_knockback_reduction", Side.RIGHT,
                             () -> getSliderPercentageText(Component.translatable("cold_sweat.config.cold_knockback_reduction.name"), ConfigSettings.COLD_KNOCKBACK_REDUCTION.get(), 0),
                             0, 1,
                             (value, button) -> ConfigSettings.COLD_KNOCKBACK_REDUCTION.set(value),
                             (button) -> button.setValue(ConfigSettings.COLD_KNOCKBACK_REDUCTION.get()),
                             true, false,
                             Component.translatable("cold_sweat.config.cold_knockback_reduction.desc"));

        // Heat Fog Distance
        this.addSliderButton("heat_fog_distance", Side.RIGHT,
                             () -> getSliderText(Component.translatable("cold_sweat.config.heat_fog_distance.name"), ConfigSettings.HEATSTROKE_FOG_DISTANCE.get().intValue(), 0, 64, 64),
                             0, 1,
                             (value, button) -> ConfigSettings.HEATSTROKE_FOG_DISTANCE.set(value * 64),
                             (button) -> button.setValue(ConfigSettings.HEATSTROKE_FOG_DISTANCE.get() / 64),
                             true, false,
                             Component.translatable("cold_sweat.config.heat_fog_distance.desc"));
    }

    @Override
    public void onClose()
    {   super.onClose();
        ConfigScreen.saveConfig();
    }
}
