package com.momosoftworks.coldsweat.compat.kubejs.event.builder;

import com.momosoftworks.coldsweat.data.codec.configuration.FuelData;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class FuelBuilderJS
{
    public final Set<Item> items = new HashSet<>();
    public double fuel = 0;
    public Predicate<ItemStack> itemPredicate = item -> true;

    public FuelBuilderJS()
    {}

    public FuelBuilderJS items(String... items)
    {
        this.items.addAll(Arrays.stream(items).map(key -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(key))).toList());
        return this;
    }

    public FuelBuilderJS itemTag(String tag)
    {
        items.addAll(BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, ResourceLocation.parse(tag))).orElseThrow().stream().map(Holder::value).toList());
        return this;
    }

    public FuelBuilderJS temperature(double temperature)
    {
        this.fuel = temperature;
        return this;
    }

    public FuelBuilderJS itemPredicate(Predicate<ItemStack> itemPredicate)
    {
        this.itemPredicate = itemPredicate;
        return this;
    }

    public FuelData build(FuelData.FuelType fuelType)
    {
        FuelData data = new FuelData(fuelType, this.fuel, new ItemRequirement(this.itemPredicate));
        data.setType(ConfigData.Type.KUBEJS);
        return data;
    }
}
