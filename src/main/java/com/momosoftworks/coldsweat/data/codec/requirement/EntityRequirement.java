package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.data.codec.requirement.sub_type.EntitySubRequirement;
import com.momosoftworks.coldsweat.data.codec.util.DoubleBounds;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.scores.Team;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public record EntityRequirement(Optional<List<Either<TagKey<EntityType<?>>, EntityType<?>>>> entities,
                                Optional<LocationRequirement> location, Optional<LocationRequirement> steppingOn,
                                Optional<EffectsRequirement> effects, Optional<NbtRequirement> nbt, Optional<EntityFlagsRequirement> flags,
                                Optional<EquipmentRequirement> equipment, Optional<EntitySubRequirement> typeSpecificData,
                                Optional<String> team, Optional<EntityRequirement> vehicle, Optional<EntityRequirement> passenger,
                                Optional<EntityRequirement> target, Optional<Map<Temperature.Trait, DoubleBounds>> temperature,
                                Optional<Predicate<Entity>> predicate)
{
    public EntityRequirement(Optional<List<Either<TagKey<EntityType<?>>, EntityType<?>>>> type, Optional<LocationRequirement> location,
                             Optional<LocationRequirement> steppingOn, Optional<EffectsRequirement> effects, Optional<NbtRequirement> nbt,
                             Optional<EntityFlagsRequirement> flags, Optional<EquipmentRequirement> equipment, Optional<EntitySubRequirement> typeSpecificData,
                             Optional<String> team, Optional<EntityRequirement> vehicle, Optional<EntityRequirement> passenger,
                             Optional<EntityRequirement> target, Optional<Map<Temperature.Trait, DoubleBounds>> temperature)
    {
        this(type, location, steppingOn, effects, nbt, flags, equipment, typeSpecificData, team, vehicle, passenger, target, temperature, Optional.empty());;
    }

    public EntityRequirement(List<Either<TagKey<EntityType<?>>, EntityType<?>>> entities)
    {
        this(Optional.of(entities),
             Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
             Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public EntityRequirement(Collection<EntityType<?>> entities, @Nullable Predicate<Entity> predicate)
    {
        this(Optional.of(entities.stream().map(Either::<TagKey<EntityType<?>>, EntityType<?>>right).toList()),
             Optional.empty(), Optional.empty(), Optional.empty(),
             Optional.empty(), Optional.empty(), Optional.empty(),
             Optional.empty(), Optional.empty(), Optional.empty(),
             Optional.empty(), Optional.empty(), Optional.empty(),
             Optional.ofNullable(predicate));
    }

    public EntityRequirement(Predicate<Entity> predicate)
    {
        this(Optional.empty(), Optional.empty(), Optional.empty(),
             Optional.empty(), Optional.empty(), Optional.empty(),
             Optional.empty(), Optional.empty(), Optional.empty(),
             Optional.empty(), Optional.empty(), Optional.empty(),
             Optional.empty(), Optional.ofNullable(predicate));
    }

    public static final EntityRequirement NONE = new EntityRequirement(Optional.empty(), Optional.empty(), Optional.empty(),
                                                                       Optional.empty(), Optional.empty(), Optional.empty(),
                                                                       Optional.empty(), Optional.empty(), Optional.empty(),
                                                                       Optional.empty(), Optional.empty(), Optional.empty(),
                                                                       Optional.empty());

    public static final Codec<EntityRequirement> SIMPLE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrBuiltinCodec(Registries.ENTITY_TYPE, ForgeRegistries.ENTITY_TYPES).listOf().optionalFieldOf("entities").forGetter(requirement -> requirement.entities),
            LocationRequirement.CODEC.optionalFieldOf("location").forGetter(requirement -> requirement.location),
            LocationRequirement.CODEC.optionalFieldOf("stepping_on").forGetter(requirement -> requirement.steppingOn),
            EffectsRequirement.CODEC.optionalFieldOf("effects").forGetter(requirement -> requirement.effects),
            NbtRequirement.CODEC.optionalFieldOf("nbt").forGetter(requirement -> requirement.nbt),
            EntityFlagsRequirement.CODEC.optionalFieldOf("flags").forGetter(requirement -> requirement.flags),
            EquipmentRequirement.CODEC.optionalFieldOf("equipment").forGetter(requirement -> requirement.equipment),
            EntitySubRequirement.CODEC.optionalFieldOf("type_data").forGetter(requirement -> requirement.typeSpecificData),
            Codec.STRING.optionalFieldOf("team").forGetter(requirement -> requirement.team),
            Codec.unboundedMap(Temperature.Trait.CODEC, DoubleBounds.CODEC).optionalFieldOf("temperature").forGetter(requirement -> requirement.temperature)
    ).apply(instance, (type, location, standingOn, effects, nbt, flags, equipment, typeData, team, temperature) ->
            new EntityRequirement(type, location, standingOn, effects, nbt, flags, equipment, typeData, team,
                                  Optional.empty(), Optional.empty(), Optional.empty(), temperature)));

    private static final List<Codec<EntityRequirement>> REQUIREMENT_CODEC_STACK = new ArrayList<>(List.of(SIMPLE_CODEC));
    // Allow for up to 16 layers of inner codecs
    static
    {   for (int i = 0; i < 16; i++)
        {   addCodecStack();
        }
    }

    public static Codec<EntityRequirement> getCodec()
    {   return REQUIREMENT_CODEC_STACK.get(REQUIREMENT_CODEC_STACK.size() - 1);
    }

    private static void addCodecStack()
    {
        var latestCodec = REQUIREMENT_CODEC_STACK.get(REQUIREMENT_CODEC_STACK.size() - 1);
        var codec = RecordCodecBuilder.<EntityRequirement>create(instance -> instance.group(
                ConfigHelper.tagOrBuiltinCodec(Registries.ENTITY_TYPE, ForgeRegistries.ENTITY_TYPES).listOf().optionalFieldOf("entities").forGetter(requirement -> requirement.entities),
                LocationRequirement.CODEC.optionalFieldOf("location").forGetter(requirement -> requirement.location),
                LocationRequirement.CODEC.optionalFieldOf("stepping_on").forGetter(requirement -> requirement.steppingOn),
                EffectsRequirement.CODEC.optionalFieldOf("effects").forGetter(requirement -> requirement.effects),
                NbtRequirement.CODEC.optionalFieldOf("nbt").forGetter(requirement -> requirement.nbt),
                EntityFlagsRequirement.CODEC.optionalFieldOf("flags").forGetter(requirement -> requirement.flags),
                EquipmentRequirement.CODEC.optionalFieldOf("equipment").forGetter(requirement -> requirement.equipment),
                EntitySubRequirement.CODEC.optionalFieldOf("type_specific").forGetter(requirement -> requirement.typeSpecificData),
                Codec.STRING.optionalFieldOf("team").forGetter(requirement -> requirement.team),
                latestCodec.optionalFieldOf("vehicle").forGetter(requirement -> requirement.vehicle),
                latestCodec.optionalFieldOf("passenger").forGetter(requirement -> requirement.passenger),
                latestCodec.optionalFieldOf("target").forGetter(requirement -> requirement.target),
                Codec.unboundedMap(Temperature.Trait.CODEC, DoubleBounds.CODEC).optionalFieldOf("temperature").forGetter(requirement -> requirement.temperature)
        ).apply(instance, EntityRequirement::new));

        REQUIREMENT_CODEC_STACK.add(codec);
    }

    public boolean test(Entity entity)
    {
        if (entity == null)
        {   return true;
        }
        if (this.predicate.isPresent())
        {   return this.predicate.get().test(entity);
        }
        if (Objects.equals(this, NONE))
        {   return true;
        }
        if (entities.isPresent())
        {
            checkEntityType:
            {
                for (int i = 0; i < entities.get().size(); i++)
                {
                    Either<TagKey<EntityType<?>>, EntityType<?>> either = entities.get().get(i);
                    if (either.map(entity.getType()::is, entity.getType()::equals))
                    {   break checkEntityType;
                    }
                }
                return false;
            }
        }
        if (location.isPresent() && !location.get().test(entity.level(), entity.position()))
        {   return false;
        }
        if (steppingOn.isPresent() && !steppingOn.get().test(entity.level(), entity.position().add(0, -0.5, 0)))
        {   return false;
        }
        if (effects.isPresent() && !effects.get().test(entity))
        {   return false;
        }
        if (nbt.isPresent() && !nbt.get().test(entity))
        {   return false;
        }
        if (flags.isPresent() && !flags.get().test(entity))
        {   return false;
        }
        if (equipment.isPresent() && !equipment.get().test(entity))
        {   return false;
        }
        if (typeSpecificData.isPresent() && !typeSpecificData.get().test(entity, entity.level(), entity.position()))
        {   return false;
        }
        if (vehicle.isPresent() && !vehicle.get().test(entity.getVehicle()))
        {   return false;
        }
        if (passenger.isPresent() && !passenger.get().test(entity.getPassengers().isEmpty() ? null : entity.getPassengers().get(0)))
        {   return false;
        }
        if (target.isPresent())
        {
            if (!(entity instanceof Monster monster) || !target.get().test(monster.getTarget()))
            {   return false;
            }
        }
        if (team.isPresent())
        {
            Team team = entity.getTeam();
            if (team == null || !team.getName().equals(this.team.get()))
            {   return false;
            }
        }
        if (temperature.isPresent() && entity instanceof LivingEntity living)
        {
            for (Map.Entry<Temperature.Trait, DoubleBounds> entry : temperature.get().entrySet())
            {
                double value = Temperature.get(living, entry.getKey());
                if (!entry.getValue().test(value))
                {   return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString()
    {   return getCodec().encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        EntityRequirement that = (EntityRequirement) obj;
        return entities.equals(that.entities)
            && location.equals(that.location)
            && steppingOn.equals(that.steppingOn)
            && effects.equals(that.effects)
            && nbt.equals(that.nbt)
            && flags.equals(that.flags)
            && equipment.equals(that.equipment)
            && typeSpecificData.equals(that.typeSpecificData)
            && vehicle.equals(that.vehicle)
            && passenger.equals(that.passenger)
            && target.equals(that.target)
            && predicate.equals(that.predicate)
            && temperature.equals(that.temperature);
    }
}