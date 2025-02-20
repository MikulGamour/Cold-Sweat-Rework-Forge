package com.momosoftworks.coldsweat.common.capability;

import com.momosoftworks.coldsweat.util.math.FastMap;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CapabilityCache<C, K extends ICapabilityProvider>
{
    protected final Map<K, LazyOptional<C>> cache = new FastMap<>();
    protected final Supplier<Capability<C>> capability;

    public CapabilityCache(Supplier<Capability<C>> capability)
    {   this.capability = capability;
    }

    public LazyOptional<C> get(K key)
    {
        return cache.computeIfAbsent(key, e ->
        {
            LazyOptional<C> cap = e.getCapability(capability.get());
            cap.addListener((opt) -> cache.remove(e));
            return cap;
        });
    }

    public void remove(K key)
    {   cache.remove(key);
    }

    public void clear()
    {   cache.clear();
    }

    public void ifPresent(K key, Consumer<C> consumer)
    {
        LazyOptional<C> cap = cache.get(key);
        if (cap != null && cap.resolve().isPresent())
        {   consumer.accept(cap.resolve().get());
        }
    }

    public void ifLazyPresent(K key, Consumer<LazyOptional<C>> consumer)
    {
        LazyOptional<C> cap = cache.get(key);
        if (cap != null)
        {   consumer.accept(cap);
        }
    }
}
