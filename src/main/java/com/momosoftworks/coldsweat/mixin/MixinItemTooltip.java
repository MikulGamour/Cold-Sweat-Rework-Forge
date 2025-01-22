package com.momosoftworks.coldsweat.mixin;

import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.client.event.TooltipHandler;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.InsulatorData;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import com.momosoftworks.coldsweat.mixin_public.MixinItemTooltipAdditional;
import com.momosoftworks.coldsweat.util.ClientOnlyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.fml.util.thread.EffectiveSide;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
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

import java.util.List;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class MixinItemTooltip
{
    @Shadow protected abstract void addModifierTooltip(Consumer<Component> pTooltipAdder, @Nullable Player pPlayer, Holder<Attribute> pAttribute, AttributeModifier pModfier);

    ItemStack stack = (ItemStack) (Object) this;

    @Inject(method = "getTooltipLines", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;addToTooltip(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V",
                                                 ordinal = 6, shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void injectBeforeAttributes(Item.TooltipContext pTooltipContext, Player player, TooltipFlag pTooltipFlag, CallbackInfoReturnable<List<Component>> cir,
                                        //locals
                                        List<Component> tooltip, MutableComponent mutablecomponent, Consumer consumer)
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
            tooltip.add(CommonComponents.EMPTY);
            tooltip.add(Component.translatable("item.modifiers.insulation").withStyle(ChatFormatting.GRAY));
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
            tooltip.add(CommonComponents.EMPTY);
            tooltip.add(Component.translatable("item.modifiers.curio").withStyle(ChatFormatting.GRAY));
            TooltipHandler.addModifierTooltipLines(tooltip, curioAttributes, true, false);
            TooltipHandler.addModifierTooltipLines(tooltip, unmetCurioAttributes, true, true);
        }
    }

    @Inject(method = "addAttributeTooltips", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void getItemAttributes(Consumer<Component> pTooltipAdder, Player player, CallbackInfo ci,
                                   // locals
                                   ItemAttributeModifiers itemattributemodifiers, EquipmentSlotGroup[] allSlots, int var5, int var6, EquipmentSlotGroup slot, MutableBoolean isFirstLine)
    {
        MixinItemTooltipAdditional.INSULATION_MODIFIERS.clear();
        MixinItemTooltipAdditional.UNMET_MODIFIERS.clear();
        MixinItemTooltipAdditional.GETTING_ATTRIBUTES = true;
        MixinItemTooltipAdditional.CURRENT_SLOT_QUERY = slot;
    }

    @Inject(method = "addAttributeTooltips", at = @At(value = "TAIL"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void clearGettingTooltipLines(Consumer<Component> pTooltipAdder, Player player, CallbackInfo ci)
    {
        MixinItemTooltipAdditional.GETTING_ATTRIBUTES = false;
    }

    @Mixin(CommonHooks.class)
    public static class GetModifiers
    {
        @Inject(method = "computeModifiedAttributes", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
        private static void getAttributeModifiers(ItemStack stack, ItemAttributeModifiers defaultModifiers, CallbackInfoReturnable<ItemAttributeModifiers> cir,
                                                  //locals
                                                  ItemAttributeModifierEvent event)
        {
            if (!EffectiveSide.get().isClient()) return;

            Player player = ClientOnlyHelper.getClientPlayer();
            if (player == null) return;
            if (MixinItemTooltipAdditional.CURRENT_SLOT_QUERY != EquipmentSlotGroup.bySlot(player.getEquipmentSlotForItem(stack)))
            {   return;
            }

            if (MixinItemTooltipAdditional.CURRENT_SLOT_QUERY.test(player.getEquipmentSlotForItem(stack)))
            {
                for (InsulatorData insulator : ConfigSettings.INSULATING_ARMORS.get().get(stack.getItem()))
                {
                    if (TooltipHandler.passesRequirement(insulator))
                    {   MixinItemTooltipAdditional.INSULATION_MODIFIERS.putAll(insulator.attributes().getMap());
                    }
                    else MixinItemTooltipAdditional.UNMET_MODIFIERS.putAll(insulator.attributes().getMap());
                }
                ItemInsulationManager.getInsulationCap(stack).ifPresent(cap ->
                {
                    cap.getInsulation().stream().map(Pair::getFirst).forEach(item ->
                    {
                        for (InsulatorData insulator : ConfigSettings.INSULATION_ITEMS.get().get(item.getItem()))
                        {
                            if (TooltipHandler.passesRequirement(insulator))
                            {   MixinItemTooltipAdditional.INSULATION_MODIFIERS.putAll(insulator.attributes().getMap());
                            }
                            else MixinItemTooltipAdditional.UNMET_MODIFIERS.putAll(insulator.attributes().getMap());
                        }
                    });
                });
            }
        }
    }

    private static Holder<Attribute> ATTRIBUTE;
    private static AttributeModifier MODIFIER;

    @Inject(method = "addModifierTooltip", at = @At(value = "HEAD"))
    private void captureMethodArgs(Consumer<Component> tooltipAdder, @Nullable Player player, Holder<Attribute> attribute, AttributeModifier modifier, CallbackInfo ci)
    {
        ATTRIBUTE = attribute;
        MODIFIER = modifier;
    }

    @ModifyArg(method = "addModifierTooltip",
               at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private Object customAttributeFormatting(Object obj)
    {
        if (obj instanceof MutableComponent component
        && ATTRIBUTE != null && MODIFIER != null)
        {
            boolean hasUnmetRequirements = MixinItemTooltipAdditional.UNMET_MODIFIERS.remove(ATTRIBUTE, MODIFIER);
            boolean isFromInsulation = MixinItemTooltipAdditional.INSULATION_MODIFIERS.remove(ATTRIBUTE, MODIFIER) || hasUnmetRequirements;

            if (EntityTempManager.isTemperatureAttribute(ATTRIBUTE))
            {
                MutableComponent newline = TooltipHandler.getFormattedAttributeModifier(ATTRIBUTE, MODIFIER.amount(), MODIFIER.operation(), isFromInsulation, hasUnmetRequirements);

                for (Component sibling : component.getSiblings())
                {   newline = newline.append(sibling);
                }
                return newline;
            }
            else return TooltipHandler.addTooltipFlags(component, isFromInsulation, hasUnmetRequirements);
        }
        return obj;
    }
}
