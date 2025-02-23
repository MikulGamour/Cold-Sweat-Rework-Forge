package com.momosoftworks.coldsweat.data.codec.requirement.sub_type;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class PiglinNeutralArmorRequirement implements EntitySubRequirement
{
    public static final PiglinNeutralArmorRequirement INSTANCE = new PiglinNeutralArmorRequirement();
    public static final MapCodec<PiglinNeutralArmorRequirement> CODEC = MapCodec.unit(INSTANCE);

    private PiglinNeutralArmorRequirement() {}

    @Override
    public MapCodec<? extends EntitySubRequirement> getCodec()
    {   return CODEC;
    }

    @Override
    public boolean test(Entity entity, Level level, @Nullable Vec3 position)
    {
        if (entity instanceof LivingEntity living)
        {
            for (ItemStack armor : living.getArmorSlots())
            {
                if (!armor.isEmpty() && armor.makesPiglinsNeutral(living))
                {   return true;
                }
            }
        }
        return false;
    }
}
