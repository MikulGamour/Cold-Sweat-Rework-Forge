package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;

import java.util.Optional;

public class EquipmentRequirement
{
    public final Optional<ItemRequirement> head;
    public final Optional<ItemRequirement> chest;
    public final Optional<ItemRequirement> legs;
    public final Optional<ItemRequirement> feet;
    public final Optional<ItemRequirement> mainHand;
    public final Optional<ItemRequirement> offHand;

    public EquipmentRequirement(Optional<ItemRequirement> head, Optional<ItemRequirement> chest,
                                Optional<ItemRequirement> legs, Optional<ItemRequirement> feet,
                                Optional<ItemRequirement> mainHand, Optional<ItemRequirement> offHand)
    {
        this.head = head;
        this.chest = chest;
        this.legs = legs;
        this.feet = feet;
        this.mainHand = mainHand;
        this.offHand = offHand;
    }

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
        return !head.isPresent() && !chest.isPresent() && !legs.isPresent() && !feet.isPresent() && !mainHand.isPresent() && !offHand.isPresent()
            || entity instanceof LivingEntity
            && (!head.isPresent() || head.get().test(((LivingEntity) entity).getItemBySlot(EquipmentSlotType.HEAD), true))
            && (!chest.isPresent() || chest.get().test(((LivingEntity) entity).getItemBySlot(EquipmentSlotType.CHEST), true))
            && (!legs.isPresent() || legs.get().test(((LivingEntity) entity).getItemBySlot(EquipmentSlotType.LEGS), true))
            && (!feet.isPresent() || feet.get().test(((LivingEntity) entity).getItemBySlot(EquipmentSlotType.FEET), true))
            && (!mainHand.isPresent() || mainHand.get().test(((LivingEntity) entity).getMainHandItem(), true))
            && (!offHand.isPresent() || offHand.get().test(((LivingEntity) entity).getOffhandItem(), true));
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