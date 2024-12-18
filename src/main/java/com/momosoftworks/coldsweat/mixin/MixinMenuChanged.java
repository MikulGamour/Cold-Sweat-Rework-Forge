package com.momosoftworks.coldsweat.mixin;

import com.momosoftworks.coldsweat.api.event.vanilla.ContainerChangedEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.function.Supplier;

@Mixin(AbstractContainerMenu.class)
public class MixinMenuChanged
{
    @Inject(method = "triggerSlotListeners", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;set(ILjava/lang/Object;)Ljava/lang/Object;"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void onMenuChanged(int slotIndex, ItemStack pStack, Supplier<ItemStack> stackSupplier, CallbackInfo ci,
                               // locals
                               ItemStack oldStack, ItemStack newStack)
    {
        ContainerChangedEvent event = new ContainerChangedEvent((AbstractContainerMenu) (Object) this, oldStack, newStack, slotIndex);
        MinecraftForge.EVENT_BUS.post(event);
    }
}
