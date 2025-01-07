package com.momosoftworks.coldsweat.common.event;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PreventPlayerSleep
{
    @SubscribeEvent
    public static void onTrySleep(PlayerSleepInBedEvent event)
    {
        PlayerEntity player = event.getPlayer();

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
            player.displayClientMessage(new TranslationTextComponent("cold_sweat.message.sleep.body." + (bodyTemp > 99 ? "hot" : "cold")), true);
            event.setResult(PlayerEntity.SleepResult.OTHER_PROBLEM);
        }
        // If the player's world temperature is critical
        else if (!CSMath.betweenExclusive(worldTemp, minTemp, maxTemp))
        {   // Let the player sleep if they're resistant to damage
            if (TempEffectsCommon.getTempResistance(event.getPlayer(), minTemp > worldTemp) >= 4)
            {   return;
            }
            // Prevent sleep with message
            player.displayClientMessage(new TranslationTextComponent("cold_sweat.message.sleep.world." + (worldTemp > maxTemp ? "hot" : "cold")), true);
            event.setResult(PlayerEntity.SleepResult.OTHER_PROBLEM);
        }
    }
}
