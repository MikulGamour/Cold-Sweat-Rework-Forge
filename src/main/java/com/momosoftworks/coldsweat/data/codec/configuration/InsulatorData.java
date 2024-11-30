package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.insulation.AdaptiveInsulation;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.insulation.StaticInsulation;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.impl.RequirementHolder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.NbtRequirement;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.*;

public class InsulatorData extends ConfigData implements RequirementHolder
{
    final Insulation.Slot slot;
    final Insulation insulation;
    final ItemRequirement data;
    final EntityRequirement predicate;
    final AttributeModifierMap attributes;
    final Map<ResourceLocation, Double> immuneTempModifiers;
    final Optional<List<String>> requiredMods;

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
        this.immuneTempModifiers = immuneTempModifiers;
        this.requiredMods = requiredMods;
    }

    public InsulatorData(Insulation.Slot slot, Insulation insulation, ItemRequirement data,
                         EntityRequirement predicate, AttributeModifierMap attributes,
                         Map<ResourceLocation, Double> immuneTempModifiers)
    {
        this(slot, insulation, data, predicate, attributes, immuneTempModifiers, Optional.empty());
    }

    public static final Codec<InsulatorData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Insulation.Slot.CODEC.fieldOf("type").forGetter(InsulatorData::slot),
            Insulation.getCodec().fieldOf("insulation").forGetter(InsulatorData::insulation),
            com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement.CODEC.fieldOf("data").forGetter(InsulatorData::data),
            EntityRequirement.getCodec().optionalFieldOf("entity", EntityRequirement.NONE).forGetter(InsulatorData::predicate),
            com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap.CODEC.optionalFieldOf("attributes", new AttributeModifierMap()).forGetter(InsulatorData::attributes),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.DOUBLE).optionalFieldOf("immune_temp_modifiers", new HashMap<>()).forGetter(InsulatorData::immuneTempModifiers),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(InsulatorData::requiredMods)
    ).apply(instance, InsulatorData::new));

    public Insulation.Slot slot()
    {   return slot;
    }
    public Insulation insulation()
    {   return insulation;
    }
    public ItemRequirement data()
    {   return data;
    }
    public EntityRequirement predicate()
    {   return predicate;
    }
    public AttributeModifierMap attributes()
    {   return attributes;
    }
    public Map<ResourceLocation, Double> immuneTempModifiers()
    {   return immuneTempModifiers;
    }
    public Optional<List<String>> requiredMods()
    {   return requiredMods;
    }

    @Override
    public boolean test(ItemStack stack)
    {   return data.test(stack, true);
    }

    @Override
    public boolean test(Entity entity)
    {   return entity == null || predicate.test(entity);
    }

    @Nullable
    public static InsulatorData fromToml(List<?> entry, Insulation.Slot slot)
    {
        String[] itemIDs = ((String) entry.get(0)).split(",");
        List<Either<TagKey<Item>, Item>> items = ConfigHelper.getItems(itemIDs);
        if (items.isEmpty())
        {   ColdSweat.LOGGER.error("Error parsing {} insulator config: string \"{}\" does not contain any valid items", slot.getSerializedName(), entry.get(0));
            return null;
        }
        if (entry.size() < 3)
        {   ColdSweat.LOGGER.error("Error parsing {} insulator config: not enough arguments", slot.getSerializedName());
            return null;
        }

        boolean adaptive = entry.size() > 3 && entry.get(3).equals("adaptive");
        CompoundTag tag = entry.size() > 4 ? NBTHelper.parseCompoundNbt((String) entry.get(4)) : new CompoundTag();
        double insulVal1 = ((Number) entry.get(1)).doubleValue();
        double insulVal2 = ((Number) entry.get(2)).doubleValue();

        Insulation insulation = adaptive ? new AdaptiveInsulation(insulVal1, insulVal2)
                                         : new StaticInsulation(insulVal1, insulVal2);

        ItemRequirement requirement = new ItemRequirement(items, new NbtRequirement(tag));

        return new InsulatorData(slot, insulation, requirement, EntityRequirement.NONE, new AttributeModifierMap(), new HashMap<>(), Optional.empty());
    }

    @Override
    public Codec<InsulatorData> getCodec()
    {   return CODEC;
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
