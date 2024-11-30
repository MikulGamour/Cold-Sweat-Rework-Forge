package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.Entity;

import java.util.Optional;

public class EntityFlagsRequirement
{
    private final Optional<Boolean> onFire;
    private final Optional<Boolean> sneaking;
    private final Optional<Boolean> sprinting;
    private final Optional<Boolean> swimming;
    private final Optional<Boolean> invisible;
    private final Optional<Boolean> glowing;
    private final Optional<Boolean> baby;

    public EntityFlagsRequirement(Optional<Boolean> onFire, Optional<Boolean> sneaking, Optional<Boolean> sprinting,
                                  Optional<Boolean> swimming, Optional<Boolean> invisible, Optional<Boolean> glowing, Optional<Boolean> baby)
    {
        this.onFire = onFire;
        this.sneaking = sneaking;
        this.sprinting = sprinting;
        this.swimming = swimming;
        this.invisible = invisible;
        this.glowing = glowing;
        this.baby = baby;
    }

    public static final Codec<EntityFlagsRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("is_on_fire").forGetter(predicate -> predicate.onFire),
            Codec.BOOL.optionalFieldOf("is_sneaking").forGetter(predicate -> predicate.sneaking),
            Codec.BOOL.optionalFieldOf("is_sprinting").forGetter(predicate -> predicate.sprinting),
            Codec.BOOL.optionalFieldOf("is_swimming").forGetter(predicate -> predicate.swimming),
            Codec.BOOL.optionalFieldOf("is_invisible").forGetter(predicate -> predicate.invisible),
            Codec.BOOL.optionalFieldOf("is_glowing").forGetter(predicate -> predicate.glowing),
            Codec.BOOL.optionalFieldOf("is_baby").forGetter(predicate -> predicate.baby)
    ).apply(instance, EntityFlagsRequirement::new));

    public Optional<Boolean> onFire()
    {   return onFire;
    }
    public Optional<Boolean> sneaking()
    {   return sneaking;
    }
    public Optional<Boolean> sprinting()
    {   return sprinting;
    }
    public Optional<Boolean> swimming()
    {   return swimming;
    }
    public Optional<Boolean> invisible()
    {   return invisible;
    }
    public Optional<Boolean> glowing()
    {   return glowing;
    }
    public Optional<Boolean> baby()
    {   return baby;
    }

    public boolean test(Entity entity)
    {
        return (!onFire.isPresent() || entity.isOnFire() == onFire.get())
            && (!sneaking.isPresent() || entity.isCrouching() == sneaking.get())
            && (!sprinting.isPresent() || entity.isSprinting() == sprinting.get())
            && (!swimming.isPresent() || entity.isInWater() == swimming.get())
            && (!invisible.isPresent() || entity.isInvisible() == invisible.get())
            && (!glowing.isPresent() || entity.isGlowing() == glowing.get())
            && (!baby.isPresent() || (entity instanceof AgeableEntity && ((AgeableEntity) entity).isBaby()) == baby.get());
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
