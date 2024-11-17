package com.momosoftworks.coldsweat.api.insulation.slot;

import com.mojang.serialization.Codec;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public abstract class ScalingFormula implements NbtSerializable
{
    Type scaling;

    protected ScalingFormula(Type scaling)
    {   this.scaling = scaling;
    }

    public abstract int getSlots(EquipmentSlot slot, ItemStack stack);
    public abstract List<? extends Number> getValues();

    public Type getType()
    {   return scaling;
    }

    @Override
    public CompoundTag serialize()
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("scaling", scaling.getSerializedName());
        return tag;
    }

    public static ScalingFormula deserialize(CompoundTag nbt)
    {
        Type scaling = Type.byName(nbt.getString("scaling"));
        return switch (scaling)
        {
            case STATIC -> Static.deserialize(nbt);
            default -> Dynamic.deserialize(nbt);
        };
    }

    public static class Static extends ScalingFormula
    {
        Map<EquipmentSlot, Integer> slots = new EnumMap<>(EquipmentSlot.class);

        public Static(int head, int body, int legs, int feet)
        {
            super(Type.STATIC);
            slots.put(EquipmentSlot.HEAD, head);
            slots.put(EquipmentSlot.CHEST, body);
            slots.put(EquipmentSlot.LEGS, legs);
            slots.put(EquipmentSlot.FEET, feet);
        }

        @Override
        public int getSlots(EquipmentSlot slot, ItemStack stack)
        {   return slots.getOrDefault(slot, 0);
        }

        @Override
        public List<? extends Number> getValues()
        {
            ArrayList<Integer> values = new ArrayList<>();
            values.add(0, slots.get(EquipmentSlot.HEAD));
            values.add(1, slots.get(EquipmentSlot.CHEST));
            values.add(2, slots.get(EquipmentSlot.LEGS));
            values.add(3, slots.get(EquipmentSlot.FEET));
            return values;
        }

        public static Static deserialize(CompoundTag nbt)
        {   return new Static(nbt.getInt("head"), nbt.getInt("body"), nbt.getInt("legs"), nbt.getInt("feet"));
        }

        @Override
        public CompoundTag serialize()
        {
            CompoundTag tag = super.serialize();
            for (Map.Entry<EquipmentSlot, Integer> entry : slots.entrySet())
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
        public int getSlots(EquipmentSlot slot, ItemStack stack)
        {
            double protection = stack.getAttributeModifiers(slot).get(Attributes.ARMOR).stream().findFirst().map(mod -> mod.getAmount()).orElse(0.0);
            return switch (scaling)
            {
                case LINEAR      -> (int) CSMath.clamp(Math.floor(protection * factor), 0, max);
                case EXPONENTIAL -> (int) CSMath.clamp(Math.floor(Math.pow(protection, factor)), 0, max);
                case LOGARITHMIC -> (int) CSMath.clamp(Math.floor(Math.sqrt(protection) * factor), 0, max);
                default -> 0;
            };
        }

        @Override
        public List<? extends Number> getValues()
        {   return List.of(factor, max);
        }

        public static Dynamic deserialize(CompoundTag nbt)
        {   return new Dynamic(Type.byName(nbt.getString("scaling")), nbt.getDouble("factor"), nbt.getDouble("max"));
        }

        @Override
        public CompoundTag serialize()
        {
            CompoundTag tag = super.serialize();
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
