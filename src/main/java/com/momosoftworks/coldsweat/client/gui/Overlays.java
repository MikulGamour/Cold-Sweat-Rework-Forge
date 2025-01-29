package com.momosoftworks.coldsweat.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.temperature.PlayerTempCap;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.IIngameOverlay;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class Overlays
{
    public static final ResourceLocation BODY_TEMP_GAUGE = new ResourceLocation("cold_sweat:textures/gui/overlay/body_temp_gauge.png");
    public static final ResourceLocation BODY_TEMP_GAUGE_HC = new ResourceLocation("cold_sweat:textures/gui/overlay/body_temp_gauge_hc.png");
    public static final ResourceLocation WORLD_TEMP_GAUGE = new ResourceLocation("cold_sweat:textures/gui/overlay/world_temp_gauge.png");
    public static final ResourceLocation WORLD_TEMP_GAUGE_HC = new ResourceLocation("cold_sweat:textures/gui/overlay/world_temp_gauge_hc.png");
    public static final ResourceLocation VAGUE_TEMP_GAUGE = new ResourceLocation("cold_sweat:textures/gui/overlay/vague_temp_gauge.png");
    public static final ResourceLocation VAGUE_TEMP_GAUGE_HC = new ResourceLocation("cold_sweat:textures/gui/overlay/vague_temp_gauge_hc.png");

    public static final Supplier<ResourceLocation> BODY_TEMP_GAUGE_LOCATION  = () ->
            ConfigSettings.HIGH_CONTRAST.get() ? BODY_TEMP_GAUGE_HC
                                               : BODY_TEMP_GAUGE;
    public static final Supplier<ResourceLocation> WORLD_TEMP_GAUGE_LOCATION = () ->
            ConfigSettings.HIGH_CONTRAST.get() ? WORLD_TEMP_GAUGE_HC
                                               : WORLD_TEMP_GAUGE;
    public static final Supplier<ResourceLocation> VAGUE_TEMP_GAUGE_LOCATION = () ->
            ConfigSettings.HIGH_CONTRAST.get() ? VAGUE_TEMP_GAUGE_HC
                                               : VAGUE_TEMP_GAUGE;

    // Stuff for world temperature
    public static double WORLD_TEMP = 0;
    static boolean ADVANCED_WORLD_TEMP = false;
    static double PREV_WORLD_TEMP = 0;
    static double PLAYER_MAX_TEMP = 0;
    static double PLAYER_MIN_TEMP = 0;

    // Stuff for body temperature
    public static double BODY_TEMP = 0;
    static double PREV_BODY_TEMP = 0;
    static int BLEND_BODY_TEMP = 0;
    static int ICON_BOB = 0;
    static double BODY_TEMP_SEVERITY = 0;

    public static IIngameOverlay WORLD_TEMP_OVERLAY = (gui, poseStack, partialTick, width, height) ->
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && ADVANCED_WORLD_TEMP
        && Minecraft.getInstance().gameMode.getPlayerMode() != GameType.SPECTATOR
        && !Minecraft.getInstance().options.hideGui && ConfigSettings.WORLD_GAUGE_ENABLED.get())
        {
            gui.setupOverlayRenderState(true, false);

            // Get player world temperature
            double temp = Temperature.convert(WORLD_TEMP, ConfigSettings.CELSIUS.get() ? Temperature.Units.C : Temperature.Units.F, Temperature.Units.MC, true);

            // Get the temperature severity
            int severity = getGaugeSeverity(temp, PLAYER_MIN_TEMP, PLAYER_MAX_TEMP);

            // Set text color
            int color = switch (severity)
            {   case  2, 3 -> 16297781;
                case  4    -> 16728089;
                case -2,-3 -> 8443135;
                case -4    -> 4236031;
                default -> 14737376;
            };


            /* Render gauge */

            poseStack.pushPose();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);

            // Set gauge texture
            RenderSystem.setShaderTexture(0, WORLD_TEMP_GAUGE_LOCATION.get());

            // Render frame
            GuiComponent.blit(poseStack, (width / 2) + 93 + ConfigSettings.WORLD_GAUGE_POS.get().x(),
                              height - 19 + ConfigSettings.WORLD_GAUGE_POS.get().y(), 0, 64 - severity * 16, 25, 16, 25, 144);

            RenderSystem.disableBlend();

            // Sets the text bobbing offset (or none if disabled)
            int bob = ConfigSettings.ICON_BOBBING.get() && !CSMath.betweenInclusive(temp, PLAYER_MIN_TEMP, PLAYER_MAX_TEMP) && player.tickCount % 2 == 0 ? 1 : 0;

            // Render text
            int blendedTemp = (int) CSMath.blend(PREV_WORLD_TEMP, WORLD_TEMP, Minecraft.getInstance().getFrameTime(), 0, 1);

                Minecraft.getInstance().font.draw(poseStack, (blendedTemp + ConfigSettings.TEMP_OFFSET.get())+"",
                        /* X */ width / 2 + 106 + (Integer.toString(blendedTemp + ConfigSettings.TEMP_OFFSET.get()).length() * -3) + ConfigSettings.WORLD_GAUGE_POS.get().x(),
                        /* Y */ height - 15 - bob + ConfigSettings.WORLD_GAUGE_POS.get().y(), color);
                poseStack.popPose();
            }
        };

    public static IIngameOverlay BODY_TEMP_OVERLAY = (gui, poseStack, partialTick, width, height) ->
    {
        gui.setupOverlayRenderState(true, false);
        Minecraft mc = Minecraft.getInstance();

        if (gui.shouldDrawSurvivalElements() && !Minecraft.getInstance().options.hideGui)
        {
            // Blend body temperature (per frame)
            BLEND_BODY_TEMP = (int) CSMath.blend(PREV_BODY_TEMP, BODY_TEMP, Minecraft.getInstance().getFrameTime(), 0, 1);

            // Get text color
            int color = switch (((int) BODY_TEMP_SEVERITY))
            {   case  7, -7 -> 16777215;
                case  6 -> 16777132;
                case  5 -> 16767856;
                case  4 -> 16759634;
                case  3 -> 16751174;
                case -3 -> 6078975;
                case -4 -> 7528447;
                case -5 -> 8713471;
                case -6 -> 11599871;
                default -> BLEND_BODY_TEMP > 0 ? 16744509
                         : BLEND_BODY_TEMP < 0 ? 4233468
                         : 11513775;
            };

            // Get the outer border color when readout is > 100
            int colorBG = BLEND_BODY_TEMP < 0 ? 1122643
                        : BLEND_BODY_TEMP > 0 ? 5376516
                        : 0;

            int bobLevel = Math.min(Math.abs(((int) BODY_TEMP_SEVERITY)), 3);
            int threatOffset = !ConfigSettings.ICON_BOBBING.get() ? 0
                             : bobLevel == 2 ? ICON_BOB
                             : bobLevel == 3 ? Minecraft.getInstance().cameraEntity.tickCount % 2
                             : 0;

            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, BODY_TEMP_GAUGE_LOCATION.get());

            if (ConfigSettings.BODY_ICON_ENABLED.get())
            {
                int icon = Math.abs(BLEND_BODY_TEMP) < 100 ?  CSMath.floor(BODY_TEMP_SEVERITY) : 4 * CSMath.sign(BODY_TEMP_SEVERITY);
                int iconX = (width / 2) - 5 + ConfigSettings.BODY_ICON_POS.get().x();
                int iconYOffset = ADVANCED_WORLD_TEMP && ConfigSettings.MOVE_BODY_ICON_WHEN_ADVANCED.get()
                                  ? 54
                                  : 47;
                int iconY = height - iconYOffset - threatOffset + ConfigSettings.BODY_ICON_POS.get().y();
                // Render icon
                GuiComponent.blit(poseStack, iconX, iconY, 0, 40 - icon * 10, 10, 10, 10, 90);

                // Render new icon if temperature changing
                if (CSMath.betweenExclusive(Math.abs(BLEND_BODY_TEMP), 0, 100))
                {
                    // Map current temp severity to filling up the icon
                    double blend = CSMath.blend(1, 9, Math.abs(BODY_TEMP_SEVERITY), Math.abs(CSMath.floor(BODY_TEMP_SEVERITY)), Math.abs(CSMath.ceil(BODY_TEMP_SEVERITY)));
                    GuiComponent.blit(poseStack, iconX, iconY + 10 - CSMath.ceil(blend), 0,
                                      // UV Y-coordinate for the icon in this stage
                                      40 - CSMath.grow(icon, BLEND_BODY_TEMP > 0 ? 0 : 2) * 10 - CSMath.ceil(blend),
                                      10, CSMath.ceil(blend), 10, 90);
                }
            }

            // Render Readout
            if (ConfigSettings.BODY_READOUT_ENABLED.get() && ADVANCED_WORLD_TEMP)
            {
                Font font = mc.font;
                int scaledWidth = mc.getWindow().getGuiScaledWidth();
                int scaledHeight = mc.getWindow().getGuiScaledHeight();

                String s = "" + Math.min(Math.abs(BLEND_BODY_TEMP), 100);
                int x = (scaledWidth - font.width(s)) / 2 + ConfigSettings.BODY_READOUT_POS.get().x();
                int y = scaledHeight - 31 - 10 + ConfigSettings.BODY_READOUT_POS.get().y();

                    // Draw the outline
                    font.draw(poseStack, s, x + 1, y, colorBG);
                    font.draw(poseStack, s, x - 1, y, colorBG);
                    font.draw(poseStack, s, x, y + 1, colorBG);
                    font.draw(poseStack, s, x, y - 1, colorBG);

                    // Draw the readout
                    font.draw(poseStack, s, x, y, color);
                }
            }
        };

    public static IIngameOverlay VAGUE_TEMP_OVERLAY = (gui, poseStack, partialTick, width, height) ->
    {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player != null && !ADVANCED_WORLD_TEMP && mc.gameMode.getPlayerMode() != GameType.SPECTATOR
        && !mc.options.hideGui && ConfigSettings.WORLD_GAUGE_ENABLED.get() && gui.shouldDrawSurvivalElements())
        {
            gui.setupOverlayRenderState(true, false);

            // Get player world temperature
            double temp = Temperature.convert(WORLD_TEMP, ConfigSettings.CELSIUS.get() ? Temperature.Units.C : Temperature.Units.F, Temperature.Units.MC, true);
            // Get the temperature severity
            int severity = getGaugeSeverity(temp, PLAYER_MIN_TEMP, PLAYER_MAX_TEMP);
            int renderOffset = CSMath.clamp(severity, -1, 1) * 2;

            poseStack.pushPose();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);

            int bobLevel = Math.min(Math.abs(((int) BODY_TEMP_SEVERITY)), 3);
            int threatOffset = !ConfigSettings.ICON_BOBBING.get()
                               ? 0
                               : bobLevel == 2
                                 ? ICON_BOB
                                 : bobLevel == 3
                                   ? Minecraft.getInstance().cameraEntity.tickCount % 2
                                   : 0;

            // Set gauge texture
            RenderSystem.setShaderTexture(0, VAGUE_TEMP_GAUGE_LOCATION.get());

            // Render frame
            GuiComponent.blit(poseStack,
                              (width / 2) - 8 + ConfigSettings.BODY_ICON_POS.get().x(),
                              height - 50 + ConfigSettings.BODY_ICON_POS.get().y() - renderOffset - threatOffset,
                              0, 64 - severity * 16, 16, 16, 16, 144);

            RenderSystem.disableBlend();
            poseStack.popPose();
        }
    };

    static
    {
        OverlayRegistry.registerOverlayBelow(ForgeIngameGui.CHAT_PANEL_ELEMENT, "world_temp", WORLD_TEMP_OVERLAY);
        OverlayRegistry.registerOverlayBelow(ForgeIngameGui.CHAT_PANEL_ELEMENT, "body_temp", BODY_TEMP_OVERLAY);
        OverlayRegistry.registerOverlayBelow(ForgeIngameGui.CHAT_PANEL_ELEMENT, "vague_temp", VAGUE_TEMP_OVERLAY);
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static final class TickOverlays
    {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event)
        {
            Player player = Minecraft.getInstance().player;
            if (event.phase == TickEvent.Phase.START && player != null)
            {
                EntityTempManager.getTemperatureCap(player).ifPresent(icap ->
                {
                    if (!(icap instanceof PlayerTempCap cap)) return;

                    cap.calculateHudVisibility(player);
                    ADVANCED_WORLD_TEMP = cap.showAdvancedWorldTemp();

                    /* World Temp */

                    // Get temperature in actual degrees
                    boolean celsius = ConfigSettings.CELSIUS.get();
                    double worldTemp = cap.getTrait(Temperature.Trait.WORLD);
                    double realTemp = Temperature.convert(worldTemp, Temperature.Units.MC, celsius ? Temperature.Units.C : Temperature.Units.F, true);
                    // Calculate the blended world temperature for this tick
                    double diff = realTemp - WORLD_TEMP;
                    PREV_WORLD_TEMP = WORLD_TEMP;
                    WORLD_TEMP += Math.abs(diff) <= 1 ? diff : CSMath.maxAbs(diff / ConfigSettings.TEMP_SMOOTHING.get(), 0.25 * CSMath.sign(diff));

                    // Update max/min temps
                    PLAYER_MAX_TEMP = cap.getTrait(Temperature.Trait.BURNING_POINT);
                    PLAYER_MIN_TEMP = cap.getTrait(Temperature.Trait.FREEZING_POINT);

                    /* Body Temp */

                    // Blend body temp (per tick)
                    PREV_BODY_TEMP = BODY_TEMP;
                    double currentTemp = cap.getTrait(Temperature.Trait.BODY);
                    BODY_TEMP = Math.abs(currentTemp - BODY_TEMP) < 0.1 ? currentTemp : BODY_TEMP + (cap.getTrait(Temperature.Trait.BODY) - BODY_TEMP) / 5;

                    // Handle effects for the icon (bobbing, stage, transition)
                    // Get icon bob
                    ICON_BOB = player.tickCount % 3 == 0 && Math.random() < 0.3 ? 1 : 0;

                    // Get the severity of the player's body temperature
                    BODY_TEMP_SEVERITY = getBodySeverity(BLEND_BODY_TEMP);
                });
            }
        }
    }

    /**
     * Gets the given temperature's severity, relative to the player's min and max temperatures.
     * @param playerMin The player's minimum temperature
     * @param playerMax The player's maximum temperature
     * @return A number between -1 and 1, representing the severity of the temperature
     */
    public static double getWorldSeverity(double temp, double playerMin, double playerMax)
    {
        if (temp < playerMin) return -1;
        if (temp > playerMax) return 1;

        double normalMin = ConfigSettings.Difficulty.NORMAL.getSetting(ConfigSettings.MIN_TEMP);
        double normalMax = ConfigSettings.Difficulty.NORMAL.getSetting(ConfigSettings.MAX_TEMP);

        double mid = calculateMidpoint(playerMin, playerMax, normalMin, normalMax);

        return (temp < mid)
               ? CSMath.blend(-1, 0, temp, playerMin, mid)
               : CSMath.blend(0, 1, temp, mid, playerMax);
    }

    private static double calculateMidpoint(double playerMin, double playerMax, double normalMin, double normalMax)
    {
        boolean bothBelowNormal = playerMin < normalMin && playerMax < normalMax;
        boolean bothAboveNormal = playerMin > normalMin && playerMax > normalMax;

        return (bothBelowNormal || bothAboveNormal)
               ? (playerMin + playerMax) / 2
               : (normalMin + normalMax) / 2;
    }

    public static int getGaugeSeverity(double temp, double playerMin, double playerMax)
    {   return (int) (getWorldSeverity(temp, playerMin, playerMax) * 4);
    }

    static double getBodySeverity(int temp)
    {   int sign = CSMath.sign(temp);
        int absTemp = Math.abs(temp);

        return (absTemp < 100 ? CSMath.blend(0d, 3d, absTemp, 0, 100)
                              : CSMath.blend(3d, 7d, absTemp, 100, 150))
                * sign;
    }

    public static void setBodyTempInstant(double temp)
    {   BODY_TEMP = temp;
        PREV_BODY_TEMP = temp;
        BLEND_BODY_TEMP = (int) temp;
    }

    public static void setWorldTempInstant(double temp)
    {   WORLD_TEMP = temp;
        PREV_WORLD_TEMP = temp;
    }
}
