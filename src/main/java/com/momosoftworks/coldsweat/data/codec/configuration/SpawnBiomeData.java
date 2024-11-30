package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class SpawnBiomeData extends ConfigData
{
    final List<Either<TagKey<Biome>, Holder<Biome>>> biomes;
    final MobCategory category;
    final int weight;
    final List<Either<TagKey<EntityType<?>>, EntityType<?>>> entities;
    final Optional<List<String>> requiredMods;

    public SpawnBiomeData(List<Either<TagKey<Biome>, Holder<Biome>>> biomes, MobCategory category,
                          int weight, List<Either<TagKey<EntityType<?>>, EntityType<?>>> entities,
                          Optional<List<String>> requiredMods)
    {
        this.biomes = biomes;
        this.category = category;
        this.weight = weight;
        this.entities = entities;
        this.requiredMods = requiredMods;
    }

    public SpawnBiomeData(List<Holder<Biome>> biomes, MobCategory category, int weight, List<EntityType<?>> entities)
    {
        this(biomes.stream().map(Either::<TagKey<Biome>, Holder<Biome>>right).toList(),
             category, weight,
             entities.stream().map(Either::<TagKey<EntityType<?>>, EntityType<?>>right).toList(),
             Optional.empty());
    }

    public static final Codec<SpawnBiomeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrHolderCodec(Registry.BIOME_REGISTRY, Biome.CODEC).listOf().fieldOf("biomes").forGetter(SpawnBiomeData::biomes),
            net.minecraft.world.entity.MobCategory.CODEC.fieldOf("category").forGetter(SpawnBiomeData::category),
            Codec.INT.fieldOf("weight").forGetter(SpawnBiomeData::weight),
            ConfigHelper.tagOrBuiltinCodec(Registry.ENTITY_TYPE_REGISTRY, ForgeRegistries.ENTITY_TYPES).listOf().fieldOf("entities").forGetter(SpawnBiomeData::entities),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(SpawnBiomeData::requiredMods)
    ).apply(instance, SpawnBiomeData::new));

    public List<Either<TagKey<Biome>, Holder<Biome>>> biomes()
    {   return biomes;
    }
    public MobCategory category()
    {   return category;
    }
    public int weight()
    {   return weight;
    }
    public List<Either<TagKey<EntityType<?>>, EntityType<?>>> entities()
    {   return entities;
    }
    public Optional<List<String>> requiredMods()
    {   return requiredMods;
    }

    @Nullable
    public static SpawnBiomeData fromToml(List<?> entry, EntityType<?> entityType, RegistryAccess registryAccess)
    {
        if (entry.size() < 2)
        {   return null;
        }
        String biomeId = ((String) entry.get(0));
        List<Holder<Biome>> biomes = ConfigHelper.parseRegistryItems(Registry.BIOME_REGISTRY, registryAccess, biomeId);
        if (biomes.isEmpty())
        {   return null;
        }
        return new SpawnBiomeData(biomes, net.minecraft.world.entity.MobCategory.CREATURE, ((Number) entry.get(1)).intValue(),
                                  java.util.List.of(entityType));
    }

    @Override
    public Codec<SpawnBiomeData> getCodec()
    {   return CODEC;
    }
}