package com.momosoftworks.coldsweat.data.codec.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;

public interface ConfigData<T>
{
    BiMap<UUID, ConfigData<?>> IDENTIFIABLES = HashBiMap.create();

    Codec<T> getCodec();

    default UUID getId()
    {
        UUID id = IDENTIFIABLES.inverse().get(this);
        if (id == null)
        {   id = UUID.randomUUID();
            setId(id);
        }
        return id;
    }

    @ApiStatus.Internal
    default void setId(UUID id)
    {   IDENTIFIABLES.put(id, this);
    }

    default String asString()
    {   return this.getClass().getSimpleName() + getCodec().encodeStart(JsonOps.INSTANCE, (T) this).result().map(JsonElement::toString).orElse("");
    }
}