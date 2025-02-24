package com.momosoftworks.coldsweat.data.codec.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

import java.util.Objects;

public record DoubleBounds(double min, double max)
{
    public static final Codec<DoubleBounds> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("min", -Double.MAX_VALUE).forGetter(bounds -> bounds.min),
            Codec.DOUBLE.optionalFieldOf("max", Double.MAX_VALUE).forGetter(bounds -> bounds.max)
    ).apply(instance, DoubleBounds::new));

    public static final Codec<DoubleBounds> CODEC = Codec.either(DIRECT_CODEC, Codec.DOUBLE).xmap(
            either -> either.map(left -> left, right -> new DoubleBounds(right, right)),
            bounds -> bounds.max == bounds.min ? Either.right(bounds.min) : Either.left(bounds)
    );

    public static DoubleBounds NONE = new DoubleBounds(-Integer.MAX_VALUE, Integer.MAX_VALUE);

    public boolean test(double value)
    {   return value >= min && value <= max;
    }

    public boolean contains(DoubleBounds bounds)
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

        DoubleBounds that = (DoubleBounds) obj;

        if (!Objects.equals(min, that.min))
        {   return false;
        }
        return Objects.equals(max, that.max);
    }
}
