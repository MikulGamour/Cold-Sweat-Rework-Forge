package com.momosoftworks.coldsweat.data.codec.requirement.sub_type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.RaiderPredicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public record RaiderRequirement(boolean hasRaid, boolean isCaptain) implements EntitySubRequirement
{
    public static final MapCodec<RaiderRequirement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("has_raid", false).forGetter(RaiderRequirement::hasRaid),
            Codec.BOOL.optionalFieldOf("is_captain", false).forGetter(RaiderRequirement::isCaptain)
    ).apply(instance, RaiderRequirement::new));

    public static final RaiderPredicate CAPTAIN_WITHOUT_RAID = new RaiderPredicate(false, true);

    @Override
    public MapCodec<? extends EntitySubRequirement> getCodec()
    {   return CODEC;
    }

    @Override
    public boolean test(Entity entity, Level level, @Nullable Vec3 position)
    {
        return entity instanceof Raider raider
            && raider.hasRaid() == this.hasRaid
            && raider.isCaptain() == this.isCaptain;
    }
}
