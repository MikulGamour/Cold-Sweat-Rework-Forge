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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class EntityTempData extends ConfigData implements RequirementHolder
{
    final EntityRequirement entity;
    final double temperature;
    final double range;
    final Temperature.Units units;

    public EntityTempData(EntityRequirement entity, double temperature, double range,
                          Temperature.Units units, List<String> requiredMods)
    {
        super(requiredMods);
        this.entity = entity;
        this.temperature = temperature;
        this.range = range;
        this.units = units;
    }

    public EntityTempData(EntityRequirement entity, double temperature, double range,
                          Temperature.Units units)
    {
        this(entity, temperature, range, units, ConfigHelper.getModIDs(CSMath.listOrEmpty(entity.entities()), BuiltInRegistries.ENTITY_TYPE));
    }

    public static final Codec<EntityTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityRequirement.getCodec().fieldOf("entity").forGetter(EntityTempData::entity),
            Codec.DOUBLE.fieldOf("temperature").forGetter(EntityTempData::temperature),
            Codec.DOUBLE.fieldOf("range").forGetter(EntityTempData::temperature),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(EntityTempData::units),
            Codec.STRING.listOf().optionalFieldOf("required_mods", List.of()).forGetter(EntityTempData::requiredMods)
    ).apply(instance, (entity, temperature, range, units, requiredMods) ->
    {
        double cTemp = Temperature.convert(temperature, units, Temperature.Units.MC, false);
        return new EntityTempData(entity, cTemp, range, units, requiredMods);
    }));

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

    @Nullable
    public static EntityTempData fromToml(List<?> entry)
    {
        if (entry.size() < 3)
        {   ColdSweat.LOGGER.error("Error parsing entity config: not enough arguments");
            return null;
        }
        List<Either<TagKey<EntityType<?>>, EntityType<?>>> entities = ConfigHelper.getEntityTypes((String) entry.get(0));

        if (entities.isEmpty())
        {   ColdSweat.LOGGER.error("Error parsing entity config: {} does not contain any valid entities", entry);
            return null;
        }
        double temp = ((Number) entry.get(1)).doubleValue();
        double range = ((Number) entry.get(2)).doubleValue();
        Temperature.Units units = entry.size() > 3
                                  ? Temperature.Units.fromID((String) entry.get(3))
                                  : Temperature.Units.MC;

        EntityRequirement requirement = new EntityRequirement(entities);

        return new EntityTempData(requirement, temp, range, units);
    }

    @Override
    public boolean test(Entity entity)
    {   return this.entity.test(entity);
    }

    public boolean test(Entity entity, Entity affectedPlayer)
    {
        return entity.distanceTo(affectedPlayer) <= range
            && this.test(entity);
    }

    public double getTemperature(Entity entity, Entity affectedPlayer)
    {   return CSMath.blend(0, this.temperature, entity.distanceTo(affectedPlayer), range, 0);
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
            && units == that.units;
    }
}
