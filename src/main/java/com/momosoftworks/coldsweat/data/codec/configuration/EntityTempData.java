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
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public class EntityTempData extends ConfigData implements RequirementHolder
{
    final EntityRequirement entity;
    final double temperature;
    final double range;
    final Temperature.Units units;
    final EntityRequirement playerRequirement;
    final double maxEffect;

    public EntityTempData(EntityRequirement entity, double temperature, double range,
                          Temperature.Units units, EntityRequirement playerRequirement,
                          double maxEffect, List<String> requiredMods)
    {
        super(requiredMods);
        this.entity = entity;
        this.temperature = temperature;
        this.range = range;
        this.units = units;
        this.playerRequirement = playerRequirement;
        this.maxEffect = maxEffect;
    }

    public EntityTempData(EntityRequirement entity, double temperature, double range,
                          Temperature.Units units, EntityRequirement playerRequirement, double maxEffect)
    {
        this(entity, temperature, range, units, playerRequirement, maxEffect, ConfigHelper.getModIDs(CSMath.listOrEmpty(entity.entities()), ForgeRegistries.ENTITY_TYPES));
    }

    public static final Codec<EntityTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityRequirement.getCodec().fieldOf("entity").forGetter(EntityTempData::entity),
            Codec.DOUBLE.fieldOf("temperature").forGetter(EntityTempData::temperature),
            Codec.DOUBLE.fieldOf("range").forGetter(EntityTempData::range),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(EntityTempData::units),
            EntityRequirement.getCodec().optionalFieldOf("player", EntityRequirement.NONE).forGetter(EntityTempData::playerRequirement),
            Codec.DOUBLE.optionalFieldOf("max_effect", Double.MAX_VALUE).forGetter(EntityTempData::maxEffect),
            Codec.STRING.listOf().optionalFieldOf("required_mods", List.of()).forGetter(EntityTempData::requiredMods)
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
    {   return playerRequirement;
    }
    public double maxEffect()
    {   return maxEffect;
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
        double maxEffect = entry.size() > 4
                           ? ((Number) entry.get(4)).doubleValue()
                           : Double.MAX_VALUE;

        EntityRequirement requirement = new EntityRequirement(entities);

        return new EntityTempData(requirement, temp, range, units, EntityRequirement.NONE, maxEffect);
    }

    @Override
    public boolean test(Entity entity)
    {   return this.entity.test(entity);
    }

    public boolean test(Entity entity, Entity affectedPlayer)
    {
        return entity.distanceTo(affectedPlayer) <= range
            && this.test(entity)
            && this.playerRequirement.test(affectedPlayer);
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
            && playerRequirement.equals(that.playerRequirement);
    }
}
