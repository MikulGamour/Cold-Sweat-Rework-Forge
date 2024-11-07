package com.momosoftworks.coldsweat.client.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.client.gui.tooltip.ClientInsulationAttributeTooltip;
import com.momosoftworks.coldsweat.client.gui.tooltip.ClientInsulationTooltip;
import com.momosoftworks.coldsweat.client.gui.tooltip.ClientSoulspringTooltip;
import com.momosoftworks.coldsweat.client.gui.tooltip.Tooltip;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.common.item.SoulspringLampItem;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import com.momosoftworks.coldsweat.config.type.Insulator;
import com.momosoftworks.coldsweat.config.type.PredicateItem;
import com.momosoftworks.coldsweat.compat.CompatManager;
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
import net.minecraft.entity.player.PlayerEntity;
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
        List<String> params = new ArrayList<>(Arrays.asList(sign + CSMath.formatDoubleOrInt(CSMath.round(value, 2)) + percent));
        if (forTooltip)
        {   params.add("show_icon");
        }
        if (strikethrough)
        {   params.add("strikethrough");
        }
        return new TranslationTextComponent(String.format("attribute.cold_sweat.modifier.%s.%s", operationString, attributeName),
                                         params.toArray())
                .withStyle(color);
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

        PlayerEntity player = event.getPlayer();
        if (player == null) return;

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
            for (PredicateItem predicateItem : ConfigSettings.FOOD_TEMPERATURES.get().get(item))
            {
                if (predicateItem.test(player, stack))
                {
                    double temp = predicateItem.value;
                    int duration = predicateItem.extraData.contains("duration")
                                   ? predicateItem.extraData.getInt("duration")
                                   : 0;
                    foodTemps.merge(duration, temp, Double::sum);
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
            List<Insulator> validInsulations = new ArrayList<>();

            // Insulation ingredient
            {
                List<Insulation> insulation = new ArrayList<>();
                List<Insulation> unmetInsulation = new ArrayList<>();
                for (Insulator insulator : ConfigSettings.INSULATION_ITEMS.get().get(item))
                {
                    if (!insulator.insulation.isEmpty())
                    {
                        if (insulator.test(player, stack))
                        {   insulation.addAll(insulator.insulation.split());
                        }
                        else unmetInsulation.addAll(insulator.insulation.split());
                        validInsulations.add(insulator);
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
                for (Insulator insulator : ConfigSettings.INSULATING_CURIOS.get().get(item))
                {
                    if (!insulator.insulation.isEmpty())
                    {
                        if (insulator.test(player, stack))
                        {   insulation.addAll(insulator.insulation.split());
                        }
                        else unmetInsulation.addAll(insulator.insulation.split());
                        validInsulations.add(insulator);
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
            if (ItemInsulationManager.isInsulatable(stack))
            {
                for (Insulator insulator : ConfigSettings.INSULATING_ARMORS.get().get(item))
                {
                    if (!validInsulations.contains(insulator))
                    {
                        if (insulator.test(player, stack))
                        {   insulation.addAll(insulator.insulation.split());
                        }
                        else unmetInsulation.addAll(insulator.insulation.split());
                    }
                }
            }

            ItemInsulationManager.getInsulationCap(stack).ifPresent(cap ->
            {
                cap.deserializeNBT(stack.getOrCreateTag());

                // Create the list of insulation pairs from NBT
                insulation.addAll(ItemInsulationManager.getAllEffectiveInsulation(stack, player));
            });

            if (!insulation.isEmpty())
            {   addTooltip(tooltipStartIndex, new ClientInsulationTooltip(insulation, Insulation.Slot.ARMOR, stack, false), elements);
            }
            if (!unmetInsulation.isEmpty())
            {   addTooltip(tooltipStartIndex, new ClientInsulationTooltip(unmetInsulation, Insulation.Slot.ARMOR, stack, true), elements);
            }
        }

        /*
         Custom tooltips for attributes from insulation
         */
        for (int i = 0; i < elements.size(); i++)
        {
            ITextComponent element = elements.get(i);
            if (element instanceof TranslationTextComponent)
            {
                TranslationTextComponent component = ((TranslationTextComponent) element);
                if (component.getArgs() != null)
                {
                    List<Object> args = Arrays.asList(component.getArgs());
                    if (args.contains("show_icon"))
                    {
                        boolean strikethrough = args.contains("strikethrough");
                        setTooltip(i, new ClientInsulationAttributeTooltip(component, Minecraft.getInstance().font, strikethrough), elements);
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

                PredicateItem itemFuel = ConfigSettings.SOULSPRING_LAMP_FUEL.get().get(carriedStack.getItem())
                                         .stream()
                                         .filter(predicate -> predicate.test(Minecraft.getInstance().player, carriedStack))
                                         .findFirst().orElse(null);
                if (!carriedStack.isEmpty()
                && itemFuel != null)
                {
                    double fuelValue = carriedStack.getCount() * itemFuel.value;
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
