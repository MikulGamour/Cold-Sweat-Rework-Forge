package com.momosoftworks.coldsweat.client.gui.tooltip;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.momosoftworks.coldsweat.client.event.TooltipHandler;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.FuelData;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class ClientSoulspringTooltip extends Tooltip
{
    private static final ResourceLocation TOOLTIP = new ResourceLocation("cold_sweat:textures/gui/tooltip/soulspring_lamp_fuel.png");
    private static final ResourceLocation TOOLTIP_HC = new ResourceLocation("cold_sweat:textures/gui/tooltip/soulspring_lamp_fuel_hc.png");
    public static final Supplier<ResourceLocation> TOOLTIP_LOCATION = () -> ConfigSettings.HIGH_CONTRAST.get()
                                                                            ? TOOLTIP_HC
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
    public int getWidth(FontRenderer font)
    {   return TooltipHandler.isShiftDown() ? Math.min(6, ConfigSettings.SOULSPRING_LAMP_FUEL.get().size()) * 16 : 32;
    }

    @Override
    public void renderImage(FontRenderer font, int x, int y, MatrixStack poseStack, ItemRenderer itemRenderer, int depth)
    {
        y += 1;
        poseStack.pushPose();
        Minecraft.getInstance().textureManager.bind(TOOLTIP_LOCATION.get());
        AbstractGui.blit(poseStack, x, y, 401, 0, 0, 30, 8, 34, 30);
        AbstractGui.blit(poseStack, x, y, 401, 0, 16, (int) (fuel / 2.1333), 8, 34, 30);
        if (TooltipHandler.isShiftDown())
        {
            AbstractGui.blit(poseStack, x + 34, y, 401, 0, 24, 16, 10, 34, 30);
            float oldBlitOffset = itemRenderer.blitOffset;
            itemRenderer.blitOffset = 300;

            int i = 0;
            for (Item item : ConfigSettings.SOULSPRING_LAMP_FUEL.get().keySet())
            {
                for (FuelData fuelData : ConfigSettings.SOULSPRING_LAMP_FUEL.get().get(item))
                {
                    ItemStack stack = new ItemStack(item, 1);
                    stack.setTag(fuelData.data.nbt.tag);
                    itemRenderer.renderGuiItem(stack,
                                               x + ((i * 16) % 96),
                                               y + 12 + CSMath.floor(i / 6d) * 16);
                    i++;
                }
            }
            itemRenderer.blitOffset = oldBlitOffset;
        }
        poseStack.popPose();
    }
}
