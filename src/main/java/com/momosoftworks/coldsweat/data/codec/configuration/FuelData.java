package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.impl.RequirementHolder;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.NbtRequirement;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.*;

public record FuelData(FuelType type, Double fuel,
                       ItemRequirement data, Optional<List<String>> requiredMods) implements NbtSerializable, RequirementHolder, ConfigData<FuelData>, IForgeRegistryEntry<FuelData>
{
    public FuelData(FuelType type, Double fuel, ItemRequirement data)
    {   this(type, fuel, data, Optional.empty());
    }

    public static final Codec<FuelData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FuelType.CODEC.fieldOf("type").forGetter(FuelData::type),
            Codec.DOUBLE.fieldOf("fuel").forGetter(FuelData::fuel),
            ItemRequirement.CODEC.fieldOf("data").forGetter(FuelData::data),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(FuelData::requiredMods)
    ).apply(instance, FuelData::new));

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
        List<Item> items = ConfigHelper.getItems(itemIDs);
        double fuel = ((Number) entry.get(1)).doubleValue();
        NbtRequirement nbtRequirement = entry.size() > 2
                                        ? new NbtRequirement(NBTHelper.parseCompoundNbt((String) entry.get(3)))
                                        : new NbtRequirement(new CompoundTag());
        ItemRequirement itemRequirement = new ItemRequirement(items, nbtRequirement);
        return new FuelData(fuelType, fuel, itemRequirement, Optional.empty());
    }

    @Override
    public CompoundTag serialize()
    {   return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
    }

    public static FuelData deserialize(CompoundTag tag)
    {   return CODEC.decode(NbtOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalStateException("Failed to deserialize FuelData")).getFirst();
    }

    @Override
    public Codec<FuelData> getCodec()
    {   return CODEC;
    }

    @Override
    public String toString()
    {   return this.asString();
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
}
