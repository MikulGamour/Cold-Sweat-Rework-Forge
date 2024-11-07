package com.momosoftworks.coldsweat.config.type;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.data.codec.configuration.ItemCarryTempData;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.util.ExtraCodecs;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTDynamicOps;

import javax.annotation.Nullable;
import java.util.List;

public class CarriedItemTemperature implements NbtSerializable
{
    public final ItemRequirement item;
    public final List<Either<IntegerBounds, EquipmentSlotType>> slots;
    public final double temperature;
    public final Temperature.Trait trait;
    public final double maxEffect;
    public final EntityRequirement entityRequirement;

    public CarriedItemTemperature(ItemRequirement item, List<Either<IntegerBounds, EquipmentSlotType>> slots, double temperature,
                                  Temperature.Trait trait, double maxEffect, EntityRequirement entityRequirement)
    {
        this.item = item;
        this.slots = slots;
        this.temperature = temperature;
        this.trait = trait;
        this.maxEffect = maxEffect;
        this.entityRequirement = entityRequirement;
    }
    public static final Codec<CarriedItemTemperature> CODEC = RecordCodecBuilder.create(instance ->
    {
        return instance.group(ItemRequirement.CODEC.fieldOf("item").forGetter(data -> data.item),
                              Codec.list(Codec.either(IntegerBounds.CODEC, ExtraCodecs.EQUIPMENT_SLOT)).fieldOf("slots").forGetter(data -> data.slots),
                              Codec.DOUBLE.fieldOf("temperature").forGetter(data -> data.temperature),
                              Temperature.Trait.CODEC.optionalFieldOf("trait", Temperature.Trait.CORE).forGetter(data -> data.trait),
                              Codec.DOUBLE.optionalFieldOf("maxEffect", Double.MAX_VALUE).forGetter(data -> data.maxEffect),
                              EntityRequirement.getCodec().optionalFieldOf("entity", EntityRequirement.NONE).forGetter(data -> data.entityRequirement))
                       .apply(instance, CarriedItemTemperature::new);
    });

    public static CarriedItemTemperature createFromData(ItemCarryTempData data)
    {
        return new CarriedItemTemperature(data.data, data.slots, data.temp,
                                          data.trait.orElse(Temperature.Trait.CORE),
                                          data.maxEffect.orElse(Double.MAX_VALUE),
                                          data.entityRequirement.orElse(EntityRequirement.NONE));
    }

    public boolean testEntity(LivingEntity entity)
    {   return entityRequirement.test(entity);
    }

    public boolean testSlot(ItemStack stack, @Nullable Integer slot, @Nullable EquipmentSlotType equipmentSlot)
    {
        if (!item.test(stack, true))
        {   return false;
        }
        if (slot == null && equipmentSlot == null)
        {   return false;
        }
        for (Either<IntegerBounds, EquipmentSlotType> either : slots)
        {
            if (slot != null && either.left().isPresent() && either.left().get().test(slot))
            {   return true;
            }
            else if (equipmentSlot != null && either.right().isPresent() && either.right().get().equals(equipmentSlot))
            {   return true;
            }
        }
        return false;
    }

    public String getSlotRangeName()
    {
        String[] strictType = {""};
        if (this.slots.size() == 1) this.slots.get(0).ifLeft(left ->
        {
            if (left.equals(IntegerBounds.NONE))
            {  strictType[0] = "inventory";
            }
            if (left.min == 36 && left.max == 44)
            {  strictType[0] = "hotbar";
            }
        });
        else if (this.slots.size() == 2
        && this.slots.get(0).right().map(right -> right == EquipmentSlotType.MAINHAND).orElse(false)
        && this.slots.get(1).right().map(right -> right == EquipmentSlotType.OFFHAND).orElse(false))
        {  strictType[0] = "hand";
        }

        return strictType[0];
    }

    @Override
    public CompoundNBT serialize()
    {
        return (CompoundNBT) CODEC.encodeStart(NBTDynamicOps.INSTANCE, this).result().orElseGet(CompoundNBT::new);
    }

    public static CarriedItemTemperature deserialize(CompoundNBT tag)
    {
        return CODEC.decode(NBTDynamicOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalArgumentException("Could not deserialize CarriedItemTemperature")).getFirst();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {   return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {   return false;
        }
        CarriedItemTemperature that = (CarriedItemTemperature) obj;
        return item.equals(that.item)
            && slots.equals(that.slots)
            && temperature == that.temperature
            && trait == that.trait
            && maxEffect == that.maxEffect
            && entityRequirement.equals(that.entityRequirement);
    }
}
