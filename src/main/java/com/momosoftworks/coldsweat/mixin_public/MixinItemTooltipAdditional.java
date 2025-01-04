package com.momosoftworks.coldsweat.mixin_public;

import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.InsulatorData;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

import java.util.Map;

@EventBusSubscriber
public class MixinItemTooltipAdditional
{
    public static boolean GETTING_ATTRIBUTES = false;
    public static Multimap<Holder<Attribute>, AttributeModifier> INSULATION_MODIFIERS = new FastMultiMap<>();
    public static Multimap<Holder<Attribute>, AttributeModifier> UNMET_MODIFIERS = new FastMultiMap<>();
    public static EquipmentSlotGroup CURRENT_SLOT_QUERY = null;

    @SubscribeEvent
    public static void addModifiers(ItemAttributeModifierEvent event)
    {
        if (!GETTING_ATTRIBUTES) return;
        if (Minecraft.getInstance().player == null
        || MixinItemTooltipAdditional.CURRENT_SLOT_QUERY != EquipmentSlotGroup.bySlot(Minecraft.getInstance().player.getEquipmentSlotForItem(event.getItemStack())))
        {   return;
        }

        for (InsulatorData data : ConfigSettings.INSULATING_ARMORS.get().get(event.getItemStack().getItem()))
        {
            for (Map.Entry<Holder<Attribute>, AttributeModifier> entry : data.attributes().getMap().entries())
            {   event.addModifier(entry.getKey(), entry.getValue(), CURRENT_SLOT_QUERY);
            }
        }
        ItemInsulationManager.getInsulationCap(event.getItemStack()).ifPresent(cap ->
        {
            cap.getInsulation().stream().map(Pair::getFirst).forEach(item ->
            {
                for (InsulatorData insulator : ConfigSettings.INSULATION_ITEMS.get().get(item.getItem()))
                {
                    for (Map.Entry<Holder<Attribute>, AttributeModifier> entry : insulator.attributes().getMap().entries())
                    {   event.addModifier(entry.getKey(), entry.getValue(), CURRENT_SLOT_QUERY);
                    }
                }
            });
        });
    }
}
