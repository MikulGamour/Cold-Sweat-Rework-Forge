package com.momosoftworks.coldsweat.compat.kubejs.event.builder;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.data.codec.configuration.ItemCarryTempData;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CarriedItemBuilderJS
{
    public final Set<Item> items = new HashSet<>();
    public final Set<Either<IntegerBounds, EquipmentSlotType>> slots = new HashSet<>();
    public double temperature = 0;
    public double maxEffect = 0;
    public Temperature.Trait trait = Temperature.Trait.WORLD;
    public Predicate<ItemStack> itemPredicate = item -> true;
    public Predicate<Entity> entityPredicate = entity -> true;

    public CarriedItemBuilderJS()
    {}

    public CarriedItemBuilderJS items(String... items)
    {
        this.items.addAll(Arrays.stream(items).map(key -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(key))).collect(Collectors.toList()));
        return this;
    }

    public CarriedItemBuilderJS itemTag(String tag)
    {
        items.addAll(ItemTags.getAllTags().getTag(new ResourceLocation(tag)).getValues());
        return this;
    }

    public CarriedItemBuilderJS temperature(double temperature)
    {
        this.temperature = temperature;
        return this;
    }

    public CarriedItemBuilderJS maxEffect(double maxEffect)
    {
        this.maxEffect = maxEffect;
        return this;
    }

    public CarriedItemBuilderJS trait(String trait)
    {
        this.trait = Temperature.Trait.fromID(trait);
        return this;
    }

    public CarriedItemBuilderJS slots(int... slots)
    {
        for (int slot : slots)
        {   this.slots.add(Either.left(new IntegerBounds(slot, slot)));
        }
        return this;
    }

    public CarriedItemBuilderJS slotsInRange(int min, int max)
    {
        this.slots.add(Either.left(new IntegerBounds(min, max)));
        return this;
    }

    public CarriedItemBuilderJS equipmentSlots(String... slots)
    {
        for (String slot : slots)
        {   this.slots.add(Either.right(EquipmentSlotType.byName(slot)));
        }
        return this;
    }

    public CarriedItemBuilderJS itemPredicate(Predicate<ItemStack> itemPredicate)
    {
        this.itemPredicate = itemPredicate;
        return this;
    }

    public CarriedItemBuilderJS entityPredicate(Predicate<Entity> entityPredicate)
    {
        this.entityPredicate = entityPredicate;
        return this;
    }

    public ItemCarryTempData build()
    {
        ItemCarryTempData data = new ItemCarryTempData(new ItemRequirement(this.itemPredicate), ImmutableList.copyOf(this.slots),
                                                       this.temperature, this.trait, maxEffect, new EntityRequirement(this.entityPredicate));
        data.setType(ConfigData.Type.KUBEJS);
        return data;
    }
}
