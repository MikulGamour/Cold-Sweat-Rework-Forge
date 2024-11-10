package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public record EntityFlagsRequirement(Optional<Boolean> onFire, Optional<Boolean> sneaking, Optional<Boolean> sprinting, Optional<Boolean> swimming, Optional<Boolean> invisible, Optional<Boolean> glowing, Optional<Boolean> baby)
{
    public static final Codec<EntityFlagsRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("is_on_fire").forGetter(predicate -> predicate.onFire),
            Codec.BOOL.optionalFieldOf("is_sneaking").forGetter(predicate -> predicate.sneaking),
            Codec.BOOL.optionalFieldOf("is_sprinting").forGetter(predicate -> predicate.sprinting),
            Codec.BOOL.optionalFieldOf("is_swimming").forGetter(predicate -> predicate.swimming),
            Codec.BOOL.optionalFieldOf("is_invisible").forGetter(predicate -> predicate.invisible),
            Codec.BOOL.optionalFieldOf("is_glowing").forGetter(predicate -> predicate.glowing),
            Codec.BOOL.optionalFieldOf("is_baby").forGetter(predicate -> predicate.baby)
    ).apply(instance, EntityFlagsRequirement::new));

    public boolean test(Entity entity)
    {
        return (onFire.isEmpty() || entity.isOnFire() == onFire.get())
            && (sneaking.isEmpty() || entity.isCrouching() == sneaking.get())
            && (sprinting.isEmpty() || entity.isSprinting() == sprinting.get())
            && (swimming.isEmpty() || entity.isInWater() == swimming.get())
            && (invisible.isEmpty() || entity.isInvisible() == invisible.get())
            && (glowing.isEmpty() || entity.isCurrentlyGlowing() == glowing.get())
            && (baby.isEmpty() || (entity instanceof AgeableMob mob && mob.isBaby()) == baby.get());
    }

    @Override
    public String toString()
    {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        EntityFlagsRequirement that = (EntityFlagsRequirement) obj;
        return onFire.equals(that.onFire)
            && sneaking.equals(that.sneaking)
            && sprinting.equals(that.sprinting)
            && swimming.equals(that.swimming)
            && invisible.equals(that.invisible)
            && glowing.equals(that.glowing)
            && baby.equals(that.baby);
    }
}
