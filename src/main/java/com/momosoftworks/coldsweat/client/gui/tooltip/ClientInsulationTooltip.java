package com.momosoftworks.coldsweat.client.gui.tooltip;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.momosoftworks.coldsweat.api.insulation.AdaptiveInsulation;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.insulation.StaticInsulation;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.entity.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class ClientInsulationTooltip extends Tooltip
{
    private static final ResourceLocation TOOLTIP = new ResourceLocation("cold_sweat:textures/gui/tooltip/insulation_bar.png");
    private static final ResourceLocation TOOLTIP_HC = new ResourceLocation("cold_sweat:textures/gui/tooltip/insulation_bar_hc.png");
    public static final Supplier<ResourceLocation> TOOLTIP_LOCATION = () -> ConfigSettings.HIGH_CONTRAST.get()
                                                                            ? TOOLTIP_HC
                                                                            : TOOLTIP;

    List<Insulation> insulation;
    Insulation.Slot slot;
    int width = 0;
    ItemStack stack;
    boolean strikethrough;

    private static final Method INNER_BLIT = ObfuscationReflectionHelper.findMethod(AbstractGui.class, "func_238469_a_",
                                                                                    MatrixStack.class, int.class, int.class, int.class,
                                                                                    int.class, int.class, int.class, int.class,
                                                                                    float.class, float.class, int.class, int.class);
    static
    {   INNER_BLIT.setAccessible(true);
    }

    public static void innerBlit(MatrixStack poseStack, int x1, int x2, int y1, int y2, int zOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight)
    {   try
    {   INNER_BLIT.invoke(null, poseStack, x1, x2, y1, y2, zOffset, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
    }
    catch (Exception e)
    {   e.printStackTrace();
    }
    }
    public static void blit(MatrixStack poseStack, int x, int y, int zOffset, int width, int height, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight)
    {   innerBlit(poseStack, x, x + width, y, y + height, zOffset, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
    }


    public ClientInsulationTooltip(List<Insulation> insulation, Insulation.Slot slot, ItemStack stack, boolean strikethrough)
    {   this.insulation = insulation;
        this.slot = slot;
        this.stack = stack;
        this.strikethrough = strikethrough;
    }

    @Override
    public int getHeight()
    {   return 10;
    }

    @Override
    public int getWidth(FontRenderer font)
    {   return width + 12;
    }

    @Override
    public void renderImage(FontRenderer font, int x, int y, MatrixStack poseStack, ItemRenderer itemRenderer, int depth)
    {
        y += 2;
        Minecraft.getInstance().textureManager.bind(TOOLTIP_LOCATION.get());

        List<Insulation> posInsulation = new ArrayList<>();
        List<Insulation> negInsulation = new ArrayList<>();

        for (Insulation ins : insulation)
        {
            if (ins instanceof StaticInsulation)
            {
                StaticInsulation insul = (StaticInsulation) ins;
                double cold = insul.getCold();
                double hot = insul.getHeat();

                if (CSMath.sign(cold) == CSMath.sign(hot))
                {
                    switch (CSMath.sign(cold))
                    {   case -1 : negInsulation.add(ins); break;
                        case 1 : posInsulation.add(ins); break;
                    }
                }
                else
                {
                    switch (CSMath.sign(cold))
                    {   case -1 : negInsulation.add(new StaticInsulation(-cold, 0)); break;
                        case 1 : posInsulation.add(new StaticInsulation(cold, 0)); break;
                    }
                    switch (CSMath.sign(hot))
                    {   case -1 : negInsulation.add(new StaticInsulation(0, hot)); break;
                        case 1 : posInsulation.add(new StaticInsulation(0, hot)); break;
                    }
                }
            }
            else if (ins instanceof AdaptiveInsulation)
            {
                AdaptiveInsulation adaptive = (AdaptiveInsulation) ins;
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
        {   width += renderBar(poseStack, x + width, y, posInsulation, slot, !negInsulation.isEmpty(), false, stack);
        }

        // Negative insulation bar
        if (!negInsulation.isEmpty())
        {   width += 4;
            width += renderBar(poseStack, x + width, y, negInsulation, slot, true, true, stack);
        }
        poseStack.popPose();
        if (this.strikethrough)
        {
            AbstractGui.fill(poseStack, x - 1, y + 2, x + width + 1, y + 3, 0xaFF63232);
            AbstractGui.fill(poseStack, x, y + 3, x + width + 2, y + 4, 0xaFF63232);
        }
    }

    static void renderCell(MatrixStack poseStack, int x, int y, double insulation, int uvX, boolean isAdaptive)
    {
        Minecraft.getInstance().textureManager.bind(TOOLTIP_LOCATION.get());
        double rounded = CSMath.roundNearest(Math.abs(insulation), 0.25);
        int uvY = isAdaptive
                  // If the amount of insulation in this cell is greater than 2, use the full cell texture, otherwise use the half cell texture
                  ? (rounded >= 2 ? 16 : 20)
                  : (rounded >= 2 ? 8 : 12);
        AbstractGui.blit(poseStack, x, y, 401, uvX, uvY, 6, 4, 24, 32);
    }

    static int renderOverloadCell(MatrixStack poseStack, FontRenderer font, int x, int y, double insulation, int textColor, Insulation.Type type)
    {
        Number insul = CSMath.truncate(insulation / 2, 2);
        if (CSMath.isInteger(insul)) insul = insul.intValue();
        String text = "x" + insul;
        int uvX = 0;
        switch (type)
        {
            case COLD : uvX = 12; break;
            case HEAT : uvX = 18; break;
            case NEUTRAL : uvX = 6; break;
            case ADAPTIVE : uvX = 12; break;
        }

        poseStack.pushPose();
        Minecraft.getInstance().textureManager.bind(TOOLTIP_LOCATION.get());
        renderCell(poseStack, x + 7, y + 1, insulation, uvX, type == Insulation.Type.ADAPTIVE);
        AbstractGui.blit(poseStack,
                          x + 6, y,
                          401, /*z*/
                          11 /*u*/, 0 /*v*/,
                          8 /*uWidth*/, 6 /*vHeight*/,
                          24, 32);
        //set the shader texture to the font atlas
        poseStack.translate(0, 0, 400);
        font.drawShadow(poseStack, text, x + 15, y - 1, textColor);
        poseStack.popPose();

        // Return the width of the cell and text
        return 12 + font.width(text);
    }

    static int renderBar(MatrixStack poseStack, int x, int y, List<Insulation> insulations, Insulation.Slot slot, boolean showSign, boolean isNegative, ItemStack stack)
    {
        Minecraft.getInstance().textureManager.bind(TOOLTIP_LOCATION.get());
        FontRenderer font = Minecraft.getInstance().font;
        List<Insulation> sortedInsulation = Insulation.sort(insulations);
        boolean overflow = sortedInsulation.size() >= 10;
        int defaultArmorSlots = ConfigSettings.INSULATION_SLOTS.get().getSlots(MobEntity.getEquipmentSlotForItem(stack), stack);
        int insulSlotCount = Math.max(slot == Insulation.Slot.ARMOR ? defaultArmorSlots : 0,
                                      insulations.size());

        int finalWidth = 0;

        // background
        for (int i = 0; i < insulSlotCount && !overflow; i++)
        {   AbstractGui.blit(poseStack, x + 7 + i * 6, y + 1, 401, 0, 0, 6, 4, 24, 32);
        }

        // slots
        poseStack.pushPose();

        // If there's too much insulation, render a compact version of the tooltip
        if (overflow)
        {
            // tally up the insulation from the sorted list into cold, hot, neutral, and adaptive
            double cold = 0;
            double hot = 0;
            double neutral = 0;
            double adaptive = 0;
            for (Insulation insulation : sortedInsulation)
            {
                if (insulation instanceof StaticInsulation)
                {
                    StaticInsulation staticInsulation = (StaticInsulation) insulation;
                    if (staticInsulation.getCold() > staticInsulation.getHeat())
                        cold += staticInsulation.getCold();
                    else if (staticInsulation.getHeat() > staticInsulation.getCold())
                        hot += staticInsulation.getHeat();
                    else
                        neutral += staticInsulation.getCold() * 2;
                }
                else if (insulation instanceof AdaptiveInsulation)
                {
                    AdaptiveInsulation adaptiveInsulation = (AdaptiveInsulation) insulation;
                    adaptive += adaptiveInsulation.getInsulation();
                }
            }
            int textColor = 10526880;

            poseStack.pushPose();
            // Render cold insulation
            if (cold > 0)
            {   int xOffs = renderOverloadCell(poseStack, font, x, y, cold, textColor, Insulation.Type.COLD);
                finalWidth += xOffs;
                poseStack.translate(xOffs, 0, 0);
            }
            if (hot > 0)
            {   int xOffs = renderOverloadCell(poseStack, font, x, y, hot, textColor, Insulation.Type.HEAT);
                finalWidth += xOffs;
                poseStack.translate(xOffs, 0, 0);
            }
            if (neutral > 0)
            {   int xOffs = renderOverloadCell(poseStack, font, x, y, neutral, textColor, Insulation.Type.NEUTRAL);
                finalWidth += xOffs;
                poseStack.translate(xOffs, 0, 0);
            }
            if (adaptive > 0)
            {   int xOffs = renderOverloadCell(poseStack, font, x, y, adaptive, textColor, Insulation.Type.ADAPTIVE);
                finalWidth += xOffs;
                poseStack.translate(xOffs, 0, 0);
            }
            poseStack.popPose();
        }
        // Insulation is small enough to represent traditionally
        else for (Insulation insulation : sortedInsulation)
        {
            if (insulation instanceof AdaptiveInsulation)
            {
                AdaptiveInsulation adaptive = (AdaptiveInsulation) insulation;
                double value = adaptive.getInsulation();

                for (int i = 0; i < CSMath.ceil(Math.abs(value) / 2) ; i++)
                {
                    double insul = CSMath.minAbs(CSMath.shrink(value, i * 2), 2 * CSMath.sign(value));
                    // adaptive cells base color
                    renderCell(poseStack, x + 7, y + 1, insul, 12, true);
                    finalWidth += 6;

                    // adaptive cells overlay
                    double blend = Math.abs(adaptive.getFactor());
                    int overlayU = 0;
                    switch (CSMath.sign(adaptive.getFactor()))
                    {
                        case -1 : overlayU = 6; break;
                        case 1  : overlayU = 18; break;
                        default : overlayU = 12; break;
                    };
                    RenderSystem.enableBlend();
                    RenderSystem.color4f(1, 1, 1, (float) blend);
                    renderCell(poseStack, x + 7, y + 1, insul, overlayU, true);
                    RenderSystem.disableBlend();
                    RenderSystem.color4f(1, 1, 1, 1f);

                    poseStack.translate(6, 0, 0);
                }
            }
            else if (insulation instanceof StaticInsulation)
            {
                StaticInsulation staticInsulation = (StaticInsulation) insulation;
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
                    renderCell(poseStack, x + 7, y + 1, coldInsul, 12, false); // cold cells
                    poseStack.translate(6, 0, 0);
                }

                // Neutral insulation
                for (int i = 0; i < CSMath.ceil(Math.abs(neutral)); i++)
                {   double neutralInsul = CSMath.minAbs(CSMath.shrink(neutral, i), CSMath.sign(neutral)) * 2;
                    renderCell(poseStack, x + 7, y + 1, neutralInsul, 6, false); // neutral cells
                    poseStack.translate(6, 0, 0);
                }

                // Hot insulation
                for (int i = 0; i < CSMath.ceil(Math.abs(heat) / 2); i++)
                {   double hotInsul = CSMath.minAbs(CSMath.shrink(heat, i * 2), 2 * CSMath.sign(heat));
                    renderCell(poseStack, x + 7, y + 1, hotInsul, 18, false); // hot cells
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
                blit(poseStack,
                     x + 7 + i * 6, //x
                     y, //y
                     401, //z
                     5, //width
                     6, //height
                     6, //u
                     0, //v
                     3, //uWidth
                     6, //vHeight
                     32,
                     24);
                blit(poseStack,
                     x + 7 + i * 6 + 4, //x
                     y, //y
                     401, //z
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
                blit(poseStack,
                     x + 7 + i * 6, //x
                     y, //y
                     401, //z
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
            case CURIO : AbstractGui.blit(poseStack, x, y - 1, 401, 24, 16, 8, 8, 24, 32); break;
            case ITEM  : AbstractGui.blit(poseStack, x, y - 1, 401, 24, 0, 8, 8, 24, 32); break;
            case ARMOR : AbstractGui.blit(poseStack, x, y - 1, 401, 24, 8, 8, 8, 24, 32); break;
        }

        if (showSign)
        {
            if (isNegative)
            {   // negative sign
                AbstractGui.blit(poseStack, x + 3, y + 3, 401, 19, 5, 5, 3, 24, 32);
            }
            else
            {   // positive sign
                AbstractGui.blit(poseStack, x + 3, y + 2, 401, 19, 0, 5, 5, 24, 32);
            }
        }
        // Return the width of the tooltip
        if (!overflow) finalWidth += 2;
        return finalWidth + 6;
    }
}

