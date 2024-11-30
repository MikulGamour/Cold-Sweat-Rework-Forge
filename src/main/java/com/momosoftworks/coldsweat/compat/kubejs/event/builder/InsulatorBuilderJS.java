package com.momosoftworks.coldsweat.compat.kubejs.event.builder;

import com.momosoftworks.coldsweat.api.insulation.AdaptiveInsulation;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.insulation.StaticInsulation;
import com.momosoftworks.coldsweat.data.codec.configuration.InsulatorData;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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
        this.items.addAll(Arrays.stream(items).map(key -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(key))).toList());
        return this;
    }

    public InsulatorBuilderJS itemTag(String tag)
    {
        items.addAll(BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, ResourceLocation.parse(tag))).orElseThrow().stream().map(Holder::value).toList());
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
        attributes.put(BuiltInRegistries.ATTRIBUTE.get(ResourceLocation.parse(attribute)),
                       new AttributeModifier(ResourceLocation.fromNamespaceAndPath("kubejs", "script"), amount, AttributeModifier.Operation.valueOf(operation.toUpperCase(Locale.ROOT))));
        return this;
    }

    public InsulatorBuilderJS immuneToModifier(String modifierId, double immunity)
    {
        immuneTempModifiers.put(ResourceLocation.parse(modifierId), immunity);
        return this;
    }

    public InsulatorData build()
    {
        InsulatorData data = new InsulatorData(slot, insulation, new ItemRequirement(itemPredicate), new EntityRequirement(entityPredicate),
                                               attributes, immuneTempModifiers);
        data.setType(ConfigData.Type.KUBEJS);
        return data;
    }
}
