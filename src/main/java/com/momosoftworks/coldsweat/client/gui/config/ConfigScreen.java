package com.momosoftworks.coldsweat.client.gui.config;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.client.event.TooltipHandler;
import com.momosoftworks.coldsweat.client.gui.config.pages.*;
import com.momosoftworks.coldsweat.config.spec.ClientSettingsConfig;
import com.momosoftworks.coldsweat.core.network.message.SyncConfigSettingsMessage;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.network.message.SyncPreferredUnitsMessage;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ConfigScreen
{
    public static final int TITLE_HEIGHT = 16;
    public static final int BOTTOM_BUTTON_HEIGHT_OFFSET = 26;
    public static final int OPTION_SIZE = 25;
    public static final int BOTTOM_BUTTON_WIDTH = 150;

    public static Minecraft MC = Minecraft.getInstance();

    public static DecimalFormat TWO_PLACES = new DecimalFormat("#.##");

    public static boolean IS_MOUSE_DOWN = false;
    public static int MOUSE_X = 0;
    public static int MOUSE_Y = 0;

    static List<Function<Screen, AbstractConfigPage>> PAGES = new ArrayList<>(Arrays.asList(ConfigPageOne::new, ConfigPageTwo::new, ConfigPageThree::new,
                                                                                            ConfigPageFour::new));
    public static int FIRST_PAGE = 0;
    public static int LAST_PAGE = PAGES.size() - 1;
    public static int CURRENT_PAGE = 0;

    public static final Supplier<Integer> SHIFT_AMOUNT = () -> TooltipHandler.isShiftDown() ? Screen.hasControlDown() ? 100 : 10 : 1;

    public static Screen getPage(int index, Screen parentScreen)
    {   return PAGES.get(CSMath.clamp(index, FIRST_PAGE, LAST_PAGE)).apply(parentScreen);
    }

    public static void saveConfig()
    {
        DynamicRegistries registryAccess = RegistryHelper.getDynamicRegistries();
        if (Minecraft.getInstance().player != null)
        {
            ColdSweatPacketHandler.INSTANCE.sendToServer(new SyncConfigSettingsMessage(registryAccess));
            ColdSweatPacketHandler.INSTANCE.sendToServer(new SyncPreferredUnitsMessage(ConfigSettings.CELSIUS.get() ? Temperature.Units.C : Temperature.Units.F));
        }
        ClientSettingsConfig.getInstance().writeAndSave();
    }

    @SubscribeEvent
    public static void onClicked(GuiScreenEvent.MouseClickedEvent event)
    {
        if (event.getButton() == 0 && Minecraft.getInstance().screen instanceof ConfigPageDifficulty)
            IS_MOUSE_DOWN = true;
    }

    @SubscribeEvent
    public static void onReleased(GuiScreenEvent.MouseReleasedEvent event)
    {
        if (event.getButton() == 0 && Minecraft.getInstance().screen instanceof ConfigPageDifficulty)
            IS_MOUSE_DOWN = false;
    }
}
