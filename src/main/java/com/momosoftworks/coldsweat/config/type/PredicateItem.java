package com.momosoftworks.coldsweat.config.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;


public record PredicateItem(Double value, ItemRequirement data, EntityRequirement requirement, CompoundTag extraData) implements NbtSerializable
{
    public static final Codec<PredicateItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("value").forGetter(PredicateItem::value),
            ItemRequirement.CODEC.fieldOf("data").forGetter(PredicateItem::data),
            EntityRequirement.getCodec().fieldOf("requirement").forGetter(PredicateItem::requirement),
            CompoundTag.CODEC.optionalFieldOf("extra_data", new CompoundTag()).forGetter(PredicateItem::extraData))
    .apply(instance, PredicateItem::new));

    public PredicateItem(Double value, ItemRequirement data, EntityRequirement requirement)
    {   this(value, data, requirement, new CompoundTag());
    }

    public boolean test(ItemStack stack)
    {   return data.test(stack, true);
    }

    public boolean test(Entity entity, ItemStack stack)
    {   return data.test(stack, true) && requirement.test(entity);
    }
 
    @Override
    public CompoundTag serialize()
    {
        return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
    }

    public static PredicateItem deserialize(CompoundTag tag)
    {
        return CODEC.decode(NbtOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalStateException("Failed to deserialize PredicateItem")).getFirst();
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
        PredicateItem that = (PredicateItem) obj;
        return this.value.equals(that.value)
            && this.data.equals(that.data)
            && this.requirement.equals(that.requirement)
            && this.extraData.equals(that.extraData);
    }
}
