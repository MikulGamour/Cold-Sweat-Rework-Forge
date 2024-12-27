package com.momosoftworks.coldsweat.compat.kubejs.event.builder;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.insulation.AdaptiveInsulation;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.insulation.StaticInsulation;
import com.momosoftworks.coldsweat.api.registry.TempModifierRegistry;
import com.momosoftworks.coldsweat.compat.kubejs.util.KubeHelper;
import com.momosoftworks.coldsweat.data.codec.configuration.InsulatorData;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        this.items.addAll(RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.ITEMS, ConfigHelper.getItems(items)));
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

    public InsulatorBuilderJS attribute(String attributeId, double amount, String operation)
    {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attributeId));
        if (!KubeHelper.expect(attributeId, attribute, Attribute.class))
        {   return this;
        }
        attributes.put(attribute, new AttributeModifier("kubejs", amount, AttributeModifier.Operation.valueOf(operation.toUpperCase(Locale.ROOT))));
        return this;
    }

    public InsulatorBuilderJS immuneToModifier(String modifierId, double immunity)
    {
        ResourceLocation location = new ResourceLocation(modifierId);
        if (!TempModifierRegistry.getEntries().containsKey(location))
        {
            ColdSweat.LOGGER.warn("Tried to add immunity to non-existent temperature modifier: {}", location);
            return this;
        }
        immuneTempModifiers.put(new ResourceLocation(modifierId), immunity);
        return this;
    }

    public InsulatorData build()
    {
        InsulatorData data = new InsulatorData(slot, insulation, new ItemRequirement(this.items, this.itemPredicate), new EntityRequirement(entityPredicate),
                                               attributes, immuneTempModifiers);
        data.setType(ConfigData.Type.KUBEJS);
        return data;
    }
}
