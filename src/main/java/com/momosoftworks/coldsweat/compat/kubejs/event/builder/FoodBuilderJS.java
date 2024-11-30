package com.momosoftworks.coldsweat.compat.kubejs.event.builder;

import com.momosoftworks.coldsweat.data.codec.configuration.FoodData;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;
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
        this.items.addAll(Arrays.stream(items).map(key -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(key))).toList());
        return this;
    }

    public FoodBuilderJS itemTag(String tag)
    {
        items.addAll(BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, ResourceLocation.parse(tag))).orElseThrow().stream().map(Holder::value).toList());
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
