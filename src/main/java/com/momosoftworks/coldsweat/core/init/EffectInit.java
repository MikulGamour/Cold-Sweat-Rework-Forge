package com.momosoftworks.coldsweat.core.init;

import com.momosoftworks.coldsweat.common.effect.FrigidnessEffect;
import net.minecraft.potion.Effect;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.common.effect.IceResistanceEffect;
import com.momosoftworks.coldsweat.common.effect.WarmthEffect;
import com.momosoftworks.coldsweat.common.effect.GraceEffect;

public class EffectInit
{
    public static final DeferredRegister<Effect> EFFECTS = DeferredRegister.create(ForgeRegistries.POTIONS, ColdSweat.MOD_ID);

    public static final RegistryObject<Effect> FRIGIDNESS = EFFECTS.register("frigidness", FrigidnessEffect::new);
    public static final RegistryObject<Effect> WARMTH = EFFECTS.register("warmth", WarmthEffect::new);
    public static final RegistryObject<Effect> GRACE = EFFECTS.register("grace", GraceEffect::new);
    public static final RegistryObject<Effect> ICE_RESISTANCE = EFFECTS.register("ice_resistance", IceResistanceEffect::new);
}
