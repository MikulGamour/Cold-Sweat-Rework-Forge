package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record FoodData(ItemRequirement data, Double value, Optional<Integer> duration, Optional<EntityRequirement> entityRequirement,
                       Optional<List<String>> requiredMods) implements IForgeRegistryEntry<FoodData>
{
    public static final Codec<FoodData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemRequirement.CODEC.fieldOf("data").forGetter(FoodData::data),
            Codec.DOUBLE.fieldOf("value").forGetter(FoodData::value),
            Codec.INT.optionalFieldOf("duration").forGetter(FoodData::duration),
            EntityRequirement.getCodec().optionalFieldOf("entity").forGetter(FoodData::entityRequirement),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(FoodData::requiredMods)
    ).apply(instance, FoodData::new));

    @Override
    public String toString()
    {
        return CODEC.encodeStart(NbtOps.INSTANCE, this).result().map(Object::toString).orElse("");
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
