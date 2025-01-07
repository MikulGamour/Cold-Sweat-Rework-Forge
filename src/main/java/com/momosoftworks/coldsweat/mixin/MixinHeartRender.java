package com.momosoftworks.coldsweat.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.client.gui.Overlays;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A needlessly complex mixin to render frozen hearts when the player's temp falls below -50
 */
@Mixin(Gui.class)
public class MixinHeartRender
{
    private static final ResourceLocation HEART_TEXTURE = new ResourceLocation(ColdSweat.MOD_ID, "textures/gui/overlay/hearts_frozen.png");
    private static final ResourceLocation ICONS_TEXTURE = new ResourceLocation("minecraft", "textures/gui/icons.png");
    private static int HEART_INDEX = 0;

    @Inject(method = "renderHeart(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Gui$HeartType;IIIZZ)V", at = @At("TAIL"), cancellable = true)
    private void renderHeart(PoseStack ps, Gui.HeartType heartType, int x, int y, int texV, boolean blink, boolean halfHeart, CallbackInfo ci)
    {
        // This check ensures that this only gets called once per heart
        if (heartType == Gui.HeartType.CONTAINER)
        {   HEART_INDEX += 1;
        }
        double heartsFreezePercentage = ConfigSettings.HEARTS_FREEZING_PERCENTAGE.get();
        Player player = Minecraft.getInstance().player;

        if (player == null) return;
        if (heartsFreezePercentage == 0 || player.hasEffect(ModEffects.GRACE)) return;
        if (player.hasEffect(ModEffects.ICE_RESISTANCE)) return;

        double temp = Overlays.BODY_TEMP;
        float maxHealth = player.getMaxHealth();
        boolean isHardcore = player.level.getLevelData().isHardcore();

        // Get protection from armor underwear
        float maxFrozenHealth = (float) CSMath.blend(maxHealth * heartsFreezePercentage, 0, Temperature.get(player, Temperature.Trait.COLD_RESISTANCE), 0, 1);
        if (maxFrozenHealth == 0) return;

        int frozenHealth = (int) CSMath.blend(0, maxHealth * heartsFreezePercentage, temp, -50, -100);
        int frozenHearts = Math.round(frozenHealth / 2f);
        boolean partialFrozen = frozenHealth % 2 == 1 && HEART_INDEX == frozenHearts;
        int u = isHardcore ? 7 : 0;
        int v = partialFrozen ? halfHeart ? 21 : 14 : halfHeart ? 7 : 0;

        // Render frozen hearts
        RenderSystem.setShaderTexture(0, HEART_TEXTURE);
        if (HEART_INDEX <= frozenHearts)
        {
            if (heartType == Gui.HeartType.CONTAINER)
            {   GuiComponent.blit(ps, x + 1, y + 1, 14, v, 7, 7, 21, 28);
            }
            else
            {  GuiComponent.blit(ps, x + 1, y + 1, u, v, 7, 7, 21, 28);
            }
        }
        RenderSystem.setShaderTexture(0, ICONS_TEXTURE);
    }

    @Inject(method = "renderHearts", at = @At("HEAD"))
    private void renderHearts(PoseStack pPoseStack, Player pPlayer, int pX, int pY, int pHeight, int p_168694_, float p_168695_, int p_168696_, int p_168697_, int p_168698_, boolean p_168699_, CallbackInfo ci)
    {   HEART_INDEX = 0;
    }
}
