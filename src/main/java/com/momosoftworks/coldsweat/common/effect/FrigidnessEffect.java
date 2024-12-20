package com.momosoftworks.coldsweat.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

import javax.annotation.Nonnull;

public class FrigidnessEffect extends MobEffect
{
    public FrigidnessEffect()
    {   super(MobEffectCategory.BENEFICIAL, 8961252);
    }

    @Nonnull
    public String getName()
    {   return "effect.frigidness";
    }

    public boolean isInstant()
    {   return false;
    }
}
