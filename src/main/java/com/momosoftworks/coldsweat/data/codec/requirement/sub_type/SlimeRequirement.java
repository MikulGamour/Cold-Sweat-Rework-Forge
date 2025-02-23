package com.momosoftworks.coldsweat.data.codec.requirement.sub_type;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public record SlimeRequirement(IntegerBounds size) implements EntitySubRequirement
{
    public static final MapCodec<SlimeRequirement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            IntegerBounds.CODEC.optionalFieldOf("size", IntegerBounds.NONE).forGetter(SlimeRequirement::size)
    ).apply(instance, SlimeRequirement::new));

    @Override
    public MapCodec<? extends EntitySubRequirement> getCodec()
    {   return CODEC;
    }

    @Override
    public boolean test(Entity entity, Level level, @Nullable Vec3 position)
    {   return entity instanceof Slime slime && this.size.test(slime.getSize());
    }
}