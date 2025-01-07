package com.momosoftworks.coldsweat.client.event;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.event.vanilla.RenderLevelEvent;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.client.gui.Overlays;
import com.momosoftworks.coldsweat.client.renderer.PostProcessShaderManager;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.init.ModEffects;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.lang.reflect.Field;
import java.util.List;

@EventBusSubscriber(Dist.CLIENT)
public class TempEffectsClient
{
    static float BLEND_TEMP = 0;

    static float PREV_X_SWAY = 0;
    static float PREV_Y_SWAY = 0;
    static float X_SWAY_SPEED = 0;
    static float Y_SWAY_SPEED = 0;
    static float X_SWAY_PHASE = 0;
    static float Y_SWAY_PHASE = 0;
    static float TIME_SINCE_NEW_SWAY = 0;

    static double COLD_IMMUNITY = 0;
    static double HOT_IMMUNITY  = 0;

    // Sway the player's camera when the player is too hot; swaying is more drastic at higher temperatures
    @SubscribeEvent
    public static void setCamera(ViewportEvent.ComputeCameraAngles event)
    {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.isAlive() && EntityTempManager.isPeacefulMode(player)) return;

        if (!Minecraft.getInstance().isPaused())
        {
            // Get the FPS of the game
            float frameTime = Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();
            float temp = (float) Temperature.get(player, Temperature.Trait.BODY);
            // Get a blended version of the player's temperature
            // More important for fog stuff
            BLEND_TEMP += (temp - BLEND_TEMP) * frameTime / 20;

            if (ConfigSettings.DISTORTION_EFFECTS.get())
            {
                // Camera "shivers" when temp is < -50
                if (BLEND_TEMP <= -50 && COLD_IMMUNITY < 1)
                {
                    double tickTime = player.tickCount + event.getPartialTick();
                    float shiverIntensity = CSMath.blend(((float) Math.sin(tickTime / 10) + 1) * 0.03f + 0.01f,
                                                0f, BLEND_TEMP, -100, -50);
                    // Multiply the effect for lower framerates
                    shiverIntensity *= Minecraft.getInstance().getTimer().getRealtimeDeltaTicks() * 10;
                    shiverIntensity = (float) CSMath.blend(shiverIntensity, 0, COLD_IMMUNITY, 0, 1);
                    float shiverRotation = (float) (Math.sin(tickTime * 2.5) * shiverIntensity);
                    // Rotate camera
                    player.setYRot(player.getYRot() + shiverRotation);
                }
                // Sway camera for heatstroke
                else if (BLEND_TEMP >= 50 && HOT_IMMUNITY < 1)
                {
                    float immunityModifier = (float) CSMath.blend(BLEND_TEMP, 50, HOT_IMMUNITY, 0, 1);
                    float factor = CSMath.blend(0, 20, immunityModifier, 50, 100);

                    // Set random sway speed every once in a while
                    if (TIME_SINCE_NEW_SWAY > 100 || X_SWAY_SPEED == 0 || Y_SWAY_SPEED == 0)
                    {
                        TIME_SINCE_NEW_SWAY = 0;
                        X_SWAY_SPEED = (float) (Math.random() * 0.003f + 0.004f);
                        Y_SWAY_SPEED = (float) (Math.random() * 0.003f + 0.004f);
                    }
                    TIME_SINCE_NEW_SWAY += frameTime;

                    // Blend to the new sway speed
                    X_SWAY_PHASE += 2 * Math.PI * frameTime * X_SWAY_SPEED;
                    Y_SWAY_PHASE += 2 * Math.PI * frameTime * Y_SWAY_SPEED;

                    // Apply the sway speed to a sin function
                    float xOffs = (float) (Math.sin(X_SWAY_PHASE) * factor);
                    float yOffs = (float) ((Math.sin(Y_SWAY_PHASE) + Math.cos(Y_SWAY_PHASE / 4) * 2) * factor * 3);

                    // Apply the sway
                    player.setXRot(player.getXRot() + xOffs - PREV_X_SWAY);
                    player.setYRot(player.getYRot() + yOffs - PREV_Y_SWAY);

                    // Save the previous sway
                    PREV_X_SWAY = xOffs;
                    PREV_Y_SWAY = yOffs;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        Player player = Minecraft.getInstance().player;
        if (player == null || EntityTempManager.isPeacefulMode(player)) return;
        if (player.tickCount % 5 == 0)
        {
            // Set cold immunity
            if (player.hasEffect(ModEffects.ICE_RESISTANCE) && ConfigSettings.ICE_RESISTANCE_ENABLED.get())
                {   COLD_IMMUNITY = 1;
                }
            else COLD_IMMUNITY = Temperature.get(player, Temperature.Trait.COLD_RESISTANCE);
                // Set heat immunity
            if (player.hasEffect(MobEffects.FIRE_RESISTANCE) && ConfigSettings.FIRE_RESISTANCE_ENABLED.get())
            {   HOT_IMMUNITY = 1;
                }
            else HOT_IMMUNITY  = Temperature.get(player, Temperature.Trait.HEAT_RESISTANCE);
        }
    }

    @SubscribeEvent
    public static void setFogDistance(ViewportEvent.RenderFog event)
    {
        Player player = Minecraft.getInstance().player;
        if (player == null || EntityTempManager.isPeacefulMode(player)) return;

        double fogDistance = ConfigSettings.HEATSTROKE_FOG_DISTANCE.get();
        if (fogDistance >= 64) return;
        if (fogDistance < Double.POSITIVE_INFINITY && BLEND_TEMP >= 50 && HOT_IMMUNITY < 1)
        {
            float tempWithResistance = (float) CSMath.blend(BLEND_TEMP, 50, HOT_IMMUNITY, 0, 1);
            if (fogDistance > (event.getFarPlaneDistance())) return;
            event.setFarPlaneDistance(CSMath.blend(event.getFarPlaneDistance(), (float) fogDistance, tempWithResistance, 50f, 90f));
            event.setNearPlaneDistance(CSMath.blend(event.getNearPlaneDistance(), (float) (fogDistance * 0.3), tempWithResistance, 50f, 90f));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void setFogColor(ViewportEvent.ComputeFogColor event)
    {
        Player player = Minecraft.getInstance().player;
        double fogDistance = ConfigSettings.HEATSTROKE_FOG_DISTANCE.get();
        if (fogDistance >= 64) return;
        if (fogDistance < Double.POSITIVE_INFINITY && player != null && BLEND_TEMP >= 50 && HOT_IMMUNITY < 4)
        {
            float tempWithResistance = (float) CSMath.blend(BLEND_TEMP, 50, HOT_IMMUNITY, 0, 4);
            event.setRed(CSMath.blend(event.getRed(), 0.01f, tempWithResistance, 50, 90));
            event.setGreen(CSMath.blend(event.getGreen(), 0.01f, tempWithResistance, 50, 90));
            event.setBlue(CSMath.blend(event.getBlue(), 0.05f, tempWithResistance, 50, 90));
        }
    }

    static final ResourceLocation HAZE_TEXTURE = ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "textures/gui/overlay/haze.png");
    static final ResourceLocation FREEZE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/powder_snow_outline.png");

    @SubscribeEvent
    public static void vignette(RenderGuiLayerEvent.Pre event)
    {
        Player player = Minecraft.getInstance().player;
        if (player == null || EntityTempManager.isPeacefulMode(player)) return;
        if (event.getName() == VanillaGuiLayers.CAMERA_OVERLAYS
        && ((BLEND_TEMP > 0 && HOT_IMMUNITY < 1) || (BLEND_TEMP < 0 && COLD_IMMUNITY < 1)))
        {
            float resistance = (float) CSMath.blend(1, 0, BLEND_TEMP > 0 ? HOT_IMMUNITY : COLD_IMMUNITY, 0, 1);
            float opacity = CSMath.blend(0f, 1f, Math.abs(BLEND_TEMP), 50, 100) * resistance;
            float tickTime = player.tickCount + event.getPartialTick().getGameTimeDeltaPartialTick(true);
            if (opacity == 0) return;
            float width  = event.getGuiGraphics().guiWidth();
            float height = event.getGuiGraphics().guiHeight();
            float scale = Minecraft.getInstance().getWindow().calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().isEnforceUnicode());

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            ResourceLocation texture;
            if (BLEND_TEMP > 0)
            {   float vignetteBrightness = opacity + ((float) Math.sin((tickTime + 3) / (Math.PI * 1.0132f)) / 5f - 0.2f) * opacity;
                RenderSystem.setShaderColor(0.231f, 0f, 0f, vignetteBrightness);
                texture = HAZE_TEXTURE;
            }
            else
            {   RenderSystem.setShaderColor(1f, 1f, 1f, opacity);
                texture = FREEZE_TEXTURE;
            }
            event.getGuiGraphics().blit(texture, 0, 0, -90, 0.0F, 0.0F, (int) width, (int) height, (int) width, (int) height);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.defaultBlendFunc();
        }
    }

    @SubscribeEvent
    public static void onRenderBlur(RenderLevelEvent.Post event)
    {
        Minecraft mc = Minecraft.getInstance();
        PostProcessShaderManager shaderManager = PostProcessShaderManager.getInstance();

        float playerTemp = (float) Overlays.BODY_TEMP;
        if (ConfigSettings.DISTORTION_EFFECTS.get() && playerTemp >= 50 && HOT_IMMUNITY < 1
        && mc.player != null && !EntityTempManager.isPeacefulMode(mc.player))
        {
            float blur = CSMath.blend(0f, 12f, playerTemp, 50, 100);
            blur = (float) CSMath.blend(blur, 0, HOT_IMMUNITY, 0, 1);
            if (!shaderManager.hasEffect("heat_blur"))
            {   shaderManager.loadEffect("heat_blur", PostProcessShaderManager.BLOBS);
            }
            Uniform blurRadius = shaderManager.getPostPasses("heat_blur").get(0).getEffect().getUniform("Radius");
            if (blurRadius != null)
            {   blurRadius.set(blur);
            }
        }
        else if (shaderManager.hasEffect("heat_blur"))
        {   shaderManager.closeEffect("heat_blur");
        }

        shaderManager.process(event.getPartialTick());
    }
}
