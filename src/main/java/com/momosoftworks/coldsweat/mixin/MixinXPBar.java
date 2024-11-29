package com.momosoftworks.coldsweat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Gui.class)
public class MixinXPBar
{
    @Inject(method = "renderExperienceBar",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/gui/Font;draw(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFI)I",
                     ordinal = 0))
    public void shiftExperienceBar(PoseStack poseStack, int xPos, CallbackInfo ci)
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
                target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V",
                ordinal = 0
            ),
            slice = @Slice
            (   from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I"),
                to   = @At(value = "RETURN")
            ))
    public void experienceBarPop(PoseStack poseStack, int xPos, CallbackInfo ci)
    {
        poseStack.popPose();
    }

    @Mixin(Gui.class)
    public static class MixinItemLabel
    {
        @Inject(method = "renderSelectedItemName(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
                at = @At(value = "HEAD"), remap = false)
        public void shiftItemName(PoseStack poseStack, CallbackInfo ci)
        {
            poseStack.pushPose();
            if (ConfigSettings.CUSTOM_HOTBAR_LAYOUT.get())
            {   poseStack.translate(0, -4, 0);
            }
        }

        @Inject(method = "renderSelectedItemName(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
                at = @At(value = "TAIL"), remap = false)
        public void itemNamePop(PoseStack poseStack, CallbackInfo ci)
        {
            poseStack.popPose();
        }
    }
}
