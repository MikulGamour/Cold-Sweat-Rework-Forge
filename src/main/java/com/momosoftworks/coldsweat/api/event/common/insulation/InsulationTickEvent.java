package com.momosoftworks.coldsweat.api.event.common.insulation;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import java.util.Map;

/**
 * Fired when the player's armor insulation has been calculated.<br>
 * Allows for changing the insulation that the player's equipped items provide.<br>
 * <br>
 * Cancelling this event will prevent the player's armor from providing any insulation.<br>
 * <br>
 * <h3>Properties</h3>
 * The insulation from equipped items is divided into "properties" that can be individually modified.<br>
 * <br>
 * List of properties:<br>
 * <ul>
 * <li><strong>cold_armor</strong> - The cold insulation from the player's armor itself (if it is an insulating armor)</li>
 * <li><strong>heat_armor</strong> - The heat insulation from the player's armor itself (if it is an insulating armor)</li>
 * <li><strong>cold_insulators</strong> - The cold insulation from the insulators on the player's armor</li>
 * <li><strong>heat_insulators</strong> - The heat insulation from the insulators on the player's armor</li>
 * <li><strong>cold_protection</strong> - The cold insulation from the player's armor protection value</li>
 * <li><strong>heat_protection</strong> - The heat insulation from the player's armor protection value</li>
 * <li><strong>cold_curios</strong> - The cold insulation from the player's equipped curios</li>
 * <li><strong>heat_curios</strong> - The heat insulation from the player's equipped curios</li>
 * <li><strong>cold</strong> - The total cold insulation</li>
 * <li><strong>heat</strong> - The total heat insulation</li>
 * </ul>
 */
@Cancelable
public class InsulationTickEvent extends Event
{
    private final PlayerEntity player;
    Map<String, Double> insulation;

    public InsulationTickEvent(PlayerEntity player, Map<String, Double> insulation)
    {
        this.player = player;
        this.insulation = insulation;
    }

    public PlayerEntity getPlayer()
    {   return player;
    }

    public double getProperty(String property)
    {
        if (property.equals("cold"))
        {   return getProperty("cold_armor") + getProperty("cold_insulators") + getProperty("cold_protection") + getProperty("cold_curios");
        }
        if (property.equals("heat"))
        {   return getProperty("heat_armor") + getProperty("heat_insulators") + getProperty("heat_protection") + getProperty("heat_curios");
        }
        return insulation.getOrDefault(property, 0.0);
    }

    public void setProperty(String property, double value)
    {   insulation.put(property, value);
    }
}
