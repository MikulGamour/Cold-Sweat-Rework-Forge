package com.momosoftworks.coldsweat.client.gui.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.momosoftworks.coldsweat.api.insulation.AdaptiveInsulation;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.insulation.StaticInsulation;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class ClientInsulationTooltip implements ClientTooltipComponent
{
    public static final ResourceLocation TOOLTIP = new ResourceLocation("cold_sweat:textures/gui/tooltip/insulation_bar.png");
    public static final ResourceLocation TOOLTIP_HC = new ResourceLocation("cold_sweat:textures/gui/tooltip/insulation_bar_hc.png");
    public static final Supplier<ResourceLocation> TOOLTIP_LOCATION = () ->
            ConfigSettings.HIGH_CONTRAST.get() ? TOOLTIP_HC
                                               : TOOLTIP;

    List<Insulation> insulation;
    Insulation.Slot slot;
    int width = 0;
    ItemStack stack;
    boolean strikethrough;

    public ClientInsulationTooltip(List<Insulation> insulation, Insulation.Slot slot, ItemStack stack, boolean strikethrough)
    {
        this.insulation = insulation;
        this.slot = slot;
        this.stack = stack;
        this.strikethrough = strikethrough;
    }

    @Override
    public int getHeight()
    {   return 10;
    }

    @Override
    public int getWidth(Font font)
    {   return width + 12;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics)
    {
        PoseStack poseStack = graphics.pose();
        List<Insulation> posInsulation = new ArrayList<>();
        List<Insulation> negInsulation = new ArrayList<>();

        for (Insulation ins : insulation)
        {
            if (ins instanceof StaticInsulation insul)
            {
                double cold = insul.getCold();
                double heat = insul.getHeat();

                if (CSMath.sign(cold) == CSMath.sign(heat))
                {
                    switch (CSMath.sign(cold))
                    {   case -1 -> negInsulation.add(ins);
                        case 1 -> posInsulation.add(ins);
                    }
                }
                else
                {
                    switch (CSMath.sign(cold))
                    {   case -1 -> negInsulation.add(new StaticInsulation(-cold, 0));
                        case 1 -> posInsulation.add(new StaticInsulation(cold, 0));
                    }
                    switch (CSMath.sign(heat))
                    {   case -1 -> negInsulation.add(new StaticInsulation(0, heat));
                        case 1 -> posInsulation.add(new StaticInsulation(0, heat));
                    }
                }
            }
            else if (ins instanceof AdaptiveInsulation adaptive)
            {
                double value = adaptive.getInsulation();
                if (value < 0)
                {   negInsulation.add(ins);
                }
                else
                {   posInsulation.add(ins);
                }
            }
        }

        /* Render Bars */
        poseStack.pushPose();
        width = 0;

        // Positive insulation bar
        if (!posInsulation.isEmpty())
        {   width += renderBar(graphics, x + width, y, posInsulation, slot, !negInsulation.isEmpty(), false, stack);
        }

        // Negative insulation bar
        if (!negInsulation.isEmpty())
        {   width += 4;
            width += renderBar(graphics, x + width, y, negInsulation, slot, true, true, stack);
        }
        poseStack.popPose();
        if (this.strikethrough)
        {
            graphics.fill(x - 1, y + 2, x + 8, y + 3, 0xFFF63232);
            graphics.fill(x, y + 3, x + 9, y + 4, 0xFFF63232);
        }
    }

    static void renderCell(GuiGraphics graphics, int x, int y, double insulation, int uvX, boolean isAdaptive)
    {
        double rounded = CSMath.roundNearest(Math.abs(insulation), 0.25);
        int uvY = isAdaptive
                  // If the amount of insulation in this cell is greater than 2, use the full cell texture, otherwise use the half cell texture
                  ? (rounded >= 2 ? 16 : 20)
                  : (rounded >= 2 ? 8 : 12);
        graphics.blit(TOOLTIP_LOCATION.get(), x, y, 0, uvX, uvY, 6, 4, 32, 24);
    }

    static int renderOverloadCell(GuiGraphics graphics, Font font, int x, int y, double insulation, int textColor, Insulation.Type type)
    {
        Number insul = CSMath.truncate(insulation / 2, 2);
        if (CSMath.isInteger(insul)) insul = insul.intValue();
        String text = "x" + insul;
        int uvX = switch (type)
        {   case COLD -> 12;
            case HEAT -> 18;
            case NEUTRAL -> 6;
            case ADAPTIVE -> 12;
        };

        renderCell(graphics, x + 7, y + 1, insulation, uvX, type == Insulation.Type.ADAPTIVE);
        graphics.blit(TOOLTIP_LOCATION.get(),
                      x + 6, y,
                      0, /*z*/
                      11 /*u*/, 0 /*v*/,
                      8 /*uWidth*/, 6 /*vHeight*/,
                      32, 24);
        graphics.drawString(font, text, x + 15, y - 1, textColor);
        // Return the width of the cell and text
        return 12 + font.width(text);
    }

    static int renderBar(GuiGraphics graphics, int x, int y, List<Insulation> insulations, Insulation.Slot slot, boolean showSign, boolean isNegative, ItemStack stack)
    {
        PoseStack poseStack = graphics.pose();
        Font font = Minecraft.getInstance().font;

        List<Insulation> sortedInsulation = Insulation.sort(insulations);
        setAdaptations(sortedInsulation, stack);

        boolean overflow = sortedInsulation.size() >= 10;
        int defaultArmorSlots = ConfigSettings.INSULATION_SLOTS.get().getSlots(LivingEntity.getEquipmentSlotForItem(stack), stack);
        int insulSlotCount = Math.max(slot == Insulation.Slot.ARMOR ? defaultArmorSlots : 0,
                                      insulations.size());

        int finalWidth = 0;

        // background
        for (int i = 0; i < insulSlotCount && !overflow; i++)
        {   graphics.blit(TOOLTIP_LOCATION.get(), x + 7 + i * 6, y + 1, 0, 0, 0, 6, 4, 32, 24);
        }

        // slots
        poseStack.pushPose();

        // If there's too much insulation, render a compact version of the tooltip
        if (overflow)
        {
            // tally up the insulation from the sorted list into cold, hot, neutral, and adaptive
            double cold = 0;
            double heat = 0;
            double neutral = 0;
            double adaptive = 0;
            for (Insulation insulation : sortedInsulation)
            {
                if (insulation instanceof StaticInsulation staticInsulation)
                {
                    if (staticInsulation.getCold() > staticInsulation.getHeat())
                        cold += staticInsulation.getCold();
                    else if (staticInsulation.getHeat() > staticInsulation.getCold())
                        heat += staticInsulation.getHeat();
                    else
                        neutral += staticInsulation.getCold() * 2;
                }
                else if (insulation instanceof AdaptiveInsulation adaptiveInsulation)
                {   adaptive += adaptiveInsulation.getInsulation();
                }
            }
            int textColor = 10526880;

            poseStack.pushPose();
            poseStack.translate(0, 0, 0);
            // Render cold insulation
            if (cold > 0)
            {   int xOffs = renderOverloadCell(graphics, font, x, y, cold, textColor, Insulation.Type.COLD);
                finalWidth += xOffs;
                poseStack.translate(xOffs, 0, 0);
            }
            if (heat > 0)
            {   int xOffs = renderOverloadCell(graphics, font, x, y, heat, textColor, Insulation.Type.HEAT);
                finalWidth += xOffs;
                poseStack.translate(xOffs, 0, 0);
            }
            if (neutral > 0)
            {   int xOffs = renderOverloadCell(graphics, font, x, y, neutral, textColor, Insulation.Type.NEUTRAL);
                finalWidth += xOffs;
                poseStack.translate(xOffs, 0, 0);
            }
            if (adaptive > 0)
            {   int xOffs = renderOverloadCell(graphics, font, x, y, adaptive, textColor, Insulation.Type.ADAPTIVE);
                finalWidth += xOffs;
                poseStack.translate(xOffs, 0, 0);
            }
            poseStack.popPose();
        }
        // Insulation is small enough to represent traditionally
        else for (Insulation insulation : sortedInsulation)
        {
            if (insulation instanceof AdaptiveInsulation adaptive)
            {
                double value = adaptive.getInsulation();

                for (int i = 0; i < CSMath.ceil(Math.abs(value) / 2) ; i++)
                {
                    double insul = CSMath.minAbs(CSMath.shrink(value, i * 2), 2 * CSMath.sign(value));
                    // adaptive cells base color
                    renderCell(graphics, x + 7, y + 1, insul, 12, true);
                    finalWidth += 6;

                    // adaptive cells overlay
                    double blend = Math.abs(adaptive.getFactor());
                    int overlayU = switch (CSMath.sign(adaptive.getFactor()))
                    {   case -1 -> 6;
                        case 1 -> 18;
                        default -> 12;
                    };
                    RenderSystem.enableBlend();
                    RenderSystem.setShaderColor(1, 1, 1, (float) blend);
                    renderCell(graphics, x + 7, y + 1, insul, overlayU, true);
                    RenderSystem.disableBlend();
                    RenderSystem.setShaderColor(1, 1, 1, 1f);

                    poseStack.translate(6, 0, 0);
                }
            }
            else if (insulation instanceof StaticInsulation staticInsulation)
            {
                double cold = staticInsulation.getCold();
                double heat = staticInsulation.getHeat();
                double neutral = cold > 0 == heat > 0
                                 ? CSMath.minAbs(cold, heat)
                                 : 0;
                cold -= neutral;
                heat -= neutral;

                // Cold insulation
                for (int i = 0; i < CSMath.ceil(Math.abs(cold) / 2); i++)
                {   double coldInsul = CSMath.minAbs(CSMath.shrink(cold, i * 2), 2 * CSMath.sign(cold));
                    renderCell(graphics, x + 7, y + 1, coldInsul, 12, false); // cold cells
                    poseStack.translate(6, 0, 0);
                }

                // Neutral insulation
                for (int i = 0; i < CSMath.ceil(Math.abs(neutral)); i++)
                {
                    double neutralInsul = CSMath.minAbs(CSMath.shrink(neutral, i), CSMath.sign(neutral)) * 2;
                    renderCell(graphics, x + 7, y + 1, neutralInsul, 6, false); // neutral cells
                    poseStack.translate(6, 0, 0);
                }

                // Hot insulation
                for (int i = 0; i < CSMath.ceil(Math.abs(heat) / 2); i++)
                {
                    double hotInsul = CSMath.minAbs(CSMath.shrink(heat, i * 2), 2 * CSMath.sign(heat));
                    renderCell(graphics, x + 7, y + 1, hotInsul, 18, false); // hot cells
                    poseStack.translate(6, 0, 0);
                }
            }
            finalWidth += 6;
        }
        poseStack.popPose();

        // border
        for (int i = 0; i < insulSlotCount && !overflow; i++)
        {
            boolean end = i == insulSlotCount - 1;
            if (end)
            {
                graphics.blit(TOOLTIP_LOCATION.get(),
                              x + 7 + i * 6, //x
                              y, //y
                              5, //width
                              6, //height
                              6, //u
                              0, //v
                              3, //uWidth
                              6, //vHeight
                              32,
                              24);
                graphics.blit(TOOLTIP_LOCATION.get(),
                              x + 7 + i * 6 + 4, //x
                              y, //y
                              3, //width
                              6, //height
                              8, //u
                              0, //v
                              3, //uWidth
                              6, //vHeight
                              32,
                              24);
            }
            else
            {
                graphics.blit(TOOLTIP_LOCATION.get(),
                              x + 7 + i * 6, //x
                              y, //y
                              6, //width
                              6, //height
                              6, //u
                              0, //v
                              3, //uWidth
                              6, //vHeight
                              32,
                              24);
            }
        }

        // icon
        switch (slot)
        {
            case CURIO -> graphics.blit(TOOLTIP_LOCATION.get(), x, y - 1, 0, 24, 16, 8, 8, 32, 24);
            case ITEM -> graphics.blit(TOOLTIP_LOCATION.get(), x, y - 1, 0, 24, 0, 8, 8, 32, 24);
            case ARMOR -> graphics.blit(TOOLTIP_LOCATION.get(), x, y - 1, 0, 24, 8, 8, 8, 32, 24);
        }

        if (showSign)
        {
            if (isNegative)
            {   // negative sign
                graphics.blit(TOOLTIP_LOCATION.get(), x + 3, y + 3, 0, 19, 5, 5, 3, 32, 24);
            }
            else
            {   // positive sign
                graphics.blit(TOOLTIP_LOCATION.get(), x + 3, y + 2, 0, 19, 0, 5, 5, 32, 24);
            }
        }
        // Return the width of the tooltip
        if (!overflow) finalWidth += 2;
        return finalWidth + 6;
    }

    static void setAdaptations(List<Insulation> insulations, ItemStack stack)
    {
        for (int i = 0; i < insulations.size(); i++)
        {
            Insulation insul = insulations.get(i).copy();
            if (insul instanceof AdaptiveInsulation adaptive)
            {
                // Set factor stored in NBT
                AdaptiveInsulation.setFactorFromNBT(adaptive, stack);
            }
            insulations.set(i, insul);
        }
    }
}
