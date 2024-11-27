package com.momosoftworks.coldsweat.client.gui.config.pages;

import com.momosoftworks.coldsweat.client.gui.config.AbstractConfigPage;
import com.momosoftworks.coldsweat.client.gui.config.ConfigScreen;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

public class ConfigPageFive extends AbstractConfigPage
{
    public ConfigPageFive(Screen parentScreen)
    {   super(parentScreen);
    }

    @Override
    public IFormattableTextComponent sectionOneTitle()
    {   return new TranslationTextComponent("cold_sweat.config.section.icebox");
    }

    @Nullable
    @Override
    public IFormattableTextComponent sectionTwoTitle()
    {   return null;
    }

    @Override
    protected void init()
    {
        super.init();

        // Smart Icebox
        this.addButton("smart_icebox", Side.LEFT, () -> getToggleButtonText(new TranslationTextComponent("cold_sweat.config.smart_source.name"), ConfigSettings.SMART_ICEBOX.get()),
                       button -> ConfigSettings.SMART_ICEBOX.set(!ConfigSettings.SMART_ICEBOX.get()),
                       true, false, false, new TranslationTextComponent("cold_sweat.config.smart_source.desc"));

        // Icebox Range
        this.addDecimalInput("icebox_range", Side.LEFT, new TranslationTextComponent("cold_sweat.config.source_range.name"),
                             value -> ConfigSettings.ICEBOX_RANGE.set(value.intValue()),
                             input -> input.setValue(ConfigSettings.ICEBOX_RANGE.get() + ""),
                             true, false, false, new TranslationTextComponent("cold_sweat.config.source_range.desc"));

        // Icebox Max Range
        this.addDecimalInput("icebox_max_range", Side.LEFT, new TranslationTextComponent("cold_sweat.config.source_max_range.name"),
                             value -> ConfigSettings.ICEBOX_MAX_RANGE.set(value.intValue()),
                             input -> input.setValue(ConfigSettings.ICEBOX_MAX_RANGE.get() + ""),
                             true, false, false, new TranslationTextComponent("cold_sweat.config.source_max_range.desc"));

        // Icebox Max Volume
        this.addDecimalInput("icebox_max_volume", Side.LEFT, new TranslationTextComponent("cold_sweat.config.source_max_volume.name"),
                             value -> ConfigSettings.ICEBOX_MAX_VOLUME.set(value.intValue()),
                             input -> input.setValue(ConfigSettings.ICEBOX_MAX_VOLUME.get() + ""),
                             true, false, false, new TranslationTextComponent("cold_sweat.config.source_max_volume.desc"));

        // Icebox Warm Up Time
        this.addDecimalInput("icebox_warm_up_time", Side.LEFT, new TranslationTextComponent("cold_sweat.config.source_warm_up_time.name"),
                             value -> ConfigSettings.ICEBOX_WARM_UP_TIME.set(value.intValue()),
                             input -> input.setValue(ConfigSettings.ICEBOX_WARM_UP_TIME.get() + ""),
                             true, false, false, new TranslationTextComponent("cold_sweat.config.source_warm_up_time.desc"));
    }

    @Override
    public void onClose()
    {   super.onClose();
        ConfigScreen.saveConfig();
    }
}
