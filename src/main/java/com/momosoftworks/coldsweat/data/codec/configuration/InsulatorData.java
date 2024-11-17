package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.insulation.AdaptiveInsulation;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.insulation.StaticInsulation;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.NbtRequirement;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;

public class InsulatorData implements NbtSerializable, RequirementHolder
{
    public final Insulation.Slot slot;
    public final Insulation insulation;
    public final ItemRequirement data;
    public final EntityRequirement predicate;
    public final AttributeModifierMap attributes;
    public Map<ResourceLocation, Double> immuneTempModifiers;
    public final Optional<List<String>> requiredMods;

    public InsulatorData(Insulation.Slot slot,
                         Insulation insulation, ItemRequirement data,
                         EntityRequirement predicate, AttributeModifierMap attributes,
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

    public InsulatorData(Insulation.Slot slot, Insulation insulation, ItemRequirement data,
                         EntityRequirement predicate, AttributeModifierMap attributes,
                         Map<ResourceLocation, Double> immuneTempModifiers)
    {
        this(slot, insulation, data, predicate, attributes, immuneTempModifiers, Optional.empty());
    }

    public static final Codec<InsulatorData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Insulation.Slot.CODEC.fieldOf("type").forGetter(data -> data.slot),
            Insulation.getCodec().fieldOf("insulation").forGetter(data -> data.insulation),
            ItemRequirement.CODEC.fieldOf("data").forGetter(data -> data.data),
            EntityRequirement.getCodec().optionalFieldOf("entity", EntityRequirement.NONE).forGetter(data -> data.predicate),
            AttributeModifierMap.CODEC.optionalFieldOf("attributes", new AttributeModifierMap()).forGetter(data -> data.attributes),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.DOUBLE).optionalFieldOf("immune_temp_modifiers", new HashMap<>()).forGetter(data -> data.immuneTempModifiers),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(data -> data.requiredMods)
    ).apply(instance, InsulatorData::new));

    @Override
    public boolean test(ItemStack stack)
    {   return data.test(stack, true);
    }

    @Override
    public boolean test(Entity entity)
    {   return predicate.test(entity);
    }

    @Nullable
    public static InsulatorData fromToml(List<?> entry, Insulation.Slot slot)
    {
        String[] itemIDs = ((String) entry.get(0)).split(",");
        List<Item> items = ConfigHelper.getItems(itemIDs);
        if (items.isEmpty())
        {   ColdSweat.LOGGER.error("Error parsing {} insulator config: string \"{}\" does not contain any valid items", slot.getSerializedName(), entry.get(0));
            return null;
        }
        if (entry.size() < 3)
        {   ColdSweat.LOGGER.error("Error parsing {} insulator config: not enough arguments", slot.getSerializedName());
            return null;
        }

        boolean adaptive = entry.size() < 4 || entry.get(3).equals("adaptive");
        CompoundNBT tag = entry.size() > 4 ? NBTHelper.parseCompoundNbt((String) entry.get(4)) : new CompoundNBT();
        double insulVal1 = ((Number) entry.get(1)).doubleValue();
        double insulVal2 = ((Number) entry.get(2)).doubleValue();

        Insulation insulation = adaptive ? new AdaptiveInsulation(insulVal1, insulVal2)
                                         : new StaticInsulation(insulVal1, insulVal2);

        ItemRequirement requirement = new ItemRequirement(items, new NbtRequirement(tag));

        return new InsulatorData(slot, insulation, requirement, EntityRequirement.NONE, new AttributeModifierMap(), new HashMap<>(), Optional.empty());
    }

    @Override
    public CompoundNBT serialize()
    {   return (CompoundNBT) CODEC.encodeStart(NBTDynamicOps.INSTANCE, this).result().orElseGet(CompoundNBT::new);
    }

    public static InsulatorData deserialize(CompoundNBT tag)
    {   return CODEC.decode(NBTDynamicOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalArgumentException("Could not deserialize Insulator")).getFirst();
    }

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
