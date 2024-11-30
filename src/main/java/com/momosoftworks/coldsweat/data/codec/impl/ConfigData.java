package com.momosoftworks.coldsweat.data.codec.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;

import java.util.UUID;

public abstract class ConfigData implements NbtSerializable
{
    private UUID id;
    private Type type;

    public abstract Codec<? extends ConfigData> getCodec();

    public UUID getId()
    {
        if (id == null)
        {   id = UUID.randomUUID();
        }
        return id;
    }

    public Type getType()
    {   return type;
    }

    public void setId(UUID id)
    {   this.id = id;
    }

    public void setType(Type type)
    {   this.type = type;
    }

    @Override
    public CompoundNBT serialize()
    {   return (CompoundNBT) ((Codec<ConfigData>) this.getCodec()).encodeStart(NBTDynamicOps.INSTANCE, this).result().orElse(new CompoundNBT());
    }

    @Override
    public String toString()
    {   return this.getClass().getSimpleName() + ((Codec) getCodec()).encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("");
    }

    public enum Type
    {
        TOML,
        JSON,
        KUBEJS
    }
}
