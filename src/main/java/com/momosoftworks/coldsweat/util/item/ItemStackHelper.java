package com.momosoftworks.coldsweat.util.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.fml.util.ObfuscationReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ItemStackHelper
{
    public static void playBucketEmptySound(ItemStack stack, @Nullable Player pPlayer, LevelAccessor pLevel, BlockPos pPos)
    {
        Method playEmptySound = ObfuscationReflectionHelper.findMethod(BucketItem.class, "playEmptySound", Player.class, LevelAccessor.class, BlockPos.class);
        playEmptySound.setAccessible(true);
        try
        {   playEmptySound.invoke(stack.getItem(), pPlayer, pLevel, pPos);
        }
        catch (Exception e)
        {   e.printStackTrace();
        }
    }

    public static Stream<ItemAttributeModifiers.Entry> getAttributeModifiers(ItemStack stack, AttributeModifier.Operation operation)
    {   return stack.getAttributeModifiers().modifiers().stream().filter(entry -> entry.modifier().operation() == operation);
    }

    public static Stream<ItemAttributeModifiers.Entry> getAttributeModifiers(ItemStack stack, EquipmentSlot slot)
    {   return stack.getAttributeModifiers().modifiers().stream().filter(entry -> entry.slot().test(slot));
    }

    public static <T> T getOrCreateComponent(ItemStack stack, DataComponentType<T> componentType, Supplier<T> componentCreator)
    {
        T component = stack.get(componentType);
        if (component == null)
        {
            component = componentCreator.get();
            stack.set(componentType, component);
        }
        return component;
    }

    public static EquipmentSlot getEquipmentSlot(ItemStack stack)
    {
        final EquipmentSlot slot = stack.getEquipmentSlot();
        // Item overrides getEquipmentSlot()
        if (slot != null)
        {   return slot;
        }
        // Item is equippable
        Equipable equipable = Equipable.get(stack);
        if (equipable != null)
        {   return equipable.getEquipmentSlot();
        }

        return EquipmentSlot.MAINHAND;
    }
}
