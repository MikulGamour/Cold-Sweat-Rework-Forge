package com.momosoftworks.coldsweat.client.gui.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.client.event.TooltipHandler;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.FuelData;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class ClientSoulspringTooltip implements ClientTooltipComponent
{
    private static final ResourceLocation TOOLTIP = ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "textures/gui/tooltip/soulspring_lamp_fuel.png");
    private static final ResourceLocation TOOLTIP_HC = ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "textures/gui/tooltip/soulspring_lamp_fuel_hc.png");
    public static final Supplier<ResourceLocation> TOOLTIP_LOCATION = () ->
            ConfigSettings.HIGH_CONTRAST.get() ? TOOLTIP_HC
                                               : TOOLTIP;

    double fuel;

    public ClientSoulspringTooltip(double fuel)
    {
        this.fuel = fuel;
    }

    @Override
    public int getHeight()
    {   return TooltipHandler.isShiftDown() ? CSMath.ceil(ConfigSettings.SOULSPRING_LAMP_FUEL.get().size() / 6d) * 16 + 14 : 12;
    }

    @Override
    public int getWidth(Font font)
    {   return TooltipHandler.isShiftDown() ? Math.min(6, ConfigSettings.SOULSPRING_LAMP_FUEL.get().size()) * 16 : 32;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics)
    {
        graphics.blit(TOOLTIP_LOCATION.get(), x, y, 0, 0, 0, 30, 8, 30, 34);
        graphics.blit(TOOLTIP_LOCATION.get(), x, y, 0, 0, 16, (int) (fuel / 2.1333), 8, 30, 34);
        if (TooltipHandler.isShiftDown())
        {
            graphics.blit(TOOLTIP_LOCATION.get(), x + 34, y, 0, 0, 24, 16, 10, 30, 34);

            int i = 0;
            for (Item item : ConfigSettings.SOULSPRING_LAMP_FUEL.get().keySet())
            {
                for (FuelData fuelData : ConfigSettings.SOULSPRING_LAMP_FUEL.get().get(item))
                {
                    graphics.renderItem(new ItemStack(Holder.direct(item), 1, fuelData.data().components().getAsPatch()),
                                        x + ((i * 16) % 96), y + 12 + CSMath.floor(i / 6d) * 16);
                    i++;
                }
            }
        }
    }
}
