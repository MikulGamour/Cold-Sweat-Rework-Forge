package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.impl.RequirementHolder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.NbtRequirement;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class FoodData extends ConfigData implements RequirementHolder, IForgeRegistryEntry<FoodData>
{
    final Double temperature;
    final ItemRequirement data;
    final int duration;
    final EntityRequirement entityRequirement;
    final Optional<List<String>> requiredMods;

    public FoodData(Double temperature, ItemRequirement data, int duration,
                    EntityRequirement entityRequirement, Optional<List<String>> requiredMods)
    {
        this.temperature = temperature;
        this.data = data;
        this.duration = duration;
        this.entityRequirement = entityRequirement;
        this.requiredMods = requiredMods;
    }

    public FoodData(Double temperature, ItemRequirement data, int duration, EntityRequirement entityRequirement)
    {   this(temperature, data, duration, entityRequirement, Optional.empty());
    }

    public static final Codec<FoodData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("temperature").forGetter(FoodData::temperature),
            ItemRequirement.CODEC.fieldOf("data").forGetter(FoodData::data),
            Codec.INT.optionalFieldOf("duration", -1).forGetter(FoodData::duration),
            EntityRequirement.getCodec().optionalFieldOf("entity", EntityRequirement.NONE).forGetter(FoodData::entityRequirement),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(FoodData::requiredMods)
    ).apply(instance, FoodData::new));

    public Double temperature()
    {   return temperature;
    }
    public ItemRequirement data()
    {   return data;
    }
    public int duration()
    {   return duration;
    }
    public EntityRequirement entityRequirement()
    {   return entityRequirement;
    }
    public Optional<List<String>> requiredMods()
    {   return requiredMods;
    }

    @Override
    public boolean test(ItemStack stack)
    {   return data.test(stack, true);
    }

    @Override
    public boolean test(Entity entity)
    {   return entityRequirement.test(entity);
    }

    @Nullable
    public static FoodData fromToml(List<?> entry)
    {
        if (entry.size() < 2)
        {   return null;
        }
        String[] itemIDs = ((String) entry.get(0)).split(",");
        List<Either<TagKey<Item>, Item>> items = ConfigHelper.getItems(itemIDs);
        if (items.isEmpty())
        {   return null;
        }

        double temperature = ((Number) entry.get(1)).doubleValue();
        NbtRequirement nbtRequirement = entry.size() > 2
                                        ? new NbtRequirement(NBTHelper.parseCompoundNbt((String) entry.get(2)))
                                        : new NbtRequirement(new CompoundTag());
        int duration = entry.size() > 3 ? ((Number) entry.get(3)).intValue() : -1;
        ItemRequirement itemRequirement = new ItemRequirement(items, nbtRequirement);

        return new FoodData(temperature, itemRequirement, duration, EntityRequirement.NONE);
    }

    @Override
    public Codec<FoodData> getCodec()
    {   return CODEC;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        FoodData that = (FoodData) obj;
        return data.equals(that.data)
            && temperature.equals(that.temperature)
            && duration == that.duration
            && entityRequirement.equals(that.entityRequirement)
            && requiredMods.equals(that.requiredMods);
    }

    @Override
    public FoodData setRegistryName(ResourceLocation name)
    {
        return null;
    }

    @Nullable
    @Override
    public ResourceLocation getRegistryName()
    {
        return null;
    }

    @Override
    public Class<FoodData> getRegistryType()
    {
        return null;
    }
}
