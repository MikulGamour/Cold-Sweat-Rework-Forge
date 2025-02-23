package com.momosoftworks.coldsweat.data.codec.requirement.sub_type;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SnowBootsRequirement implements EntitySubRequirement
{
    public static final MapCodec<SnowBootsRequirement> CODEC = MapCodec.unit(new SnowBootsRequirement());

    @Override
    public MapCodec<? extends EntitySubRequirement> getCodec()
    {   return CODEC;
    }

    @Override
    public boolean test(Entity entity, Level level, @Nullable Vec3 position)
    {
        return entity instanceof LivingEntity living
            && living.getItemBySlot(EquipmentSlot.FEET).canWalkOnPowderedSnow(living);
    }
}
