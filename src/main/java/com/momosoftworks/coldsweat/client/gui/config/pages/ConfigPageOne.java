package com.momosoftworks.coldsweat.client.gui.config.pages;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.client.gui.Overlays;
import com.momosoftworks.coldsweat.client.gui.config.AbstractConfigPage;
import com.momosoftworks.coldsweat.client.gui.config.ConfigScreen;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class ConfigPageOne extends AbstractConfigPage
{
    Screen parentScreen;

    public ConfigPageOne(Screen parentScreen)
    {
        super(parentScreen);
        if (parentScreen == null)
        {   parentScreen = Minecraft.getInstance().screen;
        }
        this.parentScreen = parentScreen;
    }

    @Override
    public ITextComponent sectionOneTitle()
    {   return new TranslationTextComponent("cold_sweat.config.section.temperature_details");
    }

    @Override
    public ITextComponent sectionTwoTitle()
    {   return new TranslationTextComponent("cold_sweat.config.section.difficulty");
    }

    @Override
    protected void init()
    {
        super.init();

        Temperature.Units[] properUnits = {ConfigSettings.CELSIUS.get() ? Temperature.Units.C : Temperature.Units.F};

        /*
         The Options
        */

        // Celsius
        this.addButton("units", Side.LEFT, () -> new TranslationTextComponent("cold_sweat.config.units.name").append(": ").append(ConfigSettings.CELSIUS.get()
                                                   ? new TranslationTextComponent("cold_sweat.config.celsius.name")
                                                   : new TranslationTextComponent("cold_sweat.config.fahrenheit.name")),
        button ->
        {
            PlayerEntity player = Minecraft.getInstance().player;

            ConfigSettings.CELSIUS.set(!ConfigSettings.CELSIUS.get());

            properUnits[0] = ConfigSettings.CELSIUS.get() ? Temperature.Units.C : Temperature.Units.F;

            // Change the max & min temps to reflect the new setting
            ((TextFieldWidget) this.getWidgetBatch("max_temp").get(0)).setValue(String.valueOf(ConfigScreen.TWO_PLACES.format(
                    Temperature.convert(ConfigSettings.MAX_TEMP.get(), Temperature.Units.MC, properUnits[0], true))));

            ((TextFieldWidget) this.getWidgetBatch("min_temp").get(0)).setValue(String.valueOf(ConfigScreen.TWO_PLACES.format(
                    Temperature.convert(ConfigSettings.MIN_TEMP.get(), Temperature.Units.MC, properUnits[0], true))));

            // Update the world temp. gauge when the button is pressed
            if (player != null)
                Overlays.setWorldTempInstant(Temperature.convert(Overlays.WORLD_TEMP, properUnits[0] == Temperature.Units.C ? Temperature.Units.F : Temperature.Units.C, properUnits[0], true));
        }, false, false, true, new TranslationTextComponent("cold_sweat.config.units.desc"));

        // Max Temperature
        this.addDecimalInput("max_temp", Side.LEFT, new TranslationTextComponent("cold_sweat.config.max_temperature.name"),
                value -> ConfigSettings.MAX_TEMP.set(Temperature.convert(value, properUnits[0], Temperature.Units.MC, true)),
                input -> input.setValue(String.valueOf(Temperature.convert(ConfigSettings.MAX_TEMP.get(), Temperature.Units.MC, properUnits[0], true))),
                true, false, false, new TranslationTextComponent("cold_sweat.config.max_temperature.desc"));

        // Min Temperature
        this.addDecimalInput("min_temp", Side.LEFT, new TranslationTextComponent("cold_sweat.config.min_temperature.name"),
                value -> ConfigSettings.MIN_TEMP.set(Temperature.convert(value, properUnits[0], Temperature.Units.MC, true)),
                input -> input.setValue(String.valueOf(Temperature.convert(ConfigSettings.MIN_TEMP.get(), Temperature.Units.MC, properUnits[0], true))),
                true, false, false, new TranslationTextComponent("cold_sweat.config.min_temperature.desc"));

        // Temp Damage
        this.addDecimalInput("temp_damage", Side.LEFT, new TranslationTextComponent("cold_sweat.config.temp_damage.name"),
                value -> ConfigSettings.TEMP_DAMAGE.set(value),
                input -> input.setValue(String.valueOf(ConfigSettings.TEMP_DAMAGE.get())),
                true, true, false, new TranslationTextComponent("cold_sweat.config.temp_damage.desc"));

        // Rate Multiplier
        this.addDecimalInput("rate", Side.LEFT, new TranslationTextComponent("cold_sweat.config.temperature_rate.name"),
                value -> ConfigSettings.TEMP_RATE.set(value),
                input -> input.setValue(String.valueOf(ConfigSettings.TEMP_RATE.get())),
                true, true, false, new TranslationTextComponent("cold_sweat.config.temperature_rate.desc"));

        // Difficulty button
        this.addButton("difficulty", Side.RIGHT, () -> new TranslationTextComponent("cold_sweat.config.difficulty.name").append(
                        " (" + ConfigSettings.Difficulty.getFormattedName(ConfigSettings.DIFFICULTY.get()).getString() + ")..."),
                button -> MINECRAFT.setScreen(new ConfigPageDifficulty(this)),
                true, false, false, new TranslationTextComponent("cold_sweat.config.difficulty.desc"));

        this.addEmptySpace(Side.RIGHT, 1);


        // Misc. Temp Effects
        this.addButton("ice_resistance", Side.RIGHT,
                () -> new TranslationTextComponent("cold_sweat.config.ice_resistance.name").append(": ").append(ConfigSettings.ICE_RESISTANCE_ENABLED.get() ? ON : OFF),
                button -> ConfigSettings.ICE_RESISTANCE_ENABLED.set(!ConfigSettings.ICE_RESISTANCE_ENABLED.get()),
                true, true, false, new TranslationTextComponent("cold_sweat.config.ice_resistance.desc"));

        this.addButton("fire_resistance", Side.RIGHT,
                () -> new TranslationTextComponent("cold_sweat.config.fire_resistance.name").append(": ").append(ConfigSettings.FIRE_RESISTANCE_ENABLED.get() ? ON : OFF),
                button -> ConfigSettings.FIRE_RESISTANCE_ENABLED.set(!ConfigSettings.FIRE_RESISTANCE_ENABLED.get()),
                true, true, false, new TranslationTextComponent("cold_sweat.config.fire_resistance.desc"));

        this.addButton("require_thermometer", Side.RIGHT,
                () -> new TranslationTextComponent("cold_sweat.config.require_thermometer.name").append(": ").append(ConfigSettings.REQUIRE_THERMOMETER.get() ? ON : OFF),
                button -> ConfigSettings.REQUIRE_THERMOMETER.set(!ConfigSettings.REQUIRE_THERMOMETER.get()),
                true, true, false, new TranslationTextComponent("cold_sweat.config.require_thermometer.desc"));

        this.addButton("use_peaceful", Side.RIGHT,
                () -> new TranslationTextComponent("cold_sweat.config.use_peaceful.name").append(": ").append(ConfigSettings.USE_PEACEFUL_MODE.get() ? ON : OFF),
                button -> ConfigSettings.USE_PEACEFUL_MODE.set(!ConfigSettings.USE_PEACEFUL_MODE.get()),
                true, true, false, new TranslationTextComponent("cold_sweat.config.use_peaceful.desc"));
    }

    @Override
    public void onClose()
    {   ConfigScreen.saveConfig();
        super.onClose();
    }
}
