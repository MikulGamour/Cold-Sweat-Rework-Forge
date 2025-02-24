package com.momosoftworks.coldsweat.mixin;

import com.momosoftworks.coldsweat.common.capability.handler.ShearableFurManager;
import com.momosoftworks.coldsweat.common.capability.shearing.IShearableCap;
import net.minecraft.client.renderer.entity.GoatRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GoatRenderer.class)
public class MixinGoatRenderer
{
    private static final ResourceLocation SHEARED_GOAT_TEXTURE = new ResourceLocation("textures/entity/goat/goat_shaven.png");

    @Inject(method = "getTextureLocation(Lnet/minecraft/world/entity/animal/goat/Goat;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"), cancellable = true)
    private void getTextureLocation(Goat goat, CallbackInfoReturnable<ResourceLocation> cir)
    {
        LazyOptional<IShearableCap> goatCap = ShearableFurManager.getFurCap(goat);
        if (goatCap.resolve().isPresent() && goatCap.resolve().get().isSheared())
        {   cir.setReturnValue(SHEARED_GOAT_TEXTURE);
        }
    }
}
