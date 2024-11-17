package com.momosoftworks.coldsweat.data.codec.configuration;


import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;

public interface RequirementHolder
{
    default boolean test(ItemStack stack)
    {   return true;
    }

    default boolean test(Entity entity)
    {   return true;
    }

    default boolean test(ItemStack stack, Entity entity)
    {   return test(stack) && test(entity);
    }

    default boolean test(Entity entity, ItemStack stack)
    {   return test(stack, entity);
    }
}
