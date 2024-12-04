package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
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

public class SpawnBiomeData extends ConfigData
{
    final List<Biome> biomes;
    final EntityClassification category;
    final int weight;
    final List<Either<ITag<EntityType<?>>, EntityType<?>>> entities;

    public SpawnBiomeData(List<Biome> biomes, EntityClassification category,
                          int weight, List<Either<ITag<EntityType<?>>, EntityType<?>>> entities,
                          List<String> requiredMods)
    {
        super(requiredMods);
        this.biomes = biomes;
        this.category = category;
        this.weight = weight;
        this.entities = entities;
    }

    public SpawnBiomeData(List<Biome> biomes, EntityClassification category,
                          int weight, List<Either<ITag<EntityType<?>>, EntityType<?>>> entities)
    {
        this(biomes, category, weight, entities, ConfigHelper.getModIDs(biomes, Registry.BIOME_REGISTRY));
    }

    public static final Codec<SpawnBiomeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.dynamicCodec(Registry.BIOME_REGISTRY).listOf().fieldOf("biomes").forGetter(data -> data.biomes),
            EntityClassification.CODEC.fieldOf("category").forGetter(data -> data.category),
            Codec.INT.fieldOf("weight").forGetter(data -> data.weight),
            Codec.either(ITag.codec(EntityTypeTags::getAllTags), Registry.ENTITY_TYPE).listOf().fieldOf("entities").forGetter(data -> data.entities),
            Codec.STRING.listOf().optionalFieldOf("required_mods", Arrays.asList()).forGetter(SpawnBiomeData::requiredMods)
    ).apply(instance, SpawnBiomeData::new));

    public List<Biome> biomes()
    {   return biomes;
    }
    public EntityClassification category()
    {   return category;
    }
    public int weight()
    {   return weight;
    }
    public List<Either<ITag<EntityType<?>>, EntityType<?>>> entities()
    {   return entities;
    }

    @Nullable
    public static SpawnBiomeData fromToml(List<?> entry, EntityType<?> entityType, DynamicRegistries registryAccess)
    {
        if (entry.size() < 2)
        {   ColdSweat.LOGGER.error("Error parsing entity spawn biome config: not enough arguments");
            return null;
        }
        List<Biome> biomes = ConfigHelper.parseRegistryItems(Registry.BIOME_REGISTRY, registryAccess, (String) entry.get(0));
        if (biomes.isEmpty())
        {   ColdSweat.LOGGER.error("Error parsing entity spawn biome config: {} does not contain any valid biomes", entry);
            return null;
        }
        return new SpawnBiomeData(biomes, EntityClassification.CREATURE, ((Number) entry.get(1)).intValue(),
                                  Arrays.asList(Either.right(entityType)));
    }

    @Override
    public Codec<SpawnBiomeData> getCodec()
    {   return CODEC;
    }
}