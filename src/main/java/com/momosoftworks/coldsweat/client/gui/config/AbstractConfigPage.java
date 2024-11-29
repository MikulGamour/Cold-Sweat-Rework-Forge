package com.momosoftworks.coldsweat.client.gui.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(Dist.CLIENT)
public abstract class AbstractConfigPage extends Screen
{
    // Count how many ticks the mouse has been still for
    static int MOUSE_STILL_TIMER = 0;
    static int TOOLTIP_DELAY = 5;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {   MOUSE_STILL_TIMER++;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY)
    {   MOUSE_STILL_TIMER = 0;
        super.mouseMoved(mouseX, mouseY);
    }

    private final Screen parentScreen;

    public Map<String, Pair<List<GuiEventListener>, Boolean>> widgetBatches = new HashMap<>();
    public Map<String, List<Component>> tooltips = new HashMap<>();

    protected int rightSideLength = 0;
    protected int leftSideLength = 0;

    private static final int TITLE_HEIGHT = ConfigScreen.TITLE_HEIGHT;
    private static final int BOTTOM_BUTTON_HEIGHT_OFFSET = ConfigScreen.BOTTOM_BUTTON_HEIGHT_OFFSET;
    private static final int BOTTOM_BUTTON_WIDTH = ConfigScreen.BOTTOM_BUTTON_WIDTH;
    public static Minecraft MINECRAFT = Minecraft.getInstance();

    static ResourceLocation TEXTURE = new ResourceLocation("cold_sweat:textures/gui/screen/config_gui.png");

    ImageButton nextNavButton;
    ImageButton prevNavButton;

    public abstract MutableComponent sectionOneTitle();

    @Nullable
    public abstract MutableComponent sectionTwoTitle();

    public AbstractConfigPage(Screen parentScreen)
    {   super(new TranslatableComponent("cold_sweat.config.title"));
        this.parentScreen = parentScreen;
    }

    /**
     * Adds an empty block to the list on the given side. One unit is the height of a button.
     */
    protected void addEmptySpace(Side side, double height)
    {
        if (side == Side.LEFT)
        {   this.leftSideLength += (int) (ConfigScreen.OPTION_SIZE * height);
        }
        else
        {   this.rightSideLength += (int) (ConfigScreen.OPTION_SIZE * height);
        }
    }

    /**
     * Adds a label with plain text to the list on the given side.
     * @param id The internal id of the label. This widget can be accessed by this id.
     */
    protected void addLabel(String id, Side side, String text, int color)
    {
        int labelX = side == Side.LEFT ? this.width / 2 - 185 : this.width / 2 + 51;
        int labelY = this.height / 4 + (side == Side.LEFT ? leftSideLength : rightSideLength);
        ConfigLabel label = new ConfigLabel(id, text, labelX, labelY, color);

        this.addWidgetBatch(id, List.of(label), true);

        if (side == Side.LEFT)
        {   this.leftSideLength += font.lineHeight + 4;
        }
        else
        {   this.rightSideLength += font.lineHeight + 4;
        }
    }

    protected void addLabel(String id, Side side, String text)
    {   this.addLabel(id, side, text, 16777215);
    }

