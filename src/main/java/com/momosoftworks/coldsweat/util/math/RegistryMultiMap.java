package com.momosoftworks.coldsweat.util.math;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Optimized for holding registries, with some key tweaks not typical of a Multimap:<br>
 * • Values are stored in a LinkedHashSet, so order is preserved.<br>
 * • {@code get()} always returns values assigned to the "null" key, in addition to those assigned to the given key.<br>
 */
public class RegistryMultiMap<K, V> implements Multimap<K, V>
{
    public RegistryMultiMap()
    {}

    public RegistryMultiMap(Multimap<K, V> multimap)
    {   putAll(multimap);
    }

    public RegistryMultiMap(Collection<Map.Entry<K, V>> entries)
    {
        for (Map.Entry<K, V> entry : entries)
        {   put(entry.getKey(), entry.getValue());
        }
    }

    private final FastMap<K, LinkedHashSet<V>> internal = new FastMap<>();
    private int totalSize = 0;

    @Override
    public int size()
    {
        return totalSize;
    }

    @Override
    public boolean isEmpty()
    {
        return totalSize == 0;
    }

    @Override
    public boolean containsKey(@Nullable Object key)
    {
        return internal.containsKey(key);
    }

    @Override
    public boolean containsValue(@Nullable Object value)
    {
        if (value == null || isEmpty())
        {
            return false;
        }
        for (Set<V> values : internal.values())
        {
            if (values.contains(value))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsEntry(@Nullable Object key, @Nullable Object value)
    {
        Set<V> values = internal.get(key);
        return values != null && values.contains(value);
    }

    @Override
    public boolean put(K key, V value)
    {
        Set<V> values = internal.computeIfAbsent(key, k -> new LinkedHashSet<>());
        if (values.add(value))
        {
            totalSize++;
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(@Nullable Object key, @Nullable Object value)
    {
        Set<V> values = internal.get(key);
        if (values != null && values.remove(value))
        {
            totalSize--;
            if (values.isEmpty())
            {
                internal.remove(key);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean putAll(K key, Iterable<? extends V> values)
    {
        boolean changed = false;
        for (V value : values)
        {
            changed |= put(key, value);
        }
        return changed;
    }

    @Override
    public boolean putAll(Multimap<? extends K, ? extends V> multimap)
    {
        boolean changed = false;
        for (Map.Entry<? extends K, ? extends V> entry : multimap.entries())
        {
            changed |= put(entry.getKey(), entry.getValue());
        }
        return changed;
    }

    public boolean putAll(Collection<Map.Entry<K, V>> entries)
    {
        boolean changed = false;
        for (Map.Entry<K, V> entry : entries)
        {
            changed |= put(entry.getKey(), entry.getValue());
        }
        return changed;
    }

    @Override
    public Set<V> replaceValues(K key, Iterable<? extends V> values)
    {
        Set<V> oldValues = internal.get(key);
        if (oldValues == null)
        {
           oldValues = new LinkedHashSet<>();
        }
        else
        {
            totalSize -= oldValues.size();
            oldValues.clear();
        }
        for (V value : values)
        {
            oldValues.add(value);
            totalSize++;
        }
        internal.put(key, oldValues);
        return oldValues;
    }

    @Override
    public Set<V> removeAll(@Nullable Object key)
    {
        Set<V> removed = internal.remove(key);
        if (removed != null)
        {
            totalSize -= removed.size();
        }
        return removed != null ? removed : new LinkedHashSet<>();
    }

    @Override
    public void clear()
    {
        internal.clear();
        totalSize = 0;
    }

    /**
     * Returns the values associated with this key, as well as those associated with the "null" key.
     */
    @Override
    public LinkedHashSet<V> get(K key)
    {
        LinkedHashSet<V> values = CSMath.orElse(internal.get(key), new LinkedHashSet<>());
        LinkedHashSet<V> nullValues = internal.get(null);
        if (nullValues != null)
        {   values.addAll(nullValues);
        }
        return values;
    }

    /**
     * Strictly returns values which are assigned to the key, excluding null entries.
     */
    public LinkedHashSet<V> getRaw(K key)
    {   return CSMath.orElse(internal.get(key), new LinkedHashSet<>());
    }

    @Override
    public Set<K> keySet()
    {
        return internal.keySet();
    }

    @Override
    public Multiset<K> keys()
    {
        HashMultiset<K> keys = HashMultiset.create();
        for (Map.Entry<K, LinkedHashSet<V>> entry : internal.entrySet())
        {
            keys.add(entry.getKey(), entry.getValue().size());
        }
        return keys;
    }

    @Override
    public Collection<V> values()
    {
        return new AbstractCollection<V>()
        {
            @Override
            public Iterator<V> iterator()
            {
                return new Iterator<V>()
                {
                    private final Iterator<LinkedHashSet<V>> setIterator = internal.values().iterator();
                    private Iterator<V> currentIterator = Collections.emptyIterator();

                    @Override
                    public boolean hasNext()
                    {
                        while (!currentIterator.hasNext() && setIterator.hasNext())
                        {
                            currentIterator = setIterator.next().iterator();
                        }
                        return currentIterator.hasNext();
                    }

                    @Override
                    public V next()
                    {
                        if (!hasNext())
                        {
                            throw new NoSuchElementException();
                        }
                        return currentIterator.next();
                    }

                    @Override
                    public void remove()
                    {
                        currentIterator.remove();
                    }
                };
            }

            @Override
            public int size()
            {
                return totalSize;
            }
        };
    }

    @Override
    public Collection<Map.Entry<K, V>> entries()
    {
        return new AbstractCollection<Map.Entry<K, V>>()
        {
            @Override
            public Iterator<Map.Entry<K, V>> iterator()
            {
                return new Iterator<Map.Entry<K, V>>()
                {
                    private final Iterator<Map.Entry<K, LinkedHashSet<V>>> entryIterator = internal.entrySet().iterator();
                    private Map.Entry<K, LinkedHashSet<V>> currentEntry;
                    private Iterator<V> valueIterator = Collections.emptyIterator();

                    @Override
                    public boolean hasNext()
                    {
                        while (!valueIterator.hasNext() && entryIterator.hasNext())
                        {
                            currentEntry = entryIterator.next();
                            valueIterator = currentEntry.getValue().iterator();
                        }
                        return valueIterator.hasNext();
                    }

                    @Override
                    public Map.Entry<K, V> next()
                    {
                        if (!hasNext())
                        {
                            throw new NoSuchElementException();
                        }
                        return new AbstractMap.SimpleEntry<>(currentEntry.getKey(), valueIterator.next());
                    }

                    @Override
                    public void remove()
                    {
                        valueIterator.remove();
                        if (currentEntry.getValue().isEmpty())
                        {
                            entryIterator.remove();
                        }
                    }
                };
            }

            @Override
            public int size()
            {
                return totalSize;
            }
        };
    }

    @Override
    public FastMap<K, Collection<V>> asMap()
    {
        FastMap<K, Collection<V>> map = new FastMap<>(internal.size());
        map.putAll(internal);
        return map;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {   return true;
        }
        if (obj instanceof Multimap)
        {
            Multimap<?, ?> other = (Multimap<?, ?>) obj;
            return internal.equals(other.asMap());
        }
        return false;
    }

    @Override
    public String toString()
    {   return internal.toString();
    }
}