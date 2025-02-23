package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.requirement.LocationRequirement;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public class SpawnBiomeData extends ConfigData
{
    final List<Either<TagKey<Biome>, Holder<Biome>>> biomes;
    final MobCategory category;
    final int weight;
    final List<Either<TagKey<EntityType<?>>, EntityType<?>>> entities;
    final IntegerBounds count;
    final LocationRequirement location;
    final LocationRequirement blockBelow;

    public SpawnBiomeData(List<Either<TagKey<Biome>, Holder<Biome>>> biomes, MobCategory category,
                          int weight, List<Either<TagKey<EntityType<?>>, EntityType<?>>> entities,
                          IntegerBounds count, LocationRequirement location,
                          LocationRequirement blockBelow, List<String> requiredMods)
    {
        super(requiredMods);
        this.biomes = biomes;
        this.category = category;
        this.weight = weight;
        this.entities = entities;
        this.count = count;
        this.location = location;
        this.blockBelow = blockBelow;
    }

    public SpawnBiomeData(List<Either<TagKey<Biome>, Holder<Biome>>> biomes, MobCategory category,
                          int weight, List<Either<TagKey<EntityType<?>>, EntityType<?>>> entities,
                          IntegerBounds count, LocationRequirement location, LocationRequirement blockBelow)
    {
        this(biomes, category, weight, entities, count, location, blockBelow, ConfigHelper.getModIDs(biomes));
    }

    public SpawnBiomeData(Collection<Holder<Biome>> biomes, MobCategory category,
                          int weight, Collection<EntityType<?>> entities,
                          IntegerBounds count, LocationRequirement location, LocationRequirement blockBelow)
    {
        this(biomes.stream().map(Either::<TagKey<Biome>, Holder<Biome>>right).toList(),
             category, weight,
             entities.stream().map(Either::<TagKey<EntityType<?>>, EntityType<?>>right).toList(),
             count, location, blockBelow);
    }

    public static final Codec<SpawnBiomeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrHolderCodec(Registries.BIOME, Biome.CODEC).listOf().fieldOf("biomes").forGetter(SpawnBiomeData::biomes),
            MobCategory.CODEC.fieldOf("category").forGetter(SpawnBiomeData::category),
            Codec.INT.fieldOf("weight").forGetter(SpawnBiomeData::weight),
            ConfigHelper.tagOrBuiltinCodec(Registries.ENTITY_TYPE, ForgeRegistries.ENTITY_TYPES).listOf().fieldOf("entities").forGetter(SpawnBiomeData::entities),
            IntegerBounds.CODEC.optionalFieldOf("count", IntegerBounds.NONE).forGetter(SpawnBiomeData::count),
            LocationRequirement.CODEC.optionalFieldOf("location", LocationRequirement.NONE).forGetter(SpawnBiomeData::location),
            LocationRequirement.CODEC.optionalFieldOf("block_below", LocationRequirement.NONE).forGetter(SpawnBiomeData::blockBelow),
            Codec.STRING.listOf().optionalFieldOf("required_mods", List.of()).forGetter(SpawnBiomeData::requiredMods)
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
    public IntegerBounds count()
    {   return count;
    }
    public LocationRequirement location()
    {   return location;
    }
    public LocationRequirement blockBelow()
    {   return blockBelow;
    }

    @Nullable
    public static SpawnBiomeData fromToml(List<?> entry, EntityType<?> entityType, RegistryAccess registryAccess)
    {
        if (entry.size() < 2)
        {   ColdSweat.LOGGER.error("Error parsing entity spawn biome config: not enough arguments");
            return null;
        }
        List<Either<TagKey<Biome>, Holder<Biome>>> biomes = ConfigHelper.parseRegistryItems(Registries.BIOME, registryAccess, (String) entry.get(0));
        if (biomes.isEmpty()) return null;

        return new SpawnBiomeData(biomes, MobCategory.CREATURE, ((Number) entry.get(1)).intValue(),
                                  List.of(Either.right(entityType)), new IntegerBounds(1, 1), LocationRequirement.NONE, LocationRequirement.NONE);
    }

    @Override
    public Codec<SpawnBiomeData> getCodec()
    {   return CODEC;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        SpawnBiomeData that = (SpawnBiomeData) obj;
        return biomes.equals(that.biomes)
            && category == that.category
            && weight == that.weight
            && entities.equals(that.entities)
            && count.equals(that.count)
            && location.equals(that.location)
            && blockBelow.equals(that.blockBelow);
    }
}