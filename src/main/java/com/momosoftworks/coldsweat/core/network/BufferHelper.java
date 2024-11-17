package com.momosoftworks.coldsweat.core.network;

import com.google.common.collect.Maps;
import net.minecraft.network.PacketBuffer;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;

public class BufferHelper
{
    public static <T> void writeOptional(PacketBuffer buffer, Optional<T> object, BiConsumer<PacketBuffer, T> writer)
    {
        if (object.isPresent())
        {
            buffer.writeBoolean(true);
            writer.accept(buffer, object.get());
        }
        else
        {   buffer.writeBoolean(false);
        }
    }

    public static <T> Optional<T> readOptional(PacketBuffer buffer, Function<PacketBuffer, T> reader)
    {   return buffer.readBoolean() ? Optional.of(reader.apply(buffer)) : Optional.empty();
    }

    public static <K, V> void writeMap(PacketBuffer buffer, Map<K, V> pMap, BiConsumer<PacketBuffer, K> pKeyWriter, BiConsumer<PacketBuffer, V> pValueWriter)
    {
        buffer.writeVarInt(pMap.size());
        pMap.forEach((p_178362_, p_178363_) ->
        {
            pKeyWriter.accept(buffer, p_178362_);
            pValueWriter.accept(buffer, p_178363_);
        });
    }

    public static <K, V, M extends Map<K, V>> M readMap(PacketBuffer buffer, IntFunction<M> mapFactory, Function<PacketBuffer, K> keyReader, Function<PacketBuffer, V> valueReader)
    {
        int i = buffer.readVarInt();
        M m = mapFactory.apply(i);

        for(int j = 0; j < i; ++j) {
            K k = keyReader.apply(buffer);
            V v = valueReader.apply(buffer);
            m.put(k, v);
        }

        return m;
    }

    public static <K, V> Map<K, V> readMap(PacketBuffer buffer, Function<PacketBuffer, K> pKeyReader, Function<PacketBuffer, V> pValueReader) {
        return readMap(buffer, Maps::newHashMapWithExpectedSize, pKeyReader, pValueReader);
    }

    public static <T> void writeCollection(PacketBuffer buffer, Collection<T> pCollection, BiConsumer<PacketBuffer, T> pElementWriter)
    {
        buffer.writeVarInt(pCollection.size());

        for (T t : pCollection)
        {   pElementWriter.accept(buffer, t);
        }
    }

    public static <T, C extends Collection<T>> C readCollection(PacketBuffer buffer, IntFunction<C> collectionFactory, Function<PacketBuffer, T> pElementReader)
    {
        int i = buffer.readVarInt();
        C collection = collectionFactory.apply(i);

        for (int j = 0; j < i; ++j)
        {   collection.add(pElementReader.apply(buffer));
        }
        return collection;
    }
}
