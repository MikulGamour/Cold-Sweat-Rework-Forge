package com.momosoftworks.coldsweat.api.insulation.slot;

import com.mojang.serialization.Codec;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.nbt.CompoundNBT;
import com.momosoftworks.coldsweat.util.serialization.StringRepresentable;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public abstract class ScalingFormula implements NbtSerializable
{
    Type scaling;

    protected ScalingFormula(Type scaling)
    {   this.scaling = scaling;
    }

    public abstract int getSlots(EquipmentSlotType slot, ItemStack stack);
    public abstract List<? extends Number> getValues();

    public Type getType()
    {   return scaling;
    }

    @Override
    public CompoundNBT serialize()
    {
        CompoundNBT tag = new CompoundNBT();
        tag.putString("scaling", scaling.getSerializedName());
        return tag;
    }

    public static ScalingFormula deserialize(CompoundNBT nbt)
    {
        Type scaling = Type.byName(nbt.getString("scaling"));
        switch (scaling)
        {
            case STATIC : return Static.deserialize(nbt);
            default : return Dynamic.deserialize(nbt);
        }
    }

    public static class Static extends ScalingFormula
    {
        Map<EquipmentSlotType, Integer> slots = new EnumMap<>(EquipmentSlotType.class);

        public Static(int head, int body, int legs, int feet)
        {
            super(Type.STATIC);
            slots.put(EquipmentSlotType.HEAD, head);
            slots.put(EquipmentSlotType.CHEST, body);
            slots.put(EquipmentSlotType.LEGS, legs);
            slots.put(EquipmentSlotType.FEET, feet);
        }

        private Static()
        {   super(Type.STATIC);
        }

        @Override
        public int getSlots(EquipmentSlotType slot, ItemStack stack)
        {   return slots.getOrDefault(slot, 0);
        }

        @Override
        public List<? extends Number> getValues()
        {
            return Arrays.asList(slots.get(EquipmentSlotType.HEAD),
                                 slots.get(EquipmentSlotType.CHEST),
                                 slots.get(EquipmentSlotType.LEGS),
                                 slots.get(EquipmentSlotType.FEET));
        }

        public static Static deserialize(CompoundNBT nbt)
        {
            Static instance = new Static();
            for (EquipmentSlotType slot : EquipmentSlotType.values())
            {   if (slot.getType() == EquipmentSlotType.Group.ARMOR)
                {   instance.slots.put(slot, nbt.getInt(slot.getName()));
                }
            }
            return instance;
        }

        @Override
        public CompoundNBT serialize()
        {
            CompoundNBT tag = super.serialize();
            for (Map.Entry<EquipmentSlotType, Integer> entry : slots.entrySet())
            {   tag.putInt(entry.getKey().getName(), entry.getValue());
            }
            return tag;
        }
    }

    public static class Dynamic extends ScalingFormula
    {
        double factor;
        double max;

        public Dynamic(Type scaling, double factor, double max)
        {   super(scaling);
            this.factor = factor;
            this.max = max;
        }

        @Override
        public int getSlots(EquipmentSlotType slot, ItemStack stack)
        {
            double protection = stack.getAttributeModifiers(slot).get(Attributes.ARMOR).stream().findFirst().map(mod -> mod.getAmount()).orElse(0.0);
            switch (scaling)
            {
                case LINEAR      : return (int) CSMath.clamp(Math.floor(protection * factor), 0, max);
                case EXPONENTIAL : return (int) CSMath.clamp(Math.floor(Math.pow(protection, factor)), 0, max);
                case LOGARITHMIC : return (int) CSMath.clamp(Math.floor(Math.sqrt(protection) * factor), 0, max);
                default : return  0;
            }
        }

        @Override
        public List<? extends Number> getValues()
        {   return Arrays.asList(factor, max);
        }

        public static Dynamic deserialize(CompoundNBT nbt)
        {   return new Dynamic(Type.byName(nbt.getString("scaling")), nbt.getDouble("factor"), nbt.getDouble("max"));
        }

        @Override
        public CompoundNBT serialize()
        {
            CompoundNBT tag = super.serialize();
            tag.putDouble("factor", factor);
            tag.putDouble("max", max);
            return tag;
        }
    }

    public enum Type implements StringRepresentable
    {
        STATIC("static"),
        LINEAR("linear"),
        EXPONENTIAL("exponential"),
        LOGARITHMIC("logarithmic");

        final String name;

        Type(String name)
        {   this.name = name;
        }

        public static final Codec<Type> CODEC = Codec.STRING.xmap(Type::byName, Type::getSerializedName);

        @Override
        public String getSerializedName()
        {   return name;
        }

        public static Type byName(String name)
        {   for (Type type : values())
            {   if (type.name.equals(name))
                {   return type;
                }
            }
            throw new IllegalArgumentException("Unknown insulation scaling: " + name);
        }
    }
}
