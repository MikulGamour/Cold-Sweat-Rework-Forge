package com.momosoftworks.coldsweat.mixin;

import com.blackgear.cavesandcliffs.client.renderer.entity.GoatRenderer;
import com.momosoftworks.coldsweat.common.capability.handler.ShearableFurManager;
import com.momosoftworks.coldsweat.common.capability.shearing.IShearableCap;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GoatRenderer.class)
public class MixinGoatRenderer
{
    private static final ResourceLocation SHEARED_GOAT_TEXTURE = new ResourceLocation("cavesandcliffs:textures/entity/goat/goat_shaven.png");

    @Inject(method = "getTextureLocation",
            at = @At("HEAD"), cancellable = true)
    private void getTextureLocation(Entity goat, CallbackInfoReturnable<ResourceLocation> cir)
    {
        LazyOptional<IShearableCap> goatCap = ShearableFurManager.getFurCap(goat);
        if (goatCap.resolve().isPresent() && goatCap.resolve().get().isSheared())
        {   cir.setReturnValue(SHEARED_GOAT_TEXTURE);
        }
    }
}
