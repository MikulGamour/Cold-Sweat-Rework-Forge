package com.momosoftworks.coldsweat.client.gui.tooltip;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class ClientInsulationAttributeTooltip extends Tooltip
{
    public static final ResourceLocation TOOLTIP = new ResourceLocation("cold_sweat:textures/gui/tooltip/insulation_bar.png");
    public static final ResourceLocation TOOLTIP_HC = new ResourceLocation("cold_sweat:textures/gui/tooltip/insulation_bar_hc.png");
    public static final Supplier<ResourceLocation> TOOLTIP_LOCATION = () ->
            ConfigSettings.HIGH_CONTRAST.get() ? TOOLTIP_HC
                                               : TOOLTIP;

    IFormattableTextComponent original;
    FontRenderer font;
    boolean strikethrough;

    public ClientInsulationAttributeTooltip(IFormattableTextComponent original, FontRenderer font, boolean strikethrough)
    {   this.original = original;
        this.font = font;
        this.strikethrough = strikethrough;
    }

    @Override
    public int getHeight()
    {   return this.font.lineHeight + 2;
    }

    @Override
    public int getWidth(FontRenderer font)
    {   return this.font.width(this.original) + 10;
    }

    @Override
    public void renderImage(FontRenderer pFont, int x, int y, MatrixStack ps, ItemRenderer pItemRenderer, int pBlitOffset)
    {
        // Icon
        Minecraft.getInstance().textureManager.bind(TOOLTIP_LOCATION.get());
        AbstractGui.blit(ps, x, y + 2, 0, 24, 8, 8, 8, 24, 32);
        // Text
        int color = Optional.ofNullable(this.original.getStyle().getColor()).map(Color::getValue).orElse(16777215);
        ps.pushPose();
        ps.translate(0, 0, 400);
        font.drawShadow(ps, this.original, x + 10, y + 1, color);
        if (strikethrough)
        {   ps.translate(0, 0, 401);
            AbstractGui.fill(ps, x - 1, y + 4, x + this.getWidth(font) + 1, y + 5, 0xFFF63232);
            AbstractGui.fill(ps, x, y + 5, x + this.getWidth(font) + 2, y + 6, 0xFFF63232);
        }
        ps.popPose();
    }
}
