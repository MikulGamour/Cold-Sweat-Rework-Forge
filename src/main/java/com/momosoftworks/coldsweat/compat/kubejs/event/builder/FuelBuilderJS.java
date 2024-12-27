package com.momosoftworks.coldsweat.compat.kubejs.event.builder;

import com.momosoftworks.coldsweat.data.codec.configuration.FuelData;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

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
        this.items.addAll(RegistryHelper.mapTaggableList(ConfigHelper.getItems(items)));
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
        FuelData data = new FuelData(fuelType, this.fuel, new ItemRequirement(this.items, this.itemPredicate));
        data.setType(ConfigData.Type.KUBEJS);
        return data;
    }
}
