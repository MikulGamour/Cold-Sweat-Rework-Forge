package com.momosoftworks.coldsweat.data.codec.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

import java.util.Objects;

public record IntegerBounds(int min, int max)
{
    public static final Codec<IntegerBounds> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("min", -Integer.MAX_VALUE).forGetter(bounds -> bounds.min),
            Codec.INT.optionalFieldOf("max", Integer.MAX_VALUE).forGetter(bounds -> bounds.max)
    ).apply(instance, IntegerBounds::new));

    public static final Codec<IntegerBounds> CODEC = Codec.either(DIRECT_CODEC, Codec.INT).xmap(
            either -> either.map(left -> left, right -> new IntegerBounds(right, right)),
            bounds -> bounds.max == bounds.min ? Either.right(bounds.min) : Either.left(bounds)
    );

    public static IntegerBounds NONE = new IntegerBounds(-Integer.MAX_VALUE, Integer.MAX_VALUE);

    public boolean test(int value)
    {   return value >= min && value <= max;
    }

    public boolean contains(IntegerBounds bounds)
    {   return bounds.min >= min && bounds.max <= max;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {   return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {   return false;
        }

        IntegerBounds that = (IntegerBounds) obj;

        if (!Objects.equals(min, that.min))
        {   return false;
        }
        return Objects.equals(max, that.max);
    }
}
