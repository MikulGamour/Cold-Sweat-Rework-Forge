package com.momosoftworks.coldsweat.data.codec.requirement.sub_type;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record LightningBoltRequirement(IntegerBounds blocksSetOnFire, Optional<EntityRequirement> entityStruck) implements EntitySubRequirement
{
    public static final MapCodec<LightningBoltRequirement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            IntegerBounds.CODEC.optionalFieldOf("blocks_set_on_fire", IntegerBounds.NONE).forGetter(LightningBoltRequirement::blocksSetOnFire),
            EntityRequirement.getCodec().optionalFieldOf("entity_struck").forGetter(LightningBoltRequirement::entityStruck)
    ).apply(instance, LightningBoltRequirement::new));

    @Override
    public MapCodec<? extends EntitySubRequirement> getCodec()
    {   return CODEC;
    }

    @Override
    public boolean test(Entity entity, Level level, @Nullable Vec3 position)
    {
        if (!(entity instanceof LightningBolt lightningbolt)) return false;
        return this.blocksSetOnFire.test(lightningbolt.getBlocksSetOnFire())
             && (this.entityStruck.isEmpty() || lightningbolt.getHitEntities().anyMatch(ent -> this.entityStruck.get().test(ent)));
    }
}
