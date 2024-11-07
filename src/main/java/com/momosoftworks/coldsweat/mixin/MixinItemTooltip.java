package com.momosoftworks.coldsweat.mixin;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.client.event.TooltipHandler;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.config.type.Insulator;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.*;

@Mixin(ItemStack.class)
public class MixinItemTooltip
{
    ItemStack stack = (ItemStack) (Object) this;

    @Inject(method = "getTooltipLines", at = @At(value = "FIELD", target = "Lnet/minecraft/item/ItemStack$TooltipDisplayFlags;MODIFIERS:Lnet/minecraft/item/ItemStack$TooltipDisplayFlags;", shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void injectBeforeAttributes(PlayerEntity player, ITooltipFlag advanced, CallbackInfoReturnable<List<ITextComponent>> cir,
                                        // local variables
                                        List<ITextComponent> tooltip)
    {
        ItemStack stack = (ItemStack) (Object) this;

        // Add insulation attributes to tooltip
        AttributeModifierMap insulatorAttributes = new AttributeModifierMap();
        AttributeModifierMap unmetInsulatorAttributes = new AttributeModifierMap();
        for (Insulator insulator : ConfigSettings.INSULATION_ITEMS.get().get(stack.getItem()))
        {
            if (insulator.test(player, stack))
            {   insulatorAttributes.putAll(insulator.attributes);
            }
            else unmetInsulatorAttributes.putAll(insulator.attributes);
        }
        if (!insulatorAttributes.isEmpty() || !unmetInsulatorAttributes.isEmpty())
        {
            tooltip.add(new StringTextComponent(""));
            tooltip.add(new TranslationTextComponent("item.modifiers.insulation").withStyle(TextFormatting.GRAY));
            TooltipHandler.addModifierTooltipLines(tooltip, insulatorAttributes, false);
            TooltipHandler.addModifierTooltipLines(tooltip, unmetInsulatorAttributes, true);
        }

        // Add curio attributes to tooltip
        AttributeModifierMap curioAttributes = new AttributeModifierMap();
        AttributeModifierMap unmetCurioAttributes = new AttributeModifierMap();
        for (Insulator insulator : ConfigSettings.INSULATING_CURIOS.get().get(stack.getItem()))
        {
            if (insulator.test(player, stack))
            {   curioAttributes.putAll(insulator.attributes);
            }
            else unmetCurioAttributes.putAll(insulator.attributes);
        }
        if (!curioAttributes.isEmpty() || !unmetCurioAttributes.isEmpty())
        {
            tooltip.add(new StringTextComponent(""));
            tooltip.add(new TranslationTextComponent("item.modifiers.curio").withStyle(TextFormatting.GRAY));
            TooltipHandler.addModifierTooltipLines(tooltip, curioAttributes, false);
            TooltipHandler.addModifierTooltipLines(tooltip, unmetCurioAttributes, true);
        }
    }

    private static EquipmentSlotType CURRENT_SLOT_QUERY = null;

    @Inject(method = "getTooltipLines", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getAttributeModifiers(Lnet/minecraft/inventory/EquipmentSlotType;)Lcom/google/common/collect/Multimap;"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void setCurrentSlot(PlayerEntity pPlayer, ITooltipFlag pIsAdvanced, CallbackInfoReturnable<List<ITextComponent>> cir,
                                // locals
                                List<TextComponent> tooltip, IFormattableTextComponent itemName, int i, EquipmentSlotType[] allSlots, int var7, int var8, EquipmentSlotType slot)
    {
        CURRENT_SLOT_QUERY = slot;
    }

    private static Multimap<Attribute, AttributeModifier> UNMET_MODIFIERS = new FastMultiMap<>();

    @ModifyVariable(method = "getTooltipLines", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    private Multimap<Attribute, AttributeModifier> modifyAttributeModifiers(Multimap<Attribute, AttributeModifier> original, PlayerEntity player, ITooltipFlag advanced)
    {
        Multimap<Attribute, AttributeModifier> modifiers = MultimapBuilder.linkedHashKeys().arrayListValues().build(original);
        Multimap<Attribute, AttributeModifier> unmetModifiers = new FastMultiMap<>();
        if (player != null && MobEntity.getEquipmentSlotForItem(stack) == CURRENT_SLOT_QUERY)
        {
            for (Insulator insulator : ConfigSettings.INSULATING_ARMORS.get().get(stack.getItem()))
            {
                modifiers.putAll(insulator.attributes.getMap());
                if (!insulator.test(player, stack))
                    unmetModifiers.putAll(insulator.attributes.getMap());
            }
            ItemInsulationManager.getInsulationCap(stack).ifPresent(cap ->
            {
                cap.getInsulation().stream().map(Pair::getFirst).forEach(item ->
                {
                    for (Insulator insulator : ConfigSettings.INSULATION_ITEMS.get().get(item.getItem()))
                    {
                        modifiers.putAll(insulator.attributes.getMap());
                        if (!insulator.test(player, item))
                            unmetModifiers.putAll(insulator.attributes.getMap());
                    }
                });
            });
        }
        UNMET_MODIFIERS = unmetModifiers;
        return modifiers;
    }

    private static List<ITextComponent> TOOLTIP = null;
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
    private void setupCustomAttributeDisplay(PlayerEntity pPlayer, ITooltipFlag pIsAdvanced, CallbackInfoReturnable<List<ITextComponent>> cir,
                                             // Locals
                                             List<ITextComponent> tooltip, IFormattableTextComponent name, int hideFlags, EquipmentSlotType[] var6, int var7, int var8,
                                             EquipmentSlotType equipmentslot, Multimap<Attribute, AttributeModifier> attributeMap, Iterator<AttributeModifier> entryIterator,
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
        if (obj instanceof IFormattableTextComponent)
        {
            IFormattableTextComponent component = ((IFormattableTextComponent) obj);
            List<ITextComponent> siblings = component.getSiblings();
            if (TOOLTIP != null && ENTRY != null && MODIFIER != null
            && EntityTempManager.isTemperatureAttribute(ENTRY.getKey()))
            {
                boolean unmet = UNMET_MODIFIERS.remove(ENTRY.getKey(), ENTRY.getValue());
                IFormattableTextComponent newline = TooltipHandler.getFormattedAttributeModifier(ENTRY.getKey(), MODIFIER.getAmount(), MODIFIER.getOperation(), true, unmet);
                for (ITextComponent sibling : siblings)
                {   newline = newline.append(sibling);
                }
                return (E) newline;
            }
        }
        return obj;
    }
}
