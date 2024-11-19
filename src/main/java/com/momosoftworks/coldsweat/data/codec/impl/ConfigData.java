package com.momosoftworks.coldsweat.data.codec.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;

public interface ConfigData<T>
{
    BiMap<UUID, ConfigData<?>> IDENTIFIABLES = HashBiMap.create();

    Codec<T> getCodec();


    default UUID getId()
    {   return IDENTIFIABLES.inverse().computeIfAbsent(this, key -> UUID.randomUUID());
    }

    @ApiStatus.Internal
    default void setId(UUID id)
    {   IDENTIFIABLES.put(id, this);
    }

    default String asString()
    {   return this.getClass().getSimpleName() + getCodec().encodeStart(JsonOps.INSTANCE, (T) this).result().map(JsonElement::toString).orElse("");
    }
}