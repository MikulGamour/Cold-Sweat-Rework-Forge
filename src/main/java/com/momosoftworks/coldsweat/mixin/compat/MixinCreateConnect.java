package com.momosoftworks.coldsweat.mixin.compat;

import com.momosoftworks.coldsweat.common.block.SmokestackBlock;
import com.momosoftworks.coldsweat.util.registries.ModBlocks;
import com.simibubi.create.content.contraptions.fluids.pipes.FluidPipeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidPipeBlock.class)
public class MixinCreateConnect
{
    /**
     * Enable Create pipes connecting to the smokestack of the hearth
     */
    @Inject(method = "canConnectTo", at = @At("HEAD"), cancellable = true, remap = false)
    private static void shouldPipesConnectTo(IBlockDisplayReader world, BlockPos neighborPos, BlockState neighbor, Direction direction, CallbackInfoReturnable<Boolean> cir)
    {
        if (direction == Direction.DOWN && neighbor.getBlock() instanceof SmokestackBlock)
        {   cir.setReturnValue(true);
        }
    }
}
