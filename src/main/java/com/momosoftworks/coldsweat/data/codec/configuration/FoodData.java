package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.NbtRequirement;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class FoodData implements NbtSerializable, RequirementHolder
{
    public Double temperature;
    public ItemRequirement data;
    public int duration;
    public EntityRequirement entityRequirement;
    public Optional<List<String>> requiredMods;

    public FoodData(Double temperature, ItemRequirement data, int duration, EntityRequirement entityRequirement, Optional<List<String>> requiredMods)
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
            Codec.DOUBLE.fieldOf("temperature").forGetter(data -> data.temperature),
            ItemRequirement.CODEC.fieldOf("data").forGetter(data -> data.data),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(data -> data.duration),
            EntityRequirement.getCodec().optionalFieldOf("entity", EntityRequirement.NONE).forGetter(data -> data.entityRequirement),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(data -> data.requiredMods)
    ).apply(instance, FoodData::new));

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
        List<Item> items = ConfigHelper.getItems(itemIDs);
        if (items.isEmpty())
        {   return null;
        }

        double temperature = ((Number) entry.get(1)).doubleValue();
        NbtRequirement nbtRequirement = entry.size() > 2
                                        ? new NbtRequirement(NBTHelper.parseCompoundNbt((String) entry.get(2)))
                                        : new NbtRequirement(new CompoundNBT());
        int duration = entry.size() > 3 ? ((Number) entry.get(3)).intValue() : -1;
        ItemRequirement itemRequirement = new ItemRequirement(items, nbtRequirement);

        return new FoodData(temperature, itemRequirement, duration, EntityRequirement.NONE, Optional.empty());
    }

    @Override
    public CompoundNBT serialize()
    {   return (CompoundNBT) CODEC.encodeStart(NBTDynamicOps.INSTANCE, this).result().orElseGet(CompoundNBT::new);
    }

    public static FoodData deserialize(CompoundNBT tag)
    {   return CODEC.decode(NBTDynamicOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalStateException("Failed to deserialize FuelData")).getFirst();
    }

    @Override
    public String toString()
    {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
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
}
