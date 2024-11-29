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
    @Inject(method = "renderExperienceBar",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/gui/FontRenderer;draw(Lcom/mojang/blaze3d/matrix/MatrixStack;Ljava/lang/String;FFI)I",
                     ordinal = 0))
    public void shiftExperienceBar(MatrixStack poseStack, int xPos, CallbackInfo ci)
    {
        poseStack.pushPose();
        // Render XP bar
        if (ConfigSettings.CUSTOM_HOTBAR_LAYOUT.get())
        {   poseStack.translate(0, 4, 0);
        }
    }

    @Inject(method = "renderExperienceBar",
            at = @At
            (   value = "INVOKE",
                target = "Lnet/minecraft/profiler/IProfiler;pop()V",
                ordinal = 0
            ),
            slice = @Slice
            (   from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;width(Ljava/lang/String;)I"),
                to   = @At(value = "RETURN")
            ))
    public void experienceBarPop(MatrixStack poseStack, int xPos, CallbackInfo ci)
    {
        poseStack.popPose();
    }

    @Mixin(IngameGui.class)
    public static class MixinItemLabel
    {
        @Inject(method = "renderSelectedItemName",
                at = @At(value = "HEAD"), remap = false)
        public void shiftItemName(MatrixStack poseStack, CallbackInfo ci)
        {
            poseStack.pushPose();
            if (ConfigSettings.CUSTOM_HOTBAR_LAYOUT.get())
            {   poseStack.translate(0, -4, 0);
            }
        }

        @Inject(method = "renderSelectedItemName",
                at = @At(value = "TAIL"), remap = false)
        public void itemNamePop(MatrixStack poseStack, CallbackInfo ci)
        {
            poseStack.popPose();
        }
    }
}