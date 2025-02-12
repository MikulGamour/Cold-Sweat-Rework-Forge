package com.momosoftworks.coldsweat.compat.kubejs.event;

import com.momosoftworks.coldsweat.api.event.common.temperautre.TempModifierEvent;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import dev.latvian.mods.kubejs.entity.LivingEntityEventJS;
import dev.latvian.mods.kubejs.entity.LivingEntityJS;
import net.minecraft.world.entity.LivingEntity;

public class AddModifierEventJS extends LivingEntityEventJS
{
    private final TempModifierEvent.Add event;

    public AddModifierEventJS(TempModifierEvent.Add event)
    {   this.event = event;
    }

    @Override
    public LivingEntityJS getEntity()
    {   return new LivingEntityJS(this.levelOf(event.getEntity()), event.getEntity());
    }

    public Temperature.Trait getTrait()
    {   return event.getTrait();
    }
    public void setTrait(Temperature.Trait trait)
    {   event.setTrait(trait);
    }

    public TempModifier getModifier()
    {   return event.getModifier();
    }
    public void setModifier(TempModifier modifier)
    {   event.setModifier(modifier);
    }
}
