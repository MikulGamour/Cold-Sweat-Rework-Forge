package com.momosoftworks.coldsweat.common.capability.handler;

import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.common.capability.insulation.ItemInsulationCap;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.init.ModItemComponents;
import com.momosoftworks.coldsweat.data.codec.configuration.InsulatorData;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import com.momosoftworks.coldsweat.util.item.ItemStackHelper;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

import javax.annotation.Nullable;
import java.util.*;

@EventBusSubscriber
public class ItemInsulationManager
{
    /**
     * Gets the insulation component from the item, or creates one if needed.<br>
     * This will always return {@code null} for non-armor items!
     */
    public static Optional<ItemInsulationCap> getInsulationCap(ItemStack stack)
    {
        if (isInsulatable(stack) && !stack.has(ModItemComponents.ARMOR_INSULATION))
        {   stack.set(ModItemComponents.ARMOR_INSULATION, new ItemInsulationCap());
        }
        return Optional.ofNullable(stack.get(ModItemComponents.ARMOR_INSULATION));
    }

    @SubscribeEvent
    public static void handleInventoryOpen(PlayerContainerEvent event)
    {   event.getEntity().getPersistentData().putBoolean("InventoryOpen", event instanceof PlayerContainerEvent.Open);
    }

    public static int getInsulationSlots(ItemStack item)
    {
        if (!isInsulatable(item)) return 0;
        return ConfigSettings.INSULATION_SLOTS.get().getSlots(((Equipable) item.getItem()).getEquipmentSlot(), item);
    }

    public static boolean isInsulatable(ItemStack stack)
    {
        return stack.getItem() instanceof Equipable
            && !ConfigSettings.INSULATION_ITEMS.get().containsKey(stack.getItem());
    }

    public static List<InsulatorData> getInsulatorsForStack(ItemStack stack)
    {
        if (stack.isEmpty()) return new ArrayList<>();

        List<InsulatorData> insulators = new ArrayList<>();
        if (isInsulatable(stack))
        {
            getInsulationCap(stack).ifPresent(cap ->
            {
                for (Pair<ItemStack, Multimap<InsulatorData, Insulation>> pair : cap.getInsulation())
                {   insulators.addAll(ConfigSettings.INSULATION_ITEMS.get().get(pair.getFirst().getItem()));
                }
            });
        }
        insulators.addAll(ConfigSettings.INSULATION_ITEMS.get().get(stack.getItem()));
        insulators.addAll(ConfigSettings.INSULATING_ARMORS.get().get(stack.getItem()));
        insulators.addAll(ConfigSettings.INSULATING_CURIOS.get().get(stack.getItem()));

        return insulators;
    }

    public static List<Insulation> getAllEffectiveInsulation(ItemStack armor, LivingEntity entity)
    {
        return ItemInsulationManager.getInsulationCap(armor)
               .map(ItemInsulationCap::getInsulation).orElse(new ArrayList<>())
               .stream()
               .map(pair -> pair.mapSecond(map -> new FastMultiMap<>(map.entries().stream().filter(entry -> entry.getKey().test(entity, armor)).toList())))
               .map(pair -> pair.getSecond().values())
               .flatMap(Collection::stream).toList();
    }

    public static List<AttributeModifier> getInsulationAttributeModifiers(ItemStack stack, Holder<Attribute> attribute, @Nullable AttributeModifier.Operation operation, @Nullable Entity owner)
    {
        List<AttributeModifier> modifiers = new ArrayList<>();
        for (InsulatorData insulator : getInsulatorsForStack(stack))
        {
            if (insulator.test(owner, stack))
            {
                modifiers.addAll(insulator.attributes().get(attribute)
                                          .stream()
                                          .filter(mod -> operation == null || mod.operation() == operation)
                                          .toList());
            }
        }
        return modifiers;
    }

    public static List<AttributeModifier> getAttributeModifiersForSlot(ItemStack stack, Holder<Attribute> attribute, EquipmentSlot slot, @Nullable AttributeModifier.Operation operation, @Nullable Entity owner)
    {
        List<AttributeModifier> modifiers = new ArrayList<>((operation != null
                                                             ? ItemStackHelper.getAttributeModifiers(stack, slot)
                                                                              .filter(entry -> entry.attribute().equals(Holder.direct(attribute)))
                                                                              .filter(entry -> entry.modifier().operation() == operation)
                                                             : ItemStackHelper.getAttributeModifiers(stack, slot)
                                                                              .filter(entry -> entry.attribute().equals(Holder.direct(attribute))))
                                                             .map(ItemAttributeModifiers.Entry::modifier)
                                                             .toList());
        modifiers.addAll(getInsulationAttributeModifiers(stack, attribute, operation, owner));
        return modifiers;
    }

    public static List<AttributeModifier> getAttributeModifiersForSlot(ItemStack stack, Holder<Attribute> attribute, EquipmentSlot slot)
    {   return getAttributeModifiersForSlot(stack, attribute, slot, null, null);
    }
}
