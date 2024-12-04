package com.momosoftworks.coldsweat.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.common.event.TempEffectsCommon;
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
    private void renderHeart(PoseStack ps, Gui.HeartType heartType, int x, int y, int texV, boolean blink, boolean half, CallbackInfo ci)
    {
        double heartsFreezePercentage = ConfigSettings.HEARTS_FREEZING_PERCENTAGE.get();
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        boolean isHardcore = player.level.getLevelData().isHardcore();

        if (heartsFreezePercentage == 0
        || player.hasEffect(ModEffects.GRACE)) return;

        // This check ensures that this only gets called once per heart
        if (heartType == Gui.HeartType.CONTAINER)
        {   HEART_INDEX += 1;
        }

        if (player.hasEffect(ModEffects.ICE_RESISTANCE)) return;
        double temp = Overlays.BODY_TEMP;

        // Get protection from armor underwear
        float unfrozenHealth = CSMath.blend((float) (1 - heartsFreezePercentage), 1, TempEffectsCommon.getColdResistance(player), 0, 4);
        if (unfrozenHealth == 1) return;

        int frozenHealth = (int) (player.getMaxHealth() - player.getMaxHealth() * CSMath.blend(unfrozenHealth, 1, temp, -100, -50));
        int frozenHearts = frozenHealth / 2;
        int u = blink || heartType == Gui.HeartType.CONTAINER ? 14 : half ? 7 : 0;

        // Render frozen hearts
        RenderSystem.setShaderTexture(0, HEART_TEXTURE);
            if (HEART_INDEX > 0 && HEART_INDEX < frozenHearts + 1)
            {
                GuiComponent.blit(ps, x, y, 21, 0, 9, 9, 30, 14);
            GuiComponent.blit(ps, x + 1, y + 1, u, 0, 7, 7, 30, 14);
            if (isHardcore)
            {   GuiComponent.blit(ps, x + 1, y + 1, 23, 10, 7, 4, 30, 14);
            }
            ci.cancel();
        }
        // Render half-frozen heart if needed
        else if (HEART_INDEX == frozenHearts + 1 && frozenHealth % 2 == 1)
        {
            GuiComponent.blit(ps, x + 1, y + 1, u, 7, 7, 7, 30, 14);
            if (isHardcore)
            {   GuiComponent.blit(ps, x + 4, y + 1, 26, 10, 4, 4, 30, 14);
            }
        }
        RenderSystem.setShaderTexture(0, ICONS_TEXTURE);
    }

    @Inject(method = "renderHearts", at = @At("HEAD"))
    private void renderHearts(PoseStack pPoseStack, Player pPlayer, int pX, int pY, int pHeight, int p_168694_, float p_168695_, int p_168696_, int p_168697_, int p_168698_, boolean p_168699_, CallbackInfo ci)
    {   HEART_INDEX = 0;
    }
}
