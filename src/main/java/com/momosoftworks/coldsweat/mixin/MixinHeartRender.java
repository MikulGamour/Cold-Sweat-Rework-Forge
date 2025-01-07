package com.momosoftworks.coldsweat.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.client.gui.Overlays;
import com.momosoftworks.coldsweat.config.ConfigSettings;
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
        boolean halfHeart = HEART_INDEX == lastHeartIndex + 1 && health % 2 == 1;
        if (player == null) return;


        if (heartsFreezePercentage == 0
        || player.hasEffect(ModEffects.GRACE)) return;
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
        bind(HEART_TEXTURE);
        if (HEART_INDEX <= frozenHearts)
        {
            if (IS_CONTAINER)
            {   AbstractGui.blit(ps, x + 1, y + 1, 14, v, 7, 7, 21, 28);
            }
            else
            {  AbstractGui.blit(ps, x + 1, y + 1, u, v, 7, 7, 21, 28);
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
