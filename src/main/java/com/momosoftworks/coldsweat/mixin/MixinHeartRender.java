package com.momosoftworks.coldsweat.mixin;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
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
        boolean isHardcore = player.level().getLevelData().isHardcore();

        // Get protection from armor underwear
        float maxFrozenHealth = (float) CSMath.blend(maxHealth * heartsFreezePercentage, 0, Temperature.get(player, Temperature.Trait.COLD_RESISTANCE), 0, 1);
        if (maxFrozenHealth == 0) return;

        int frozenHealth = (int) CSMath.blend(0, maxHealth * heartsFreezePercentage, temp, -50, -100);
        int frozenHearts = Math.round(frozenHealth / 2f);
        boolean partialFrozen = frozenHealth % 2 == 1 && HEART_INDEX == frozenHearts;
        int u = isHardcore ? 7 : 0;
        int v = partialFrozen ? halfHeart ? 21 : 14 : halfHeart ? 7 : 0;

        // Render frozen hearts
        if (HEART_INDEX <= frozenHearts)
        {
            if (heartType == Gui.HeartType.CONTAINER)
            {   guiGraphics.blit(HEART_TEXTURE, x + 1, y + 1, 14, v, 7, 7, 21, 28);
            }
            else
            {  guiGraphics.blit(HEART_TEXTURE, x + 1, y + 1, u, v, 7, 7, 21, 28);
            }
        }
    }

    @Inject(method = "renderHearts", at = @At("HEAD"))
    private void renderHearts(GuiGraphics pGuiGraphics, Player pPlayer, int pX, int pY, int pHeight, int pOffsetHeartIndex, float pMaxHealth, int pCurrentHealth, int pDisplayHealth, int pAbsorptionAmount, boolean pRenderHighlight, CallbackInfo ci)
    {   HEART_INDEX = 0;
    }
}