    /**
     * Adds a button to the list on the given side.
     * @param id The internal id of the button. This widget can be accessed by this id.
     * @param dynamicLabel A supplier that returns the label of the button. The label is updated when the button is pressed.
     * @param onClick The action to perform when the button is pressed.
     * @param requireOP Whether the button should be disabled if the player is not OP.
     * @param setsCustomDifficulty Sets Cold Sweat's difficulty to custom when pressed, if true.
     * @param clientside Whether the button is clientside only (renders the clientside icon).
     * @param tooltip The tooltip of the button when hovered.
     */
    protected void addButton(String id, Side side, Supplier<Component> dynamicLabel, Consumer<Button> onClick,
                             boolean requireOP, boolean setsCustomDifficulty, boolean clientside,
                             Component... tooltip)
    {
        Component label = dynamicLabel.get();

        boolean shouldBeActive = !requireOP || MINECRAFT.player == null || MINECRAFT.player.hasPermissions(2);
        int buttonX = this.width / 2;
        int xOffset = side == Side.LEFT ? -179 : 56;
        int buttonY = this.height / 4 - 8 + (side == Side.LEFT ? leftSideLength : rightSideLength);
        // Extend the button if the text is too long
        int buttonWidth = 152 + Math.max(0, font.width(label) - 140);

        // Make the button
        Button button = new ConfigButton(buttonX + xOffset, buttonY, buttonWidth, 20, label, button1 ->
        {
            onClick.accept(button1);
            button1.setMessage(dynamicLabel.get());
        })
        {
            @Override
            public boolean setsCustomDifficulty()
            {   return setsCustomDifficulty;
            }
        };
        button.active = shouldBeActive;

        // Add the clientside indicator
        if (clientside)
        {   this.addRenderableOnly(new ConfigImage(TEXTURE, this.width / 2 + xOffset - 18, buttonY + 3, 16, 15, 0, 144));
        }

        List<Component> tooltipList = new ArrayList<>(Arrays.asList(tooltip));
        // Add the client disclaimer if the setting is marked clientside
        if (clientside)
        {   tooltipList.add(new TranslatableComponent("cold_sweat.config.clientside_warning").withStyle(ChatFormatting.DARK_GRAY));
        }
        // Assign the tooltip
        this.setTooltip(id, tooltipList);

        this.addWidgetBatch(id, List.of(button), shouldBeActive);

        // Mark this space as used
        if (side == Side.LEFT)
            this.leftSideLength += ConfigScreen.OPTION_SIZE;
        else
            this.rightSideLength += ConfigScreen.OPTION_SIZE;
    }

    /**
     * Adds an input that accepts decimal numbers to the list on the given side.
     * @param id The internal id of the input. This widget can be accessed by this id.
     * @param label The label text of the input.
     * @param onEdited The action to perform when the input is changed.
     * @param onInit The action to perform when the input is initialized (when the screen is created).
     * @param requireOP Whether the input should be disabled if the player is not OP.
     * @param setsCustomDifficulty Sets Cold Sweat's difficulty to custom when edited, if true.
     * @param clientside Whether the input is clientside only.
     * @param tooltip The tooltip of the input when hovered.
     */
    protected void addDecimalInput(String id, Side side, Component label, Consumer<Double> onEdited, Consumer<EditBox> onInit,
                                   boolean requireOP, boolean setsCustomDifficulty, boolean clientside,
                                   Component... tooltip)
    {
        boolean shouldBeActive = !requireOP || MINECRAFT.player == null || MINECRAFT.player.hasPermissions(2);
        int xOffset = side == Side.LEFT ? -82 : 151;
        int yOffset = (side == Side.LEFT ? this.leftSideLength : this.rightSideLength) - 2;
        int labelOffset = font.width(label.getString()) > 90 ?
                          font.width(label.getString()) - 84 : 0;

        // Make the input
        EditBox textBox = new EditBox(this.font, this.width / 2 + xOffset + labelOffset, this.height / 4 - 6 + yOffset, 51, 22, new TextComponent(""))
        {
            public void onEdit()
            {
                CSMath.tryCatch(() ->
                {
                    onEdited.accept(Double.parseDouble(this.getValue()));
                    if (setsCustomDifficulty)
                    {   ConfigSettings.DIFFICULTY.set(ConfigSettings.Difficulty.CUSTOM);
                    }
                });
            }

            @Override
            public void insertText(String text)
            {
                super.insertText(text);
                this.onEdit();
            }
            @Override
            public void deleteWords(int i)
            {
                super.deleteWords(i);
                this.onEdit();
            }
            @Override
            public void deleteChars(int i)
            {
                super.deleteChars(i);
                this.onEdit();
            }
        };

        // Disable the input if the player is not OP
        textBox.setEditable(shouldBeActive);

        // Set the initial value
        onInit.accept(textBox);

        // Round the input to 2 decimal places
        textBox.setValue(ConfigScreen.TWO_PLACES.format(Double.parseDouble(textBox.getValue())));

        // Make the label
        ConfigLabel configLabel = new ConfigLabel(id, label.getString(), this.width / 2 + xOffset - 95, this.height / 4 + yOffset, shouldBeActive ? 16777215 : 8421504);
        // Add the clientside indicator
        if (clientside)
        {   this.addRenderableOnly(new ConfigImage(TEXTURE, this.width / 2 + xOffset - 115, this.height / 4 - 4 + yOffset, 16, 15, 0, 144));
        }

        List<Component> tooltipList = new ArrayList<>(Arrays.asList(tooltip));
        // Add the client disclaimer if the setting is marked clientside
        if (clientside)
        {   tooltipList.add(new TranslatableComponent("cold_sweat.config.clientside_warning").withStyle(ChatFormatting.DARK_GRAY));
        }
        // Assign the tooltip
        this.setTooltip(id, tooltipList);

        // Add the widget
        this.addWidgetBatch(id, List.of(textBox, configLabel), shouldBeActive);

        // Mark this space as used
        if (side == Side.LEFT)
            this.leftSideLength += ConfigScreen.OPTION_SIZE * 1.2;
        else
            this.rightSideLength += ConfigScreen.OPTION_SIZE * 1.2;
    }

