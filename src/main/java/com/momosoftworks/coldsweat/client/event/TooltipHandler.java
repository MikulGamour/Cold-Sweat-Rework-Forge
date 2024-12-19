package com.momosoftworks.coldsweat.client.event;

import com.google.common.collect.Multimap;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.client.gui.tooltip.ClientInsulationAttributeTooltip;
import com.momosoftworks.coldsweat.client.gui.tooltip.ClientInsulationTooltip;
import com.momosoftworks.coldsweat.client.gui.tooltip.ClientSoulspringTooltip;
import com.momosoftworks.coldsweat.client.gui.tooltip.Tooltip;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.common.item.SoulspringLampItem;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.core.network.message.SyncItemPredicatesMessage;
import com.momosoftworks.coldsweat.data.codec.configuration.FoodData;
import com.momosoftworks.coldsweat.data.codec.configuration.FuelData;
import com.momosoftworks.coldsweat.data.codec.configuration.InsulatorData;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import com.momosoftworks.coldsweat.util.entity.EntityHelper;
import com.momosoftworks.coldsweat.util.exceptions.RegistryFailureException;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.FastMap;
import com.momosoftworks.coldsweat.util.registries.ModAttributes;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import com.momosoftworks.coldsweat.util.serialization.ListBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class TooltipHandler
{
    public static final Style COLD = Style.EMPTY.withColor(Color.fromRgb(3767039));
    public static final Style HOT = Style.EMPTY.withColor(Color.fromRgb(16736574));
    public static final IFormattableTextComponent EXPAND_TOOLTIP = new StringTextComponent("?").withStyle(Style.EMPTY.withColor(TextFormatting.BLUE).withUnderlined(true))
                                           .append(new StringTextComponent(" 'Shift'").withStyle(Style.EMPTY.withColor(TextFormatting.GRAY).withUnderlined(false)));

    private static int TOOLTIP_BACKGROUND_COLOR = 0;

    private static int HOVERED_ITEM_UPDATE_COOLDOWN = 0;
    private static ItemStack HOVERED_STACK = ItemStack.EMPTY;
    public static FastMap<UUID, Boolean> HOVERED_STACK_PREDICATES = new FastMap<>();

    public static <T extends ConfigData> boolean passesRequirement(T element)
    {   return HOVERED_STACK_PREDICATES.getOrDefault(element.getId(), true);
    }

    public static boolean isShiftDown()
    {   return Screen.hasShiftDown() || ConfigSettings.EXPAND_TOOLTIPS.get();
    }

    public static int getTooltipTitleIndex(List<ITextComponent> tooltip, ItemStack stack)
    {
        if (tooltip.isEmpty()) return 0;

        int tooltipStartIndex;
        String hoverName = stack.getHoverName().getString();

        for (tooltipStartIndex = 0; tooltipStartIndex < tooltip.size(); tooltipStartIndex++)
        {
            if (tooltip.get(tooltipStartIndex).getString().trim().equals(hoverName))
            {   tooltipStartIndex++;
                break;
            }
        }
        tooltipStartIndex = CSMath.clamp(tooltipStartIndex, 0, tooltip.size());
        return tooltipStartIndex;
    }

    public static int getTooltipEndIndex(List<ITextComponent> tooltip, ItemStack stack)
    {
        int tooltipEndIndex = tooltip.size();
        if (Minecraft.getInstance().options.advancedItemTooltips)
        {
            for (--tooltipEndIndex; tooltipEndIndex > 0; tooltipEndIndex--)
            {
                if (tooltip.get(tooltipEndIndex).getString().equals(stack.getItem().getRegistryName().toString()))
                {   break;
                }
            }
        }
        tooltipEndIndex = CSMath.clamp(tooltipEndIndex, 0, tooltip.size());
        return tooltipEndIndex;
    }

    public static void addModifierTooltipLines(List<ITextComponent> tooltip, AttributeModifierMap map, boolean strikethrough)
    {
        map.getMap().asMap().forEach((attribute, modifiers) ->
        {
            for (AttributeModifier.Operation operation : AttributeModifier.Operation.values())
            {
                double value = 0;
                for (AttributeModifier modifier : modifiers.stream().filter(mod -> mod.getOperation() == operation).collect(Collectors.toList()))
                {   value += modifier.getAmount();
                }
                if (value != 0)
                {   tooltip.add(getFormattedAttributeModifier(attribute, value, operation, false, strikethrough));
                }
            }
        });
    }

    public static IFormattableTextComponent getFormattedAttributeModifier(Attribute attribute, double amount, AttributeModifier.Operation operation,
                                                                 boolean forTooltip, boolean strikethrough)
    {
        if (attribute == null) return new StringTextComponent("");
        double value = amount;
        String attributeName = attribute.getDescriptionId().replace("attribute.", "");

        if (operation == AttributeModifier.Operation.ADDITION
                && (attribute == ModAttributes.FREEZING_POINT
                || attribute == ModAttributes.BURNING_POINT
                || attribute == ModAttributes.WORLD_TEMPERATURE
                || attribute == ModAttributes.BASE_BODY_TEMPERATURE))
        {
            value = Temperature.convert(value, Temperature.Units.MC, ConfigSettings.CELSIUS.get() ? Temperature.Units.C : Temperature.Units.F, false);
        }
        String operationString = operation == AttributeModifier.Operation.ADDITION ? "add" : "multiply";
        TextFormatting color;
        String sign;
        if (value >= 0)
        {
            color = TextFormatting.BLUE;
            sign = "+";
        }
        else
        {   color = TextFormatting.RED;
            sign = "";
        }
        String percent;
        if (operation != AttributeModifier.Operation.ADDITION
                || attribute == ModAttributes.HEAT_RESISTANCE
                || attribute == ModAttributes.COLD_RESISTANCE
                || attribute == ModAttributes.HEAT_DAMPENING
                || attribute == ModAttributes.COLD_DAMPENING)
        {   percent = "%";
            value *= 100;
        }
        else
        {   percent = "";
        }
        List<Object> params = new ArrayList<>(Arrays.asList(sign + CSMath.formatDoubleOrInt(CSMath.round(value, 2)) + percent));
        IFormattableTextComponent component;
        if (EntityTempManager.isTemperatureAttribute(attribute))
        {   component = new TranslationTextComponent(String.format("attribute.cold_sweat.modifier.%s.%s", operationString, attributeName), params.toArray());
        }
        else
        {
            component = getFormattedVanillaAttributeModifier(attribute, amount, operation);
            Object[] contents = ((TranslationTextComponent) component).getArgs();
            params.addAll(0, Arrays.asList(contents));
            component = setComponentContents(getFormattedVanillaAttributeModifier(attribute, amount, operation), contents);
        }
        component = component.withStyle(color);
        component = addTooltipFlags(component, forTooltip, strikethrough);
        return component;
    }

    public static IFormattableTextComponent getFormattedVanillaAttributeModifier(Attribute attribute, double amount, AttributeModifier.Operation operation)
    {
        double adjustedAmount;
        if (operation == AttributeModifier.Operation.ADDITION)
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
            return new TranslationTextComponent("attribute.modifier.plus." + operation.toValue(), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(adjustedAmount),
                                          new TranslationTextComponent(attribute.getDescriptionId())).withStyle(TextFormatting.BLUE);
        }
        else
        {   adjustedAmount *= -1;
            return new TranslationTextComponent("attribute.modifier.take." + operation.toValue(), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(adjustedAmount),
                                            new TranslationTextComponent(attribute.getDescriptionId())).withStyle(TextFormatting.RED);
        }
    }

    public static IFormattableTextComponent setComponentContents(IFormattableTextComponent component, Object[] newContents)
    {
        if (component instanceof TranslationTextComponent)
        {
            TranslationTextComponent translatable = (TranslationTextComponent) component;
            IFormattableTextComponent newComponent = new TranslationTextComponent(translatable.getKey(), newContents).setStyle(component.getStyle());
            component.getSiblings().forEach(newComponent::append);
            return newComponent;
        }
        return component;
    }

    public static IFormattableTextComponent addTooltipFlags(IFormattableTextComponent component, boolean showIcon, boolean strikethrough)
    {
        if (component instanceof TranslationTextComponent)
        {
            TranslationTextComponent translatable = (TranslationTextComponent) component;
            List<Object> params = new ArrayList<>(Arrays.asList(translatable.getArgs()));
            if (showIcon)
            {   params.add("show_icon");
            }
            if (strikethrough)
            {   params.add("strikethrough");
            }
            IFormattableTextComponent newComponent = setComponentContents(component, params.toArray());
            if (strikethrough)
            {   newComponent.setStyle(Style.EMPTY.withColor(Color.fromRgb(7561572)));
            }
            return newComponent;
        }
        return component;
    }

    @SubscribeEvent
    public static void trackTooltipColor(RenderTooltipEvent.Color event)
    {   TOOLTIP_BACKGROUND_COLOR = event.getBackground();
    }

    private static final List<Object> TOOLTIP_INSERTIONS = new ArrayList<>();

    private static void addTooltip(int index, Tooltip tooltip, List<ITextComponent> elements)
    {
        TOOLTIP_INSERTIONS.add(index, tooltip);
        elements.add(index, getTooltipCode(tooltip.getClass()));
    }

    private static void setTooltip(int index, Tooltip tooltip, List<ITextComponent> elements)
    {
        TOOLTIP_INSERTIONS.set(index, tooltip);
        elements.set(index, getTooltipCode(tooltip.getClass()));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void updateHoveredItem(GuiScreenEvent.DrawScreenEvent.Pre event)
    {
        if (event.getGui() instanceof ContainerScreen<?>)
        {
            ContainerScreen menu = (ContainerScreen<?>) event.getGui();
            Slot hoveredSlot = menu.getSlotUnderMouse();
            if (hoveredSlot == null) return;

            ItemStack stack = hoveredSlot.getItem();

            EquipmentSlotType equipmentSlot = EntityHelper.getEquipmentSlot(hoveredSlot.index);
            if (!HOVERED_STACK.equals(stack))
            {
                if (stack.isEmpty())
                {   HOVERED_STACK = stack;
                    return;
                }
                if (HOVERED_ITEM_UPDATE_COOLDOWN <= 0
                || ItemInsulationManager.getInsulatorsForStack(stack).stream().map(InsulatorData::getId).anyMatch(id -> !HOVERED_STACK_PREDICATES.containsKey(id)))
                {
                    HOVERED_STACK = stack;
                    HOVERED_ITEM_UPDATE_COOLDOWN = 5;
                    ColdSweatPacketHandler.INSTANCE.sendToServer(SyncItemPredicatesMessage.fromClient(stack, hoveredSlot.index, equipmentSlot));
                }
            }
        }
    }

    @SubscribeEvent
    public static void tickHoverCooldown(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END && HOVERED_ITEM_UPDATE_COOLDOWN > 0)
        {   HOVERED_ITEM_UPDATE_COOLDOWN--;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void addSimpleTooltips(ItemTooltipEvent event)
    {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        List<ITextComponent> elements = event.getToolTip();
        boolean hideTooltips = ConfigSettings.HIDE_TOOLTIPS.get() && !isShiftDown();
        if (stack.isEmpty()) return;

        TOOLTIP_INSERTIONS.clear();
        TOOLTIP_INSERTIONS.addAll(ListBuilder.begin().fill(elements.size(), Object::new).build());

        // Get the index at which the tooltip should be inserted
        int tooltipStartIndex = getTooltipTitleIndex(elements, stack);
        // Get the index of the end of the tooltip, before the debug info (if enabled)
        int tooltipEndIndex = getTooltipEndIndex(elements, stack);

        /*
         Tooltips for soulspring lamp
         */
        if (stack.getItem() instanceof SoulspringLampItem)
        {   if (!isShiftDown())
            {   elements.add(tooltipStartIndex, EXPAND_TOOLTIP);
            }
            else for (int i = 0; i < CSMath.ceil(ConfigSettings.SOULSPRING_LAMP_FUEL.get().size() / 6d) + 1; i++)
            {   elements.add(tooltipStartIndex, new StringTextComponent(""));
            }
            addTooltip(tooltipStartIndex, new ClientSoulspringTooltip(stack.getOrCreateTag().getDouble("Fuel")), elements);
        }

        /*
         Tooltip for food temperature
         */
        if (stack.getUseAnimation() == UseAction.DRINK || stack.getUseAnimation() == UseAction.EAT)
        {
            // Check if Diet has their own tooltip already
            int dietTooltipSectionIndex = CSMath.getIndexOf(elements, line -> line.getString().equalsIgnoreCase(new TranslationTextComponent("tooltip.diet.eaten").getString()));
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

                IFormattableTextComponent consumeEffects = temp > 0
                                                  ? new TranslationTextComponent("tooltip.cold_sweat.temperature_effect", "+" + CSMath.formatDoubleOrInt(temp)).withStyle(HOT) :
                                                  temp == 0
                                                  ? new TranslationTextComponent("tooltip.cold_sweat.temperature_effect", "+" + CSMath.formatDoubleOrInt(temp)) :
                                                  new TranslationTextComponent("tooltip.cold_sweat.temperature_effect", CSMath.formatDoubleOrInt(temp)).withStyle(COLD);
                // Add a duration to the tooltip if it exists
                if (duration > 0)
                {   consumeEffects.append(" (" + StringUtils.formatTickDuration(duration) + ")");
                }
                // Add the effect to the tooltip
                elements.add(index, consumeEffects);
            }

            // Don't add our own section title if one already exists
            if (!foodTemps.isEmpty() && dietTooltipSectionIndex == -1)
            {
                elements.add(tooltipEndIndex, new TranslationTextComponent("tooltip.cold_sweat.consumed").withStyle(TextFormatting.GRAY));
                elements.add(tooltipEndIndex, new StringTextComponent(""));
            }
        }

        /*
         Tooltips for insulation
         */
        if (!hideTooltips && !stack.isEmpty())
        {
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
                if (!insulation.isEmpty())
                {   addTooltip(tooltipStartIndex, new ClientInsulationTooltip(insulation, Insulation.Slot.ITEM, stack, false), elements);
                }
                if (!unmetInsulation.isEmpty())
                {   addTooltip(tooltipStartIndex, new ClientInsulationTooltip(unmetInsulation, Insulation.Slot.ITEM, stack, true), elements);
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
                {   addTooltip(tooltipStartIndex, new ClientInsulationTooltip(insulation, Insulation.Slot.CURIO, stack, false), elements);
                }
                if (!unmetInsulation.isEmpty())
                {   addTooltip(tooltipStartIndex, new ClientInsulationTooltip(unmetInsulation, Insulation.Slot.CURIO, stack, true), elements);
                }
            }

            List<Insulation> insulation = new ArrayList<>();
            List<Insulation> unmetInsulation = new ArrayList<>();

            // Insulating armor
            for (InsulatorData insulator : ConfigSettings.INSULATING_ARMORS.get().get(item))
            {
                if (!insulator.insulation().isEmpty())
                {
                    if (passesRequirement(insulator))
                    {   insulation.addAll(insulator.insulation().split());
                    }
                    else unmetInsulation.addAll(insulator.insulation().split());
                }
            }

            ItemInsulationManager.getInsulationCap(stack).ifPresent(cap ->
            {
                cap.deserializeNBT(stack.getOrCreateTag());

                // Iterate over both the insulation items and the checks for each item
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
                            {   insulation.addAll(insulator.insulation().split());
                            }
                            else unmetInsulation.addAll(insulator.insulation().split());
                        }
                    }
                }

                if (!insulation.isEmpty())
                {   addTooltip(tooltipStartIndex, new ClientInsulationTooltip(insulation, Insulation.Slot.ARMOR, stack, false), elements);
                }
                if (!unmetInsulation.isEmpty())
                {   addTooltip(tooltipStartIndex, new ClientInsulationTooltip(unmetInsulation, Insulation.Slot.ARMOR, stack, true), elements);
                }
            });
        }

        /*
         Custom tooltips for attributes from insulation
         */
        boolean foundUnmetAttribute = false;
        for (int i = 0; i < elements.size(); i++)
        {
            ITextComponent element = elements.get(i);
            if (element instanceof TranslationTextComponent)
            {
                TranslationTextComponent component = ((TranslationTextComponent) element);
                if (component.getArgs() != null)
                {
                    // Indicates the start of a new section; reset the flag
                    if (component.getKey().contains("item.modifiers"))
                    {   foundUnmetAttribute = false;
                    }
                    List<Object> args = Arrays.asList(component.getArgs());
                    boolean strikethrough = args.contains("strikethrough");
                    // At the first unmet attribute modifier for each section, insert the "Unmet Requirements" tooltip line
                    if (strikethrough && !foundUnmetAttribute)
                    {
                        IFormattableTextComponent unmetAttributesTooltip = new TranslationTextComponent("tooltip.cold_sweat.unmet_attributes").withStyle(TextFormatting.RED);
                        addTooltip(i, new ClientInsulationAttributeTooltip(unmetAttributesTooltip, Minecraft.getInstance().font, false), elements);
                        foundUnmetAttribute = true;
                        i++;
                    }
                    // If the insulation icon should be shown, convert the tooltip into an InsulationAttributeTooltip
                    if (args.contains("show_icon"))
                    {   setTooltip(i, new ClientInsulationAttributeTooltip(component, Minecraft.getInstance().font, strikethrough), elements);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void renderTooltips(RenderTooltipEvent.PostText event)
    {
        if (TOOLTIP_INSERTIONS.isEmpty()) return;

        // Find the empty line that this tooltip should fill
        int y = event.getY() - 10;
        List<? extends ITextProperties> tooltipLines = event.getLines();
        for (int i = 0; i < Math.min(tooltipLines.size(), TOOLTIP_INSERTIONS.size()); i++)
        {
            y += 10;
            ITextProperties tooltipLine = tooltipLines.get(i);
            String line = tooltipLine.getString();
            if (line.isEmpty()) continue;

            Object nextInsertion = TOOLTIP_INSERTIONS.get(i);
            if (!(nextInsertion instanceof Tooltip)) continue;
            Tooltip nextTooltip = (Tooltip) nextInsertion;
            String tooltipID = TOOLTIPS.get(nextTooltip.getClass());

            if (!line.equals(tooltipID))
            {   continue;
            }

            nextTooltip.renderImage(Minecraft.getInstance().font, event.getX(), y, event.getMatrixStack(), Minecraft.getInstance().getItemRenderer(), 0);
            nextTooltip.renderText(Minecraft.getInstance().font, event.getX(), y, event.getMatrixStack(), Minecraft.getInstance().getItemRenderer(), 0);
        }
    }

    static int FUEL_FADE_TIMER = 0;

    @SubscribeEvent
    public static void renderSoulLampInsertTooltip(GuiScreenEvent.DrawScreenEvent.Post event)
    {
        if (event.getGui() instanceof ContainerScreen)
        {
            ContainerScreen<?> inventoryScreen = (ContainerScreen<?>) event.getGui();
            PlayerEntity player = Minecraft.getInstance().player;

            if (player != null && inventoryScreen.getSlotUnderMouse() != null
            && inventoryScreen.getSlotUnderMouse().getItem().getItem() == ModItems.SOULSPRING_LAMP)
            {
                double fuel = inventoryScreen.getSlotUnderMouse().getItem().getOrCreateTag().getDouble("Fuel");
                ItemStack carriedStack = player.inventory.getCarried();

                FuelData itemFuel = ConfigSettings.SOULSPRING_LAMP_FUEL.get().get(carriedStack.getItem())
                                         .stream()
                                         .filter(predicate -> predicate.test(carriedStack))
                                         .findFirst().orElse(null);
                if (!carriedStack.isEmpty()
                && itemFuel != null)
                {
                    double fuelValue = carriedStack.getCount() * itemFuel.fuel();
                    int slotX = inventoryScreen.getSlotUnderMouse().x + ((ContainerScreen<?>) event.getGui()).getGuiLeft();
                    int slotY = inventoryScreen.getSlotUnderMouse().y + ((ContainerScreen<?>) event.getGui()).getGuiTop();

                    MatrixStack ms = event.getMatrixStack();

                    // If the mouse is above the slot, move the box to the bottom
                    if (event.getMouseY() < slotY + 8)
                        ms.translate(0, 32, 0);

                    event.getGui().renderTooltip(ms, new StringTextComponent("       "), slotX - 18, slotY);

                    RenderSystem.defaultBlendFunc();

                    // Render background
                    Minecraft.getInstance().textureManager.bind(ClientSoulspringTooltip.TOOLTIP_LOCATION.get());
                    AbstractGui.blit(ms, slotX - 7, slotY - 12, 401, 0, 0, 30, 8, 34, 30);

                    // Render ghost overlay
                    RenderSystem.enableBlend();
                    RenderSystem.color4f(1f, 1f, 1f, 0.15f + (float) ((Math.sin(FUEL_FADE_TIMER / 5f) + 1f) / 2f) * 0.4f);
                    AbstractGui.blit(ms, slotX - 7, slotY - 12, 401, 0, 8, Math.min(30, (int) ((fuel + fuelValue) / 2.1333f)), 8, 34, 30);
                    RenderSystem.disableBlend();

                    // Render current fuel
                    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1f);
                    AbstractGui.blit(ms, slotX - 7, slotY - 12, 401, 0, 16, (int) (fuel / 2.1333f), 8, 34, 30);
                }
            }
        }
    }

    @SubscribeEvent
    public static void tickSoulLampInsertTooltip(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {   FUEL_FADE_TIMER++;
        }
    }

    private static final Map<Class<? extends Tooltip>, String> TOOLTIPS = new HashMap<>();
    private static int TOOLTIP_REGISTRY_SIZE = 0;

    private static void registerTooltip(Class<? extends Tooltip> tooltip)
    {
        if (!TOOLTIPS.containsKey(tooltip))
        {
            if (TOOLTIP_REGISTRY_SIZE >= 63)
            {   throw new RuntimeException("Too many tooltips registered!");
            }
            String code = Integer.toBinaryString(TOOLTIP_REGISTRY_SIZE);
            while (code.length() < 5)
            {   code = "0" + code;
            }
            code = code.replace("0", "-");
            code = code.replace("1", "+");
            TOOLTIPS.put(tooltip, code);
            TOOLTIP_REGISTRY_SIZE++;
        }
        else
        {
            throw new RegistryFailureException(tooltip, "Tooltips", "Tooltip already registered!", null);
        }
    }

    private static ITextComponent getTooltipCode(Class<? extends Tooltip> tooltip)
    {
        return new StringTextComponent(TOOLTIPS.get(tooltip)).withStyle(Style.EMPTY.withColor(Color.fromRgb(TOOLTIP_BACKGROUND_COLOR)));
    }

    static
    {
        registerTooltip(ClientInsulationTooltip.class);
        registerTooltip(ClientSoulspringTooltip.class);
        registerTooltip(ClientInsulationAttributeTooltip.class);
    }
}
