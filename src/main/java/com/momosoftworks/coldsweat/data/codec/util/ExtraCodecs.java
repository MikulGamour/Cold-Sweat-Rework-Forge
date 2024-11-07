package com.momosoftworks.coldsweat.data.codec.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.world.entity.EquipmentSlot;

public class ExtraCodecs
{
    public static final Codec<EquipmentSlot> EQUIPMENT_SLOT = Codec.STRING.xmap(EquipmentSlot::byName, EquipmentSlot::getName);

    public static Codec<Object> anyOf(Codec... codecs)
    {
        return new Codec<>()
        {

            @Override
            public <T> DataResult<T> encode(Object input, DynamicOps<T> ops, T prefix)
            {
                for (Codec codec : codecs)
                {
                    DataResult<T> result = codec.encode(input, ops, prefix);
                    if (result.result().isPresent())
                    {
                        return result;
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
}
