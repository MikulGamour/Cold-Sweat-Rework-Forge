package com.momosoftworks.coldsweat.api.event.core.registry;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.momosoftworks.coldsweat.data.codec.configuration.RemoveRegistryData;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.eventbus.api.Event;

import java.util.Collection;

/**
 * Gives subscribers unrestricted access to Cold Sweat's registries as they are being loaded.<br>
 * <br>
 * Fired on the Forge event bus when Cold Sweat's registries are gathered, but before they are committed to {@link com.momosoftworks.coldsweat.config.ConfigSettings} where they become usable.<br>
 */
public abstract class CreateRegistriesEvent extends Event
{
    DynamicRegistries registryAccess;
    Multimap<RegistryKey<Registry<? extends ConfigData>>, ? extends ConfigData> registries;

    public CreateRegistriesEvent(DynamicRegistries registryAccess, Multimap<RegistryKey<Registry<? extends ConfigData>>, ? extends ConfigData> registries)
    {
        this.registryAccess = registryAccess;
        this.registries = registries;
    }

    public DynamicRegistries getRegistryAccess()
    {   return registryAccess;
    }

    public Multimap<RegistryKey<Registry<? extends ConfigData>>, ? extends ConfigData> getRegistries()
    {   return registries;
    }

    public <T> Collection<T> getRegistry(RegistryKey<Registry<T>> key)
    {   return (Collection<T>) registries.get((RegistryKey) key);
    }

    /**
     * Fired directly after registries have been gathered, before registry removals are triggered.
     */
    public static class Pre extends CreateRegistriesEvent
    {
        private Multimap<RegistryKey<Registry<? extends ConfigData>>, RemoveRegistryData<?>> removals;

        public Pre(DynamicRegistries registryAccess, Multimap<RegistryKey<Registry<? extends ConfigData>>, ? extends ConfigData> registries,
                   Multimap<RegistryKey<Registry<? extends ConfigData>>, RemoveRegistryData<?>> removals)
        {   super(registryAccess, registries);
        }

        /**
         * @return An IMMUTABLE multimap of registry removals.
         */
        public Multimap<RegistryKey<Registry<? extends ConfigData>>, RemoveRegistryData<?>> getRegistryRemovals()
        {   return ImmutableMultimap.copyOf(removals);
        }
    }

    /**
     * Fired after Cold Sweat's registries have been gathered and committed to {@link com.momosoftworks.coldsweat.config.ConfigSettings}.<br>
     * Registry removals have been processed at this point.<br>
     * <br>
     * This event should be used to commit your custom registries.
     */
    public static class Post extends CreateRegistriesEvent
    {
        public Post(DynamicRegistries registryAccess, Multimap<RegistryKey<Registry<? extends ConfigData>>, ? extends ConfigData> registries)
        {   super(registryAccess, registries);
        }
    }
}
