package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.impl.RequirementHolder;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.NbtRequirement;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
import com.momosoftworks.coldsweat.util.serialization.StringRepresentable;
import net.minecraft.tags.ITag;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

public class FuelData extends ConfigData implements RequirementHolder
{
    final FuelType type;
    final Double fuel;
    final ItemRequirement data;
    final Optional<List<String>> requiredMods;

    public FuelData(FuelType type, Double fuel, ItemRequirement data, Optional<List<String>> requiredMods)
    {
        this.type = type;
        this.fuel = fuel;
        this.data = data;
        this.requiredMods = requiredMods;
    }

    public FuelData(FuelType type, Double fuel, ItemRequirement data)
    {   this(type, fuel, data, Optional.empty());
    }

    public static final Codec<FuelData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FuelType.CODEC.fieldOf("type").forGetter(data -> data.type),
            Codec.DOUBLE.fieldOf("fuel").forGetter(data -> data.fuel),
            ItemRequirement.CODEC.fieldOf("data").forGetter(data -> data.data),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(data -> data.requiredMods)
    ).apply(instance, FuelData::new));

    public FuelType type()
    {   return type;
    }
    public Double fuel()
    {   return fuel;
    }
    public ItemRequirement data()
    {   return data;
    }
    public Optional<List<String>> requiredMods()
    {   return requiredMods;
    }

    @Override
    public boolean test(ItemStack stack)
    {   return data.test(stack, true);
    }

    @Nullable
    public static FuelData fromToml(List<?> entry, FuelType fuelType)
    {
        if (entry.size() < 2)
        {   return null;
        }
        String[] itemIDs = ((String) entry.get(0)).split(",");
        List<Either<ITag<Item>, Item>> items = ConfigHelper.getItems(itemIDs);
        double fuel = ((Number) entry.get(1)).doubleValue();
        NbtRequirement nbtRequirement = entry.size() > 2
                                        ? new NbtRequirement(NBTHelper.parseCompoundNbt((String) entry.get(3)))
                                        : new NbtRequirement(new CompoundNBT());
        ItemRequirement itemRequirement = new ItemRequirement(items, nbtRequirement);
        return new FuelData(fuelType, fuel, itemRequirement, Optional.empty());
    }

    @Override
    public Codec<FuelData> getCodec()
    {   return CODEC;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        FuelData that = (FuelData) obj;
        return fuel.equals(that.fuel)
            && data.equals(that.data)
            && requiredMods.equals(that.requiredMods);
    }

    public enum FuelType implements StringRepresentable
    {
        BOILER("boiler"),
        ICEBOX("icebox"),
        HEARTH("hearth"),
        SOUL_LAMP("soulspring_lamp");

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
}
