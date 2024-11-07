package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import com.momosoftworks.coldsweat.util.serialization.StringRepresentable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.nbt.StringNBT;

import java.util.*;

public class FuelData implements NbtSerializable
{
    public final FuelType type;
    public final Double fuel;
    public final ItemRequirement data;
    public final Optional<List<String>> requiredMods;

    public FuelData(FuelType type, Double fuel, ItemRequirement data, Optional<List<String>> requiredMods)
    {
        this.type = type;
        this.fuel = fuel;
        this.data = data;
        this.requiredMods = requiredMods;
    }
    public static final Codec<FuelData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FuelType.CODEC.fieldOf("type").forGetter(data -> data.type),
            Codec.DOUBLE.fieldOf("fuel").forGetter(data -> data.fuel),
            ItemRequirement.CODEC.fieldOf("data").forGetter(data -> data.data),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(data -> data.requiredMods)
    ).apply(instance, FuelData::new));

    @Override
    public CompoundNBT serialize()
    {
        return (CompoundNBT) CODEC.encodeStart(NBTDynamicOps.INSTANCE, this).result().orElseGet(CompoundNBT::new);
    }

    public static FuelData deserialize(CompoundNBT nbt)
    {
        return CODEC.decode(NBTDynamicOps.INSTANCE, nbt).result().orElseThrow(() -> new IllegalStateException("Failed to deserialize FuelData")).getFirst();
    }

    public enum FuelType implements StringRepresentable
    {
        BOILER("boiler"),
        ICEBOX("icebox"),
        HEARTH("hearth"),
        SOUL_LAMP("soul_lamp");

        public static Codec<FuelType> CODEC = StringRepresentable.fromEnum(FuelType::values);

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
    public String toString()
    {
        return CODEC.encodeStart(NBTDynamicOps.INSTANCE, this).result().map(Object::toString).orElse("");
    }
}
