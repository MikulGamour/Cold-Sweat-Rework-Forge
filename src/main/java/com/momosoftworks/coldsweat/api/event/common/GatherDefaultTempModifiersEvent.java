package com.momosoftworks.coldsweat.api.event.common;

import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Placement;
import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

import java.util.ArrayList;
import java.util.List;

public class GatherDefaultTempModifiersEvent extends Event
{
    private final List<TempModifier> modifiers;
    private final LivingEntity entity;
    private final Temperature.Trait trait;

    public GatherDefaultTempModifiersEvent(LivingEntity entity, Temperature.Trait trait)
    {
        this.entity = entity;
        this.trait = trait;
        this.modifiers = new ArrayList<>(Temperature.getModifiers(entity, trait));
    }

    public List<TempModifier> getModifiers()
    {   return modifiers;
    }

    public LivingEntity getEntity()
    {   return entity;
    }

    public Temperature.Trait getTrait()
    {   return trait;
    }

    public void addModifier(TempModifier modifier)
    {   modifiers.add(modifier);
    }

    public void addModifiers(List<TempModifier> modifiers)
    {   this.modifiers.addAll(modifiers);
    }

    public void addModifier(TempModifier modifier, boolean allowDupes, Placement params)
    {   Temperature.addModifier(modifiers, modifier, allowDupes, 1, params);
    }
}
