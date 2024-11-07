package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityRequirement
{
    public final Optional<EntityType<?>> type;
    public final Optional<ITag<EntityType<?>>> tag;
    public final Optional<LocationRequirement> location;
    public final Optional<LocationRequirement> steppingOn;
    public final Optional<EffectsRequirement> effects;
    public final Optional<NbtRequirement> nbt;
    public final Optional<EntityFlagsRequirement> flags;
    public final Optional<EquipmentRequirement> equipment;
    public final Optional<PlayerDataRequirement> playerData;
    public final Optional<EntityRequirement> vehicle;
    public final Optional<EntityRequirement> passenger;
    public final Optional<EntityRequirement> target;

    public EntityRequirement(Optional<EntityType<?>> type, Optional<ITag<EntityType<?>>> tag,
                                Optional<LocationRequirement> location, Optional<LocationRequirement> steppingOn,
                             Optional<EffectsRequirement> effects, Optional<NbtRequirement> nbt, Optional<EntityFlagsRequirement> flags,
                             Optional<EquipmentRequirement> equipment, Optional<PlayerDataRequirement> playerData,
                             Optional<EntityRequirement> vehicle, Optional<EntityRequirement> passenger, Optional<EntityRequirement> target)
    {
        this.type = type;
        this.tag = tag;
        this.location = location;
        this.steppingOn = steppingOn;
        this.effects = effects;
        this.nbt = nbt;
        this.flags = flags;
        this.equipment = equipment;
        this.playerData = playerData;
        this.vehicle = vehicle;
        this.passenger = passenger;
        this.target = target;
    }
    public static EntityRequirement NONE = new EntityRequirement(Optional.empty(), Optional.empty(), Optional.empty(),
                                                                Optional.empty(), Optional.empty(), Optional.empty(),
                                                                Optional.empty(), Optional.empty(), Optional.empty(),
                                                                Optional.empty(), Optional.empty(), Optional.empty());

    public static Codec<EntityRequirement> SIMPLE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Registry.ENTITY_TYPE.optionalFieldOf("type").forGetter(requirement -> requirement.type),
            ITag.codec(EntityTypeTags::getAllTags).optionalFieldOf("tag").forGetter(requirement -> requirement.tag),
            LocationRequirement.CODEC.optionalFieldOf("location").forGetter(requirement -> requirement.location),
            LocationRequirement.CODEC.optionalFieldOf("stepping_on").forGetter(requirement -> requirement.steppingOn),
            EffectsRequirement.CODEC.optionalFieldOf("effects").forGetter(requirement -> requirement.effects),
            NbtRequirement.CODEC.optionalFieldOf("nbt").forGetter(requirement -> requirement.nbt),
            EntityFlagsRequirement.CODEC.optionalFieldOf("flags").forGetter(requirement -> requirement.flags),
            EquipmentRequirement.CODEC.optionalFieldOf("equipment").forGetter(requirement -> requirement.equipment)
    ).apply(instance, (type, tag, location, standingOn, effects, nbt, flags, equipment) -> new EntityRequirement(type, tag, location, standingOn, effects, nbt, flags, equipment,
                                                                                                            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())));

    private static final List<Codec<EntityRequirement>> REQUIREMENT_CODEC_STACK = new ArrayList<>(Arrays.asList(SIMPLE_CODEC));
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
        Codec<EntityRequirement> latestCodec = REQUIREMENT_CODEC_STACK.get(REQUIREMENT_CODEC_STACK.size() - 1);
        Codec<EntityRequirement> codec = RecordCodecBuilder.create(instance -> instance.group(
                net.minecraft.util.registry.Registry.ENTITY_TYPE.optionalFieldOf("type").forGetter(requirement -> requirement.type),
                ITag.codec(EntityTypeTags::getAllTags).optionalFieldOf("tag").forGetter(requirement -> requirement.tag),
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
            if (!(entity instanceof MonsterEntity))
            {   return false;
            }
            MonsterEntity monster = (MonsterEntity) entity;
            if (!target.get().test(monster.getTarget()))
            {   return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {   return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {   return false;
        }

        EntityRequirement that = (EntityRequirement) obj;

        return type.equals(that.type)
            && location.equals(that.location)
            && steppingOn.equals(that.steppingOn)
            && effects.equals(that.effects)
            && nbt.equals(that.nbt)
            && flags.equals(that.flags)
            && equipment.equals(that.equipment)
            && playerData.equals(that.playerData)
            && vehicle.equals(that.vehicle)
            && passenger.equals(that.passenger)
            && target.equals(that.target);
    }

    @Override
    public String toString()
    {   return getCodec().encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
    }
}