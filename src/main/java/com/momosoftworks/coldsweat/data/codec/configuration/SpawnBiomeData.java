package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
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

public class SpawnBiomeData extends ConfigData
{
    final List<Either<TagKey<Biome>, Holder<Biome>>> biomes;
    final MobCategory category;
    final int weight;
    final List<Either<TagKey<EntityType<?>>, EntityType<?>>> entities;

    public SpawnBiomeData(List<Either<TagKey<Biome>, Holder<Biome>>> biomes, MobCategory category,
                          int weight, List<Either<TagKey<EntityType<?>>, EntityType<?>>> entities,
                          List<String> requiredMods)
    {
        super(requiredMods);
        this.biomes = biomes;
        this.category = category;
        this.weight = weight;
        this.entities = entities;
    }

    public SpawnBiomeData(List<Either<TagKey<Biome>, Holder<Biome>>> biomes, MobCategory category,
                          int weight, List<Either<TagKey<EntityType<?>>, EntityType<?>>> entities)
    {
        this(biomes, category, weight, entities, ConfigHelper.getModIDs(biomes));
    }

    public static final Codec<SpawnBiomeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrHolderCodec(Registry.BIOME_REGISTRY, Biome.CODEC).listOf().fieldOf("biomes").forGetter(SpawnBiomeData::biomes),
            MobCategory.CODEC.fieldOf("category").forGetter(SpawnBiomeData::category),
            Codec.INT.fieldOf("weight").forGetter(SpawnBiomeData::weight),
            ConfigHelper.tagOrBuiltinCodec(Registry.ENTITY_TYPE_REGISTRY, ForgeRegistries.ENTITY_TYPES).listOf().fieldOf("entities").forGetter(SpawnBiomeData::entities),
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

    @Nullable
    public static SpawnBiomeData fromToml(List<?> entry, EntityType<?> entityType, RegistryAccess registryAccess)
    {
        if (entry.size() < 2)
        {   ColdSweat.LOGGER.error("Error parsing entity spawn biome config: not enough arguments");
            return null;
        }
        List<Either<TagKey<Biome>, Holder<Biome>>> biomes = ConfigHelper.parseRegistryItems(Registry.BIOME_REGISTRY, registryAccess, (String) entry.get(0));
        if (biomes.isEmpty())
        {   ColdSweat.LOGGER.error("Error parsing entity spawn biome config: {} does not contain any valid biomes", entry);
            return null;
        }
        return new SpawnBiomeData(biomes, net.minecraft.world.entity.MobCategory.CREATURE, ((Number) entry.get(1)).intValue(),
                                  List.of(Either.right(entityType)));
    }

    @Override
    public Codec<SpawnBiomeData> getCodec()
    {   return CODEC;
    }
}