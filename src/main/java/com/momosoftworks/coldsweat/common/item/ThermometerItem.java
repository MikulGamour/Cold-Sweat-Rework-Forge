package com.momosoftworks.coldsweat.common.item;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ThermometerItem extends Item
{
    public ThermometerItem(Properties properties)
    {   super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {   // Display the ambient temperature on right-click
        if (CompatManager.isSupplementariesLoaded() && !player.level().isClientSide)
        {
            // Get the temperature, in the player's preferred units
            Temperature.Units units = EntityTempManager.getTemperatureCap(player).map(cap -> cap.getPreferredUnits()).orElse(Temperature.Units.F);
            int temperature = (int) Temperature.convert(WorldHelper.getTemperatureAt(player.level(), player.blockPosition()), Temperature.Units.MC, units, true);
            // Display the temperature to the player
            player.displayClientMessage(Component.literal(temperature + " " + units.getFormattedName()), true);
            player.swing(hand, true);
        }
        return super.use(level, player, hand);
    }
}
