package com.momosoftworks.coldsweat.api.insulation;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.checkerframework.checker.units.qual.C;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class Insulation implements NbtSerializable
{
    private static Codec<Insulation> CODEC = null;
    private static StreamCodec<FriendlyByteBuf, Insulation> STREAM_CODEC = null;
    public static Codec<Insulation> getCodec()
    {
        if (CODEC == null)
            CODEC = Codec.either(StaticInsulation.CODEC, AdaptiveInsulation.CODEC).xmap(
                    either -> either.map(stat -> stat, adapt -> adapt),
                    insul -> insul instanceof StaticInsulation ? Either.left((StaticInsulation) insul) : Either.right((AdaptiveInsulation) insul));
        return CODEC;
    }

    public static StreamCodec<FriendlyByteBuf, Insulation> getNetworkCodec()
    {
        if (STREAM_CODEC == null)
        STREAM_CODEC = StreamCodec.of(
        (buf, insul) ->
        {
            if (insul instanceof StaticInsulation st)
            {
                buf.writeUtf("static");
                buf.writeDouble(st.getCold());
                buf.writeDouble(st.getHeat());
            }
            else if (insul instanceof AdaptiveInsulation ad)
            {
                buf.writeUtf("adaptive");
                buf.writeDouble(ad.getInsulation());
                buf.writeDouble(ad.getFactor());
                buf.writeDouble(ad.getSpeed());
            }
            else buf.writeUtf("none");
        },
        (buf) ->
        {
            String type = buf.readUtf();
            if (type.equals("static"))
            {   return new StaticInsulation(buf.readDouble(), buf.readDouble());
            }
            else if (type.equals("adaptive"))
            {   return new AdaptiveInsulation(buf.readDouble(), buf.readDouble(), buf.readDouble());
            }
            return null;
        });
        return STREAM_CODEC;
    }

    /**
     * @return True if this insulation has no value.
     */
    public abstract boolean isEmpty();

    /**
     * If this insulation is bigger than one slot, split it into multiple insulations.
     * @return A list of insulations.
     */
    public abstract List<Insulation> split();

    public abstract double getCold();
    public abstract double getHeat();

    /**
     * Sort the list of insulation items, starting with cold insulation, then neutral, then heat, then adaptive.<br>
     * This method does not modify the input list
     * @return A sorted list of insulation items.
     */
    public static List<Insulation> sort(List<Insulation> pairs)
    {
        List<Insulation> newPairs = new ArrayList<>(pairs);
        newPairs.sort(Comparator.comparingDouble(Insulation::getCompareValue));
        return newPairs;
    }

    public int getCompareValue()
    {
        if (this instanceof AdaptiveInsulation insul)
        {   return Math.abs(insul.getInsulation()) >= 2 ? 6 : 7;
        }
        else if (this instanceof StaticInsulation insul)
        {
            double absCold = Math.abs(insul.getCold());
            double absHot = Math.abs(insul.getHeat());
            if (absCold >= 2 && absHot >= 2)
                return 2;
            else if (absCold >= 2)
                return 0;
            else if (absHot >= 2)
                return 4;
            else if (absCold >= 1 && absHot >= 1)
                return 3;
            else if (absCold >= 1)
                return 1;
            else if (absHot >= 1)
                return 5;
            else
                return 1;
        }
        return 0;
    }

    public static Insulation deserialize(CompoundTag tag)
    {
        if (tag.contains("cold") && tag.contains("heat"))
        {   return StaticInsulation.deserialize(tag);
        }
        else if (tag.contains("insulation"))
        {   return AdaptiveInsulation.deserialize(tag);
        }
        return null;
    }

    public enum Slot implements StringRepresentable
    {
        ITEM("item"),
        CURIO("curio"),
        ARMOR("armor");

        final String name;

        Slot(String name)
        {   this.name = name;
        }

        public static final Codec<Slot> CODEC = Codec.STRING.xmap(Slot::byName, Slot::getSerializedName);

        @Override
        public String getSerializedName()
        {   return name;
        }

        @Nullable
        public static Slot byName(String name)
        {
            for (Slot type : values())
            {   if (type.name.equals(name))
                {   return type;
                }
            }
            return null;
        }
    }

    public enum Type implements StringRepresentable
    {
        COLD("cold"),
        HEAT("heat"),
        NEUTRAL("neutral"),
        ADAPTIVE("adaptive");

        final String name;

        Type(String name)
        {   this.name = name;
        }

        public static final Codec<Type> CODEC = Codec.STRING.xmap(Type::byName, Type::getSerializedName);

        @Override
        public String getSerializedName()
        {   return name;
        }

        public static Type byName(String name)
        {   for (Type type : values())
            {   if (type.name.equals(name))
                {   return type;
                }
            }
            throw new IllegalArgumentException("Unknown insulation type: " + name);
        }
    }
}