    /**
     * Adds a 4-way direction button panel with a reset button to the list on the given side.
     * @param id The internal id of the panel. This widget can be accessed by this id.
     * @param label The label text of the panel.
     * @param leftRightPressed The action to perform when the left or right button is pressed. 1 for right, -1 for left.
     * @param upDownPressed The action to perform when the up or down button is pressed. -1 for up, 1 for down.
     * @param reset The action to perform when the reset button is pressed.
     * @param requireOP Whether the panel should be disabled if the player is not OP.
     * @param setsCustomDifficulty Sets Cold Sweat's difficulty to custom when edited, if true.
     * @param clientside Whether the panel is clientside only (renders the clientside icon).
     * @param tooltip The tooltip of the panel when hovered.
     */
    protected void addDirectionPanel(String id, Side side, Component label, Consumer<Integer> leftRightPressed, Consumer<Integer> upDownPressed, Runnable reset, Supplier<Boolean> hide,
                                     boolean requireOP, boolean setsCustomDifficulty, boolean clientside, boolean canHide, Component... tooltip)
    {
        int xOffset = side == Side.LEFT ? -97 : 136;
        int yOffset = side == Side.LEFT ? this.leftSideLength : this.rightSideLength;

        boolean shouldBeActive = !requireOP || MINECRAFT.player == null || MINECRAFT.player.hasPermissions(2);

        int labelWidth = font.width(label.getString());
        int labelOffset = labelWidth > 84
                        ? labelWidth - 84
                        : 0;

        List<GuiEventListener> widgetBatch = new ArrayList<>();

        // Left button
        ImageButton leftButton = new ImageButton(this.width / 2 + xOffset + labelOffset, this.height / 4 - 8 + yOffset, 14, 20, 0, 0, 20, TEXTURE, button ->
        {
            leftRightPressed.accept(-1);
            if (setsCustomDifficulty)
            {   ConfigSettings.DIFFICULTY.set(ConfigSettings.Difficulty.CUSTOM);
            }
        });
        leftButton.active = shouldBeActive;
        widgetBatch.add(leftButton);

        // Up button
        ImageButton upButton = new ImageButton(this.width / 2 + xOffset + 14 + labelOffset, this.height / 4 - 8 + yOffset, 20, 10, 14, 0, 20, TEXTURE, button ->
        {
            upDownPressed.accept(-1);
            if (setsCustomDifficulty)
            {   ConfigSettings.DIFFICULTY.set(ConfigSettings.Difficulty.CUSTOM);
            }
        });
        upButton.active = shouldBeActive;
        widgetBatch.add(upButton);

        // Down button
        ImageButton downButton = new ImageButton(this.width / 2 + xOffset + 14 + labelOffset, this.height / 4 + 2 + yOffset, 20, 10, 14, 10, 20, TEXTURE, button ->
        {
            upDownPressed.accept(1);
            if (setsCustomDifficulty)
            {   ConfigSettings.DIFFICULTY.set(ConfigSettings.Difficulty.CUSTOM);
            }
        });
        downButton.active = shouldBeActive;
        widgetBatch.add(downButton);

        // Right button
        ImageButton rightButton = new ImageButton(this.width / 2 + xOffset + 34 + labelOffset, this.height / 4 - 8 + yOffset, 14, 20, 34, 0, 20, TEXTURE, button ->
        {
            leftRightPressed.accept(1);
            if (setsCustomDifficulty)
            {   ConfigSettings.DIFFICULTY.set(ConfigSettings.Difficulty.CUSTOM);
            }
        });
        rightButton.active = shouldBeActive;
        widgetBatch.add(rightButton);

        // Reset button
        ImageButton resetButton = new ImageButton(this.width / 2 + xOffset + 52 + labelOffset, this.height / 4 - 8 + yOffset, 20, canHide ? 10 : 20, canHide ? 68 : 48, 0, 20, TEXTURE, button ->
        {
            reset.run();
            if (setsCustomDifficulty)
            {   ConfigSettings.DIFFICULTY.set(ConfigSettings.Difficulty.CUSTOM);
            }
        });
        resetButton.active = shouldBeActive;
        widgetBatch.add(resetButton);

        // hide button, displayed directly under the reset button if canHide is true
        if (canHide)
        {
            ImageButton hideButton = new ImageButton(this.width / 2 + xOffset + 52 + labelOffset, this.height / 4 + 2 + yOffset, 20, 10, 68, 10, 20, TEXTURE, button ->
            {
                if (setsCustomDifficulty)
                {   ConfigSettings.DIFFICULTY.set(ConfigSettings.Difficulty.CUSTOM);
                }
                setImageX((ImageButton) button, hide.get());
            });
            hide.get();
            setImageX(hideButton, hide.get());
            hideButton.active = shouldBeActive;
            widgetBatch.add(hideButton);
        }

        // Add the option text
        ConfigLabel configLabel = new ConfigLabel(id, label.getString(), this.width / 2 + xOffset - 79, this.height / 4 + yOffset, shouldBeActive ? 16777215 : 8421504);
        // Add the clientside indicator
        if (clientside)
        {   this.addRenderableOnly(new ConfigImage(TEXTURE, this.width / 2 + xOffset - 98, this.height / 4 - 8 + yOffset + 5, 16, 15, 0, 144));
        }
        widgetBatch.add(configLabel);

        List<Component> tooltipList = new ArrayList<>(Arrays.asList(tooltip));
        // Add the client disclaimer if the setting is marked clientside
        if (clientside)
        {   tooltipList.add(new TranslatableComponent("cold_sweat.config.clientside_warning").withStyle(ChatFormatting.DARK_GRAY));
        }
        // Assign the tooltip
        this.setTooltip(id, tooltipList);

        this.addWidgetBatch(id, widgetBatch, shouldBeActive);

        // Add height to the list
        if (side == Side.LEFT)
            this.leftSideLength += ConfigScreen.OPTION_SIZE * 1.2;
        else
            this.rightSideLength += ConfigScreen.OPTION_SIZE * 1.2;
    }

