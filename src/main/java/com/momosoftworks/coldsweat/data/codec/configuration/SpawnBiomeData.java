package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record SpawnBiomeData(List<Either<TagKey<Biome>, Biome>> biomes, MobCategory category,
                             int weight, List<Either<TagKey<EntityType<?>>, EntityType<?>>> entities, Optional<List<String>> requiredMods) implements IForgeRegistryEntry<SpawnBiomeData>
{
    public SpawnBiomeData(List<Biome> biomes, MobCategory category, int weight, List<EntityType<?>> entities)
    {
        this(biomes.stream().map(Either::<TagKey<Biome>, Biome>right).toList(),
             category, weight,
             entities.stream().map(Either::<TagKey<EntityType<?>>, EntityType<?>>right).toList(),
             Optional.empty());
    }

    public static final Codec<SpawnBiomeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrVanillaRegistryCodec(Registry.BIOME_REGISTRY, Biome.CODEC).listOf().fieldOf("biomes").forGetter(SpawnBiomeData::biomes),
            MobCategory.CODEC.fieldOf("category").forGetter(SpawnBiomeData::category),
            Codec.INT.fieldOf("weight").forGetter(SpawnBiomeData::weight),
            ConfigHelper.tagOrForgeRegistryCodec(Registry.ENTITY_TYPE_REGISTRY, ForgeRegistries.ENTITIES).listOf().fieldOf("entities").forGetter(SpawnBiomeData::entities),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(SpawnBiomeData::requiredMods)
    ).apply(instance, SpawnBiomeData::new));

    @Nullable
    public static SpawnBiomeData fromToml(List<?> entry, EntityType<?> entityType, RegistryAccess registryAccess)
    {
        if (entry.size() < 2)
        {   return null;
        }
        String biomeId = ((String) entry.get(0));
        List<Biome> biomes = ConfigHelper.parseRegistryItems(Registry.BIOME_REGISTRY, registryAccess, biomeId);
        if (biomes.isEmpty())
        {   return null;
        }
        return new SpawnBiomeData(biomes, MobCategory.CREATURE, ((Number) entry.get(1)).intValue(),
                                  List.of(entityType));
    }

    @Override
    public SpawnBiomeData setRegistryName(ResourceLocation resourceLocation)
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
    public Class<SpawnBiomeData> getRegistryType()
    {
        return null;
    }

    @Override
    public String toString()
    {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
    }
}