package com.momosoftworks.coldsweat.common.event;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PreventPlayerSleep
{
    @SubscribeEvent
    public static void onTrySleep(PlayerSleepInBedEvent event)
    {
        Player player = event.getEntity();

        // There's already something blocking the player from sleeping
        if (event.getResultStatus() != null || !ConfigSettings.CHECK_SLEEP_CONDITIONS.get()
        || ConfigSettings.SLEEP_CHECK_IGNORE_BLOCKS.get().contains(player.level.getBlockState(event.getPos()).getBlock()))
        {   return;
        }

        double bodyTemp = Temperature.get(player, Temperature.Trait.BODY);
        double worldTemp = Temperature.get(player, Temperature.Trait.WORLD);
        double minTemp = Temperature.get(player, Temperature.Trait.FREEZING_POINT);
        double maxTemp = Temperature.get(player, Temperature.Trait.BURNING_POINT);

        // If the player's body temperature is critical
        if (!CSMath.betweenExclusive(bodyTemp, -100, 100))
        {   // Let the player sleep if they're resistant to damage
            if (Temperature.get(player, bodyTemp > 0 ? Temperature.Trait.HEAT_RESISTANCE : Temperature.Trait.COLD_RESISTANCE) >= 1)
            {   return;
            }
            // Prevent sleep with message
            player.displayClientMessage(Component.translatable("cold_sweat.message.sleep.body." + (bodyTemp > 99 ? "hot" : "cold")), true);
            event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
        }
        // If the player's world temperature is critical
        else if (!CSMath.betweenExclusive(worldTemp, minTemp, maxTemp))
        {   // Let the player sleep if they're resistant to damage
            if (TempEffectsCommon.getTempResistance(event.getEntity(), minTemp > worldTemp) >= 4)
            {   return;
            }
            // Prevent sleep with message
            player.displayClientMessage(Component.translatable("cold_sweat.message.sleep.world." + (worldTemp > maxTemp ? "hot" : "cold")), true);
            event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
        }
    }
}
