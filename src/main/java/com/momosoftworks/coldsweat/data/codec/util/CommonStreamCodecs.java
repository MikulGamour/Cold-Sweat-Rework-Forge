package com.momosoftworks.coldsweat.data.codec.util;

import com.mojang.datafixers.util.Either;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.*;

public class CommonStreamCodecs
{
    public static final StreamCodec<FriendlyByteBuf, Double> DOUBLE = StreamCodec.of(
            FriendlyByteBuf::writeDouble,
            FriendlyByteBuf::readDouble
    );

    public static <T, B extends FriendlyByteBuf> StreamCodec<B, Collection<T>> listCodec(StreamCodec<B, T> elementCodec)
    {
        return StreamCodec.of((buf, list) ->
        {
            buf.writeVarInt(list.size());
            list.forEach(element -> elementCodec.encode(buf, element));
        },
        buf ->
        {
            int size = buf.readVarInt();
            Collection<T> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++)
            {   list.add(elementCodec.decode(buf));
            }
            return list;
        });
    }

    public static <T> StreamCodec<FriendlyByteBuf, TagKey<T>> tagKeyCodec(ResourceKey<? extends Registry<T>> key)
    {
        return StreamCodec.of((buf, tagkey) ->
                              {
                                  buf.writeResourceKey(tagkey.registry());
                                  buf.writeResourceLocation(tagkey.location());
                              },
                              buf ->
                              {
                                  ResourceKey<? extends Registry<T>> registry = buf.readResourceKey(key).registryKey();
                                  ResourceLocation location = buf.readResourceLocation();
                                  return TagKey.create(registry, location);
                              });
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, Either<TagKey<T>, T>> tagOrRegistryCodec(ResourceKey<? extends Registry<T>> key)
    {
        return StreamCodec.of((buf, either) ->
                              {
                                  buf.writeBoolean(either.left().isPresent());
                                  either.ifLeft(tag -> buf.writeUtf(tag.location().toString()))
                                        .ifRight(reg -> ByteBufCodecs.registry(key).encode(buf, reg));
                              },
                              buf ->
                              {
                                    if (buf.readBoolean())
                                    {   return Either.left(TagKey.create(key, ResourceLocation.parse(buf.readUtf())));
                                    }
                                    else
                                    {   return Either.right(ByteBufCodecs.registry(key).decode(buf));
                                    }
                              });
    }

    public static <T, B extends FriendlyByteBuf> Optional<T> readOptional(B buf, StreamDecoder<B, T> reader)
    {
        if (buf.readBoolean())
        {   return Optional.of(reader.decode(buf));
        }
        else
        {   return Optional.empty();
        }
    }

    public static <T, B extends FriendlyByteBuf> void writeOptional(B buf, Optional<T> optional, StreamEncoder<B, T> writer)
    {
        if (optional.isPresent())
        {
            buf.writeBoolean(true);
            writer.encode(buf, optional.get());
        }
        else
        {   buf.writeBoolean(false);
        }
    }

    public static <T, B extends FriendlyByteBuf> Optional<List<T>> readOptionalList(B buf, StreamDecoder<B, T> reader)
    {
        if (buf.readBoolean())
        {
            int size = buf.readVarInt();
            List<T> collection = new java.util.ArrayList<>(size);
            for (int i = 0; i < size; i++)
            {   collection.add(reader.decode(buf));
            }
            return Optional.of(collection);
        }
        else
        {   return Optional.empty();
        }
    }

    public static <T, B extends FriendlyByteBuf> void writeOptionalList(B buf, Optional<? extends List<T>> optional, StreamEncoder<B, T> writer)
    {
        if (optional.isPresent())
        {
            List<T> collection = optional.get();

            buf.writeBoolean(true);
            buf.writeVarInt(optional.get().size());
            for (T t : collection)
            {   writer.encode(buf, t);
            }
        }
        else
        {   buf.writeBoolean(false);
        }
    }

    public static <K, V, U extends FriendlyByteBuf, W extends FriendlyByteBuf> void writeMap(FriendlyByteBuf buf, Map<K, V> map, StreamEncoder<U, ? super K> keyWriter, StreamEncoder<W, ? super V> valueWriter)
    {
        buf.writeVarInt(map.size());
        for (Map.Entry<K, V> entry : map.entrySet()) {
            keyWriter.encode((U) buf, entry.getKey());
            valueWriter.encode((W) buf, entry.getValue());
        }
    }

    public static <K, V, U extends FriendlyByteBuf, W extends FriendlyByteBuf> Map<K, V> readMap(FriendlyByteBuf buf, StreamDecoder<U, K> keyReader, StreamDecoder<W, V> valueReader)
    {
        int size = buf.readVarInt();
        Map<K, V> map = new HashMap<>(size);
        for (int i = 0; i < size; i++)
        {
            K key = keyReader.decode((U) buf);
            V value = valueReader.decode((W) buf);
            map.put(key, value);
        }
        return map;
    }
}
