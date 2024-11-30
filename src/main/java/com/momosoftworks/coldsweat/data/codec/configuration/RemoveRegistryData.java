package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.ModRegistries;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.requirement.NbtRequirement;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class RemoveRegistryData<T extends ConfigData> extends ConfigData implements IForgeRegistryEntry<RemoveRegistryData<?>>
{
    ResourceKey<Registry<T>> registry;
    List<CompoundTag> entries;

    public RemoveRegistryData(ResourceKey<Registry<T>> registry, List<CompoundTag> entries)
    {
        this.registry = registry;
        this.entries = entries;
    }

    public static final Codec<RemoveRegistryData<?>> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(s -> (ResourceKey)ModRegistries.getRegistry(s), key -> ModRegistries.getRegistryName(key)).fieldOf("registry").forGetter(data -> data.registry()),
            CompoundTag.CODEC.listOf().fieldOf("matches").forGetter(data -> data.entries)
    ).apply(instance, RemoveRegistryData::new));

    public ResourceKey<Registry<T>> registry()
    {   return registry;
    }
    public List<CompoundTag> entries()
    {   return entries;
    }

    public boolean matches(T object)
    {
        Optional<Tag> serialized = ModRegistries.getCodec((ResourceKey) registry).encodeStart(NbtOps.INSTANCE, object).result();
        if (serialized.isPresent())
        {
            for (CompoundTag data : entries)
            {
                if (NbtRequirement.compareNbt(data, serialized.get(), true))
                {   return true;
                }
            }
        }
        return false;
    }

    @Override
    public Codec<? extends ConfigData> getCodec()
    {   return CODEC;
    }

    @Override
    public RemoveRegistryData<?> setRegistryName(ResourceLocation resourceLocation)
    {
        return null;
    }

    @Override
    public @Nullable ResourceLocation getRegistryName()
    {
        return null;
    }

    @Override
    public Class<RemoveRegistryData<?>> getRegistryType()
    {
        return null;
    }
}
