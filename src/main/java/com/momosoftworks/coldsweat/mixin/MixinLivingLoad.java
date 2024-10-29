package com.momosoftworks.coldsweat.mixin;

import com.momosoftworks.coldsweat.api.event.vanilla.LivingEntityLoadAdditionalEvent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class MixinLivingLoad
{
    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void postLivingLoadEvent(CompoundNBT nbt, CallbackInfo ci)
    {
        LivingEntityLoadAdditionalEvent event = new LivingEntityLoadAdditionalEvent((LivingEntity)(Object)this, nbt);
        MinecraftForge.EVENT_BUS.post(event);
    }
}
