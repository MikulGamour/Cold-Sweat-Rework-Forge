package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemComponentsRequirement;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.impl.RequirementHolder;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.*;

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
            FuelType.CODEC.fieldOf("type").forGetter(FuelData::type),
            Codec.DOUBLE.fieldOf("fuel").forGetter(FuelData::fuel),
            ItemRequirement.CODEC.fieldOf("data").forGetter(FuelData::data),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(FuelData::requiredMods)
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
        List<Either<TagKey<Item>, Item>> items = ConfigHelper.getItems(itemIDs);
        double fuel = ((Number) entry.get(1)).doubleValue();
        ItemComponentsRequirement componentsRequirement = entry.size() > 2
                                                          ? ItemComponentsRequirement.parse((String) entry.get(2))
                                                          : new ItemComponentsRequirement();
        ItemRequirement itemRequirement = new ItemRequirement(items, componentsRequirement);
        return new FuelData(fuelType, fuel, itemRequirement, java.util.Optional.empty());
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
