package com.momosoftworks.coldsweat.mixin;

import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.registries.ModDamageSources;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoulFireBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFireBlock.class)
public class MixinSoulFire
{
    @Final
    @Shadow
    private float fireDamage;

    @Inject(method = "entityInside(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)V",
    at = @At("HEAD"), cancellable = true)
    public void entityInside(BlockState state, World level, BlockPos pos, Entity entity, CallbackInfo ci)
    {
        if (state.getBlock() instanceof SoulFireBlock && ConfigSettings.COLD_SOUL_FIRE.get()
        && !(entity instanceof ItemEntity && CompatManager.isSpiritLoaded()))
        {
            entity.hurt(ModDamageSources.COLD, this.fireDamage);
            entity.clearFire();
            ci.cancel();
        }
    }
}
