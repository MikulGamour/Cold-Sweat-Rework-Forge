package com.momosoftworks.coldsweat.api.event.core.init;

import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Called during startup to enable temperature for every entity type.<br>
 * This is an {@link ICancellableEvent} and is called on the Forge event bus. <br>
 * Cancelling this event will prevent temperature from being enabled for the entity type if it is already enabled.
 */
public class EnableTemperatureEvent extends Event implements ICancellableEvent
{
    final EntityType<?> entityType;
    boolean enabled = false;

    public EnableTemperatureEvent(EntityType<?> entityType)
    {   this.entityType = entityType;
    }

    public EntityType<?> getEntityType()
    {   return entityType;
    }

    public void setEnabled(boolean enabled)
    {   this.enabled = enabled;
    }

    public boolean isEnabled()
    {   return enabled;
    }
}
