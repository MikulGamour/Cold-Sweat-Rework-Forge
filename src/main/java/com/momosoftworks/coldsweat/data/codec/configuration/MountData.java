package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.impl.RequirementHolder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class MountData extends ConfigData implements RequirementHolder
{
    final List<Either<TagKey<EntityType<?>>, EntityType<?>>> entities;
    final double coldInsulation;
    final double heatInsulation;
    final EntityRequirement requirement;
    final Optional<List<String>> requiredMods;

    public MountData(List<Either<TagKey<EntityType<?>>, EntityType<?>>> entities, double coldInsulation,
                     double heatInsulation, EntityRequirement requirement, Optional<List<String>> requiredMods)
    {
        this.entities = entities;
        this.coldInsulation = coldInsulation;
        this.heatInsulation = heatInsulation;
        this.requirement = requirement;
        this.requiredMods = requiredMods;
    }

    public MountData(List<EntityType<?>> entities, double coldInsulation, double heatInsulation, EntityRequirement requirement)
    {
        this(entities.stream().map(Either::<TagKey<EntityType<?>>, EntityType<?>>right).toList(),
             coldInsulation, heatInsulation, requirement, Optional.empty());
    }

    public static Codec<MountData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrBuiltinCodec(Registry.ENTITY_TYPE_REGISTRY, ForgeRegistries.ENTITY_TYPES).listOf().fieldOf("entities").forGetter(MountData::entities),
            Codec.DOUBLE.fieldOf("cold_insulation").forGetter(MountData::coldInsulation),
            Codec.DOUBLE.fieldOf("heat_insulation").forGetter(MountData::heatInsulation),
            com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement.getCodec().fieldOf("entity").forGetter(MountData::requirement),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(MountData::requiredMods)
    ).apply(instance, MountData::new));

    public List<Either<TagKey<EntityType<?>>, EntityType<?>>> entities()
    {   return entities;
    }
    public double coldInsulation()
    {   return coldInsulation;
    }
    public double heatInsulation()
    {   return heatInsulation;
    }
    public EntityRequirement requirement()
    {   return requirement;
    }
    public Optional<List<String>> requiredMods()
    {   return requiredMods;
    }

    @Nullable
    public static MountData fromToml(List<?> entry)
    {
        if (entry.size() < 2)
        {   return null;
        }
        String entityID = (String) entry.get(0);
        double coldInsul = ((Number) entry.get(1)).doubleValue();
        double hotInsul = entry.size() < 3
                          ? coldInsul
                          : ((Number) entry.get(2)).doubleValue();
        List<EntityType<?>> entities = ConfigHelper.getEntityTypes(entityID);
        if (entities.isEmpty())
        {   return null;
        }
        return new MountData(entities, coldInsul, hotInsul, com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement.NONE);
    }

    @Override
    public boolean test(Entity entity)
    {   return requirement.test(entity);
    }

    @Override
    public Codec<MountData> getCodec()
    {   return CODEC;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        MountData that = (MountData) obj;
        return Double.compare(that.coldInsulation, coldInsulation) == 0
            && Double.compare(that.heatInsulation, heatInsulation) == 0
            && entities.equals(that.entities)
            && requirement.equals(that.requirement)
            && requiredMods.equals(that.requiredMods);
    }
}
