package com.momosoftworks.coldsweat.mixin;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.client.event.TooltipHandler;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.InsulatorData;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.*;

@Mixin(ItemStack.class)
public class MixinItemTooltip
{
    ItemStack stack = (ItemStack) (Object) this;

    @Inject(method = "getTooltipLines", at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/ItemStack$TooltipPart;MODIFIERS:Lnet/minecraft/world/item/ItemStack$TooltipPart;", shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void injectBeforeAttributes(Player player, TooltipFlag advanced, CallbackInfoReturnable<List<MutableComponent>> cir,
                                      List<MutableComponent> tooltip)
    {
        ItemStack stack = (ItemStack) (Object) this;

        // Add insulation attributes to tooltip
        AttributeModifierMap insulatorAttributes = new AttributeModifierMap();
        AttributeModifierMap unmetInsulatorAttributes = new AttributeModifierMap();
        for (InsulatorData insulator : ConfigSettings.INSULATION_ITEMS.get().get(stack.getItem()))
        {
            if (TooltipHandler.passesRequirement(insulator))
            {   insulatorAttributes.putAll(insulator.attributes());
            }
            else unmetInsulatorAttributes.putAll(insulator.attributes());
        }
        if (!insulatorAttributes.isEmpty() || !unmetInsulatorAttributes.isEmpty())
        {
            tooltip.add(new TextComponent(""));
            tooltip.add(new TranslatableComponent("item.modifiers.insulation").withStyle(ChatFormatting.GRAY));
            TooltipHandler.addModifierTooltipLines(tooltip, insulatorAttributes, true, false);
            TooltipHandler.addModifierTooltipLines(tooltip, unmetInsulatorAttributes, true, true);
        }

        // Add curio attributes to tooltip
        AttributeModifierMap curioAttributes = new AttributeModifierMap();
        AttributeModifierMap unmetCurioAttributes = new AttributeModifierMap();
        for (InsulatorData insulator : ConfigSettings.INSULATING_CURIOS.get().get(stack.getItem()))
        {
            if (TooltipHandler.passesRequirement(insulator))
            {   curioAttributes.putAll(insulator.attributes());
            }
            else unmetCurioAttributes.putAll(insulator.attributes());
        }
        if (!curioAttributes.isEmpty() || !unmetCurioAttributes.isEmpty())
        {
            tooltip.add(new TextComponent(""));
            tooltip.add(new TranslatableComponent("item.modifiers.curio").withStyle(ChatFormatting.GRAY));
            TooltipHandler.addModifierTooltipLines(tooltip, curioAttributes, true, false);
            TooltipHandler.addModifierTooltipLines(tooltip, unmetCurioAttributes, true, true);
        }
    }

    private static EquipmentSlot CURRENT_SLOT_QUERY = null;

    @Inject(method = "getTooltipLines", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getAttributeModifiers(Lnet/minecraft/world/entity/EquipmentSlot;)Lcom/google/common/collect/Multimap;"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void setCurrentSlot(Player player, TooltipFlag advanced, CallbackInfoReturnable<List<MutableComponent>> cir,
                                List<MutableComponent> tooltip, MutableComponent name, int hideFlags, EquipmentSlot[] allSlots, int var7, int var8, EquipmentSlot slot)
    {
        CURRENT_SLOT_QUERY = slot;
    }

    private static Multimap<Attribute, AttributeModifier> INSULATION_MODIFIERS = new FastMultiMap<>();
    private static Multimap<Attribute, AttributeModifier> UNMET_MODIFIERS = new FastMultiMap<>();

    @ModifyVariable(method = "getTooltipLines", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    private Multimap<Attribute, AttributeModifier> modifyAttributeModifiers(Multimap<Attribute, AttributeModifier> original, Player player, TooltipFlag advanced)
    {
        INSULATION_MODIFIERS.clear();
        UNMET_MODIFIERS.clear();
        Multimap<Attribute, AttributeModifier> modifiers = MultimapBuilder.linkedHashKeys().arrayListValues().build(original);
        if (player != null && LivingEntity.getEquipmentSlotForItem(stack) == CURRENT_SLOT_QUERY)
        {
            for (InsulatorData insulator : ConfigSettings.INSULATING_ARMORS.get().get(stack.getItem()))
            {
                modifiers.putAll(insulator.attributes().getMap());
                if (TooltipHandler.passesRequirement(insulator))
                {   INSULATION_MODIFIERS.putAll(insulator.attributes().getMap());
                }
                else UNMET_MODIFIERS.putAll(insulator.attributes().getMap());
            }
            ItemInsulationManager.getInsulationCap(stack).ifPresent(cap ->
            {
                cap.getInsulation().stream().map(Pair::getFirst).forEach(item ->
                {
                    for (InsulatorData insulator : ConfigSettings.INSULATION_ITEMS.get().get(item.getItem()))
                    {
                        modifiers.putAll(insulator.attributes().getMap());
                        if (TooltipHandler.passesRequirement(insulator))
                        {   INSULATION_MODIFIERS.putAll(insulator.attributes().getMap());
                        }
                        else UNMET_MODIFIERS.putAll(insulator.attributes().getMap());
                    }
                });
            });
        }
        return modifiers;
    }

    private static List<Component> TOOLTIP = null;
    private static Map.Entry<Attribute, AttributeModifier> ENTRY = null;
    private static AttributeModifier MODIFIER = null;

    @Inject(method = "getTooltipLines",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"),
            slice = @Slice
                    (
                            from = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 7),
                            to = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 9)
                    ),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void setupCustomAttributeDisplay(Player pPlayer, TooltipFlag pIsAdvanced, CallbackInfoReturnable<List<Component>> cir,
                                             // Locals
                                             List<Component> tooltip, MutableComponent name, int hideFlags, EquipmentSlot[] var6, int var7, int var8,
                                             EquipmentSlot equipmentslot, Multimap<Attribute, AttributeModifier> attributeMap, Iterator<AttributeModifier> entryIterator,
                                             Map.Entry<Attribute, AttributeModifier> entry, AttributeModifier modifier, double d0, boolean flag, double d1)
    {
        TOOLTIP = tooltip;
        ENTRY = entry;
        MODIFIER = modifier;
    }

    @ModifyArg(method = "getTooltipLines",
               at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"),
               slice = @Slice
               (
                   from = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 7),
                   to = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 9)
               ))
    private <E> E customAttributeFormatting(E obj)
    {
        if (obj instanceof MutableComponent component)
        {
            List<Component> siblings = component.getSiblings();

            if (TOOLTIP != null && ENTRY != null && MODIFIER != null)
            {
                boolean hasUnmetRequirements = UNMET_MODIFIERS.remove(ENTRY.getKey(), ENTRY.getValue());
                boolean isFromInsulation = INSULATION_MODIFIERS.remove(ENTRY.getKey(), ENTRY.getValue()) || hasUnmetRequirements;

                if (EntityTempManager.isTemperatureAttribute(ENTRY.getKey()))
                {
                    MutableComponent newline = TooltipHandler.getFormattedAttributeModifier(ENTRY.getKey(), MODIFIER.getAmount(), MODIFIER.getOperation(), true, hasUnmetRequirements);

                    for (Component sibling : siblings)
                    {   newline = newline.append(sibling);
                    }
                    return (E) newline;
                }
                else return (E) TooltipHandler.addTooltipFlags(component, isFromInsulation, hasUnmetRequirements);
            }
        }
        return obj;
    }
}