    protected void addSliderButton(String id, Side side, Supplier<Component> dynamicLabel, double minVal, double maxVal,
                                   BiConsumer<Double, ConfigSliderButton> onChanged, Consumer<ConfigSliderButton> onInit,
                                   boolean requireOP, boolean clientside,
                                   Component... tooltip)
    {
        Component label = dynamicLabel.get();
        boolean shouldBeActive = !requireOP || MINECRAFT.player == null || MINECRAFT.player.hasPermissions(2);
        int buttonX = this.width / 2;
        int xOffset = side == Side.LEFT ? -179 : 56;
        int buttonY = this.height / 4 - 8 + (side == Side.LEFT ? leftSideLength : rightSideLength);
        int buttonWidth = 152 + Math.max(0, font.width(label) - 140);

        // Make the input
        ConfigSliderButton sliderButton = new ConfigSliderButton(buttonX + xOffset, buttonY, buttonWidth, 20, label, 0d)
        {
            @Override
            protected void updateMessage()
            {
                this.setMessage(dynamicLabel.get());
                onChanged.accept(CSMath.blend(minVal, maxVal, CSMath.truncate(this.value, 2), 0, 1), this);
            }

            @Override
            protected void applyValue()
            {   this.updateMessage();
            }
        };

        // Disable the input if the player is not OP
        sliderButton.active = shouldBeActive;

        // Set the initial value
        onInit.accept(sliderButton);

        // Add the clientside indicator
        if (clientside)
        {   this.addRenderableOnly(new ConfigImage(TEXTURE, this.width / 2 + xOffset - 115, buttonY, 16, 15, 0, 144));
        }

        List<Component> tooltipList = new ArrayList<>(Arrays.asList(tooltip));
        // Add the client disclaimer if the setting is marked clientside
        if (clientside)
        {   tooltipList.add(new TranslatableComponent("cold_sweat.config.clientside_warning").withStyle(ChatFormatting.DARK_GRAY));
        }
        // Assign the tooltip
        this.setTooltip(id, tooltipList);

        // Add the widget
        this.addWidgetBatch(id, List.of(sliderButton), shouldBeActive);

        // Mark this space as used
        if (side == Side.LEFT)
            this.leftSideLength += ConfigScreen.OPTION_SIZE;
        else
            this.rightSideLength += ConfigScreen.OPTION_SIZE;

    }

