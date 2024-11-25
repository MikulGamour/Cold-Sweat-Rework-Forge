package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.impl.RequirementHolder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public record MountData(List<Either<TagKey<EntityType<?>>, EntityType<?>>> entities, double coldInsulation,
                        double heatInsulation, EntityRequirement requirement,
                        Optional<List<String>> requiredMods) implements NbtSerializable, RequirementHolder, ConfigData<MountData>, IForgeRegistryEntry<MountData>
{
    public MountData(List<EntityType<?>> entities, double coldInsulation, double heatInsulation, EntityRequirement requirement)
    {
        this(entities.stream().map(Either::<TagKey<EntityType<?>>, EntityType<?>>right).toList(),
             coldInsulation, heatInsulation, requirement, Optional.empty());
    }

    public static Codec<MountData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrBuiltinCodec(Registry.ENTITY_TYPE_REGISTRY, ForgeRegistries.ENTITIES).listOf().fieldOf("entities").forGetter(MountData::entities),
            Codec.DOUBLE.fieldOf("cold_insulation").forGetter(MountData::coldInsulation),
            Codec.DOUBLE.fieldOf("heat_insulation").forGetter(MountData::heatInsulation),
            EntityRequirement.getCodec().fieldOf("entity").forGetter(MountData::requirement),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(MountData::requiredMods)
    ).apply(instance, MountData::new));

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
        return new MountData(entities, coldInsul, hotInsul, EntityRequirement.NONE);
    }

    @Override
    public boolean test(Entity entity)
    {   return requirement.test(entity);
    }

    @Override
    public CompoundTag serialize()
    {   return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
    }

    public static MountData deserialize(CompoundTag tag)
    {   return CODEC.decode(NbtOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalStateException("Failed to deserialize MountData")).getFirst();
    }

    @Override
    public Codec<MountData> getCodec()
    {   return CODEC;
    }

    @Override
    public String toString()
    {   return this.asString();
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

    @Override
    public MountData setRegistryName(ResourceLocation name)
    {
        return null;
    }

    @Override
    public ResourceLocation getRegistryName()
    {
        return null;
    }

    @Override
    public Class<MountData> getRegistryType()
    {
        return MountData.class;
    }
}
