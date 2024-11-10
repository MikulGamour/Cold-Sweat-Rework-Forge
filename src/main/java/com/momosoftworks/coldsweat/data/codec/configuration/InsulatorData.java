package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InsulatorData
{
    public final Insulation.Slot slot;
    public final Insulation insulation;
    public final ItemRequirement data;
    public final EntityRequirement predicate;
    public final Optional<AttributeModifierMap> attributes;
    public Map<ResourceLocation, Double> immuneTempModifiers;
    public final Optional<List<String>> requiredMods;

    public InsulatorData(Insulation.Slot slot,
                         Insulation insulation, ItemRequirement data,
                         EntityRequirement predicate, Optional<AttributeModifierMap> attributes,
                         Map<ResourceLocation, Double> immuneTempModifiers,
                         Optional<List<String>> requiredMods)
    {
        this.slot = slot;
        this.insulation = insulation;
        this.data = data;
        this.predicate = predicate;
        this.attributes = attributes;
        this.requiredMods = requiredMods;
        this.immuneTempModifiers = immuneTempModifiers;
    }

    public static final Codec<InsulatorData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Insulation.Slot.CODEC.fieldOf("type").forGetter(data -> data.slot),
            Insulation.getCodec().fieldOf("insulation").forGetter(data -> data.insulation),
            ItemRequirement.CODEC.fieldOf("data").forGetter(data -> data.data),
            EntityRequirement.getCodec().optionalFieldOf("entity", EntityRequirement.NONE).forGetter(data -> data.predicate),
            AttributeModifierMap.CODEC.optionalFieldOf("attributes").forGetter(data -> data.attributes),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.DOUBLE).optionalFieldOf("immune_temp_modifiers", new HashMap<>()).forGetter(data -> data.immuneTempModifiers),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(data -> data.requiredMods)
    ).apply(instance, InsulatorData::new));

    @Override
    public String toString()
    {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        InsulatorData that = (InsulatorData) obj;
        return slot == that.slot
            && insulation.equals(that.insulation)
            && data.equals(that.data)
            && predicate.equals(that.predicate)
            && attributes.equals(that.attributes)
            && immuneTempModifiers.equals(that.immuneTempModifiers)
            && requiredMods.equals(that.requiredMods);
    }
}