    @Override
    protected void init()
    {
        this.leftSideLength = 0;
        this.rightSideLength = 0;

        this.addRenderableWidget(new Button(
            this.width / 2 - BOTTOM_BUTTON_WIDTH / 2,
            this.height - BOTTOM_BUTTON_HEIGHT_OFFSET,
            BOTTOM_BUTTON_WIDTH, 20,
            new TranslatableComponent("gui.done"),
            button -> this.close())
        );

        // Navigation
        nextNavButton = new ImageButton(this.width - 32, 12, 20, 20, 0, 88, 20, TEXTURE,
                button ->
                {   ConfigScreen.CURRENT_PAGE++;
                    MINECRAFT.setScreen(ConfigScreen.getPage(ConfigScreen.CURRENT_PAGE, parentScreen));
                });
        if (ConfigScreen.CURRENT_PAGE < ConfigScreen.LAST_PAGE)
            this.addRenderableWidget(nextNavButton);

        prevNavButton = new ImageButton(this.width - 76, 12, 20, 20, 20, 88, 20, TEXTURE,
                button ->
                {   ConfigScreen.CURRENT_PAGE--;
                    MINECRAFT.setScreen(ConfigScreen.getPage(ConfigScreen.CURRENT_PAGE, parentScreen));
                });
        if (ConfigScreen.CURRENT_PAGE > ConfigScreen.FIRST_PAGE)
            this.addRenderableWidget(prevNavButton);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(poseStack);

        // Page Title
        drawCenteredString(poseStack, this.font, this.title.getString(), this.width / 2, TITLE_HEIGHT, 0xFFFFFF);

        // Page Number
        drawString(poseStack, this.font, new TextComponent((ConfigScreen.CURRENT_PAGE + 1) + "/" + (ConfigScreen.LAST_PAGE + 1)), this.width - 53, 18, 16777215);

        // Section 1 Title
        drawString(poseStack, this.font, this.sectionOneTitle(), this.width / 2 - 204, this.height / 4 - 28, 16777215);

        // Section 1 Divider
        RenderSystem.setShaderTexture(0, TEXTURE);
        this.blit(poseStack, this.width / 2 - 202, this.height / 4 - 16, 255, 0, 1, 154);

        if (this.sectionTwoTitle() != null)
        {
            // Section 2 Title
            drawString(poseStack, this.font, this.sectionTwoTitle(), this.width / 2 + 32, this.height / 4 - 28, 16777215);

            // Section 2 Divider
            RenderSystem.setShaderTexture(0, TEXTURE);
            this.blit(poseStack, this.width / 2 + 34, this.height / 4 - 16, 255, 0, 1, 154);
        }

        super.render(poseStack, mouseX, mouseY, partialTicks);

        // Render tooltip
        if (this.isDragging())
        {   MOUSE_STILL_TIMER = 0;
        }
        if (MOUSE_STILL_TIMER < TOOLTIP_DELAY) return;

        for (Map.Entry<String, Pair<List<GuiEventListener>, Boolean>> entry : widgetBatches.entrySet())
        {
            String id = entry.getKey();
            List<GuiEventListener> widgets = entry.getValue().getFirst();
            boolean enabled = entry.getValue().getSecond();
            int minX = 0, minY = 0, maxX = 0, maxY = 0;
            for (GuiEventListener listener : widgets)
            {
                if (listener instanceof AbstractWidget widget)
                {
                    if (minX == 0 || widget.x < minX)
                        minX = widget.x;
                    if (minY == 0 || widget.y < minY)
                        minY = widget.y;
                    if (maxX == 0 || widget.x + widget.getWidth() > maxX)
                        maxX = widget.x + widget.getWidth();
                    if (maxY == 0 || widget.y + widget.getHeight() > maxY)
                        maxY = widget.y + widget.getHeight();
                }
            }

            // if the mouse is hovering over any of the widgets in the batch, show the corresponding tooltip
            if (CSMath.betweenInclusive(mouseX, minX, maxX) && CSMath.betweenInclusive(mouseY, minY, maxY))
            {
                List<Component> tooltipList = enabled
                                              ? this.tooltips.get(id)
                                              : List.of(new TranslatableComponent("cold_sweat.config.require_op").withStyle(ChatFormatting.RED));
                if (tooltipList != null && !tooltipList.isEmpty())
                {   this.renderComponentTooltip(poseStack, tooltipList, mouseX, mouseY);
                }
                break;
            }
        }
    }

