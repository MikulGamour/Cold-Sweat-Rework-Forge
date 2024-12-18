package com.momosoftworks.coldsweat.data.codec.util;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

public class ExtraCodecs
{
    public static final Codec<EquipmentSlotType> EQUIPMENT_SLOT = Codec.STRING.xmap(EquipmentSlotType::byName, EquipmentSlotType::getName);

    private static final Map<String, RegistryKey<?>> REGISTRY_VALUES = Collections.synchronizedMap(Maps.newIdentityHashMap());

    public static <T> Codec<RegistryKey<T>> codec(RegistryKey<? extends Registry<T>> p_195967_)
    {
        return ResourceLocation.CODEC.xmap((p_195979_) -> {
            return create(p_195967_, p_195979_);
        }, RegistryKey::location);
    }

    public static <T> RegistryKey<T> create(RegistryKey<? extends Registry<T>> pRegistryKey, ResourceLocation pLocation)
    {
        String s = (pRegistryKey + ":" + pLocation).intern();
        return (RegistryKey<T>) REGISTRY_VALUES.computeIfAbsent(s, (p_195971_) -> {
            return RegistryKey.create(pRegistryKey, pLocation);
        });
    }

    public static Codec<Object> anyOf(Codec<?>... codecs)
    {
        return new Codec<Object>()
        {
            @Override
            public <T> DataResult<T> encode(Object input, DynamicOps<T> ops, T prefix)
            {
                for (Codec codec : codecs)
                {
                    try
                    {
                        DataResult<T> result = codec.encode(input, ops, prefix);
                        if (result.result().isPresent())
                        {
                            return result;
                        }
                    } catch (ClassCastException ignored)
                    {
                    }
                }
                return DataResult.error("No codecs could encode input " + input);
            }

            @Override
            public <T> DataResult<Pair<Object, T>> decode(DynamicOps<T> ops, T input)
            {
                for (Codec codec : codecs)
                {
                    DataResult<Pair<Object, T>> result = codec.decode(ops, input);
                    if (result.result().isPresent())
                    {
                        return result;
                    }
                }
                return DataResult.error("No codecs could decode input " + input);
            }
        };
    }

    public static <E> Codec<E> orCompressed(final Codec<E> pFirst, final Codec<E> pSecond)
    {
        return new Codec<E>()
        {
            public <T> DataResult<T> encode(E p_184483_, DynamicOps<T> p_184484_, T p_184485_)
            {
                return p_184484_.compressMaps()
                       ? pSecond.encode(p_184483_, p_184484_, p_184485_)
                       : pFirst.encode(p_184483_, p_184484_, p_184485_);
            }

            public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> p_184480_, T p_184481_)
            {
                return p_184480_.compressMaps()
                       ? pSecond.decode(p_184480_, p_184481_)
                       : pFirst.decode(p_184480_, p_184481_);
            }

            public String toString()
            {   return pFirst + " orCompressed " + pSecond;
            }
        };
    }

    public static <E> Codec<E> stringResolverCodec(Function<E, String> encoder, Function<String, E> decoder)
    {
        return Codec.STRING.flatXmap((p_184404_) -> {
            return Optional.ofNullable(decoder.apply(p_184404_)).map(DataResult::success).orElseGet(
                    () -> DataResult.error("Unknown element name:" + p_184404_));
        }, (p_184401_) -> {
            return Optional.ofNullable(encoder.apply(p_184401_)).map(DataResult::success).orElseGet(
                    () -> DataResult.error("Element with unknown name: " + p_184401_));
        });
    }

    public static <E> Codec<E> idResolverCodec(ToIntFunction<E> encoder, IntFunction<E> decoder, int id)
    {
        return Codec.INT.flatXmap((p_184414_) -> {
            return Optional.ofNullable(decoder.apply(p_184414_)).map(DataResult::success).orElseGet(() -> {
                return DataResult.error("Unknown element id: " + p_184414_);
            });
        }, (p_274850_) -> {
            int i = encoder.applyAsInt(p_274850_);
            return i == id ? DataResult.error("Element with unknown id: " + p_274850_) : DataResult.success(i);
        });
    }
}
