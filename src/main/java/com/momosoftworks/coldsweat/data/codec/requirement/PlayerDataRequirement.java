package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.configuration.RequirementHolder;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.util.entity.EntityHelper;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Optional;

public record PlayerDataRequirement(Optional<GameType> gameType, Optional<Map<StatRequirement, IntegerBounds>> stats,
                                    Optional<Map<ResourceLocation, Boolean>> recipes,
                                    Optional<Map<ResourceLocation, Either<AdvancementCompletionRequirement, AdvancementCriteriaRequirement>>> advancements,
                                    Optional<EntityRequirement> lookingAt) implements RequirementHolder
{
    public static final Codec<PlayerDataRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(GameType::byName, GameType::getName).optionalFieldOf("gameType").forGetter(requirement -> requirement.gameType),
            Codec.unboundedMap(StatRequirement.CODEC, IntegerBounds.CODEC).optionalFieldOf("stats").forGetter(requirement -> requirement.stats),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.BOOL).optionalFieldOf("recipes").forGetter(requirement -> requirement.recipes),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.either(AdvancementCompletionRequirement.CODEC, AdvancementCriteriaRequirement.CODEC)).optionalFieldOf("advancements").forGetter(requirement -> requirement.advancements),
            EntityRequirement.getCodec().optionalFieldOf("lookingAt").forGetter(requirement -> requirement.lookingAt)
    ).apply(instance, PlayerDataRequirement::new));

    public static Codec<PlayerDataRequirement> getCodec(Codec<EntityRequirement> lastCodec)
    {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.xmap(GameType::byName, GameType::getName).optionalFieldOf("gameType").forGetter(requirement -> requirement.gameType),
                Codec.unboundedMap(StatRequirement.CODEC, IntegerBounds.CODEC).optionalFieldOf("stats").forGetter(requirement -> requirement.stats),
                Codec.unboundedMap(ResourceLocation.CODEC, Codec.BOOL).optionalFieldOf("recipes").forGetter(requirement -> requirement.recipes),
                Codec.unboundedMap(ResourceLocation.CODEC, Codec.either(AdvancementCompletionRequirement.CODEC, AdvancementCriteriaRequirement.CODEC)).optionalFieldOf("advancements").forGetter(requirement -> requirement.advancements),
                lastCodec.optionalFieldOf("lookingAt").forGetter(requirement -> requirement.lookingAt)
        ).apply(instance, PlayerDataRequirement::new));
    }

    @Override
    public boolean test(Entity entity)
    {
        if (!(entity instanceof Player player)) return false;
        ServerPlayer serverPlayer = EntityHelper.getServerPlayer(player);

        if (gameType.isPresent() && EntityHelper.getGameModeForPlayer(player) != gameType.get())
        {   return false;
        }
        if (stats.isPresent())
        {
            for (Map.Entry<StatRequirement, IntegerBounds> entry : stats.get().entrySet())
            {
                int value = serverPlayer.getStats().getValue(entry.getKey().stat());
                if (!entry.getKey().test(entry.getKey().stat(), value))
                {   return false;
                }
            }
        }
        if (recipes.isPresent())
        {
            for (Map.Entry<ResourceLocation, Boolean> entry : recipes.get().entrySet())
            {
                if (serverPlayer.getRecipeBook().contains(entry.getKey()) != entry.getValue())
                {   return false;
                }
            }
        }
        if (advancements.isPresent())
        {
            for (Map.Entry<ResourceLocation, Either<AdvancementCompletionRequirement, AdvancementCriteriaRequirement>> entry : advancements.get().entrySet())
            {
                AdvancementProgress progress = serverPlayer.getAdvancements().getOrStartProgress(serverPlayer.getServer().getAdvancements().getAdvancement(entry.getKey()));
                if (entry.getValue().map(complete -> complete.test(progress), criteria -> criteria.test(progress)))
                {   return false;
                }
            }
        }
        if (lookingAt.isPresent())
        {
            Vec3 vec3 = player.getEyePosition();
            Vec3 vec31 = player.getViewVector(1.0F);
            Vec3 vec32 = vec3.add(vec31.x * 100.0D, vec31.y * 100.0D, vec31.z * 100.0D);
            EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(player.level, player, vec3, vec32, (new AABB(vec3, vec32)).inflate(1.0D), (ent) -> !ent.isSpectator(), 0.0F);
            if (entityhitresult == null || entityhitresult.getType() != HitResult.Type.ENTITY)
            {   return false;
            }

            Entity hitEntity = entityhitresult.getEntity();
            if (!this.lookingAt.get().test(hitEntity) || !player.hasLineOfSight(hitEntity))
            {   return false;
            }
        }
        return true;
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

        PlayerDataRequirement that = (PlayerDataRequirement) obj;
        return gameType.equals(that.gameType)
            && stats.equals(that.stats)
            && recipes.equals(that.recipes)
            && advancements.equals(that.advancements)
            && lookingAt.equals(that.lookingAt);
    }

    public record StatRequirement(StatType<?> type, ResourceLocation statId, Stat<?> stat, IntegerBounds value)
    {
        public static final Codec<StatRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ForgeRegistries.STAT_TYPES.getCodec().fieldOf("type").forGetter(stat -> stat.type),
                ResourceLocation.CODEC.fieldOf("stat").forGetter(stat -> stat.statId),
                IntegerBounds.CODEC.fieldOf("value").forGetter(stat -> stat.value)
        ).apply(instance, StatRequirement::new));

        public StatRequirement(StatType<?> type, ResourceLocation statId, IntegerBounds value)
        {   this(type, statId, (Stat<?>) type.getRegistry().get(statId), value);
        }

        public StatType<?> type()
        {   return this.type;
        }
        public Stat<?> stat()
        {   return this.stat;
        }
        public IntegerBounds value()
        {   return this.value;
        }

        public boolean test(Stat<?> stat, int value)
        {   return statId.equals(ForgeRegistries.STAT_TYPES.getKey(stat.getType())) && this.value.test(value);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            StatRequirement that = (StatRequirement) obj;
            return type.equals(that.type)
                && statId.equals(that.statId)
                && value.equals(that.value);
        }

        @Override
        public String toString()
        {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
        }
    }

    public record AdvancementCompletionRequirement(Boolean complete)
    {
        public static final Codec<AdvancementCompletionRequirement> CODEC = Codec.BOOL.xmap(AdvancementCompletionRequirement::new, AdvancementCompletionRequirement::complete);

        public boolean test(AdvancementProgress progress)
        {   return progress.isDone() == this.complete;
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

            AdvancementCompletionRequirement that = (AdvancementCompletionRequirement) obj;
            return complete.equals(that.complete);
        }
    }
    public record AdvancementCriteriaRequirement(Map<String, Boolean> criteria)
    {
        public static final Codec<AdvancementCriteriaRequirement> CODEC = Codec.unboundedMap(Codec.STRING, Codec.BOOL).xmap(AdvancementCriteriaRequirement::new, AdvancementCriteriaRequirement::criteria);

        public boolean test(AdvancementProgress progress)
        {
            for (Map.Entry<String, Boolean> entry : this.criteria.entrySet())
            {
                CriterionProgress criterionprogress = progress.getCriterion(entry.getKey());
                if (criterionprogress == null || criterionprogress.isDone() != entry.getValue())
                {   return false;
                }
            }
            return true;
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

            AdvancementCriteriaRequirement that = (AdvancementCriteriaRequirement) obj;
            return criteria.equals(that.criteria);
        }
    }
}
