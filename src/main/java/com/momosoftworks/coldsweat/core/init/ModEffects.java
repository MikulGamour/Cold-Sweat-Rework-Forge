package com.momosoftworks.coldsweat.core.init;

import com.momosoftworks.coldsweat.common.effect.FrigidnessEffect;
import com.momosoftworks.coldsweat.common.effect.WarmthEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.common.effect.IceResistanceEffect;
import com.momosoftworks.coldsweat.common.effect.GraceEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects
{
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ColdSweat.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> FRIGIDNESS = EFFECTS.register("chill", FrigidnessEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> WARMTH = EFFECTS.register("warmth", WarmthEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> GRACE = EFFECTS.register("grace", GraceEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> ICE_RESISTANCE = EFFECTS.register("ice_resistance", IceResistanceEffect::new);
}
