package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.ModRegistries;
import com.momosoftworks.coldsweat.data.codec.requirement.NbtRequirement;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.Registry;

import java.util.List;
import java.util.Optional;

public class RemoveRegistryData<T>
{
    public final RegistryKey<Registry<?>> registry;
    public final List<CompoundNBT> entries;
    
    public RemoveRegistryData(RegistryKey<Registry<?>> registry, List<CompoundNBT> entries)
    {
        this.registry = registry;
        this.entries = entries;
    }

    public static final Codec<RemoveRegistryData<?>> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(ModRegistries::getRegistry, ModRegistries::getRegistryName).fieldOf("registry").forGetter(data -> data.registry),
            CompoundNBT.CODEC.listOf().fieldOf("matches").forGetter(data -> data.entries)
    ).apply(instance, RemoveRegistryData::new));

    public RegistryKey<Registry<T>> getRegistry()
    {   return (RegistryKey) registry;
    }

    public boolean matches(T object)
    {
        Optional<INBT> serialized = ModRegistries.getCodec((RegistryKey) registry).encodeStart(NBTDynamicOps.INSTANCE, object).result();
        if (serialized.isPresent())
        {
            for (CompoundNBT data : entries)
            {
                if (NbtRequirement.compareNbt(data, serialized.get(), true))
                {   return true;
                }
            }
        }
        return false;
    }
}
