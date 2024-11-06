package com.momosoftworks.coldsweat.common.capability.temperature;

import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;

public interface ITemperatureCap
{
    double getTrait(Temperature.Trait trait);
    EnumMap<Temperature.Trait, Double> getTraits();
    void setTrait(Temperature.Trait trait, double value);

    List<TempModifier> getModifiers(Temperature.Trait trait);
    boolean hasModifier(Temperature.Trait trait, Class<? extends TempModifier> mod);
    void addModifier(TempModifier modifier, Temperature.Trait trait);
    void clearModifiers(Temperature.Trait trait);

    void markPersistentAttribute(Attribute attribute);
    void clearPersistentAttribute(Attribute attribute);
    Collection<Attribute> getPersistentAttributes();

    void tick(LivingEntity entity);
    void tickDummy(LivingEntity entity);

    Temperature.Units getPreferredUnits();
    void setPreferredUnits(Temperature.Units units);

    void copy(ITemperatureCap cap);
    void syncValues(LivingEntity entity);

    CompoundTag serializeNBT();
    CompoundTag serializeModifiers();
    CompoundTag serializeTraits();
    void deserializeNBT(CompoundTag tag);
    void deserializeModifiers(CompoundTag tag);
    void deserializeTraits(CompoundTag tag);
}