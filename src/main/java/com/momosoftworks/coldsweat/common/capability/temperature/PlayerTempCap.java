package com.momosoftworks.coldsweat.common.capability.temperature;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.init.ModItems;
import com.momosoftworks.coldsweat.compat.CompatManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Holds all the information regarding the entity's temperature. This should very rarely be used directly.
 */
public class PlayerTempCap extends AbstractTempCap
{
    @Override
    public void tickHurting(LivingEntity entity, double heatResistance, double coldResistance)
    {
        if ((!(entity instanceof Player player) || !player.isCreative()) && !entity.isSpectator())
        {   super.tickHurting(entity, heatResistance, coldResistance);
        }
    }

    @Override
    public void tick(LivingEntity entity)
    {
        super.tick(entity);
        if (entity instanceof Player player)
        {
            if (player.tickCount % 20 == 0)
            {   calculateHudVisibility(player);
            }
            if (player.isCreative())
            {   this.setTrait(Temperature.Trait.CORE, 0);
            }
        }
    }

    @Override
    public void tickDummy(LivingEntity entity)
    {
        super.tickDummy(entity);
    }

    public void calculateHudVisibility(Player player)
    {
        showWorldTemp = !ConfigSettings.REQUIRE_THERMOMETER.get()
                || player.isCreative()
                || player.getInventory().items.stream().limit(9).anyMatch(stack -> stack.getItem() == ModItems.THERMOMETER.value())
                || player.getOffhandItem().getItem() == ModItems.THERMOMETER.value()
                || CompatManager.Curios.hasCurio(player, ModItems.THERMOMETER.value());
        showBodyTemp = !player.isCreative() && !player.isSpectator();
    }
}
