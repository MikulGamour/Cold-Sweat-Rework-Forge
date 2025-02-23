package com.momosoftworks.coldsweat.compat.kubejs.event.builder;

import com.mojang.datafixers.util.Either;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.data.codec.configuration.SpawnBiomeData;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.requirement.LocationRequirement;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class SpawnBiomeBuilderJS
{
    public final Set<Holder<Biome>> biomes = new HashSet<>();
    public MobCategory category;
    public int weight;
    public final Set<EntityType<?>> entities = new HashSet<>();
    public IntegerBounds count;
    public Predicate<BlockInWorld> locationPredicate = null;

    public SpawnBiomeBuilderJS()
    {}

    public SpawnBiomeBuilderJS biomes(String... biomes)
    {
        RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();
        if (registryAccess == null)
        {   ColdSweat.LOGGER.error("Could not parse biomes for addSpawnBiomes(), as registries are not built yet.");
            return this;
        }
        List<Either<TagKey<Biome>, Holder<Biome>>> biomesList = ConfigHelper.parseRegistryItems(Registries.BIOME, registryAccess, biomes);
        this.biomes.addAll(RegistryHelper.mapRegistryTagList(Registries.BIOME, biomesList, registryAccess));
        return this;
    }

    public SpawnBiomeBuilderJS category(String category)
    {
        for (MobCategory mobCategory : MobCategory.values())
        {
            if (mobCategory.getName().equals(category))
            {
                this.category = mobCategory;
                return this;
            }
        }
        return this;
    }

    public SpawnBiomeBuilderJS weight(int weight)
    {
        this.weight = weight;
        return this;
    }

    public SpawnBiomeBuilderJS entities(String... entities)
    {
        this.entities.addAll(RegistryHelper.mapBuiltinRegistryTagList(BuiltInRegistries.ENTITY_TYPE, ConfigHelper.getEntityTypes(entities)));
        return this;
    }

    public SpawnBiomeBuilderJS count(int min, int max)
    {
        this.count = new IntegerBounds(min, max);
        return this;
    }

    public SpawnBiomeBuilderJS locationPredicate(Predicate<BlockInWorld> locationPredicate)
    {
        this.locationPredicate = locationPredicate;
        return this;
    }

    public SpawnBiomeData build()
    {
        SpawnBiomeData data = new SpawnBiomeData(biomes, category, weight,
                                                 entities,
                                                 count, new LocationRequirement(locationPredicate), LocationRequirement.NONE);
        data.setType(ConfigData.Type.KUBEJS);
        return data;
    }
}
