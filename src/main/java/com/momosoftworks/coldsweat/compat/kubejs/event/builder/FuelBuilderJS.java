package com.momosoftworks.coldsweat.compat.kubejs.event.builder;

import com.momosoftworks.coldsweat.config.type.PredicateItem;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FuelBuilderJS
{
    public final Set<Item> items = new HashSet<>();
    public double fuel = 0;
    public Predicate<ItemStack> itemPredicate = item -> true;

    public FuelBuilderJS()
    {}

    public FuelBuilderJS items(String... items)
    {
        this.items.addAll(Arrays.stream(items).map(key -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(key))).collect(Collectors.toList()));
        return this;
    }

    public FuelBuilderJS itemTag(String tag)
    {
        items.addAll(ItemTags.getAllTags().getTag(new ResourceLocation(tag)).getValues());
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

    public PredicateItem build()
    {
        return new PredicateItem(this.fuel, new ItemRequirement(this.itemPredicate), EntityRequirement.NONE, new CompoundNBT());
    }
}
