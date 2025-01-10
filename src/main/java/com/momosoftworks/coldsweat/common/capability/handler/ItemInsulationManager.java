package com.momosoftworks.coldsweat.common.capability.handler;

import com.google.common.collect.ImmutableList;
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

    public static List<InsulatorData> getAllInsulatorsForStack(ItemStack stack)
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

    /**
     * Returns a list of all valid insulation applied to the given armor item.<br>
     * Insulation is considered valid if its requirement passes for the given armor and entity.
     * @param armor The armor item from which to get insulation.
     * @param entity The entity wearing the item. If null, the insulators' entity requirements will always pass.
     * @return an IMMUTABLE list of valid insulation on the armor item
     */
    public static List<InsulatorData> getEffectiveAppliedInsulation(ItemStack armor, @Nullable LivingEntity entity)
    {
        return ItemInsulationManager.getInsulationCap(armor)
               .map(ItemInsulationCap::getInsulation).orElse(new ArrayList<>())
               .stream()
               .map(pair -> pair.mapSecond(map -> new FastMultiMap<>(map.entries().stream().filter(entry -> entry.getKey().test(entity, pair.getFirst())).toList())))
                .map(map -> map.getSecond().keySet())
               .flatMap(Collection::stream).toList();
    }

    /**
     * Gets both applied an intrinsic insulation on the armor item.<br>
     * See {@link #getEffectiveAppliedInsulation(ItemStack, LivingEntity)} for more information.
     */
    public static List<InsulatorData> getAllEffectiveInsulation(ItemStack armor, @Nullable LivingEntity entity)
    {
        List<InsulatorData> insulation = new ArrayList<>(getEffectiveAppliedInsulation(armor, entity));
        insulation.addAll(ConfigSettings.INSULATING_ARMORS.get().get(armor.getItem()).stream().filter(insulator -> insulator.test(entity, armor)).toList());
        return ImmutableList.copyOf(insulation);
    }

    public static List<AttributeModifier> getAppliedInsulationAttributes(ItemStack stack, Holder<Attribute> attribute, @Nullable AttributeModifier.Operation operation, @Nullable Entity owner)
    {
        List<AttributeModifier> modifiers = new ArrayList<>();
        for (InsulatorData insulator : getAllInsulatorsForStack(stack))
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
        modifiers.addAll(getAppliedInsulationAttributes(stack, attribute, operation, owner));
        return modifiers;
    }

    public static List<AttributeModifier> getAttributeModifiersForSlot(ItemStack stack, Holder<Attribute> attribute, EquipmentSlot slot)
    {   return getAttributeModifiersForSlot(stack, attribute, slot, null, null);
    }
}
