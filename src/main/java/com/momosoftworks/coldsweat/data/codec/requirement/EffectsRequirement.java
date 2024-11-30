package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.registry.Registry;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class EffectsRequirement
{
    private final Map<Effect, Instance> effects;

    public EffectsRequirement(Map<Effect, Instance> effects)
    {
        this.effects = effects;
    }

    public static final Codec<EffectsRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Registry.MOB_EFFECT, Instance.CODEC).fieldOf("effects").forGetter(predicate -> predicate.effects)
    ).apply(instance, EffectsRequirement::new));

    public Map<Effect, Instance> effects()
    {   return effects;
    }

    public boolean test(Entity entity)
    {   return !(entity instanceof LivingEntity) || test(((LivingEntity) entity).getActiveEffects());
    }

    public boolean test(Collection<EffectInstance> effects)
    {
        for (Map.Entry<Effect, Instance> entry : this.effects.entrySet())
        {
            Effect effect = entry.getKey();
            Instance instance = entry.getValue();
            int amplifier = 0;
            int duration = 0;
            boolean ambient = true;
            boolean visible = true;
            for (EffectInstance effectInstance : effects)
            {
                if (effectInstance.getEffect() == effect)
                {
                    amplifier = effectInstance.getAmplifier();
                    duration = effectInstance.getDuration();
                    ambient = effectInstance.isAmbient();
                    visible = effectInstance.isVisible();
                    break;
                }
            }
            if (!instance.amplifier.test(amplifier) || !instance.duration.test(duration))
            {   return false;
            }
            if (instance.ambient.isPresent() && instance.ambient.get() != ambient)
            {   return false;
            }
            if (instance.visible.isPresent() && instance.visible.get() != visible)
            {   return false;
            }
        }
        return true;
    }

    public boolean test(EffectInstance effect)
    {
        Instance instance = effects.get(effect.getEffect());
        if (instance == null)
        {   return true;
        }

        return instance.amplifier.test(effect.getAmplifier()) && instance.duration.test(effect.getDuration())
            && (!instance.ambient.isPresent() || instance.ambient.get() == effect.isAmbient())
            && (!instance.visible.isPresent() || instance.visible.get() == effect.isVisible());
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

        EffectsRequirement that = (EffectsRequirement) obj;
        return effects.equals(that.effects);
    }

    public static class Instance
    {
        private final IntegerBounds amplifier;
        private final IntegerBounds duration;
        private final Optional<Boolean> ambient;
        private final Optional<Boolean> visible;

        public Instance(IntegerBounds amplifier, IntegerBounds duration, Optional<Boolean> ambient, Optional<Boolean> visible)
        {
            this.amplifier = amplifier;
            this.duration = duration;
            this.ambient = ambient;
            this.visible = visible;
        }

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                IntegerBounds.CODEC.fieldOf("amplifier").forGetter(effect -> effect.amplifier),
                IntegerBounds.CODEC.fieldOf("duration").forGetter(effect -> effect.duration),
                Codec.BOOL.optionalFieldOf("ambient").forGetter(effect -> effect.ambient),
                Codec.BOOL.optionalFieldOf("visible").forGetter(effect -> effect.visible)
        ).apply(instance, Instance::new));

        @Override
        public String toString()
        {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            Instance that = (Instance) obj;
            return amplifier.equals(that.amplifier)
                && duration.equals(that.duration)
                && ambient.equals(that.ambient)
                && visible.equals(that.visible);
        }
    }
}
