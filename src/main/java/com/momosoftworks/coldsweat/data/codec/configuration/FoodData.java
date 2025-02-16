package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.impl.RequirementHolder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.NbtRequirement;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.ITag;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

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
        this(temperature, data, duration, entityRequirement, ConfigHelper.getModIDs(CSMath.listOrEmpty(data.items()), ForgeRegistries.ITEMS));
    }

    public static final Codec<FoodData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("temperature").forGetter(data -> data.temperature),
            ItemRequirement.CODEC.fieldOf("data").forGetter(data -> data.data),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(data -> data.duration),
            EntityRequirement.getCodec().optionalFieldOf("entity", EntityRequirement.NONE).forGetter(data -> data.entityRequirement),
            Codec.STRING.listOf().optionalFieldOf("required_mods", Arrays.asList()).forGetter(FoodData::requiredMods)
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
        {   ColdSweat.LOGGER.error("Error parsing food config: not enough arguments");
            return null;
        }
        List<Either<ITag<Item>, Item>> items = ConfigHelper.getItems((String) entry.get(0));
        if (items.isEmpty()) return null;
        double temperature = ((Number) entry.get(1)).doubleValue();
        NbtRequirement nbtRequirement = entry.size() > 2
                                        ? new NbtRequirement(NBTHelper.parseCompoundNbt((String) entry.get(2)))
                                        : new NbtRequirement(new CompoundNBT());
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
        return super.equals(obj)
            && data.equals(that.data)
            && temperature.equals(that.temperature)
            && duration == that.duration
            && entityRequirement.equals(that.entityRequirement);
    }
}
