package com.momosoftworks.coldsweat.compat.kubejs.event.builder;

import com.momosoftworks.coldsweat.api.insulation.AdaptiveInsulation;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.insulation.StaticInsulation;
import com.momosoftworks.coldsweat.config.type.Insulator;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.function.Predicate;

public class InsulatorBuilderJS
{
    public final Set<Item> items = new HashSet<>();
    public Insulation insulation;
    public Insulation.Slot slot;
    public Predicate<ItemStack> itemPredicate = item -> true;
    public Predicate<Entity> entityPredicate = entity -> true;
    public AttributeModifierMap attributes = new AttributeModifierMap();
    public Map<ResourceLocation, Double> immuneTempModifiers = new HashMap<>();

    public InsulatorBuilderJS()
    {}

    public InsulatorBuilderJS items(String... items)
    {
        this.items.addAll(Arrays.stream(items).map(key -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(key))).toList());
        return this;
    }

    public InsulatorBuilderJS itemTag(String tag)
    {
        items.addAll(ForgeRegistries.ITEMS.tags().getTag(TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(tag))).stream().toList());
        return this;
    }

    public InsulatorBuilderJS insulation(double cold, double heat)
    {
        this.insulation = new StaticInsulation(cold, heat);
        return this;
    }

    public InsulatorBuilderJS adaptiveInsulation(double insulation, double speed)
    {
        this.insulation = new AdaptiveInsulation(insulation, speed);
        return this;
    }

    public InsulatorBuilderJS slot(String slot)
    {
        this.slot = Insulation.Slot.byName(slot);
        return this;
    }

    public InsulatorBuilderJS itemPredicate(Predicate<ItemStack> itemPredicate)
    {
        this.itemPredicate = itemPredicate;
        return this;
    }

    public InsulatorBuilderJS entityPredicate(Predicate<Entity> entityPredicate)
    {
        this.entityPredicate = entityPredicate;
        return this;
    }

    public InsulatorBuilderJS attribute(String attribute, double amount, String operation)
    {
        attributes.put(ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attribute)),
                       new AttributeModifier("kubejs", amount, AttributeModifier.Operation.valueOf(operation.toUpperCase(Locale.ROOT))));
        return this;
    }

    public InsulatorBuilderJS immuneToModifier(String modifierId, double immunity)
    {
        immuneTempModifiers.put(new ResourceLocation(modifierId), immunity);
        return this;
    }

    public Insulator build()
    {
        return new Insulator(insulation, slot, new ItemRequirement(itemPredicate), new EntityRequirement(entityPredicate),
                             attributes, immuneTempModifiers);
    }
}
