package com.momosoftworks.coldsweat.api.event.core.init;

import com.momosoftworks.coldsweat.api.registry.TempModifierRegistry;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Placement;
import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Determines the default {@link TempModifier}s that will be applied to an entity upon spawning.<br>
 * This event is fired EVERY TIME the entity joins the world, so make sure to properly check for duplicates.
 */
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

    public void addModifier(TempModifier modifier, Placement.Duplicates duplicatePolicy, Placement params)
    {   Temperature.addModifier(modifiers, modifier, duplicatePolicy, 1, params);
    }

    public void addModifierById(ResourceLocation id, Consumer<TempModifier> modifierConsumer, Placement.Duplicates duplicatePolicy, Placement params)
    {
        TempModifierRegistry.getValue(id).ifPresent(mod ->
        {
            modifierConsumer.accept(mod);
            addModifier(mod, duplicatePolicy, params);
        });
    }
}
