package com.momosoftworks.coldsweat.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.client.gui.Overlays;
import com.momosoftworks.coldsweat.common.event.TempEffectsCommon;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.config.spec.MainSettingsConfig;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModEffects;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.gui.ForgeIngameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static net.minecraft.client.gui.AbstractGui.GUI_ICONS_LOCATION;

/**
 * A needlessly complex mixin to render frozen hearts when the player's temp falls below -50
 */
@Mixin(ForgeIngameGui.class)
public abstract class MixinHeartRender
{
    @Shadow(remap = false)
    protected abstract void bind(ResourceLocation res);

    private static final ResourceLocation HEART_TEXTURE = new ResourceLocation(ColdSweat.MOD_ID, "textures/gui/overlay/hearts_frozen.png");
    private static int HEART_INDEX = 0;
    private static boolean IS_CONTAINER = false;

    @Inject(method = "renderHealth",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/gui/ForgeIngameGui;blit(Lcom/mojang/blaze3d/matrix/MatrixStack;IIIIII)V", shift = At.Shift.AFTER),
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;ceil(F)I", ordinal = 3),
                           to = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;disableBlend()V")),
            locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true, remap = false)
    private void renderHeart(int width, int height, MatrixStack ps, CallbackInfo ci,
                             // Locals
                             PlayerEntity player, int health, boolean blink, int healthLast,
                             ModifiableAttributeInstance attrMaxHealth, float healthMax,
                             float absorb, int healthRows, int rowHeight, int left,
                             int top, int regen, int TOP, int BACKGROUND, int MARGIN,
                             float absorbRemaining, int i, int row, int x, int y)
    {
        double heartsFreezePercentage = ConfigSettings.HEARTS_FREEZING_PERCENTAGE.get();
        int hearts = CSMath.ceil(health / 2d);
        int lastHeartIndex = (int) (healthMax / 2 - hearts);
        boolean half = HEART_INDEX == lastHeartIndex + 1 && health % 2 == 1;
        if (player == null) return;

        boolean isHardcore = player.level.getLevelData().isHardcore();

        if (heartsFreezePercentage == 0
        || player.hasEffect(ModEffects.GRACE)) return;

        if (player.hasEffect(ModEffects.ICE_RESISTANCE)) return;
        double temp = Overlays.BODY_TEMP;

        // Get protection from armor underwear
        float unfrozenHealth = CSMath.blend((float) (1 - heartsFreezePercentage), 1, TempEffectsCommon.getColdResistance(player), 0, 4);
        if (unfrozenHealth == 1) return;

        int frozenHealth = (int) (player.getMaxHealth() - player.getMaxHealth() * CSMath.blend(unfrozenHealth, 1, temp, -100, -50));
        int frozenHearts = frozenHealth / 2;
        int u = blink || IS_CONTAINER ? 14 : half ? 7 : 0;

        // Render frozen hearts
        bind(HEART_TEXTURE);
        if (HEART_INDEX > 0 && HEART_INDEX < frozenHearts + 1)
        {
            AbstractGui.blit(ps, x, y, 21, 0, 9, 9, 30, 14);
            AbstractGui.blit(ps, x + 1, y + 1, u, 0, 7, 7, 30, 14);
            if (isHardcore)
            {   AbstractGui.blit(ps, x + 1, y + 1, 23, 10, 7, 4, 30, 14);
            }
            ci.cancel();
        }
        // Render half-frozen heart if needed
        else if (HEART_INDEX == frozenHearts + 1 && frozenHealth % 2 == 1)
        {
            AbstractGui.blit(ps, x + 1, y + 1, u, 7, 7, 7, 30, 14);
            if (isHardcore)
            {   AbstractGui.blit(ps, x + 4, y + 1, 26, 10, 4, 4, 30, 14);
            }
        }
        bind(GUI_ICONS_LOCATION);
        IS_CONTAINER = false;
    }

    @Inject(method = "renderHealth",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;ceil(F)I", ordinal = 4),
            remap = false)
    private void incrementHeartIndex(int width, int height, MatrixStack mStack, CallbackInfo ci)
    {   HEART_INDEX++;
        IS_CONTAINER = true;
    }

    @Inject(method = "renderHealth", at = @At(value = "HEAD"), remap = false)
    private void resetHeartIndex(int width, int height, MatrixStack mStack, CallbackInfo ci)
    {   HEART_INDEX = 0;
    }
}
