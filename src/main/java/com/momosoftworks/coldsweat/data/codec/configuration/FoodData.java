package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import net.minecraft.nbt.NBTDynamicOps;

import java.util.List;
import java.util.Optional;

public class FoodData
{
    public ItemRequirement data;
    public Double value;
    public Optional<Integer> duration;
    public Optional<EntityRequirement> entityRequirement;
    public Optional<List<String>> requiredMods;

    public static final Codec<FoodData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemRequirement.CODEC.fieldOf("data").forGetter(data -> data.data),
            Codec.DOUBLE.fieldOf("value").forGetter(data -> data.value),
            Codec.INT.optionalFieldOf("duration").forGetter(data -> data.duration),
            EntityRequirement.getCodec().optionalFieldOf("entity").forGetter(data -> data.entityRequirement),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(data -> data.requiredMods)
    ).apply(instance, FoodData::new));

    public FoodData(ItemRequirement data, Double value, Optional<Integer> duration, Optional<EntityRequirement> entityRequirement, Optional<List<String>> requiredMods)
    {
        this.data = data;
        this.value = value;
        this.duration = duration;
        this.entityRequirement = entityRequirement;
        this.requiredMods = requiredMods;
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
            && value.equals(that.value)
            && duration.equals(that.duration)
            && entityRequirement.equals(that.entityRequirement)
            && requiredMods.equals(that.requiredMods);
    }
}
