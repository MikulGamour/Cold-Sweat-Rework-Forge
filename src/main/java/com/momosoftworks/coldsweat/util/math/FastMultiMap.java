package com.momosoftworks.coldsweat.util.math;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FastMultiMap<K, V> implements Multimap<K, V>
{
    public FastMultiMap()
    {}

    public FastMultiMap(Multimap<K, V> multimap)
    {   putAll(multimap);
    }

    public FastMultiMap(Collection<Map.Entry<K, V>> entries)
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

    @Override
    public Set<V> replaceValues(K key, Iterable<? extends V> values)
    {
        Set<V> oldValues = internal.get(key);
        if (oldValues == null)
        {
            oldValues = new HashSet<>();
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
        return removed != null ? removed : new HashSet<>();
    }

    @Override
    public void clear()
    {
        internal.clear();
        totalSize = 0;
    }

    @Override
    public LinkedHashSet<V> get(K key)
    {
        LinkedHashSet<V> values = internal.get(key);
        return values != null ? values : new LinkedHashSet<>();
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
        for (Map.Entry<K, LinkedHashSet<V>> entry : internal.entrySet())
        {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    @Override
    public String toString()
    {   return internal.toString();
    }
}