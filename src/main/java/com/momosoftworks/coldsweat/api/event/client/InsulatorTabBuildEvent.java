package com.momosoftworks.coldsweat.api.event.client;

import com.momosoftworks.coldsweat.data.codec.configuration.InsulatorData;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.Event;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * Fired when insulation items are about to be added to the "Cold Sweat: Insulators" tab.<br>
 * <br>
 * This event is fired for each type of insulator (item, armor, curio) being added to the tab.
 */
public class InsulatorTabBuildEvent extends Event
{
    private final Collection<Map.Entry<Item, InsulatorData>> items;

    public InsulatorTabBuildEvent(Collection<Map.Entry<Item, InsulatorData>> items)
    {   this.items = items;
    }

    public Collection<Map.Entry<Item, InsulatorData>> getItems()
    {   return items;
    }

    public void addCheck(BiPredicate<Item, InsulatorData> predicate)
    {   items.removeIf(entry -> !predicate.test(entry.getKey(), entry.getValue()));
    }
}
