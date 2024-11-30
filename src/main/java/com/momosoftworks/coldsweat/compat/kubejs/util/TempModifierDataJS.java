package com.momosoftworks.coldsweat.compat.kubejs.util;

import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.entity.LivingEntity;

public class TempModifierDataJS
{
    public final LivingEntity entity;
    public final Temperature.Trait trait;

    public TempModifierDataJS(LivingEntity entity, Temperature.Trait trait)
    {
        this.entity = entity;
        this.trait = trait;
    }
}
