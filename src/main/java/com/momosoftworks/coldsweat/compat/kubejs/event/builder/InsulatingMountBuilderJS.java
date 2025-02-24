package com.momosoftworks.coldsweat.compat.kubejs.event.builder;

import com.momosoftworks.coldsweat.data.codec.configuration.MountData;
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

public class InsulatingMountBuilderJS
{
    public final Set<EntityType<?>> entities = new HashSet<>();
    public Predicate<Entity> entityPredicate = null;
    public Predicate<Entity> riderPredicate = null;
    public double coldInsulation = 0;
    public double heatInsulation = 0;

    public InsulatingMountBuilderJS()
    {}

    public InsulatingMountBuilderJS entities(String... entities)
    {
        this.entities.addAll(RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ENTITY_TYPES, ConfigHelper.getEntityTypes(entities)));
        return this;
    }

    public InsulatingMountBuilderJS entityPredicate(Predicate<Entity> entityPredicate)
    {
        this.entityPredicate = entityPredicate;
        return this;
    }

    public InsulatingMountBuilderJS riderPredicate(Predicate<Entity> riderPredicate)
    {
        this.riderPredicate = riderPredicate;
        return this;
    }

    public InsulatingMountBuilderJS coldInsulation(double coldInsulation)
    {
        this.coldInsulation = coldInsulation;
        return this;
    }

    public InsulatingMountBuilderJS heatInsulation(double heatInsulation)
    {
        this.heatInsulation = heatInsulation;
        return this;
    }

    public MountData build()
    {
        MountData data = new MountData(new EntityRequirement(this.entities, this.entityPredicate), new EntityRequirement(this.riderPredicate), this.coldInsulation, this.heatInsulation);
        data.setType(ConfigData.Type.KUBEJS);
        return data;
    }
}