    @Override
    public void tick()
    {   super.tick();
    }

    @Override
    public boolean isPauseScreen()
    {   return true;
    }

    public void close()
    {   this.onClose();
        Minecraft.getInstance().setScreen(this.parentScreen);
    }

    public enum Side
    {
        LEFT,
        RIGHT
    }

    protected void addWidgetBatch(String id, List<GuiEventListener> elements, boolean enabled)
    {
        for (GuiEventListener element : elements)
        {
            if (element instanceof Widget && element instanceof NarratableEntry widget)
                this.addRenderableWidget((GuiEventListener & Widget & NarratableEntry) widget);
        }
        this.widgetBatches.put(id, Pair.of(elements, enabled));
    }

    public List<GuiEventListener> getWidgetBatch(String id)
    {
        return this.widgetBatches.get(id).getFirst();
    }

    protected void setTooltip(String id, List<Component> tooltip)
    {
        List<Component> wrappedTooltip = new ArrayList<>();
        for (Component component : tooltip)
        {  // wrap lines at 300 px
           List<FormattedText> wrappedText = font.getSplitter().splitLines(component, 300, component.getStyle());
           // convert FormattedText back to styled Components
           wrappedTooltip.addAll(wrappedText.stream().map(text -> new TextComponent(text.getString()).withStyle(component.getStyle())).toList());
        }
        this.tooltips.put(id, wrappedTooltip);
    }

    public static void setImageX(ImageButton button, boolean enabled)
    {
        Field imageX = ObfuscationReflectionHelper.findField(ImageButton.class, "f_94224_");
        imageX.setAccessible(true);
        try
        {   imageX.set(button, enabled ? 68 : 88);
        }
        catch (Exception ignored) {}
    }

    @Override
    public void onClose()
    {   MINECRAFT.setScreen(this.parentScreen);
        ConfigScreen.saveConfig();
    }

    public MutableComponent getToggleButtonText(MutableComponent text, boolean on)
    {
        return text.append(": ")
                   .append(on ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }

    public MutableComponent getSliderPercentageText(MutableComponent message, double value, double offAt)
    {
        return message.append(": ")
                      .append(Double.compare(offAt, value) != 0
                              ? new TextComponent((int) (value * 100) + "%")
                              : new TextComponent(CommonComponents.OPTION_OFF.getString()));
    }

    public MutableComponent getSliderText(MutableComponent message, int value, int min, int max, int offAt)
    {
        return message.append(": ")
                      .append((value > min || value < max) && value != offAt
                              ? new TextComponent(value + "")
                              : new TextComponent(CommonComponents.OPTION_OFF.getString()));
    }

    public MutableComponent getSliderText(MutableComponent message, double value, double min, double max, double offAt)
    {
        return message.append(": ")
                .append((value > min || value < max) && Double.compare(value, offAt) != 0
                        ? new TextComponent(CSMath.truncate(value, 1) + "")
                        : new TextComponent(CommonComponents.OPTION_OFF.getString()));
    }
}
