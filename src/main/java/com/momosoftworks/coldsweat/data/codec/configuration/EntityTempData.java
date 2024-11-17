package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.PlayerDataRequirement;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record EntityTempData(EntityRequirement entity, double temperature, double range,
                             Temperature.Units units,
                             Optional<PlayerDataRequirement> playerRequirement,
                             Optional<List<String>> requiredMods) implements RequirementHolder, IForgeRegistryEntry<EntityTempData>
{
    public static final Codec<EntityTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityRequirement.getCodec().fieldOf("entity").forGetter(EntityTempData::entity),
            Codec.DOUBLE.fieldOf("temperature").forGetter(EntityTempData::temperature),
            Codec.DOUBLE.fieldOf("range").forGetter(EntityTempData::temperature),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(EntityTempData::units),
            PlayerDataRequirement.CODEC.optionalFieldOf("player").forGetter(EntityTempData::playerRequirement),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(EntityTempData::requiredMods)
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

    @Override
    public EntityTempData setRegistryName(ResourceLocation resourceLocation)
    {   return this;
    }

    @Override
    public @Nullable ResourceLocation getRegistryName()
    {   return null;
    }

    @Override
    public Class<EntityTempData> getRegistryType()
    {   return EntityTempData.class;
    }
}
