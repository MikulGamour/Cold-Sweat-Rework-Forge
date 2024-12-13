package com.momosoftworks.coldsweat.common.item;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

public class ThermometerItem extends Item
{
    public ThermometerItem(Properties properties)
    {   super(properties);
    }

    @Override
    public ActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand)
    {   // Display the ambient temperature on right-click
        if (CompatManager.isSupplementariesLoaded() && !player.level.isClientSide)
        {
            // Get the temperature, in the player's preferred units
            Temperature.Units units = EntityTempManager.getTemperatureCap(player).map(cap -> cap.getPreferredUnits()).orElse(Temperature.Units.F);
            int temperature = (int) Temperature.convert(WorldHelper.getTemperatureAt(player.level, player.blockPosition()), Temperature.Units.MC, units, true);
            // Display the temperature to the player
            player.displayClientMessage(new StringTextComponent(temperature + " " + units.getFormattedName()), true);
            player.swing(hand, true);
        }
        return super.use(level, player, hand);
    }
}
