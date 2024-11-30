package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.ModRegistries;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.requirement.NbtRequirement;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.Registry;

import java.util.List;
import java.util.Optional;

public class RemoveRegistryData<T extends ConfigData> extends ConfigData
{
    private final RegistryKey<Registry<T>> registry;
    private final List<CompoundNBT> entries;

    public RemoveRegistryData(RegistryKey<Registry<T>> registry, List<CompoundNBT> entries)
    {
        this.registry = registry;
        this.entries = entries;
    }

    public static final Codec<RemoveRegistryData<? extends ConfigData>> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(ModRegistries::getRegistry, ModRegistries::getRegistryName).fieldOf("registry").forGetter(data -> (RegistryKey) data.registry),
            CompoundNBT.CODEC.listOf().fieldOf("matches").forGetter(data -> data.entries)
    ).apply(instance, (key, ent) -> new RemoveRegistryData<>((RegistryKey) key, (List<CompoundNBT>) ent)));

    public RegistryKey<Registry<T>> registry()
    {   return registry;
    }
    public List<CompoundNBT> entries()
    {   return entries;
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

    @Override
    public Codec<? extends ConfigData> getCodec()
    {   return CODEC;
    }
}
