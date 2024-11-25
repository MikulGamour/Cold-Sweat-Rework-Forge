package com.momosoftworks.coldsweat.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.common.blockentity.HearthBlockEntity;
import com.momosoftworks.coldsweat.common.container.HearthContainer;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Arrays;

public class HearthScreen extends AbstractHearthScreen<HearthContainer>
{
    private static final ResourceLocation HEARTH_GUI = new ResourceLocation(ColdSweat.MOD_ID, "textures/gui/screen/hearth_gui.png");

    @Override
    HearthBlockEntity getBlockEntity()
    {   return this.menu.te;
    }

    public HearthScreen(HearthContainer screenContainer, PlayerInventory inv, ITextComponent titleIn)
    {
        super(screenContainer, inv, new TranslationTextComponent("container." + ColdSweat.MOD_ID + ".hearth"));
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(MatrixStack poseStack, float partialTicks, int mouseX, int mouseY)
    {   RenderSystem.color4f(1f, 1f, 1f, 1f);
        this.minecraft.textureManager.bind(HEARTH_GUI);
        this.blit(poseStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int hotFuel  = (int) (this.menu.getHotFuel()  / 27.7);
        int coldFuel = (int) (this.menu.getColdFuel() / 27.7);

        // Render hot/cold fuel gauges
        blit(poseStack, leftPos + 61,  topPos + 66 - hotFuel,  176, 36 - hotFuel,  12, hotFuel, 256, 256);
        blit(poseStack, leftPos + 103, topPos + 66 - coldFuel, 188, 36 - coldFuel, 12, coldFuel, 256, 256);

        // Render redstone indicators
        if (!ConfigSettings.SMART_HEARTH.get())
        {
            boolean sidePowered = this.menu.te.isSidePowered();
            boolean backPowered = this.menu.te.isBackPowered();

            blit(poseStack, leftPos + 60, topPos + 21, 176, backPowered ? 60 : 68, 14, 8, 256, 256);
            blit(poseStack, leftPos + 102, topPos + 21, 190, sidePowered ? 60 : 68, 14, 8, 256, 256);

            if (CSMath.betweenInclusive(mouseX, leftPos + 56, leftPos + 75) && CSMath.betweenInclusive(mouseY, topPos + 17, topPos + 29))
            {   this.renderComponentTooltip(poseStack, Arrays.asList(new TranslationTextComponent(backPowered ? "gui.cold_sweat.hearth.powered" : "gui.cold_sweat.hearth.unpowered")), mouseX, mouseY);;
            }
            if (CSMath.betweenInclusive(mouseX, leftPos + 99, leftPos + 117) && CSMath.betweenInclusive(mouseY, topPos + 17, topPos + 29))
            {   this.renderComponentTooltip(poseStack, Arrays.asList(new TranslationTextComponent(sidePowered ? "gui.cold_sweat.hearth.powered" : "gui.cold_sweat.hearth.unpowered")), mouseX, mouseY);
            }
        }
    }
}
