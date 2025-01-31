package com.momosoftworks.coldsweat.mixin;

import com.momosoftworks.coldsweat.common.capability.handler.ShearableFurManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShearsDispenseItemBehavior.class)
public class MixinShearsDispenseBehavior
{
    private static ItemStack SHEARS = ItemStack.EMPTY;

    @Inject(method = "execute", at = @At("HEAD"))
    private void storeShearsItem(BlockSource blockSource, ItemStack stack, CallbackInfoReturnable<ItemStack> cir)
    {   SHEARS = stack;
    }

    @Inject(method = "tryShearLivingEntity", at = @At("TAIL"))
    private static void tryShearFurCapability(ServerLevel level, BlockPos pos, ItemStack stack, CallbackInfoReturnable<Boolean> cir)
    {
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos), EntitySelector.NO_SPECTATORS))
        {   ShearableFurManager.shear(living, SHEARS, null);
        }
        SHEARS = ItemStack.EMPTY;
    }
}
