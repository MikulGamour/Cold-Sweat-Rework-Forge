package com.momosoftworks.coldsweat.api.event.core.registry;

import com.google.common.collect.Multimap;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.Event;

import java.util.*;

/**
 * Gives subscribers unrestricted access to Cold Sweat's registries as they are being loaded.<br>
 * <br>
 * Fired on the Forge event bus when Cold Sweat's registries are gathered, but before they are committed to {@link com.momosoftworks.coldsweat.config.ConfigSettings} where they become usable.<br>
 * <br>
 * This is not an {@link net.neoforged.bus.api.ICancellableEvent}.
 */
public class CreateRegistriesEvent extends Event
{
    RegistryAccess registryAccess;
    Multimap<ResourceKey<Registry<? extends ConfigData>>, Holder<? extends ConfigData>> registries;

    public CreateRegistriesEvent(RegistryAccess registryAccess, Multimap<ResourceKey<Registry<? extends ConfigData>>, Holder<? extends ConfigData>> registries)
    {
        this.registryAccess = registryAccess;
        this.registries = registries;
    }

    public RegistryAccess getRegistryAccess()
    {   return registryAccess;
    }

    public Multimap<ResourceKey<Registry<? extends ConfigData>>, Holder<? extends ConfigData>> getRegistries()
    {   return registries;
    }

    public <T> Collection<Holder<T>> getRegistry(ResourceKey<Registry<T>> key)
    {   return (Collection<Holder<T>>) registries.get((ResourceKey) key);
    }

    /**
     * Fired after Cold Sweat's registries have been gathered and committed to {@link com.momosoftworks.coldsweat.config.ConfigSettings}.<br>
     * Registry removals have been processed at this point.<br>
     * <br>
     * This event should be used to commit your custom registries.
     */
    public static class Post extends CreateRegistriesEvent
    {
        public Post(RegistryAccess registryAccess, Multimap<ResourceKey<Registry<? extends ConfigData>>, Holder<? extends ConfigData>> registries)
        {   super(registryAccess, registries);
        }
    }
}
