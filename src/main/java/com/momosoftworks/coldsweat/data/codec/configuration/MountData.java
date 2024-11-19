package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.impl.RequirementHolder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.tags.ITag;
import net.minecraft.util.registry.Registry;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MountData implements NbtSerializable, RequirementHolder, ConfigData<MountData>
{
    public List<Either<ITag<EntityType<?>>, EntityType<?>>> entities;
    public double coldInsulation;
    public double heatInsulation;
    public EntityRequirement requirement;
    public Optional<List<String>> requiredMods;

    public MountData(List<Either<ITag<EntityType<?>>, EntityType<?>>> entities, double coldInsulation, double heatInsulation,
                     EntityRequirement requirement, Optional<List<String>> requiredMods)
    {   this.entities = entities;
        this.coldInsulation = coldInsulation;
        this.heatInsulation = heatInsulation;
        this.requirement = requirement;
        this.requiredMods = requiredMods;
    }

    public MountData(List<EntityType<?>> entities, double coldInsulation, double heatInsulation, EntityRequirement requirement)
    {
        this(entities.stream().map(Either::<ITag<EntityType<?>>, EntityType<?>>right).collect(Collectors.toList()),
             coldInsulation, heatInsulation, requirement, Optional.empty());
    }

    public static Codec<MountData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrBuiltinCodec(Registry.ENTITY_TYPE_REGISTRY, Registry.ENTITY_TYPE).listOf().fieldOf("entities").forGetter(data -> data.entities),
            Codec.DOUBLE.fieldOf("cold_insulation").forGetter(data -> data.coldInsulation),
            Codec.DOUBLE.fieldOf("heat_insulation").forGetter(data -> data.heatInsulation),
            EntityRequirement.getCodec().fieldOf("entity").forGetter(data -> data.requirement),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(data -> data.requiredMods)
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
    public CompoundNBT serialize()
    {   return (CompoundNBT) CODEC.encodeStart(NBTDynamicOps.INSTANCE, this).result().orElseGet(CompoundNBT::new);
    }

    public static MountData deserialize(CompoundNBT tag)
    {   return CODEC.decode(NBTDynamicOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalStateException("Failed to deserialize MountData")).getFirst();
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
}
