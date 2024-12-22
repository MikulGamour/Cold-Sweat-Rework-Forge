package com.momosoftworks.coldsweat.client.gui.config.pages;

import com.momosoftworks.coldsweat.client.gui.config.AbstractConfigPage;
import com.momosoftworks.coldsweat.client.gui.config.ConfigScreen;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public class ConfigPageFour extends AbstractConfigPage
{
    public ConfigPageFour(Screen parentScreen)
    {   super(parentScreen);
    }

    @Override
    public Component sectionOneTitle()
    {   return Component.translatable("cold_sweat.config.section.thermal_source");
    }

    @Nullable
    @Override
    public Component sectionTwoTitle()
    {   return null;
    }

    @Override
    protected void init()
    {
        super.init();

        // Smart Hearth
        this.addButton("smart_hearth", Side.LEFT, () -> getToggleButtonText(Component.translatable("cold_sweat.config.smart_source.name", Component.translatable("block.cold_sweat.hearth_bottom").getString()),
                                                                            ConfigSettings.SMART_HEARTH.get()),
                       button -> ConfigSettings.SMART_HEARTH.set(!ConfigSettings.SMART_HEARTH.get()),
                       true, false, false, Component.translatable("cold_sweat.config.smart_source.desc"));

        // Smart Boiler
        this.addButton("smart_boiler", Side.LEFT, () -> getToggleButtonText(Component.translatable("cold_sweat.config.smart_source.name", Component.translatable("block.cold_sweat.boiler").getString()),
                                                                            ConfigSettings.SMART_BOILER.get()),
                       button -> ConfigSettings.SMART_BOILER.set(!ConfigSettings.SMART_BOILER.get()),
                       true, false, false, Component.translatable("cold_sweat.config.smart_source.desc"));

        // Smart Icebox
        this.addButton("smart_icebox", Side.LEFT, () -> getToggleButtonText(Component.translatable("cold_sweat.config.smart_source.name", Component.translatable("block.cold_sweat.icebox").getString()),
                                                                            ConfigSettings.SMART_ICEBOX.get()),
                       button -> ConfigSettings.SMART_ICEBOX.set(!ConfigSettings.SMART_ICEBOX.get()),
                       true, false, false, Component.translatable("cold_sweat.config.smart_source.desc"));

        // Source Debug
        this.addButton("source_debug", Side.LEFT, () -> getToggleButtonText(Component.translatable("cold_sweat.config.source_debug.name"), ConfigSettings.HEARTH_DEBUG.get()),
                       button -> ConfigSettings.HEARTH_DEBUG.set(!ConfigSettings.HEARTH_DEBUG.get()),
                       true, false, false, Component.translatable("cold_sweat.config.source_debug.desc"));
    }

    @Override
    public void onClose()
    {   super.onClose();
        ConfigScreen.saveConfig();
    }
}
