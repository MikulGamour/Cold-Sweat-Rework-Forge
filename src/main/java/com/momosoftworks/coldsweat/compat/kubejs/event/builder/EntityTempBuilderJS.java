package com.momosoftworks.coldsweat.compat.kubejs.event.builder;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.data.codec.configuration.EntityTempData;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class EntityTempBuilderJS
{
    public final Set<EntityType<?>> entities = new HashSet<>();
    public double temperature = 0;
    public double range = 0;
    public Temperature.Units units = Temperature.Units.MC;
    public double maxEffect = 0;
    public Predicate<Entity> entityPredicate = null;
    public Predicate<Entity> otherEntityPredicate = null;
    public boolean affectsSelf = false;

    public EntityTempBuilderJS()
    {}

    public EntityTempBuilderJS entities(String... entities)
    {
        this.entities.addAll(RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ENTITY_TYPES, ConfigHelper.getEntityTypes(entities)));
        return this;
    }

    public EntityTempBuilderJS temperature(double temperature)
    {
        this.temperature = temperature;
        return this;
    }

    public EntityTempBuilderJS range(double range)
    {
        this.range = range;
        return this;
    }

    public EntityTempBuilderJS units(String units)
    {
        this.units = Temperature.Units.fromID(units);
        return this;
    }

    public EntityTempBuilderJS maxEffect(double maxEffect)
    {
        this.maxEffect = maxEffect;
        return this;
    }

    public EntityTempBuilderJS entityPredicate(Predicate<Entity> entityPredicate)
    {
        this.entityPredicate = entityPredicate;
        return this;
    }

    public EntityTempBuilderJS otherEntityPredicate(Predicate<Entity> otherEntityPredicate)
    {
        this.otherEntityPredicate = otherEntityPredicate;
        return this;
    }

    public EntityTempBuilderJS affectsSelf(boolean affectsSelf)
    {
        this.affectsSelf = affectsSelf;
        return this;
    }

    public EntityTempData build()
    {
        EntityTempData data = new EntityTempData(new EntityRequirement(this.entities, this.entityPredicate), this.temperature, this.range, this.units,
                                                 new EntityRequirement(this.otherEntityPredicate), this.maxEffect, this.affectsSelf);
        data.setType(ConfigData.Type.KUBEJS);
        return data;
    }
}
