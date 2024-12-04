package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.impl.RequirementHolder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemComponentsRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class FoodData extends ConfigData implements RequirementHolder
{
    final Double temperature;
    final ItemRequirement data;
    final int duration;
    final EntityRequirement entityRequirement;

    public FoodData(Double temperature, ItemRequirement data, int duration,
                    EntityRequirement entityRequirement, List<String> requiredMods)
    {
        super(requiredMods);
        this.temperature = temperature;
        this.data = data;
        this.duration = duration;
        this.entityRequirement = entityRequirement;
    }

    public FoodData(Double temperature, ItemRequirement data, int duration,
                    EntityRequirement entityRequirement)
    {
        this(temperature, data, duration, entityRequirement, ConfigHelper.getModIDs(CSMath.listOrEmpty(data.items()), BuiltInRegistries.ITEM));
    }

    public static final Codec<FoodData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("temperature").forGetter(FoodData::temperature),
            ItemRequirement.CODEC.fieldOf("data").forGetter(FoodData::data),
            Codec.INT.optionalFieldOf("duration", -1).forGetter(FoodData::duration),
            EntityRequirement.getCodec().optionalFieldOf("entity", EntityRequirement.NONE).forGetter(FoodData::entityRequirement),
            Codec.STRING.listOf().optionalFieldOf("required_mods", List.of()).forGetter(FoodData::requiredMods)
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
        {   ColdSweat.LOGGER.error("Error parsing entity config: not enough arguments");
            return null;
        }
        List<Either<TagKey<Item>, Item>> items = ConfigHelper.getItems((String) entry.get(0));

        if (items.isEmpty())
        {   ColdSweat.LOGGER.error("Error parsing food config: {} does not contain any valid items", entry);
            return null;
        }
        double temperature = ((Number) entry.get(1)).doubleValue();
        ItemComponentsRequirement componentsRequirement = entry.size() > 2
                                                          ? ItemComponentsRequirement.parse((String) entry.get(2))
                                                          : new ItemComponentsRequirement();
        int duration = entry.size() > 3 ? ((Number) entry.get(3)).intValue() : -1;
        ItemRequirement itemRequirement = new ItemRequirement(items, componentsRequirement);

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
        return super.equals(obj)
            && data.equals(that.data)
            && temperature.equals(that.temperature)
            && duration == that.duration
            && entityRequirement.equals(that.entityRequirement);
    }
}
