package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;

public class EntityDropData implements NbtSerializable
{
    private final int interval;
    private final int cooldown;
    private final double chance;

    public EntityDropData(int interval, int cooldown, double chance)
    {
        this.interval = interval;
        this.cooldown = cooldown;
        this.chance = chance;
    }
    public static final Codec<EntityDropData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.INT.fieldOf("interval").forGetter(EntityDropData::interval),
            Codec.INT.fieldOf("cooldown").forGetter(EntityDropData::cooldown),
            Codec.DOUBLE.fieldOf("chance").forGetter(EntityDropData::chance)
    ).apply(builder, EntityDropData::new));

    public int interval()
    {   return interval;
    }
    public int cooldown()
    {   return cooldown;
    }
    public double chance()
    {   return chance;
    }

    public CompoundNBT serialize()
    {   return (CompoundNBT) CODEC.encodeStart(NBTDynamicOps.INSTANCE, this).result().orElse(new CompoundNBT());
    }

    public static EntityDropData deserialize(CompoundNBT nbt)
    {   return CODEC.decode(NBTDynamicOps.INSTANCE, nbt).result().map(Pair::getFirst).orElseThrow(() -> new IllegalArgumentException("Could not deserialize EntityDropData"));
    }
}
