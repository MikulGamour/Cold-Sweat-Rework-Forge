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
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public class MountData extends ConfigData implements RequirementHolder
{
    final EntityRequirement entityData;
    final EntityRequirement riderData;
    final double coldInsulation;
    final double heatInsulation;

    public MountData(EntityRequirement entityData, EntityRequirement riderData, double coldInsulation, double heatInsulation, List<String> requiredMods)
    {
        super(requiredMods);
        this.entityData = entityData;
        this.riderData = riderData;
        this.coldInsulation = coldInsulation;
        this.heatInsulation = heatInsulation;
    }

    public MountData(EntityRequirement entityData, EntityRequirement riderData, double coldInsulation, double heatInsulation)
    {
        this(entityData, riderData, coldInsulation, heatInsulation, ConfigHelper.getModIDs(CSMath.listOrEmpty(entityData.entities()), ForgeRegistries.ENTITY_TYPES));
    }

    public static Codec<MountData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityRequirement.getCodec().fieldOf("entity").forGetter(MountData::entityData),
            EntityRequirement.getCodec().optionalFieldOf("rider", EntityRequirement.NONE).forGetter(MountData::rider),
            Codec.DOUBLE.fieldOf("cold_insulation").forGetter(MountData::coldInsulation),
            Codec.DOUBLE.fieldOf("heat_insulation").forGetter(MountData::heatInsulation),
            Codec.STRING.listOf().optionalFieldOf("required_mods", List.of()).forGetter(MountData::requiredMods)
    ).apply(instance, MountData::new));

    public EntityRequirement entityData()
    {   return entityData;
    }
    public EntityRequirement rider()
    {   return riderData;
    }
    public double coldInsulation()
    {   return coldInsulation;
    }
    public double heatInsulation()
    {   return heatInsulation;
    }

    @Nullable
    public static MountData fromToml(List<?> entry)
    {
        if (entry.size() < 2)
        {   ColdSweat.LOGGER.error("Error parsing insulating mount config: not enough arguments");
            return null;
        }
        List<Either<TagKey<EntityType<?>>, EntityType<?>>> entities = ConfigHelper.getEntityTypes((String) entry.get(0));
        if (entities.isEmpty()) return null;

        double coldInsul = ((Number) entry.get(1)).doubleValue();
        double hotInsul = entry.size() < 3
                          ? coldInsul
                          : ((Number) entry.get(2)).doubleValue();

        return new MountData(new EntityRequirement(entities), EntityRequirement.NONE, coldInsul, hotInsul);
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
            && riderData.equals(that.riderData)
            && Double.compare(that.coldInsulation, coldInsulation) == 0
            && Double.compare(that.heatInsulation, heatInsulation) == 0;
    }
}
