package com.momosoftworks.coldsweat.compat.kubejs.event;

import com.momosoftworks.coldsweat.api.event.core.init.GatherDefaultTempModifiersEvent;
import com.momosoftworks.coldsweat.api.registry.TempModifierRegistry;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Placement;
import com.momosoftworks.coldsweat.api.util.Temperature;
import dev.latvian.mods.kubejs.entity.KubeLivingEntityEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class DefaultModifiersEventJS implements KubeLivingEntityEvent
{
    private final GatherDefaultTempModifiersEvent event;

    public DefaultModifiersEventJS(GatherDefaultTempModifiersEvent event)
    {   this.event = event;
    }

    @Override
    public LivingEntity getEntity()
    {   return this.event.getEntity();
    }

    public List<TempModifier> getModifiers()
    {   return this.event.getModifiers();
    }

    public Temperature.Trait getTrait()
    {   return event.getTrait();
    }

    public void addModifier(TempModifier modifier)
    {   event.getModifiers().add(modifier);
    }

    public void addModifiers(TempModifier... modifiers)
    {   event.getModifiers().addAll(List.of(modifiers));
    }

    public void addModifier(TempModifier modifier, String duplicatePolicy, Placement params)
    {   Temperature.addModifier(event.getModifiers(), modifier, Placement.Duplicates.byName(duplicatePolicy), 1, params);
    }

    public void addModifierById(ResourceLocation id, Consumer<TempModifier> modifierBuilder, String duplicatePolicy, Placement params)
    {
        TempModifierRegistry.getValue(id).ifPresent(mod ->
        {
            modifierBuilder.accept(mod);
            addModifier(mod, duplicatePolicy, params);
        });
    }

    public void removeModifiers(TempModifier modifier, String matchPolicy)
    {
        Placement.Duplicates policy = Placement.Duplicates.byName(matchPolicy);
        event.getModifiers().removeIf(mod -> Placement.Duplicates.check(policy, mod, modifier));
    }

    public Placement placed(String mode, String order, Predicate<TempModifier> predicate)
    {   return Placement.of(Placement.Mode.byName(mode), Placement.Order.byName(order), predicate);
    }
}
