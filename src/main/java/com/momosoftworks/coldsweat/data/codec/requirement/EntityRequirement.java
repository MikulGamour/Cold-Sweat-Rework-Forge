package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.util.serialization.SerializablePredicate;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public record EntityRequirement(Optional<EntityType<?>> type, Optional<TagKey<EntityType<?>>> tag,
                                Optional<LocationRequirement> location, Optional<LocationRequirement> steppingOn,
                                Optional<EffectsRequirement> effects, Optional<NbtRequirement> nbt, Optional<EntityFlagsRequirement> flags,
                                Optional<EquipmentRequirement> equipment, Optional<PlayerDataRequirement> playerData,
                                Optional<EntityRequirement> vehicle, Optional<EntityRequirement> passenger, Optional<EntityRequirement> target,
                                Optional<SerializablePredicate<Entity>> predicate)
{
    public static EntityRequirement NONE = new EntityRequirement(Optional.empty(), Optional.empty(), Optional.empty(),
                                                                 Optional.empty(), Optional.empty(), Optional.empty(),
                                                                 Optional.empty(), Optional.empty(), Optional.empty(),
                                                                 Optional.empty(), Optional.empty(), Optional.empty());

    public static Codec<EntityRequirement> SIMPLE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ForgeRegistries.ENTITIES.getCodec().optionalFieldOf("type").forGetter(requirement -> requirement.type),
            TagKey.codec(Registry.ENTITY_TYPE_REGISTRY).optionalFieldOf("tag").forGetter(requirement -> requirement.tag),
            LocationRequirement.CODEC.optionalFieldOf("location").forGetter(requirement -> requirement.location),
            LocationRequirement.CODEC.optionalFieldOf("stepping_on").forGetter(requirement -> requirement.steppingOn),
            EffectsRequirement.CODEC.optionalFieldOf("effects").forGetter(requirement -> requirement.effects),
            NbtRequirement.CODEC.optionalFieldOf("nbt").forGetter(requirement -> requirement.nbt),
            EntityFlagsRequirement.CODEC.optionalFieldOf("flags").forGetter(requirement -> requirement.flags),
            EquipmentRequirement.CODEC.optionalFieldOf("equipment").forGetter(requirement -> requirement.equipment)
    ).apply(instance, (type, tag, location, standingOn, effects, nbt, flags, equipment) -> new EntityRequirement(type, tag, location, standingOn, effects, nbt, flags, equipment,
                                                                                                                 Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                                                                                                                 Optional.empty())));

    private static final List<Codec<EntityRequirement>> REQUIREMENT_CODEC_STACK = new ArrayList<>(List.of(SIMPLE_CODEC));
    // Allow for up to 16 layers of inner codecs
    static
    {   for (int i = 0; i < 16; i++)
        {   addCodecStack();
        }
    }

    public EntityRequirement(Predicate<Entity> predicate)
    {
        this(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
             Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
             Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
             Optional.of(new SerializablePredicate<>(predicate)));
    }

    public EntityRequirement(Optional<EntityType<?>> type, Optional<TagKey<EntityType<?>>> tag, Optional<LocationRequirement> location,
                             Optional<LocationRequirement> steppingOn, Optional<EffectsRequirement> effects, Optional<NbtRequirement> nbt,
                             Optional<EntityFlagsRequirement> flags, Optional<EquipmentRequirement> equipment, Optional<PlayerDataRequirement> playerData,
                             Optional<EntityRequirement> vehicle, Optional<EntityRequirement> passenger, Optional<EntityRequirement> target)
    {
        this(type, tag, location, steppingOn, effects, nbt, flags, equipment, playerData, vehicle, passenger, target, Optional.empty());
    }

    public static Codec<EntityRequirement> getCodec()
    {   return REQUIREMENT_CODEC_STACK.get(REQUIREMENT_CODEC_STACK.size() - 1);
    }

    private static void addCodecStack()
    {
        var latestCodec = REQUIREMENT_CODEC_STACK.get(REQUIREMENT_CODEC_STACK.size() - 1);
        var codec = RecordCodecBuilder.<EntityRequirement>create(instance -> instance.group(
                ForgeRegistries.ENTITIES.getCodec().optionalFieldOf("type").forGetter(requirement -> requirement.type),
                TagKey.codec(Registry.ENTITY_TYPE_REGISTRY).optionalFieldOf("tag").forGetter(requirement -> requirement.tag),
                LocationRequirement.CODEC.optionalFieldOf("location").forGetter(requirement -> requirement.location),
                LocationRequirement.CODEC.optionalFieldOf("stepping_on").forGetter(requirement -> requirement.steppingOn),
                EffectsRequirement.CODEC.optionalFieldOf("effects").forGetter(requirement -> requirement.effects),
                NbtRequirement.CODEC.optionalFieldOf("nbt").forGetter(requirement -> requirement.nbt),
                EntityFlagsRequirement.CODEC.optionalFieldOf("flags").forGetter(requirement -> requirement.flags),
                EquipmentRequirement.CODEC.optionalFieldOf("equipment").forGetter(requirement -> requirement.equipment),
                PlayerDataRequirement.getCodec(latestCodec).optionalFieldOf("player_data").forGetter(requirement -> requirement.playerData),
                latestCodec.optionalFieldOf("vehicle").forGetter(requirement -> requirement.vehicle),
                latestCodec.optionalFieldOf("passenger").forGetter(requirement -> requirement.passenger),
                latestCodec.optionalFieldOf("target").forGetter(requirement -> requirement.target)
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
        if (type.isPresent() && !type.get().equals(entity.getType()))
        {   return false;
        }
        if (tag.isPresent() && !entity.getType().is(tag.get()))
        {   return false;
        }
        if (location.isPresent() && !location.get().test(entity.level, entity.position()))
        {   return false;
        }
        if (steppingOn.isPresent() && !steppingOn.get().test(entity.level, entity.position().add(0, -0.5, 0)))
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
        if (playerData.isPresent() && !playerData.get().test(entity))
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
            if (!(entity instanceof Monster monster))
            {   return false;
            }
            if (!target.get().test(monster.getTarget()))
            {   return false;
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
        return type.equals(that.type)
            && tag.equals(that.tag)
            && location.equals(that.location)
            && steppingOn.equals(that.steppingOn)
            && effects.equals(that.effects)
            && nbt.equals(that.nbt)
            && flags.equals(that.flags)
            && equipment.equals(that.equipment)
            && playerData.equals(that.playerData)
            && vehicle.equals(that.vehicle)
            && passenger.equals(that.passenger)
            && target.equals(that.target)
            && predicate.equals(that.predicate);
    }
}