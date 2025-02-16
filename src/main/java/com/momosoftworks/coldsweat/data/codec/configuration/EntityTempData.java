package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
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

public class EntityTempData extends ConfigData implements RequirementHolder
{
    final EntityRequirement entity;
    final double temperature;
    final double range;
    final Temperature.Units units;
    final EntityRequirement otherEntityRequirement;
    final double maxEffect;
    final boolean affectsSelf;

    public EntityTempData(EntityRequirement entity, double temperature, double range,
                          Temperature.Units units, EntityRequirement otherEntityRequirement,
                          double maxEffect, boolean affectsSelf, List<String> requiredMods)
    {
        super(requiredMods);
        this.entity = entity;
        this.temperature = temperature;
        this.range = range;
        this.units = units;
        this.otherEntityRequirement = otherEntityRequirement;
        this.maxEffect = maxEffect;
        this.affectsSelf = affectsSelf;
    }

    public EntityTempData(EntityRequirement entity, double temperature, double range,
                          Temperature.Units units, EntityRequirement otherEntityRequirement,
                          double maxEffect, boolean affectsSelf)
    {
        this(entity, temperature, range, units, otherEntityRequirement, maxEffect, affectsSelf, ConfigHelper.getModIDs(CSMath.listOrEmpty(entity.entities()), ForgeRegistries.ENTITIES));
    }

    public static final Codec<EntityTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityRequirement.getCodec().fieldOf("entity").forGetter(data -> data.entity),
            Codec.DOUBLE.fieldOf("temperature").forGetter(data -> data.temperature),
            Codec.DOUBLE.fieldOf("range").forGetter(data -> data.range),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(data -> data.units),
            EntityRequirement.getCodec().optionalFieldOf("affected_entity", EntityRequirement.NONE).forGetter(data -> data.otherEntityRequirement),
            Codec.DOUBLE.optionalFieldOf("max_effect", Double.MAX_VALUE).forGetter(EntityTempData::maxEffect),
            Codec.BOOL.optionalFieldOf("affects_self", false).forGetter(EntityTempData::affectsSelf),
            Codec.STRING.listOf().optionalFieldOf("required_mods", Arrays.asList()).forGetter(EntityTempData::requiredMods)
    ).apply(instance, EntityTempData::new));

    public EntityRequirement entity()
    {   return entity;
    }
    public double temperature()
    {   return temperature;
    }
    public double range()
    {   return range;
    }
    public Temperature.Units units()
    {   return units;
    }
    public EntityRequirement playerRequirement()
    {   return otherEntityRequirement;
    }
    public double maxEffect()
    {   return maxEffect;
    }
    public boolean affectsSelf()
    {   return affectsSelf;
    }

    public double getTemperature()
    {   return Temperature.convert(temperature, units, Temperature.Units.MC, false);
    }
    public double getMaxEffect()
    {   return Temperature.convert(maxEffect, units, Temperature.Units.MC, false);
    }

    @Nullable
    public static EntityTempData fromToml(List<?> entry)
    {
        if (entry.size() < 3)
        {   ColdSweat.LOGGER.error("Error parsing entity temp config: not enough arguments");
            return null;
        }
        List<Either<ITag<EntityType<?>>, EntityType<?>>> entities = ConfigHelper.getEntityTypes((String) entry.get(0));
        if (entities.isEmpty()) return null;

        double temp = ((Number) entry.get(1)).doubleValue();
        double range = ((Number) entry.get(2)).doubleValue();
        Temperature.Units units = entry.size() > 3
                                  ? Temperature.Units.fromID((String) entry.get(3))
                                  : Temperature.Units.MC;
        double maxEffect = entry.size() > 4
                           ? ((Number) entry.get(4)).doubleValue()
                           : Double.MAX_VALUE;

        EntityRequirement requirement = new EntityRequirement(entities);

        return new EntityTempData(requirement, temp, range, units, EntityRequirement.NONE, maxEffect, false);
    }

    @Override
    public boolean test(Entity entity)
    {   return this.entity.test(entity);
    }

    public boolean test(Entity entity, Entity affectedEntity)
    {
        return (this.affectsSelf || entity != affectedEntity)
            && entity.distanceTo(affectedEntity) <= range
            && this.test(entity)
            && this.otherEntityRequirement.test(affectedEntity);
    }

    public double getTemperatureEffect(Entity entity, Entity affectedPlayer)
    {   return CSMath.blend(0, this.getTemperature(), entity.distanceTo(affectedPlayer), range, 0);
    }

    @Override
    public Codec<EntityTempData> getCodec()
    {   return CODEC;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        EntityTempData that = (EntityTempData) obj;
        return super.equals(obj)
            && Double.compare(that.temperature, temperature) == 0
            && Double.compare(that.range, range) == 0
            && entity.equals(that.entity)
            && units == that.units
            && otherEntityRequirement.equals(that.otherEntityRequirement)
            && Double.compare(that.maxEffect, maxEffect) == 0
            && affectsSelf == that.affectsSelf;
    }
}
