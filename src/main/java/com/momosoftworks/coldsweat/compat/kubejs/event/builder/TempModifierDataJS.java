package com.momosoftworks.coldsweat.compat.kubejs.event.builder;

import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.world.entity.LivingEntity;

public record TempModifierDataJS(LivingEntity entity, Temperature.Trait trait)
{
}
