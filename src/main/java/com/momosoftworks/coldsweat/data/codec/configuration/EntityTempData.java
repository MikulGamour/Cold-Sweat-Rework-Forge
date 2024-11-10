package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.sub_type.EntitySubRequirement;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Optional;

public record EntityTempData(EntityRequirement entity, double temperature, double range,
                             Temperature.Units units,
                             Optional<List<String>> requiredMods)
{
    public static final Codec<EntityTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityRequirement.getCodec().fieldOf("entity").forGetter(EntityTempData::entity),
            Codec.DOUBLE.fieldOf("temperature").forGetter(EntityTempData::temperature),
            Codec.DOUBLE.fieldOf("range").forGetter(EntityTempData::temperature),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(EntityTempData::units),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(EntityTempData::requiredMods)
    ).apply(instance, EntityTempData::new));

    public boolean test(Entity entity)
    {   return this.entity().test(entity);
    }

    public boolean test(Entity entity, Entity affectedPlayer)
    {
        return entity.distanceTo(affectedPlayer) <= range
            && this.entity().test(entity);
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
            && requiredMods.equals(that.requiredMods);
    }
}
