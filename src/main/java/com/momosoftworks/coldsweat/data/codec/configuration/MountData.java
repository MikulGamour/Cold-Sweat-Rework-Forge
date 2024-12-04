package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.impl.RequirementHolder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.tags.ITag;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class MountData extends ConfigData implements RequirementHolder
{
    final EntityRequirement entityData;
    final double coldInsulation;
    final double heatInsulation;

    public MountData(EntityRequirement entityData, double coldInsulation, double heatInsulation, List<String> requiredMods)
    {
        super(requiredMods);
        this.entityData = entityData;
        this.coldInsulation = coldInsulation;
        this.heatInsulation = heatInsulation;
    }

    public MountData(EntityRequirement entityData, double coldInsulation, double heatInsulation)
    {
        this(entityData, coldInsulation, heatInsulation, ConfigHelper.getModIDs(CSMath.listOrEmpty(entityData.entities()), ForgeRegistries.ENTITIES));
    }

    public static Codec<MountData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityRequirement.getCodec().fieldOf("entity").forGetter(MountData::entityData),
            Codec.DOUBLE.fieldOf("cold_insulation").forGetter(MountData::coldInsulation),
            Codec.DOUBLE.fieldOf("heat_insulation").forGetter(MountData::heatInsulation),
            Codec.STRING.listOf().optionalFieldOf("required_mods", Arrays.asList()).forGetter(MountData::requiredMods)
    ).apply(instance, MountData::new));

    public double coldInsulation()
    {   return coldInsulation;
    }
    public double heatInsulation()
    {   return heatInsulation;
    }
    public EntityRequirement entityData()
    {   return entityData;
    }

    @Nullable
    public static MountData fromToml(List<?> entry)
    {
        if (entry.size() < 2)
        {   ColdSweat.LOGGER.error("Error parsing mount config: not enough arguments");
            return null;
        }
        List<Either<ITag<EntityType<?>>, EntityType<?>>> entities = ConfigHelper.getEntityTypes((String) entry.get(0));

        if (entities.isEmpty())
        {   ColdSweat.LOGGER.error("Error parsing mount config: {} does not contain any valid entities", entry);
            return null;
        }
        double coldInsul = ((Number) entry.get(1)).doubleValue();
        double hotInsul = entry.size() < 3
                          ? coldInsul
                          : ((Number) entry.get(2)).doubleValue();

        return new MountData(new EntityRequirement(entities), coldInsul, hotInsul);
    }

    @Override
    public boolean test(Entity entity)
    {   return entityData.test(entity);
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
        return super.equals(obj)
            && entityData.equals(that.entityData)
            && Double.compare(that.coldInsulation, coldInsulation) == 0
            && Double.compare(that.heatInsulation, heatInsulation) == 0;
    }
}
