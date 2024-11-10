package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import net.minecraft.inventory.EquipmentSlotType;

import java.util.List;
import java.util.Optional;

public class ItemCarryTempData
{
    public final ItemRequirement data;
    public final List<Either<IntegerBounds, EquipmentSlotType>> slots;
    public final double temp;
    public final Optional<Temperature.Trait> trait;
    public final Optional<Double> maxEffect;
    public final Optional<EntityRequirement> entityRequirement;
    public final Optional<List<String>> requiredMods;

    public ItemCarryTempData(ItemRequirement data, List<Either<IntegerBounds, EquipmentSlotType>> slots, double temp,
                             Optional<Temperature.Trait> trait,
                             Optional<Double> maxEffect,
                             Optional<EntityRequirement> entityRequirement,
                             Optional<List<String>> requiredMods)
    {
        this.data = data;
        this.slots = slots;
        this.temp = temp;
        this.trait = trait;
        this.maxEffect = maxEffect;
        this.entityRequirement = entityRequirement;
        this.requiredMods = requiredMods;
    }

    public static final Codec<ItemCarryTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemRequirement.CODEC.fieldOf("data").forGetter(obj -> obj.data),
            Codec.either(IntegerBounds.CODEC, Codec.STRING.xmap(EquipmentSlotType::byName, EquipmentSlotType::getName))
                 .listOf().fieldOf("slots").forGetter(obj -> obj.slots),
            Codec.DOUBLE.fieldOf("temperature").forGetter(obj -> obj.temp),
            Temperature.Trait.CODEC.optionalFieldOf("trait").forGetter(obj -> obj.trait),
            Codec.DOUBLE.optionalFieldOf("max_effect").forGetter(obj -> obj.maxEffect),
            EntityRequirement.getCodec().optionalFieldOf("entity").forGetter(obj -> obj.entityRequirement),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(obj -> obj.requiredMods)
    ).apply(instance, ItemCarryTempData::new));

    public String toString()
    {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ItemCarryTempData that = (ItemCarryTempData) obj;
        return temp == that.temp
            && data.equals(that.data)
            && slots.equals(that.slots)
            && trait.equals(that.trait)
            && maxEffect.equals(that.maxEffect)
            && entityRequirement.equals(that.entityRequirement)
            && requiredMods.equals(that.requiredMods);
    }
}
