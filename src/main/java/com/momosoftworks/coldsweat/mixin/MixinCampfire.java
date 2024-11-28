package com.momosoftworks.coldsweat.mixin;

import com.momosoftworks.coldsweat.common.item.FilledWaterskinItem;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireBlockEntity.class)
public class MixinCampfire
{
    /**
     * Heat waterskins gradually
     */
    @Inject(method = "cookTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/CampfireBlockEntity;)V",
            at = @At(value = "HEAD"))
    private static void onItemCook(Level level, BlockPos pos, BlockState state, CampfireBlockEntity blockEntity, CallbackInfo ci)
    {
        double waterskinStrength = ConfigSettings.WATERSKIN_STRENGTH.get();
        double tempRate = ConfigSettings.TEMP_RATE.get();

        for (int i = 0; i < blockEntity.getItems().size(); i++)
        {
            ItemStack stack = blockEntity.getItems().get(i);
            if (stack.is(ModItems.FILLED_WATERSKIN) && (level.getGameTime() & 4) == 0)
            {
                CompoundTag tag = stack.getOrCreateTag();
                double temperature = tag.getDouble(FilledWaterskinItem.NBT_TEMPERATURE);

                // If the block ID contains "soul", it's a soul campfire
                if (state.is(BlockTags.CAMPFIRES) && CSMath.getIfNotNull(ForgeRegistries.BLOCKS.getKey(state.getBlock()), ResourceLocation::toString, "").contains("soul")
                && tag.getDouble(FilledWaterskinItem.NBT_TEMPERATURE) > -waterskinStrength * 0.6)
                {
                    tag.putDouble(FilledWaterskinItem.NBT_TEMPERATURE,
                                  temperature + tempRate * 0.1 * (ConfigSettings.COLD_SOUL_FIRE.get() ? -1 : 1));
                }
                else if (state.is(BlockTags.CAMPFIRES) && tag.getDouble(FilledWaterskinItem.NBT_TEMPERATURE) < waterskinStrength * 0.6)
                {
                    tag.putDouble(FilledWaterskinItem.NBT_TEMPERATURE,
                                  temperature + tempRate * 0.1);
                }
            }
        }
    }

    /**
     * Ensure waterskin temperature is not reset when cooking finishes
     */
    @ModifyArg(method = "cookTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/CampfireBlockEntity;)V",
               at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Containers;dropItemStack(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"), index = 4)
    private static ItemStack onItemFinishedCooking(Level level, double x, double y, double z, ItemStack result)
    {
        if (result.is(ModItems.FILLED_WATERSKIN))
        {
            double waterskinStrength = ConfigSettings.WATERSKIN_STRENGTH.get();
            CompoundTag tag = result.getOrCreateTag();
            BlockState state = level.getBlockState(new BlockPos(x, y, z));

            if (state.is(BlockTags.CAMPFIRES) && CSMath.getIfNotNull(ForgeRegistries.BLOCKS.getKey(state.getBlock()), ResourceLocation::toString, "").contains("soul"))
            {
                tag.putDouble(FilledWaterskinItem.NBT_TEMPERATURE,
                              waterskinStrength * 0.6 * (ConfigSettings.COLD_SOUL_FIRE.get() ? -1 : 1));
            }
            else if (state.is(BlockTags.CAMPFIRES))
            {
                tag.putDouble(FilledWaterskinItem.NBT_TEMPERATURE,
                              waterskinStrength * 0.6);
            }
        }
        return result;
    }
}
