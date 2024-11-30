package com.momosoftworks.coldsweat.data.codec.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import org.jetbrains.annotations.ApiStatus;

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

    @ApiStatus.Internal
    public void setId(UUID id)
    {   this.id = id;
    }

    @ApiStatus.Internal
    public void setType(Type type)
    {   this.type = type;
    }

    @Override
    public CompoundTag serialize()
    {   return (CompoundTag) ((Codec<ConfigData>) this.getCodec()).encodeStart(NbtOps.INSTANCE, this).result().orElse(new CompoundTag());
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
