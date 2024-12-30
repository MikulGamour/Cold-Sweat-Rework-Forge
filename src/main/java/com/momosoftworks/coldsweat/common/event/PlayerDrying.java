package com.momosoftworks.coldsweat.common.event;

import com.momosoftworks.coldsweat.api.temperature.modifier.WaterTempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.DryingItemData;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PlayerDrying
{
    @SubscribeEvent
    public static void onDryingItemUsed(PlayerInteractEvent.RightClickItem event)
    {
        ItemStack stack = event.getItemStack();
        Player player = event.getPlayer();
        DryingItemData dryingResult = ConfigSettings.DRYING_ITEMS.get().get(stack.getItem());

        if (!player.level.isClientSide() && dryingResult != null && dryingResult.test(stack, player)
        && Temperature.hasModifier(player, Temperature.Trait.WORLD, WaterTempModifier.class))
        {
            // Create result item
            ItemStack newStack = dryingResult.result();
            CompoundTag oldTag = NBTHelper.getTagOrEmpty(stack).copy();
            if (!oldTag.isEmpty())
            {   newStack.getOrCreateTag().merge(NBTHelper.getTagOrEmpty(stack).copy());
            }
            // Remove item from player's inventory
            if (!player.getAbilities().instabuild)
            {
                stack.shrink(1);
                // Add result item to player's inventory
                if (!player.getInventory().add(newStack))
                {   player.drop(newStack, false);
                }
            }
            // Effects
            player.swing(event.getHand(), true);
            WorldHelper.playEntitySound(dryingResult.sound(), player, SoundSource.PLAYERS, 1.0F, 1.0F);
            // Remove water temperature modifier
            Temperature.removeModifiers(player, Temperature.Trait.WORLD, mod -> mod instanceof WaterTempModifier);
        }
    }
}
