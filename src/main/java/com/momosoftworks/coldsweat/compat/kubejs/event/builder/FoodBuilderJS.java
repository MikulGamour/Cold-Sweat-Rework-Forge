package com.momosoftworks.coldsweat.compat.kubejs.event.builder;

import com.momosoftworks.coldsweat.config.type.PredicateItem;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

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
        this.items.addAll(Arrays.stream(items).map(key -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(key))).toList());
        return this;
    }

    public FoodBuilderJS itemTag(String tag)
    {
        items.addAll(ForgeRegistries.ITEMS.tags().getTag(TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(tag))).stream().toList());
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

    public PredicateItem build()
    {
        CompoundTag extraData = new CompoundTag();
        if (this.duration != -1)
        {   extraData.putInt("duration", this.duration);
        }
        return new PredicateItem(this.temperature, new ItemRequirement(this.itemPredicate), new EntityRequirement(this.entityPredicate), extraData);
    }
}
