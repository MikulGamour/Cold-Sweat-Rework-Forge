package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.*;

public record FuelData(FuelType type, Double fuel,
                       ItemRequirement data, Optional<List<String>> requiredMods) implements NbtSerializable, IForgeRegistryEntry<FuelData>
{
    public static final Codec<FuelData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FuelType.CODEC.fieldOf("type").forGetter(FuelData::type),
            Codec.DOUBLE.fieldOf("fuel").forGetter(FuelData::fuel),
            ItemRequirement.CODEC.fieldOf("data").forGetter(FuelData::data),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(FuelData::requiredMods)
    ).apply(instance, FuelData::new));

    @Override
    public CompoundTag serialize()
    {
        return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
    }

    public static FuelData deserialize(CompoundTag nbt)
    {
        return CODEC.decode(NbtOps.INSTANCE, nbt).result().orElseThrow(() -> new IllegalStateException("Failed to deserialize FuelData")).getFirst();
    }

    public enum FuelType implements StringRepresentable
    {
        BOILER("boiler"),
        ICEBOX("icebox"),
        HEARTH("hearth"),
        SOUL_LAMP("soul_lamp");

        public static Codec<FuelType> CODEC = StringRepresentable.fromEnum(FuelType::values, FuelType::byName);

        private final String name;

        FuelType(String name)
        {   this.name = name;
        }

        @Override
        public String getSerializedName()
        {   return name;
        }

        public static FuelType byName(String name)
        {   for (FuelType type : values())
            {   if (type.name.equals(name)) return type;
            }
            return null;
        }
    }

    @Override
    public FuelData setRegistryName(ResourceLocation name)
    {
        return null;
    }

    @Override
    public ResourceLocation getRegistryName()
    {
        return null;
    }

    @Override
    public Class<FuelData> getRegistryType()
    {
        return FuelData.class;
    }

    @Override
    public String toString()
    {
        return CODEC.encodeStart(NbtOps.INSTANCE, this).result().map(Object::toString).orElse("");
    }
}
