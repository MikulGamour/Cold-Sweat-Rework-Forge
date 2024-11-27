package com.momosoftworks.coldsweat.client.gui.config.pages;

import com.momosoftworks.coldsweat.client.gui.config.AbstractConfigPage;
import com.momosoftworks.coldsweat.client.gui.config.ConfigScreen;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.Nullable;

public class ConfigPageFour extends AbstractConfigPage
{
    public ConfigPageFour(Screen parentScreen)
    {   super(parentScreen);
    }

    @Override
    public MutableComponent sectionOneTitle()
    {   return new TranslatableComponent("cold_sweat.config.section.hearth");
    }

    @Nullable
    @Override
    public MutableComponent sectionTwoTitle()
    {   return new TranslatableComponent("cold_sweat.config.section.boiler");
    }

    @Override
    protected void init()
    {
        super.init();

        // Smart Hearth
        this.addButton("smart_hearth", Side.LEFT, () -> getToggleButtonText(new TranslatableComponent("cold_sweat.config.smart_source.name"), ConfigSettings.SMART_HEARTH.get()),
                       button -> ConfigSettings.SMART_HEARTH.set(!ConfigSettings.SMART_HEARTH.get()),
                       true, false, false, new TranslatableComponent("cold_sweat.config.smart_source.desc"));

        // Hearth Range
        this.addDecimalInput("hearth_range", Side.LEFT, new TranslatableComponent("cold_sweat.config.source_range.name"),
                             value -> ConfigSettings.HEARTH_RANGE.set(value.intValue()),
                             input -> input.setValue(ConfigSettings.HEARTH_RANGE.get() + ""),
                             true, false, false, new TranslatableComponent("cold_sweat.config.source_range.desc"));

        // Hearth Max Range
        this.addDecimalInput("hearth_max_range", Side.LEFT, new TranslatableComponent("cold_sweat.config.source_max_range.name"),
                             value -> ConfigSettings.HEARTH_MAX_RANGE.set(value.intValue()),
                             input -> input.setValue(ConfigSettings.HEARTH_MAX_RANGE.get() + ""),
                             true, false, false, new TranslatableComponent("cold_sweat.config.source_max_range.desc"));

        // Hearth Max Volume
        this.addDecimalInput("hearth_max_volume", Side.LEFT, new TranslatableComponent("cold_sweat.config.source_max_volume.name"),
                             value -> ConfigSettings.HEARTH_MAX_VOLUME.set(value.intValue()),
                             input -> input.setValue(ConfigSettings.HEARTH_MAX_VOLUME.get() + ""),
                             true, false, false, new TranslatableComponent("cold_sweat.config.source_max_volume.desc"));

        // Hearth Warm Up Time
        this.addDecimalInput("hearth_warm_up_time", Side.LEFT, new TranslatableComponent("cold_sweat.config.source_warm_up_time.name"),
                             value -> ConfigSettings.HEARTH_WARM_UP_TIME.set(value.intValue()),
                             input -> input.setValue(ConfigSettings.HEARTH_WARM_UP_TIME.get() + ""),
                             true, false, false, new TranslatableComponent("cold_sweat.config.source_warm_up_time.desc"));

        // Source Debug
        this.addButton("source_debug", Side.LEFT, () -> getToggleButtonText(new TranslatableComponent("cold_sweat.config.source_debug.name"), ConfigSettings.HEARTH_DEBUG.get()),
                       button -> ConfigSettings.HEARTH_DEBUG.set(!ConfigSettings.HEARTH_DEBUG.get()),
                       true, false, false, new TranslatableComponent("cold_sweat.config.source_debug.desc"));

        // Smart Boiler
        this.addButton("smart_boiler", Side.RIGHT, () -> getToggleButtonText(new TranslatableComponent("cold_sweat.config.smart_source.name"), ConfigSettings.SMART_BOILER.get()),
                       button -> ConfigSettings.SMART_BOILER.set(!ConfigSettings.SMART_BOILER.get()),
                       true, false, false, new TranslatableComponent("cold_sweat.config.smart_source.desc"));

        // Boiler Range
        this.addDecimalInput("boiler_range", Side.RIGHT, new TranslatableComponent("cold_sweat.config.source_range.name"),
                             value -> ConfigSettings.BOILER_RANGE.set(value.intValue()),
                             input -> input.setValue(ConfigSettings.BOILER_RANGE.get() + ""),
                             true, false, false, new TranslatableComponent("cold_sweat.config.source_range.desc"));

        // Boiler Max Range
        this.addDecimalInput("boiler_max_range", Side.RIGHT, new TranslatableComponent("cold_sweat.config.source_max_range.name"),
                             value -> ConfigSettings.BOILER_MAX_RANGE.set(value.intValue()),
                             input -> input.setValue(ConfigSettings.BOILER_MAX_RANGE.get() + ""),
                             true, false, false, new TranslatableComponent("cold_sweat.config.source_max_range.desc"));

        // Boiler Max Volume
        this.addDecimalInput("boiler_max_volume", Side.RIGHT, new TranslatableComponent("cold_sweat.config.source_max_volume.name"),
                             value -> ConfigSettings.BOILER_MAX_VOLUME.set(value.intValue()),
                             input -> input.setValue(ConfigSettings.BOILER_MAX_VOLUME.get() + ""),
                             true, false, false, new TranslatableComponent("cold_sweat.config.source_max_volume.desc"));

        // Boiler Warm Up Time
        this.addDecimalInput("boiler_warm_up_time", Side.RIGHT, new TranslatableComponent("cold_sweat.config.source_warm_up_time.name"),
                             value -> ConfigSettings.BOILER_WARM_UP_TIME.set(value.intValue()),
                             input -> input.setValue(ConfigSettings.BOILER_WARM_UP_TIME.get() + ""),
                             true, false, false, new TranslatableComponent("cold_sweat.config.source_warm_up_time.desc"));
    }

    @Override
    public void onClose()
    {   super.onClose();
        ConfigScreen.saveConfig();
    }
}
