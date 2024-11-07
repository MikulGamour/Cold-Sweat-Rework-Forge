package com.momosoftworks.coldsweat.config.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;


public class Insulator implements NbtSerializable
{
    public final Insulation insulation;
    public final Insulation.Slot slot;
    public final ItemRequirement data;
    public final EntityRequirement predicate;
    public final AttributeModifierMap attributes;
    public final Map<ResourceLocation, Double> immuneTempModifiers;

    public Insulator(Insulation insulation, Insulation.Slot slot, ItemRequirement data,
                     EntityRequirement predicate, AttributeModifierMap attributes,
                     Map<ResourceLocation, Double> immuneTempModifiers)
    {
        this.insulation = insulation;
        this.slot = slot;
        this.data = data;
        this.predicate = predicate;
        this.attributes = attributes;
        this.immuneTempModifiers = immuneTempModifiers;
    }
    public boolean test(Entity entity, ItemStack stack)
    {   return predicate.test(entity) && data.test(stack, true);
    }

    public static final Codec<Insulator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Insulation.getCodec().fieldOf("insulation").forGetter(data -> data.insulation),
            Insulation.Slot.CODEC.fieldOf("slot").forGetter(data -> data.slot),
            ItemRequirement.CODEC.fieldOf("data").forGetter(data -> data.data),
            EntityRequirement.getCodec().optionalFieldOf("predicate", EntityRequirement.NONE).forGetter(data -> data.predicate),
            AttributeModifierMap.CODEC.optionalFieldOf("attributes", new AttributeModifierMap()).forGetter(data -> data.attributes),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.DOUBLE).optionalFieldOf("immune_temp_modifiers", new HashMap<>()).forGetter(data -> data.immuneTempModifiers)
    ).apply(instance, Insulator::new));

    @Override
    public CompoundNBT serialize()
    {   return (CompoundNBT) CODEC.encodeStart(NBTDynamicOps.INSTANCE, this).result().orElseGet(CompoundNBT::new);
    }

    public static Insulator deserialize(CompoundNBT tag)
    {   return CODEC.decode(NBTDynamicOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalArgumentException("Could not deserialize Insulator")).getFirst();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Insulator insulator = (Insulator) obj;
        return insulation.equals(insulator.insulation)
            && data.equals(insulator.data)
            && predicate.equals(insulator.predicate)
            && attributes.equals(insulator.attributes);
    }
}
