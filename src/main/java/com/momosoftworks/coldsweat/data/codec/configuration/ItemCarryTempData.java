package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.impl.RequirementHolder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemComponentsRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.FastMap;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ItemCarryTempData extends ConfigData implements RequirementHolder
{
    final ItemRequirement data;
    final List<Either<IntegerBounds, SlotType>> slots;
    final double temperature;
    final Temperature.Trait trait;
    final Double maxEffect;
    final EntityRequirement entityRequirement;
    final AttributeModifierMap attributeModifiers;
    final Map<ResourceLocation, Double> immuneTempModifiers;

    public ItemCarryTempData(ItemRequirement data, List<Either<IntegerBounds, SlotType>> slots, double temperature,
                             Temperature.Trait trait, Double maxEffect, EntityRequirement entityRequirement, AttributeModifierMap attributeModifiers,
                             Map<ResourceLocation, Double> immuneTempModifiers, List<String> requiredMods)
    {
        super(requiredMods);
        this.data = data;
        this.slots = slots;
        this.temperature = temperature;
        this.trait = trait;
        this.maxEffect = maxEffect;
        this.entityRequirement = entityRequirement;
        this.attributeModifiers = attributeModifiers;
        this.immuneTempModifiers = immuneTempModifiers;
    }

    public ItemCarryTempData(ItemRequirement data, List<Either<IntegerBounds, SlotType>> slots, double temperature,
                             Temperature.Trait trait, Double maxEffect, EntityRequirement entityRequirement, AttributeModifierMap attributeModifiers,
                             Map<ResourceLocation, Double> immuneTempModifiers)
    {
        this(data, slots, temperature, trait, maxEffect, entityRequirement, attributeModifiers, immuneTempModifiers, ConfigHelper.getModIDs(CSMath.listOrEmpty(data.items()), BuiltInRegistries.ITEM));
    }

    public static final Codec<ItemCarryTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemRequirement.CODEC.fieldOf("data").forGetter(ItemCarryTempData::data),
            Codec.either(IntegerBounds.CODEC, SlotType.CODEC).listOf().fieldOf("slots").forGetter(ItemCarryTempData::slots),
            Codec.DOUBLE.fieldOf("temperature").forGetter(ItemCarryTempData::temperature),
            Temperature.Trait.CODEC.optionalFieldOf("trait", Temperature.Trait.WORLD).forGetter(ItemCarryTempData::trait),
            Codec.DOUBLE.optionalFieldOf("max_effect", java.lang.Double.MAX_VALUE).forGetter(ItemCarryTempData::maxEffect),
            EntityRequirement.getCodec().optionalFieldOf("entity", EntityRequirement.NONE).forGetter(ItemCarryTempData::entityRequirement),
            AttributeModifierMap.CODEC.optionalFieldOf("attributes", new AttributeModifierMap()).forGetter(ItemCarryTempData::attributeModifiers),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.DOUBLE).optionalFieldOf("immune_temp_modifiers", new HashMap<>()).forGetter(ItemCarryTempData::immuneTempModifiers),
            Codec.STRING.listOf().optionalFieldOf("required_mods", List.of()).forGetter(ItemCarryTempData::requiredMods)
    ).apply(instance, ItemCarryTempData::new));

    public ItemRequirement data()
    {   return data;
    }
    public List<Either<IntegerBounds, SlotType>> slots()
    {   return slots;
    }
    public double temperature()
    {   return temperature;
    }
    public Temperature.Trait trait()
    {   return trait;
    }
    public Double maxEffect()
    {   return maxEffect;
    }
    public EntityRequirement entityRequirement()
    {   return entityRequirement;
    }
    public AttributeModifierMap attributeModifiers()
    {   return attributeModifiers;
    }
    public Map<ResourceLocation, Double> immuneTempModifiers()
    {   return immuneTempModifiers;
    }

    @Override
    public boolean test(Entity entity)
    {   return entityRequirement.test(entity);
    }

    public boolean test(Entity entity, ItemStack stack, @Nullable Integer slot, @Nullable EquipmentSlot equipmentSlot)
    {   return test(stack, slot, equipmentSlot) && test(entity);
    }

    public boolean test(Entity entity, ItemStack stack, SlotType slot)
    {
        if (!test(entity) || !data().test(stack, true)) return false;
        for (int i = 0; i < this.slots().size(); i++)
        {
            Optional<SlotType> slotType = this.slots().get(i).right();
            if (slotType.isPresent() && slotType.get().equals(slot))
            {   return true;
            }
        }
        return false;
    }

    public boolean test(ItemStack stack, @Nullable Integer slot, @Nullable EquipmentSlot equipmentSlot)
    {
        if (!data.test(stack, true))
        {   return false;
        }
        if (slot == null && equipmentSlot == null)
        {   return false;
        }
        for (Either<IntegerBounds, SlotType> either : slots)
        {
            if (either.left().isPresent())
            {
                if (slot != null && either.left().get().test(slot))
                {   return true;
                }
            }
            else if (either.right().isPresent())
            {
                if (equipmentSlot != null && either.right().get().matches(equipmentSlot)
                || slot != null && either.right().get().matches(slot))
                {   return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public static ItemCarryTempData fromToml(List<?> entry)
    {
        if (entry.size() < 4)
        {   return null;
        }
        List<Either<TagKey<Item>, Item>> items = ConfigHelper.getItems((String) entry.get(0));

        if (items.isEmpty())
        {   ColdSweat.LOGGER.error("Error parsing carried item temp config: {} does not contain any valid items", entry);
            return null;
        }
        //temp
        double temp = ((Number) entry.get(1)).doubleValue();
        // slots
        SlotType slotType = SlotType.byName((String) entry.get(2));
        if (slotType == null)
        {   ColdSweat.LOGGER.error("Error parsing carried item temp config: \"{}\" is not a valid slot type", entry.get(2));
            return null;
        }
        // trait
        Temperature.Trait trait = Temperature.Trait.fromID((String) entry.get(3));
        // nbt
        ItemComponentsRequirement componentsRequirement = entry.size() > 4
                                                         ? ItemComponentsRequirement.parse((String) entry.get(4))
                                                         : new ItemComponentsRequirement();
        // max effect
        double maxEffect = entry.size() > 5 ? ((Number) entry.get(5)).doubleValue() : Double.MAX_VALUE;
        // compile item requirement
        ItemRequirement itemRequirement = new ItemRequirement(items, componentsRequirement);

        return new ItemCarryTempData(itemRequirement, List.of(Either.right(slotType)), temp, trait, maxEffect, EntityRequirement.NONE, new AttributeModifierMap(), new FastMap<>());
    }

    public String getSlotRangeName()
    {
        if (slots.size() != 1 || slots.get(0).left().isPresent())
        {   return "";
        }
        return slots.get(0).right().get().getSerializedName();
    }

    @Override
    public Codec<ItemCarryTempData> getCodec()
    {   return CODEC;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ItemCarryTempData that = (ItemCarryTempData) obj;
        return super.equals(obj)
            && temperature == that.temperature
            && data.equals(that.data)
            && slots.equals(that.slots)
            && trait.equals(that.trait)
            && maxEffect.equals(that.maxEffect)
            && entityRequirement.equals(that.entityRequirement);
    }

    public enum SlotType implements StringRepresentable
    {
        HEAD("head", List.of(Either.right(EquipmentSlot.HEAD))),
        CHEST("chest", List.of(Either.right(EquipmentSlot.CHEST))),
        LEGS("legs", List.of(Either.right(EquipmentSlot.LEGS))),
        FEET("feet", List.of(Either.right(EquipmentSlot.FEET))),
        INVENTORY("inventory", List.of(Either.left(IntegerBounds.NONE))),
        HOTBAR("hotbar", List.of(Either.left(new IntegerBounds(36, 44)))),
        CURIO("curio", List.of()),
        HAND("hand", List.of(Either.right(EquipmentSlot.MAINHAND), Either.right(EquipmentSlot.OFFHAND)));

        public static final Codec<SlotType> CODEC = StringRepresentable.fromEnum(SlotType::values);

        private final String name;
        private final List<Either<IntegerBounds, EquipmentSlot>> slots;

        SlotType(String name, List<Either<IntegerBounds, EquipmentSlot>> slots)
        {   this.name = name;
            this.slots = slots;
        }

        public List<Either<IntegerBounds, EquipmentSlot>> getSlots()
        {   return slots;
        }

        public boolean matches(int slotId)
        {
            for (int i = 0; i < slots.size(); i++)
            {
                Either<IntegerBounds, EquipmentSlot> either = slots.get(i);
                if (either.left().isPresent() && either.left().get().test(slotId))
                {   return true;
                }
            }
            return false;
        }

        public boolean matches(EquipmentSlot slot)
        {
            for (int i = 0; i < slots.size(); i++)
            {
                Either<IntegerBounds, EquipmentSlot> either = slots.get(i);
                if (either.right().isPresent() && either.right().get().equals(slot))
                {   return true;
                }
            }
            return false;
        }

        @Override
        public String getSerializedName()
        {   return name;
        }

        public static SlotType byName(String name)
        {
            for (SlotType type : values())
            {
                if (type.name.equals(name))
                {   return type;
                }
            }
            return null;
        }

        public static SlotType fromEquipment(EquipmentSlot slot)
        {
            return switch (slot)
            {
                case HEAD -> HEAD;
                case CHEST -> CHEST;
                case LEGS -> LEGS;
                case FEET -> FEET;
                default -> HAND;
            };
        }
    }
}
