package com.momosoftworks.coldsweat.common.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.util.thread.EffectiveSide;

import java.util.function.Consumer;

public class SidedCapabilityCache<C, K extends ICapabilityProvider> extends CapabilityCache<C, K>
{
    private final CapabilityCache<C, K> clientCache = new CapabilityCache<>(this.capability);

    public SidedCapabilityCache(Capability<C> capability)
    {   super(capability);
    }

    @Override
    public LazyOptional<C> get(K key)
    {
        boolean isClient = EffectiveSide.get().isClient();
        return isClient ? clientCache.get(key) : super.get(key);
    }

    public void clearClient()
    {   clientCache.clear();
    }

    public void clearServer()
    {   super.clear();
    }

    @Override
    public void clear()
    {
        if (EffectiveSide.get().isClient())
        {   this.clearClient();
        }
        else
        {   this.clearServer();
        }
    }

    @Override
    public void ifPresent(K key, Consumer<C> consumer)
    {
        boolean isClient = EffectiveSide.get().isClient();
        if (isClient)
        {   clientCache.ifPresent(key, consumer);
        }
        else
        {   super.ifPresent(key, consumer);
        }
    }

    @Override
    public void ifLazyPresent(K key, Consumer<LazyOptional<C>> consumer)
    {
        boolean isClient = EffectiveSide.get().isClient();
        if (isClient)
        {   clientCache.ifLazyPresent(key, consumer);
        }
        else
        {   super.ifLazyPresent(key, consumer);
        }
    }
}
