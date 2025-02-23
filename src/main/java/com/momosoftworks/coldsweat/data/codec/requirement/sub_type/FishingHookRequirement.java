package com.momosoftworks.coldsweat.data.codec.requirement.sub_type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;

public record FishingHookRequirement(Optional<Boolean> inOpenWater) implements EntitySubRequirement
{
    public static final FishingHookRequirement NONE = new FishingHookRequirement(Optional.empty());
    public static final MapCodec<FishingHookRequirement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("in_open_water").forGetter(FishingHookRequirement::inOpenWater)
    ).apply(instance, FishingHookRequirement::new));

    @Override
    public MapCodec<? extends EntitySubRequirement> getCodec()
    {   return CODEC;
    }

    @Override
    public boolean test(Entity entity, Level level, @Nullable Vec3 position)
    {
        if (!this.inOpenWater.isEmpty())
        {   return entity instanceof FishingHook fishinghook && this.inOpenWater.get() == fishinghook.isOpenWaterFishing();
        }
        return true;
    }
}
