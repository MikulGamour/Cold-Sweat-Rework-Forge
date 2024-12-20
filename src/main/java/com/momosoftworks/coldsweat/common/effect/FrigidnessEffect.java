package com.momosoftworks.coldsweat.common.effect;

import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

import javax.annotation.Nonnull;

public class FrigidnessEffect extends Effect
{
    public FrigidnessEffect()
    {   super(EffectType.BENEFICIAL, 8961252);
    }

    @Nonnull
    public String getName()
    {   return "effect.frigidness";
    }

    public boolean isInstant()
    {   return false;
    }
}
