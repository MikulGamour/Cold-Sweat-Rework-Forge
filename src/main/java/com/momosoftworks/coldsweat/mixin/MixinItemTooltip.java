package com.momosoftworks.coldsweat.mixin;

import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.client.event.TooltipHandler;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.InsulatorData;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class MixinItemTooltip
{
    @Shadow protected abstract void addModifierTooltip(Consumer<Component> pTooltipAdder, @Nullable Player pPlayer, Holder<Attribute> pAttribute, AttributeModifier pModfier);

    ItemStack stack = (ItemStack) (Object) this;

    private static List<Component> TOOLTIP;

    @Inject(method = "getTooltipLines", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;addToTooltip(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V",
                                                 ordinal = 6, shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void injectBeforeAttributes(Item.TooltipContext pTooltipContext, Player player, TooltipFlag pTooltipFlag, CallbackInfoReturnable<List<Component>> cir,
                                        //locals
                                        List<Component> tooltip, MutableComponent mutablecomponent, Consumer consumer)
    {
        ItemStack stack = (ItemStack) (Object) this;
        TOOLTIP = tooltip;

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
            tooltip.add(CommonComponents.EMPTY);
            tooltip.add(Component.translatable("item.modifiers.insulation").withStyle(ChatFormatting.GRAY));
            TooltipHandler.addModifierTooltipLines(tooltip, insulatorAttributes, false);
            TooltipHandler.addModifierTooltipLines(tooltip, unmetInsulatorAttributes, true);
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
            tooltip.add(CommonComponents.EMPTY);
            tooltip.add(Component.translatable("item.modifiers.curio").withStyle(ChatFormatting.GRAY));
            TooltipHandler.addModifierTooltipLines(tooltip, curioAttributes, false);
            TooltipHandler.addModifierTooltipLines(tooltip, unmetCurioAttributes, true);
        }
    }

    private static Multimap<Holder<Attribute>, AttributeModifier> INSULATION_MODIFIERS = new FastMultiMap<>();
    private static Multimap<Holder<Attribute>, AttributeModifier> UNMET_MODIFIERS = new FastMultiMap<>();

    @Inject(method = "addAttributeTooltips", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V", shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void getItemAttributes(Consumer<Component> pTooltipAdder, Player player, CallbackInfo ci,
                                   // locals
                                   ItemAttributeModifiers itemattributemodifiers, EquipmentSlotGroup[] allSlots, int var5, int var6, EquipmentSlotGroup slot, MutableBoolean isFirstLine)
    {
        INSULATION_MODIFIERS.clear();
        UNMET_MODIFIERS.clear();

        // We don't care if the item is not equipped in the correct slot
        if (player == null || EquipmentSlotGroup.bySlot(Minecraft.getInstance().player.getEquipmentSlotForItem(stack)) != slot
        || Arrays.stream(EquipmentSlot.values()).noneMatch(slot::test))
        {   return;
        }

        for (InsulatorData insulator : ConfigSettings.INSULATING_ARMORS.get().get(stack.getItem()))
        {
            boolean strikethrough = !TooltipHandler.passesRequirement(insulator);
            for (Map.Entry<Holder<Attribute>, AttributeModifier> entry : insulator.attributes().getMap().entries())
            {
                if (strikethrough) UNMET_MODIFIERS.put(entry.getKey(), entry.getValue());
                else INSULATION_MODIFIERS.put(entry.getKey(), entry.getValue());
                pTooltipAdder.accept(TooltipHandler.getFormattedAttributeModifier(entry.getKey(), entry.getValue().amount(), entry.getValue().operation(), true, strikethrough));
            }
        }
        ItemInsulationManager.getInsulationCap(stack).ifPresent(cap ->
        {
            cap.getInsulation().stream().map(Pair::getFirst).forEach(item ->
            {
                for (InsulatorData insulator : ConfigSettings.INSULATION_ITEMS.get().get(item.getItem()))
                {
                    boolean strikethrough = !TooltipHandler.passesRequirement(insulator);
                    for (Map.Entry<Holder<Attribute>, AttributeModifier> entry : insulator.attributes().getMap().entries())
                    {
                        if (strikethrough) UNMET_MODIFIERS.put(entry.getKey(), entry.getValue());
                        else INSULATION_MODIFIERS.put(entry.getKey(), entry.getValue());
                        pTooltipAdder.accept(TooltipHandler.getFormattedAttributeModifier(entry.getKey(), entry.getValue().amount(), entry.getValue().operation(), true, strikethrough));
                    }
                }
            });
        });
    }

    @ModifyArg(method = "addModifierTooltip",
               at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"),
               index = 0)
    private Component setupModdedAttributeDisplay(Component tooltip, Consumer<Component> tooltipAdder, @Nullable Player player, Holder<Attribute> attribute, AttributeModifier modifier)
    {
        boolean hasUnmetRequirements = UNMET_MODIFIERS.remove(attribute, modifier);
        boolean isFromInsulation = INSULATION_MODIFIERS.remove(attribute, modifier) || hasUnmetRequirements;

        // TODO: Check this
        if (EntityTempManager.isTemperatureAttribute(attribute.value()))
        {   return TooltipHandler.getFormattedAttributeModifier(attribute, modifier.amount(), modifier.operation(), isFromInsulation, hasUnmetRequirements);
        }
        else if (tooltip instanceof MutableComponent mutable)
        {   return TooltipHandler.addTooltipFlags(mutable, isFromInsulation, hasUnmetRequirements);
        }
        return tooltip;
    }
}
