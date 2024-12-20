package com.momosoftworks.coldsweat.core.init;

import com.momosoftworks.coldsweat.common.effect.FrigidnessEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.common.effect.IceResistanceEffect;
import com.momosoftworks.coldsweat.common.effect.WarmthEffect;
import com.momosoftworks.coldsweat.common.effect.GraceEffect;
import net.minecraftforge.registries.RegistryObject;

public class EffectInit
{
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ColdSweat.MOD_ID);

    public static final RegistryObject<MobEffect> FRIGIDNESS = EFFECTS.register("frigidness", FrigidnessEffect::new);
    public static final RegistryObject<MobEffect> WARMTH = EFFECTS.register("warmth", WarmthEffect::new);
    public static final RegistryObject<MobEffect> GRACE = EFFECTS.register("grace", GraceEffect::new);
    public static final RegistryObject<MobEffect> ICE_RESISTANCE = EFFECTS.register("ice_resistance", IceResistanceEffect::new);
}
