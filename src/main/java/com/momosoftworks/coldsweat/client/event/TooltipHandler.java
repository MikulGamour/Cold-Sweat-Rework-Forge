package com.momosoftworks.coldsweat.client.event;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.client.gui.tooltip.ClientSoulspringTooltip;
import com.momosoftworks.coldsweat.client.gui.tooltip.InsulationAttributeTooltip;
import com.momosoftworks.coldsweat.client.gui.tooltip.InsulationTooltip;
import com.momosoftworks.coldsweat.client.gui.tooltip.SoulspringTooltip;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.common.item.SoulspringLampItem;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import com.momosoftworks.coldsweat.core.network.message.SyncItemPredicatesMessage;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.configuration.FoodData;
import com.momosoftworks.coldsweat.data.codec.configuration.FuelData;
import com.momosoftworks.coldsweat.data.codec.configuration.InsulatorData;
import com.momosoftworks.coldsweat.core.init.ModAttributes;
import com.momosoftworks.coldsweat.core.init.ModItemComponents;
import com.momosoftworks.coldsweat.core.init.ModItems;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.entity.EntityHelper;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.FastMap;
import com.momosoftworks.coldsweat.util.serialization.DynamicHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.ApiStatus;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

@EventBusSubscriber(Dist.CLIENT)
public class TooltipHandler
{
    public static final Style COLD = Style.EMPTY.withColor(3767039);
    public static final Style HOT = Style.EMPTY.withColor(16736574);
    public static final Component EXPAND_TOOLTIP = Component.literal("?").withStyle(Style.EMPTY.withColor(ChatFormatting.BLUE).withUnderlined(true))
                                           .append(Component.literal(" 'Shift'").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withUnderlined(false)));
    public static final DecimalFormat ATTRIBUTE_MODIFIER_FORMAT = Util.make(new DecimalFormat("#.##"), (format) -> {
        format.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
    });

    private static int HOVERED_ITEM_UPDATE_COOLDOWN = 0;
    private static ItemStack HOVERED_STACK = ItemStack.EMPTY;
    public static HashMap<UUID, Boolean> HOVERED_STACK_PREDICATES = new HashMap<>();

    public static <T extends ConfigData> boolean passesRequirement(T element)
    {   return HOVERED_STACK_PREDICATES.getOrDefault(element.getId(), true);
    }

    public static boolean isShiftDown()
    {   return Screen.hasShiftDown() || ConfigSettings.EXPAND_TOOLTIPS.get();
    }

    public static int getTooltipTitleIndex(List<Either<FormattedText, TooltipComponent>> tooltip, ItemStack stack)
    {
        if (tooltip.isEmpty()) return 0;

        int tooltipStartIndex;
        String hoverName = stack.getHoverName().getString();

        if (CompatManager.isIcebergLoaded())
        {   tooltipStartIndex = CompatManager.LegendaryTooltips.getTooltipStartIndex(tooltip) + 1;
        }
        else for (tooltipStartIndex = 0; tooltipStartIndex < tooltip.size(); tooltipStartIndex++)
        {
            if (tooltip.get(tooltipStartIndex).left().map(FormattedText::getString).map(String::strip).orElse("").equals(hoverName))
            {   tooltipStartIndex++;
                break;
            }
        }
        tooltipStartIndex = CSMath.clamp(tooltipStartIndex, 0, tooltip.size());
        return tooltipStartIndex;
    }

    public static int getTooltipEndIndex(List<Either<FormattedText, TooltipComponent>> tooltip, ItemStack stack)
    {
        int tooltipEndIndex = tooltip.size();
        if (Minecraft.getInstance().options.advancedItemTooltips)
        {
            for (--tooltipEndIndex; tooltipEndIndex > 0; tooltipEndIndex--)
            {
                if (tooltip.get(tooltipEndIndex).left().map(text -> text.getString().equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())).orElse(false))
                {   break;
                }
            }
        }
        tooltipEndIndex = CSMath.clamp(tooltipEndIndex, 0, tooltip.size());
        return tooltipEndIndex;
    }

    public static void addModifierTooltipLines(List<Component> tooltip, AttributeModifierMap map, boolean showIcon, boolean strikethrough)
    {
        map.getMap().asMap().forEach((attribute, modifiers) ->
        {
            for (AttributeModifier.Operation operation : AttributeModifier.Operation.values())
            {
                double value = 0;
                for (AttributeModifier modifier : modifiers.stream().filter(mod -> mod.operation() == operation).toList())
                {   value += modifier.amount();
                }
                if (value != 0)
                {   tooltip.add(getFormattedAttributeModifier(attribute, value, operation, showIcon, strikethrough));
                }
            }
        });
    }

    public static MutableComponent getFormattedAttributeModifier(Holder<Attribute> attribute, double value, AttributeModifier.Operation operation,
                                                                 boolean forTooltip, boolean strikethrough)
    {
        if (attribute == null) return Component.empty();
        String attributeName = attribute.value().getDescriptionId().replace("attribute.", "");

        if (operation == AttributeModifier.Operation.ADD_VALUE
        && (attribute == ModAttributes.FREEZING_POINT.value()
        || attribute == ModAttributes.BURNING_POINT.value()
        || attribute == ModAttributes.WORLD_TEMPERATURE.value()))
        {
            value = Temperature.convert(value, Temperature.Units.MC, ConfigSettings.CELSIUS.get() ? Temperature.Units.C : Temperature.Units.F, false);
        }
        String operationString = operation == AttributeModifier.Operation.ADD_VALUE ? "add" : "multiply";
        ChatFormatting color;
        String sign;
        if (value >= 0)
        {
            color = ChatFormatting.BLUE;
            sign = "+";
        }
        else
        {   color = ChatFormatting.RED;
            sign = "";
        }
        String percent;
        if (operation != AttributeModifier.Operation.ADD_VALUE
        || attribute == ModAttributes.HEAT_RESISTANCE.value()
        || attribute == ModAttributes.COLD_RESISTANCE.value()
        || attribute == ModAttributes.HEAT_DAMPENING.value()
        || attribute == ModAttributes.COLD_DAMPENING.value())
        {   percent = "%";
            value *= 100;
        }
        else
        {   percent = "";
        }
        List<Object> params = new ArrayList<>(List.of(sign + CSMath.formatDoubleOrInt(CSMath.round(value, 2)) + percent));
        MutableComponent component;
        if (EntityTempManager.isTemperatureAttribute(attribute))
        {   component = Component.translatable(String.format("attribute.cold_sweat.modifier.%s.%s", operationString, attributeName), params.toArray());
        }
        else
        {
            component = getFormattedVanillaAttributeModifier(attribute, value, operation);
            TranslatableContents contents = (TranslatableContents) component.getContents();
            params.addAll(0, Arrays.asList(contents.getArgs()));
            component = setComponentContents(getFormattedVanillaAttributeModifier(attribute, value, operation), new TranslatableContents(contents.getKey(), contents.getFallback(), params.toArray()));
        }
        component = component.withStyle(color);
        component = addTooltipFlags(component, forTooltip, strikethrough);
        return component;
    }

    public static MutableComponent getFormattedVanillaAttributeModifier(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation)
    {
        double adjustedAmount;
        if (operation == AttributeModifier.Operation.ADD_VALUE)
        {
            if (attribute.equals(Attributes.KNOCKBACK_RESISTANCE))
            {   adjustedAmount = amount * 10.0D;
            }
            else
            {   adjustedAmount = amount;
            }
        }
        else
        {   adjustedAmount = amount * 100.0D;
        }

        if (amount >= 0.0D)
        {
            return Component.translatable("attribute.modifier.plus." + operation.getSerializedName(), ATTRIBUTE_MODIFIER_FORMAT.format(adjustedAmount),
                                          Component.translatable(attribute.value().getDescriptionId())).withStyle(ChatFormatting.BLUE);
        }
        else
        {   adjustedAmount *= -1;
            return Component.translatable("attribute.modifier.take." + operation.getSerializedName(), ATTRIBUTE_MODIFIER_FORMAT.format(adjustedAmount),
                                            Component.translatable(attribute.value().getDescriptionId())).withStyle(ChatFormatting.RED);
        }
    }

    public static MutableComponent setComponentContents(MutableComponent component, ComponentContents newContents)
    {
        MutableComponent newComponent = MutableComponent.create(newContents).setStyle(component.getStyle());
        component.getSiblings().forEach(newComponent::append);
        return newComponent;
    }

    public static MutableComponent addTooltipFlags(MutableComponent component, boolean showIcon, boolean strikethrough)
    {
        if (component.getContents() instanceof TranslatableContents translatable)
        {
            List<Object> params = new ArrayList<>(Arrays.asList(translatable.getArgs()));
            if (showIcon)
            {   params.add("show_icon");
            }
            if (strikethrough)
            {   params.add("strikethrough");
            }
            MutableComponent newComponent = setComponentContents(component, new TranslatableContents(translatable.getKey(), "_", params.toArray()));
            if (strikethrough)
            {   newComponent.setStyle(Style.EMPTY.withColor(7561572));
            }
            return newComponent;
        }
        return component;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void updateHoveredItem(ScreenEvent.Render.Pre event)
    {
        if (event.getScreen() instanceof AbstractContainerScreen<?> menu)
        {
            Slot hoveredSlot = menu.getSlotUnderMouse();
            if (hoveredSlot == null) return;

            ItemStack stack = hoveredSlot.getItem();

            EquipmentSlot equipmentSlot = EntityHelper.getEquipmentSlot(hoveredSlot.index);
            if (!HOVERED_STACK.equals(stack))
            {
                if (stack.isEmpty())
                {   HOVERED_STACK = stack;
                    return;
                }
                if (HOVERED_ITEM_UPDATE_COOLDOWN <= 0
                || ItemInsulationManager.getAllInsulatorsForStack(stack).stream().map(InsulatorData::getId).anyMatch(id -> !HOVERED_STACK_PREDICATES.containsKey(id)))
                {
                    HOVERED_STACK = stack;
                    HOVERED_ITEM_UPDATE_COOLDOWN = 5;
                    PacketDistributor.sendToServer(SyncItemPredicatesMessage.fromClient(stack, hoveredSlot.index, equipmentSlot));
                }
            }
        }
    }

    @SubscribeEvent
    public static void tickHoverCooldown(ClientTickEvent.Post event)
    {
        if (HOVERED_ITEM_UPDATE_COOLDOWN > 0)
        {   HOVERED_ITEM_UPDATE_COOLDOWN--;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void addCustomTooltips(RenderTooltipEvent.GatherComponents event)
    {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        var elements = event.getTooltipElements();
        boolean hideTooltips = ConfigSettings.HIDE_TOOLTIPS.get() && !isShiftDown();
        if (stack.isEmpty()) return;

        // Get the index at which the tooltip should be inserted
        int tooltipStartIndex = getTooltipTitleIndex(elements, stack);
        // Get the index of the end of the tooltip, before the debug info (if enabled)
        int tooltipEndIndex = getTooltipEndIndex(elements, stack);

        /*
         Tooltips for soulspring lamp
         */
        if (stack.getItem() instanceof SoulspringLampItem)
        {   if (!isShiftDown())
            {   elements.add(tooltipStartIndex, Either.left(EXPAND_TOOLTIP));
            }
            elements.add(tooltipStartIndex, Either.right(new SoulspringTooltip(stack.getOrDefault(ModItemComponents.SOULSPRING_LAMP_FUEL, 0d))));
        }

        /*
         Tooltip for food temperature
         */
        if (stack.getUseAnimation() == UseAnim.DRINK || stack.getUseAnimation() == UseAnim.EAT)
        {
            // Check if Diet has their own tooltip already
            int dietTooltipSectionIndex = CSMath.getIndexOf(elements, line -> line.left().map(text -> text.getString().equalsIgnoreCase(Component.translatable("tooltip.diet.eaten").getString())).orElse(false));
            int index = dietTooltipSectionIndex != -1
                        ? dietTooltipSectionIndex + 1
                        : tooltipEndIndex;

            Map<Integer, Double> foodTemps = new FastMap<>();
            for (FoodData foodData : ConfigSettings.FOOD_TEMPERATURES.get().get(item))
            {
                if (passesRequirement(foodData))
                {   foodTemps.merge(foodData.duration(), foodData.temperature(), Double::sum);
                }
            }

            for (Map.Entry<Integer, Double> entry : foodTemps.entrySet())
            {
                double temp = entry.getValue();
                int duration = entry.getKey();

                MutableComponent consumeEffects = temp > 0
                                                  ? Component.translatable("tooltip.cold_sweat.temperature_effect", "+" + CSMath.formatDoubleOrInt(temp)).withStyle(HOT) :
                                                  temp == 0
                                                  ? Component.translatable("tooltip.cold_sweat.temperature_effect", "+" + CSMath.formatDoubleOrInt(temp)) :
                                                  Component.translatable("tooltip.cold_sweat.temperature_effect", CSMath.formatDoubleOrInt(temp)).withStyle(COLD);
                // Add a duration to the tooltip if it exists
                if (duration > 0)
                {   consumeEffects.append(" (" + StringUtil.formatTickDuration(duration, 20) + ")");
                }
                // Add the effect to the tooltip
                elements.add(index, Either.left(consumeEffects));
            }

            // Don't add our own section title if one already exists
            if (!foodTemps.isEmpty() && dietTooltipSectionIndex == -1)
            {
                elements.add(tooltipEndIndex, Either.left(Component.translatable("tooltip.cold_sweat.consumed").withStyle(ChatFormatting.GRAY)));
                elements.add(tooltipEndIndex, Either.left(Component.empty()));
            }
        }

        /*
         Tooltips for insulation
         */
        if (!hideTooltips && !stack.isEmpty())
        {
            // Insulating armor
            List<Insulation> armorInsulation = new ArrayList<>();
            List<Insulation> unmetArmorInsulation = new ArrayList<>();
            {
                for (InsulatorData insulator : ConfigSettings.INSULATING_ARMORS.get().get(item))
                {
                    if (!insulator.insulation().isEmpty())
                    {
                        if (passesRequirement(insulator))
                        {   armorInsulation.addAll(insulator.insulation().split());
                        }
                        else unmetArmorInsulation.addAll(insulator.insulation().split());
                    }
                }

                ItemInsulationManager.getInsulationCap(stack).ifPresent(cap ->
                {
                    List<Pair<ItemStack, Multimap<InsulatorData, Insulation>>> insulators = cap.getInsulation();

                    for (int i = 0; i < insulators.size(); i++)
                    {
                        Pair<ItemStack, Multimap<InsulatorData, Insulation>> pair = insulators.get(i);
                        Multimap<InsulatorData, Insulation> insulatorMap = pair.getSecond();

                        for (InsulatorData insulator : insulatorMap.keySet())
                        {
                            if (!insulator.insulation().isEmpty())
                            {
                                if (passesRequirement(insulator))
                                {   armorInsulation.addAll(insulator.insulation().split());
                                }
                                else unmetArmorInsulation.addAll(insulator.insulation().split());
                            }
                        }
                    }
                });

                if (!armorInsulation.isEmpty())
                {   elements.add(tooltipStartIndex, Either.right(new InsulationTooltip(armorInsulation, Insulation.Slot.ARMOR, stack, false)));
                }
                if (!unmetArmorInsulation.isEmpty())
                {   elements.add(tooltipStartIndex, Either.right(new InsulationTooltip(unmetArmorInsulation, Insulation.Slot.ARMOR, stack, true)));
                }
            }

            // Insulation ingredient
            {
                List<Insulation> insulation = new ArrayList<>();
                List<Insulation> unmetInsulation = new ArrayList<>();
                for (InsulatorData insulator : ConfigSettings.INSULATION_ITEMS.get().get(item))
                {
                    if (!insulator.insulation().isEmpty())
                    {
                        if (passesRequirement(insulator))
                        {   insulation.addAll(insulator.insulation().split());
                        }
                        else unmetInsulation.addAll(insulator.insulation().split());
                    }
                }
                if (!insulation.isEmpty() && !insulation.equals(armorInsulation))
                {   elements.add(tooltipStartIndex, Either.right(new InsulationTooltip(insulation, Insulation.Slot.ITEM, stack, false)));
                }
                if (!unmetInsulation.isEmpty() && !unmetInsulation.equals(unmetArmorInsulation))
                {   elements.add(tooltipStartIndex, Either.right(new InsulationTooltip(unmetInsulation, Insulation.Slot.ITEM, stack, true)));
                }
            }

            // Insulating curio
            if (CompatManager.isCuriosLoaded())
            {
                List<Insulation> insulation = new ArrayList<>();
                List<Insulation> unmetInsulation = new ArrayList<>();
                for (InsulatorData insulator : ConfigSettings.INSULATING_CURIOS.get().get(item))
                {
                    if (!insulator.insulation().isEmpty())
                    {
                        if (passesRequirement(insulator))
                        {   insulation.addAll(insulator.insulation().split());
                        }
                        else unmetInsulation.addAll(insulator.insulation().split());
                    }
                }
                if (!insulation.isEmpty())
                {   elements.add(tooltipStartIndex, Either.right(new InsulationTooltip(insulation, Insulation.Slot.CURIO, stack, false)));
                }
                if (!unmetInsulation.isEmpty())
                {   elements.add(tooltipStartIndex, Either.right(new InsulationTooltip(unmetInsulation, Insulation.Slot.CURIO, stack, true)));
                }
            }
        }

        /*
         Custom tooltips for attributes from insulation
         */
        boolean foundUnmetAttribute = false;
        for (int i = 0; i < elements.size(); i++)
        {
            Either<FormattedText, TooltipComponent> element = elements.get(i);
            if (element.left().isPresent() && element.left().get() instanceof Component component)
            {
                if (component.getContents() instanceof TranslatableContents translatableContents
                && translatableContents.getArgs() != null)
                {
                    // Indicates the start of a new section; reset the flag
                    if (translatableContents.getKey().contains("item.modifiers"))
                    {   foundUnmetAttribute = false;
                    }
                    List<Object> args = Arrays.asList(translatableContents.getArgs());
                    boolean strikethrough = args.contains("strikethrough");
                    // At the first unmet attribute modifier for each section, insert the "Unmet Requirements" tooltip line
                    if (strikethrough && !foundUnmetAttribute)
                    {
                        MutableComponent unmetAttributesTooltip = Component.translatable("tooltip.cold_sweat.unmet_attributes").withStyle(ChatFormatting.RED);
                        elements.add(i, Either.right(new InsulationAttributeTooltip(unmetAttributesTooltip, Minecraft.getInstance().font, false)));
                        foundUnmetAttribute = true;
                        i++;
                    }
                    // If the insulation icon should be shown, convert the tooltip into an InsulationAttributeTooltip
                    if (args.contains("show_icon"))
                    {   elements.set(i, Either.right(new InsulationAttributeTooltip(component, Minecraft.getInstance().font, strikethrough)));
                    }
                }
            }
        }
    }

    static int FUEL_FADE_TIMER = 0;

    @SubscribeEvent
    public static void renderSoulLampInsertTooltip(ScreenEvent.Render.Post event)
    {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen)
        {
            if (screen.getSlotUnderMouse() != null && screen.getSlotUnderMouse().getItem().getItem() == ModItems.SOULSPRING_LAMP.value())
            {
                double fuel = screen.getSlotUnderMouse().getItem().getOrDefault(ModItemComponents.SOULSPRING_LAMP_FUEL, 0d);
                ItemStack carriedStack = screen.getMenu().getCarried();

                FuelData itemFuel = ConfigSettings.SOULSPRING_LAMP_FUEL.get().get(carriedStack.getItem())
                                         .stream()
                                         .filter(predicate -> predicate.test(carriedStack))
                                         .findFirst().orElse(null);
                if (!carriedStack.isEmpty()
                && itemFuel != null)
                {
                    double fuelValue = screen.getMenu().getCarried().getCount() * itemFuel.fuel();
                    int slotX = screen.getSlotUnderMouse().x + screen.getGuiLeft();
                    int slotY = screen.getSlotUnderMouse().y + screen.getGuiTop();

                    GuiGraphics graphics = event.getGuiGraphics();
                    PoseStack ps = graphics.pose();
                    if (event.getMouseY() < slotY + 8)
                    {   ps.translate(0, 32, 0);
                    }

                    graphics.renderTooltip(Minecraft.getInstance().font, List.of(Component.literal("       ")), Optional.empty(), slotX - 18, slotY + 1);

                    RenderSystem.defaultBlendFunc();

                    // Render background
                    graphics.blit(ClientSoulspringTooltip.TOOLTIP_LOCATION.get(), slotX - 7, slotY - 11, 401, 0, 0, 30, 8, 30, 34);

                    // Render ghost overlay
                    RenderSystem.enableBlend();
                    RenderSystem.setShaderColor(1f, 1f, 1f, 0.15f + (float) ((Math.sin(FUEL_FADE_TIMER / 5f) + 1f) / 2f) * 0.4f);
                    graphics.blit(ClientSoulspringTooltip.TOOLTIP_LOCATION.get(), slotX - 7, slotY - 11, 401, 0, 8, Math.min(30, (int) ((fuel + fuelValue) / 2.1333f)), 8, 30, 34);
                    RenderSystem.disableBlend();

                    // Render fuel
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1f);
                    graphics.blit(ClientSoulspringTooltip.TOOLTIP_LOCATION.get(), slotX - 7, slotY - 11, 401, 0, 16, (int) (fuel / 2.1333f), 8, 30, 34);
                }
            }
        }
    }

    @SubscribeEvent
    public static void tickSoulLampInsertTooltip(ClientTickEvent.Post event)
    {   FUEL_FADE_TIMER++;
    }
}
