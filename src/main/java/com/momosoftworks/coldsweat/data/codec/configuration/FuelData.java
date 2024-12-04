package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.impl.RequirementHolder;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemComponentsRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.NbtRequirement;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.core.registries.BuiltInRegistries;
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

    public FuelData(FuelType type, Double fuel, ItemRequirement data, List<String> requiredMods)
    {
        super(requiredMods);
        this.type = type;
        this.fuel = fuel;
        this.data = data;
    }

    public FuelData(FuelType type, Double fuel, ItemRequirement data)
    {
        this(type, fuel, data, ConfigHelper.getModIDs(CSMath.listOrEmpty(data.items()), BuiltInRegistries.ITEM));
    }

    public static final Codec<FuelData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FuelType.CODEC.fieldOf("type").forGetter(FuelData::type),
            Codec.DOUBLE.fieldOf("fuel").forGetter(FuelData::fuel),
            ItemRequirement.CODEC.fieldOf("data").forGetter(FuelData::data),
            Codec.STRING.listOf().optionalFieldOf("required_mods", List.of()).forGetter(FuelData::requiredMods)
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

    @Override
    public boolean test(ItemStack stack)
    {   return data.test(stack, true);
    }

    @Nullable
    public static FuelData fromToml(List<?> entry, FuelType fuelType)
    {
        if (entry.size() < 2)
        {   ColdSweat.LOGGER.error("Error parsing entity config: not enough arguments");
            return null;
        }
        List<Either<TagKey<Item>, Item>> items = ConfigHelper.getItems((String) entry.get(0));

        if (items.isEmpty())
        {   ColdSweat.LOGGER.error("Error parsing fuel config: {} does not contain any valid items", entry);
            return null;
        }
        double fuel = ((Number) entry.get(1)).doubleValue();
        ItemComponentsRequirement componentsRequirement = entry.size() > 2
                                                          ? ItemComponentsRequirement.parse((String) entry.get(2))
                                                          : new ItemComponentsRequirement();
        ItemRequirement itemRequirement = new ItemRequirement(items, componentsRequirement);

        return new FuelData(fuelType, fuel, itemRequirement);
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
        return super.equals(obj)
            && fuel.equals(that.fuel)
            && data.equals(that.data);
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
