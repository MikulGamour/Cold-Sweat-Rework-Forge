package com.momosoftworks.coldsweat.mixin;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.common.event.TempEffectsCommon;
import com.momosoftworks.coldsweat.client.gui.Overlays;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.init.ModEffects;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A needlessly complex mixin to render frozen hearts when the player's temp falls below -50
 */
@Mixin(Gui.class)
public class MixinHeartRender
{
    private static final ResourceLocation HEART_TEXTURE = ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "textures/gui/overlay/hearts_frozen.png");
    // Ticks up as hearts are rendered, representing the "index" of the current heart
    private static int HEART_INDEX = 0;

    @Surrogate
    @Inject(method = "renderHeart", at = @At("TAIL"), cancellable = true)
    private void renderHeart(GuiGraphics guiGraphics, Gui.HeartType heartType, int x, int y, boolean hardcore, boolean halfHeart, boolean blink, CallbackInfo ci)
    {
        double heartsFreezePercentage = ConfigSettings.HEARTS_FREEZING_PERCENTAGE.get();
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        boolean isHardcore = player.level().getLevelData().isHardcore();

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
        int u = blink || heartType == Gui.HeartType.CONTAINER ? 14 : halfHeart ? 7 : 0;

        // Render frozen hearts
        if (HEART_INDEX > 0 && HEART_INDEX < frozenHearts + 1)
        {
            guiGraphics.blit(HEART_TEXTURE, x, y, 21, 0, 9, 9, 30, 14);
            guiGraphics.blit(HEART_TEXTURE, x + 1, y + 1, u, 0, 7, 7, 30, 14);
            if (isHardcore)
            {   guiGraphics.blit(HEART_TEXTURE, x + 1, y + 1, 23, 10, 7, 4, 30, 14);
            }
            ci.cancel();
        }
        // Render half-frozen heart if needed
        else if (HEART_INDEX == frozenHearts + 1 && frozenHealth % 2 == 1)
        {
            guiGraphics.blit(HEART_TEXTURE, x + 1, y + 1, u, 7, 7, 7, 30, 14);
            if (isHardcore)
            {   guiGraphics.blit(HEART_TEXTURE, x + 4, y + 1, 26, 10, 4, 4, 30, 14);
            }
        }
    }

    @Inject(method = "renderHearts", at = @At("HEAD"))
    private void renderHearts(GuiGraphics pGuiGraphics, Player pPlayer, int pX, int pY, int pHeight, int pOffsetHeartIndex, float pMaxHealth, int pCurrentHealth, int pDisplayHealth, int pAbsorptionAmount, boolean pRenderHighlight, CallbackInfo ci)
    {   HEART_INDEX = 0;
    }
}
