package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public record EquipmentRequirement(Optional<ItemRequirement> head, Optional<ItemRequirement> chest,
                                   Optional<ItemRequirement> legs, Optional<ItemRequirement> feet,
                                   Optional<ItemRequirement> mainHand, Optional<ItemRequirement> offHand)
{
    public static final Codec<EquipmentRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemRequirement.CODEC.optionalFieldOf("head").forGetter(requirement -> requirement.head),
            ItemRequirement.CODEC.optionalFieldOf("chest").forGetter(requirement -> requirement.chest),
            ItemRequirement.CODEC.optionalFieldOf("legs").forGetter(requirement -> requirement.legs),
            ItemRequirement.CODEC.optionalFieldOf("feet").forGetter(requirement -> requirement.feet),
            ItemRequirement.CODEC.optionalFieldOf("mainhand").forGetter(requirement -> requirement.mainHand),
            ItemRequirement.CODEC.optionalFieldOf("offhand").forGetter(requirement -> requirement.offHand)
    ).apply(instance, EquipmentRequirement::new));

    public boolean test(Entity entity)
    {
        return head.isEmpty() && chest.isEmpty() && legs.isEmpty() && feet.isEmpty() && mainHand.isEmpty() && offHand.isEmpty()
            || entity instanceof LivingEntity living
            && (head.isEmpty()  || head.get().test(living.getItemBySlot(EquipmentSlot.HEAD), true))
            && (chest.isEmpty() || chest.get().test(living.getItemBySlot(EquipmentSlot.CHEST), true))
            && (legs.isEmpty()  || legs.get().test(living.getItemBySlot(EquipmentSlot.LEGS), true))
            && (feet.isEmpty()  || feet.get().test(living.getItemBySlot(EquipmentSlot.FEET), true))
            && (mainHand.isEmpty() || mainHand.get().test(living.getMainHandItem(), true))
            && (offHand.isEmpty()  || offHand.get().test(living.getOffhandItem(), true));
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

        EquipmentRequirement that = (EquipmentRequirement) obj;
        return head.equals(that.head)
            && chest.equals(that.chest)
            && legs.equals(that.legs)
            && feet.equals(that.feet)
            && mainHand.equals(that.mainHand)
            && offHand.equals(that.offHand);
    }
}