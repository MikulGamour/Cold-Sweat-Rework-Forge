package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpawnBiomeData implements ConfigData<SpawnBiomeData>
{
    public final List<Biome> biomes;
    public final EntityClassification category;
    public final int weight;
    public final List<Either<ITag<EntityType<?>>, EntityType<?>>> entities;
    public final Optional<List<String>> requiredMods;

    public SpawnBiomeData(List<Biome> biomes, EntityClassification category,
                          int weight, List<Either<ITag<EntityType<?>>, EntityType<?>>> entities, Optional<List<String>> requiredMods)
    {
        this.biomes = biomes;
        this.category = category;
        this.weight = weight;
        this.entities = entities;
        this.requiredMods = requiredMods;
    }

    public SpawnBiomeData(List<Biome> biomes, EntityClassification category, int weight, List<EntityType<?>> entities)
    {
        this(biomes, category, weight,
             entities.stream().map(Either::<ITag<EntityType<?>>, EntityType<?>>right).collect(Collectors.toList()),
             Optional.empty());
    }

    public static final Codec<SpawnBiomeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.dynamicCodec(Registry.BIOME_REGISTRY).listOf().fieldOf("biomes").forGetter(data -> data.biomes),
            EntityClassification.CODEC.fieldOf("category").forGetter(data -> data.category),
            Codec.INT.fieldOf("weight").forGetter(data -> data.weight),
            Codec.either(ITag.codec(EntityTypeTags::getAllTags), Registry.ENTITY_TYPE).listOf().fieldOf("entities").forGetter(data -> data.entities),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(data -> data.requiredMods)
    ).apply(instance, SpawnBiomeData::new));

    @Nullable
    public static SpawnBiomeData fromToml(List<?> entry, EntityType<?> entityType, DynamicRegistries registryAccess)
    {
        if (entry.size() < 2)
        {   return null;
        }
        String biomeId = ((String) entry.get(0));
        List<Biome> biomes = ConfigHelper.parseRegistryItems(Registry.BIOME_REGISTRY, registryAccess, biomeId);
        if (biomes.isEmpty())
        {   return null;
        }
        return new SpawnBiomeData(biomes, EntityClassification.CREATURE, ((Number) entry.get(1)).intValue(),
                                  Arrays.asList(entityType));
    }

    @Override
    public Codec<SpawnBiomeData> getCodec()
    {   return CODEC;
    }

    @Override
    public String toString()
    {   return this.asString();
    }
}