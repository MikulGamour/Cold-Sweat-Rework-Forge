package com.momosoftworks.coldsweat.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.common.blockentity.HearthBlockEntity;
import com.momosoftworks.coldsweat.common.container.IceboxContainer;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Arrays;

public class IceboxScreen extends AbstractHearthScreen<IceboxContainer>
{
    private static final ResourceLocation ICEBOX_GUI = new ResourceLocation(ColdSweat.MOD_ID, "textures/gui/screen/icebox_gui.png");

    @Override
    HearthBlockEntity getBlockEntity()
    {   return this.menu.te;
    }

    public IceboxScreen(IceboxContainer screenContainer, PlayerInventory inv, ITextComponent titleIn)
    {
        super(screenContainer, inv, titleIn);
        this.imageWidth = 176;
        this.imageHeight = 172;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = leftPos + this.imageWidth / 2 - Minecraft.getInstance().font.width(this.getTitle()) / 2;
    }

    @Override
    public void render(MatrixStack ps, int mouseX, int mouseY, float partialTicks)
    {
        super.render(ps, mouseX, mouseY, partialTicks);
    }

    @Override
    public void init()
    {
        super.init();
        if (particleButton != null)
        {   particleButton.x = leftPos + 151;
            particleButton.y = topPos + 63;
        }
    }

    @Override
    protected void renderBg(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY)
    {
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        Minecraft.getInstance().textureManager.bind(ICEBOX_GUI);
        this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Draw fuel gauge
        blit(matrixStack, leftPos + 109, topPos + 63, 176, 0, (int) (menu.te.getFuel() / 31.25), 14, 256, 256);

        if (!ConfigSettings.SMART_HEARTH.get() && this.menu.te.hasSmokeStack())
        {
            boolean powered = this.menu.te.isSidePowered();

            blit(matrixStack, leftPos + 117, topPos + 78, 176, powered ? 14 : 22, 14, 8, 256, 256);

            if (CSMath.betweenInclusive(mouseX, leftPos + 114, leftPos + 132) && CSMath.betweenInclusive(mouseY, topPos + 78, topPos + 86))
            {   this.renderComponentTooltip(matrixStack, Arrays.asList(new TranslationTextComponent(powered ? "gui.cold_sweat.hearth.powered" : "gui.cold_sweat.hearth.unpowered")), mouseX, mouseY);;
            }
        }
    }
}
