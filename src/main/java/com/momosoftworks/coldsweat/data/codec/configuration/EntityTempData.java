package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.PlayerDataRequirement;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class EntityTempData implements RequirementHolder
{
    public final EntityRequirement entity;
    public final double temperature;
    public final double range;
    public final Temperature.Units units;
    public final Optional<PlayerDataRequirement> playerRequirement;
    public final Optional<List<String>> requiredMods;

    public EntityTempData(EntityRequirement entity, double temperature, double range,
                          Temperature.Units units,
                          Optional<PlayerDataRequirement> playerRequirement,
                          Optional<List<String>> requiredMods)
    {
        this.entity = entity;
        this.temperature = temperature;
        this.range = range;
        this.units = units;
        this.playerRequirement = playerRequirement;
        this.requiredMods = requiredMods;
    }

    public static final Codec<EntityTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityRequirement.getCodec().fieldOf("entity").forGetter(data -> data.entity),
            Codec.DOUBLE.fieldOf("temperature").forGetter(data -> data.temperature),
            Codec.DOUBLE.fieldOf("range").forGetter(data -> data.range),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(data -> data.units),
            PlayerDataRequirement.CODEC.optionalFieldOf("player").forGetter(data -> data.playerRequirement),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(data -> data.requiredMods)
    ).apply(instance, (entity, temperature, range, units, playerRequirement, requiredMods) ->
    {
        double cTemp = Temperature.convert(temperature, units, Temperature.Units.MC, false);
        return new EntityTempData(entity, cTemp, range, units, playerRequirement, requiredMods);
    }));

    @Nullable
    public static EntityTempData fromToml(List<?> entry)
    {
        if (entry.size() < 3)
        {   return null;
        }
        String entityID = (String) entry.get(0);
        List<EntityType<?>> entities = ConfigHelper.getEntityTypes(entityID);
        if (entities.isEmpty())
        {   return null;
        }
        double temp = ((Number) entry.get(1)).doubleValue();
        double range = ((Number) entry.get(2)).doubleValue();
        Temperature.Units units = entry.size() > 3
                                  ? Temperature.Units.fromID((String) entry.get(3))
                                  : Temperature.Units.MC;

        EntityRequirement requirement = new EntityRequirement(entities);

        return new EntityTempData(requirement, temp, range, units, Optional.empty(), Optional.empty());
    }

    @Override
    public boolean test(Entity entity)
    {   return this.entity.test(entity);
    }

    public boolean test(Entity entity, Entity affectedPlayer)
    {
        return entity.distanceTo(affectedPlayer) <= range
            && this.test(entity)
            && this.playerRequirement.map(req -> req.test(affectedPlayer)).orElse(true);
    }

    public double getTemperature(Entity entity, Entity affectedPlayer)
    {   return CSMath.blend(0, this.temperature, entity.distanceTo(affectedPlayer), range, 0);
    }

    @Override
    public String toString()
    {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        EntityTempData that = (EntityTempData) obj;
        return Double.compare(that.temperature, temperature) == 0
            && Double.compare(that.range, range) == 0
            && entity.equals(that.entity)
            && units == that.units
            && playerRequirement.equals(that.playerRequirement)
            && requiredMods.equals(that.requiredMods);
    }
}
