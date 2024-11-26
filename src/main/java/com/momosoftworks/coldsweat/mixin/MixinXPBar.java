package com.momosoftworks.coldsweat.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.IngameGui;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IngameGui.class)
public class MixinXPBar
{
    @Inject(method = "renderExperienceBar(Lcom/mojang/blaze3d/matrix/MatrixStack;I)V",
            at = @At
            (
                value = "INVOKE",
                target = "Lnet/minecraft/profiler/IProfiler;push(Ljava/lang/String;)V",
                shift = At.Shift.AFTER
            ),
            slice = @Slice
            (
                from = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/IProfiler;pop()V", ordinal = 0),
                to   = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;width(Ljava/lang/String;)I")
            ))
    public void moveXPNumberDown(MatrixStack poseStack, int xPos, CallbackInfo ci)
    {
        poseStack.pushPose();
        // Render XP bar
        if (ConfigSettings.CUSTOM_HOTBAR_LAYOUT.get())
        {   poseStack.translate(0, 4, 0);
        }
    }

    @Inject(method = "renderExperienceBar(Lcom/mojang/blaze3d/matrix/MatrixStack;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/IProfiler;pop()V", ordinal = 1))
    public void renderXPNumberPost(MatrixStack poseStack, int xPos, CallbackInfo ci)
    {
        poseStack.popPose();
    }

    @Mixin(IngameGui.class)
    public static class MixinItemLabel
    {
        @Inject(method = "renderSelectedItemName(Lcom/mojang/blaze3d/matrix/MatrixStack;)V",
                at = @At(value = "HEAD"))
        public void moveItemNameUp(MatrixStack poseStack, CallbackInfo ci)
        {
            poseStack.pushPose();
            if (ConfigSettings.CUSTOM_HOTBAR_LAYOUT.get())
            {   poseStack.translate(0, -4, 0);
            }
        }

        @Inject(method = "renderSelectedItemName(Lcom/mojang/blaze3d/matrix/MatrixStack;)V",
                at = @At(value = "TAIL"))
        public void renderItemNamePost(MatrixStack poseStack, CallbackInfo ci)
        {
            poseStack.popPose();
        }
    }
}