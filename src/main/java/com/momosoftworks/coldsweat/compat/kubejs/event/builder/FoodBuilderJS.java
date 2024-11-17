package com.momosoftworks.coldsweat.compat.kubejs.event.builder;

import com.momosoftworks.coldsweat.data.codec.configuration.FoodData;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        this.items.addAll(Arrays.stream(items).map(key -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(key))).collect(Collectors.toList()));
        return this;
    }

    public FoodBuilderJS itemTag(String tag)
    {
        items.addAll(ItemTags.getAllTags().getTag(new ResourceLocation(tag)).getValues());
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
        return new FoodData(this.temperature, new ItemRequirement(this.itemPredicate), this.duration, new EntityRequirement(this.entityPredicate));
    }
}
