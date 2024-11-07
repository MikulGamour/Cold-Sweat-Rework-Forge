package com.momosoftworks.coldsweat.config.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;


public class PredicateItem implements NbtSerializable
{
    public Double value;
    public ItemRequirement data;
    public EntityRequirement requirement;
    public CompoundNBT extraData;

    public PredicateItem(Double value, ItemRequirement data, EntityRequirement requirement, CompoundNBT extraData)
    {
        this.value = value;
        this.data = data;
        this.requirement = requirement;
        this.extraData = extraData;
    }

    public static final Codec<PredicateItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("value").forGetter(data -> data.value),
            ItemRequirement.CODEC.fieldOf("data").forGetter(data -> data.data),
            EntityRequirement.getCodec().fieldOf("requirement").forGetter(data -> data.requirement),
            CompoundNBT.CODEC.optionalFieldOf("extra_data", new CompoundNBT()).forGetter(data -> data.extraData))
    .apply(instance, PredicateItem::new));

    public PredicateItem(Double value, ItemRequirement data, EntityRequirement requirement)
    {   this(value, data, requirement, new CompoundNBT());
    }

    public boolean test(ItemStack stack)
    {   return data.test(stack, true);
    }

    public boolean test(Entity entity, ItemStack stack)
    {   return data.test(stack, true) && requirement.test(entity);
    }

    @Override
    public CompoundNBT serialize()
    {
        return (CompoundNBT) CODEC.encodeStart(NBTDynamicOps.INSTANCE, this).result().orElseGet(CompoundNBT::new);
    }

    public static PredicateItem deserialize(CompoundNBT tag)
    {
        return CODEC.decode(NBTDynamicOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalStateException("Failed to deserialize PredicateItem")).getFirst();
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
