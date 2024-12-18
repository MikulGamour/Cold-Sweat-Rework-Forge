package com.momosoftworks.coldsweat.compat.kubejs.event.builder;

import com.momosoftworks.coldsweat.data.codec.configuration.FoodData;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class FoodBuilderJS
{
    public final Set<Item> items = new HashSet<>();
    public double temperature = 0;
    public int duration = -1;
    public Predicate<ItemStack> itemPredicate = item -> true;
    public Predicate<Entity> entityPredicate = entity -> true;

    public FoodBuilderJS()
    {}

    public FoodBuilderJS items(String... items)
    {
        this.items.addAll(RegistryHelper.mapTaggableList(ConfigHelper.getItems(items)));
        return this;
    }

    public FoodBuilderJS temperature(double temperature)
    {
        this.temperature = temperature;
        return this;
    }

    public FoodBuilderJS duration(int duration)
    {
        this.duration = duration;
        return this;
    }

    public FoodBuilderJS itemPredicate(Predicate<ItemStack> itemPredicate)
    {
        this.itemPredicate = itemPredicate;
        return this;
    }

    public FoodBuilderJS entityPredicate(Predicate<Entity> entityPredicate)
    {
        this.entityPredicate = entityPredicate;
        return this;
    }

    public FoodData build()
    {
        FoodData data = new FoodData(this.temperature, new ItemRequirement(this.itemPredicate), this.duration, new EntityRequirement(this.entityPredicate));
        data.setType(ConfigData.Type.KUBEJS);
        return data;
    }
}
