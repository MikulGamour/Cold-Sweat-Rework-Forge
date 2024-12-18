package com.momosoftworks.coldsweat.mixin;

import com.momosoftworks.coldsweat.api.event.vanilla.ContainerChangedEvent;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Container.class)
public class MixinMenuChanged
{
    @Inject(method = "broadcastChanges", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/NonNullList;set(ILjava/lang/Object;)Ljava/lang/Object;"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void onMenuChanged(CallbackInfo ci, int index, ItemStack oldStack, ItemStack newStack, boolean clientStackChanged)
    {
        ContainerChangedEvent event = new ContainerChangedEvent((Container) (Object) this, oldStack, newStack, index);
        MinecraftForge.EVENT_BUS.post(event);
    }
}
