package com.momosoftworks.coldsweat.client.event;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.event.vanilla.RenderLevelEvent;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.client.gui.Overlays;
import com.momosoftworks.coldsweat.client.renderer.PostProcessShaderManager;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.common.event.TempEffectsCommon;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(Dist.CLIENT)
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

    static int COLD_IMMUNITY = 0;
    static int HOT_IMMUNITY  = 0;

    // Sway the player's camera when the player is too hot; swaying is more drastic at higher temperatures
    @SubscribeEvent
    public static void setCamera(EntityViewRenderEvent.CameraSetup event)
    {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.isAlive() && EntityTempManager.immuneToTempEffects(player)) return;

        if (!Minecraft.getInstance().isPaused())
        {
            // Get the FPS of the game
            float frameTime = Minecraft.getInstance().getDeltaFrameTime();
            float temp = (float) Temperature.get(player, Temperature.Trait.BODY);
            // Get a blended version of the player's temperature
            // More important for fog stuff
            BLEND_TEMP += (temp - BLEND_TEMP) * frameTime / 20;

            if (ConfigSettings.DISTORTION_EFFECTS.get())
            {
                // Camera "shivers" when temp is < -50
                if (BLEND_TEMP <= -50 && COLD_IMMUNITY < 4)
                {
                    double tickTime = player.tickCount + event.getPartialTicks();
                    float shiverIntensity = CSMath.blend(((float) Math.sin(tickTime / 10) + 1) * 0.03f + 0.01f,
                                                0f, BLEND_TEMP, -100, -50);
                    // Multiply the effect for lower framerates
                    shiverIntensity *= Minecraft.getInstance().getDeltaFrameTime() * 10;
                    float shiverRotation = (float) (Math.sin(tickTime * 2.5) * shiverIntensity) / (1 + COLD_IMMUNITY);
                    // Rotate camera
                    player.setYRot(player.getYRot() + shiverRotation);
                }
                // Sway camera for heatstroke
                else if (BLEND_TEMP >= 50 && HOT_IMMUNITY < 4)
                {
                    float immunityModifier = CSMath.blend(BLEND_TEMP, 50, HOT_IMMUNITY, 0, 4);
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
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END)
        {
            Player player = Minecraft.getInstance().player;
            if (player == null || EntityTempManager.immuneToTempEffects(player)) return;
            if (player.tickCount % 5 == 0)
            {
                COLD_IMMUNITY = 0;
                HOT_IMMUNITY = 0;
                boolean hasGrace = player.hasEffect(ModEffects.GRACE);
                if (player.hasEffect(ModEffects.ICE_RESISTANCE) || hasGrace) COLD_IMMUNITY = 4;
                else COLD_IMMUNITY = 0;
                if (player.hasEffect(MobEffects.FIRE_RESISTANCE) || hasGrace) HOT_IMMUNITY = 4;
                else HOT_IMMUNITY = 0;

                if (COLD_IMMUNITY != 4) COLD_IMMUNITY = TempEffectsCommon.getColdResistance(player);
                if (HOT_IMMUNITY  != 4) HOT_IMMUNITY  = TempEffectsCommon.getHeatResistance(player);
            }
        }
    }

    @SubscribeEvent
    public static void renderFog(EntityViewRenderEvent event)
    {
        if (!(event instanceof EntityViewRenderEvent.RenderFogEvent || event instanceof EntityViewRenderEvent.FogColors)) return;

        Player player = Minecraft.getInstance().player;
        if (player == null || EntityTempManager.immuneToTempEffects(player)) return;

        double fogDistance = ConfigSettings.HEATSTROKE_FOG_DISTANCE.get();
        if (fogDistance >= 64) return;
        if (fogDistance < Double.POSITIVE_INFINITY&& BLEND_TEMP >= 50 && HOT_IMMUNITY < 4)
        {
            float tempWithResistance = CSMath.blend(BLEND_TEMP, 50, HOT_IMMUNITY, 0, 4);
            if (event instanceof EntityViewRenderEvent.RenderFogEvent fog)
            {
                if (fogDistance > (fog.getFarPlaneDistance())) return;
                fog.setFarPlaneDistance(CSMath.blend(fog.getFarPlaneDistance(), (float) fogDistance, tempWithResistance, 50f, 90f));
                fog.setNearPlaneDistance(CSMath.blend(fog.getNearPlaneDistance(), (float) (fogDistance * 0.3), tempWithResistance, 50f, 90f));
                fog.setCanceled(true);
            }
            else
            {   EntityViewRenderEvent.FogColors fogColor = (EntityViewRenderEvent.FogColors) event;
                fogColor.setRed(CSMath.blend(fogColor.getRed(), 0.01f, tempWithResistance, 50, 90));
                fogColor.setGreen(CSMath.blend(fogColor.getGreen(), 0.01f, tempWithResistance, 50, 90));
                fogColor.setBlue(CSMath.blend(fogColor.getBlue(), 0.05f, tempWithResistance, 50, 90));
            }
        }
    }

    static ResourceLocation HAZE_TEXTURE = new ResourceLocation(ColdSweat.MOD_ID, "textures/gui/overlay/haze.png");
    static final ResourceLocation FREEZE_TEXTURE = new ResourceLocation("textures/misc/powder_snow_outline.png");

    @SubscribeEvent
    public static void vignette(RenderGameOverlayEvent.PreLayer event)
    {
        Player player = Minecraft.getInstance().player;
        if (player == null || EntityTempManager.immuneToTempEffects(player)) return;

        if (event.getOverlay() == ForgeIngameGui.VIGNETTE_ELEMENT
        && ((BLEND_TEMP > 0 && HOT_IMMUNITY < 4) || (BLEND_TEMP < 0 && COLD_IMMUNITY < 4)))
        {
            float resistance = CSMath.blend(1, 0, BLEND_TEMP > 0 ? HOT_IMMUNITY : COLD_IMMUNITY, 0, 4);
            float opacity = CSMath.blend(0f, 1f, Math.abs(BLEND_TEMP), 50, 100) * resistance;
            float tickTime = player.tickCount + event.getPartialTicks();
            if (opacity == 0) return;
            double width = event.getWindow().getWidth();
            double height = event.getWindow().getHeight();
            double scale = event.getWindow().getGuiScale();

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            if (BLEND_TEMP > 0)
            {   float vignetteBrightness = opacity + ((float) Math.sin((tickTime + 3) / (Math.PI * 1.0132f)) / 5f - 0.2f) * opacity;
                RenderSystem.setShaderColor(0.231f, 0f, 0f, vignetteBrightness);
                RenderSystem.setShaderTexture(0, HAZE_TEXTURE);
            }
            else
            {   RenderSystem.setShaderColor(1f, 1f, 1f, opacity);
                RenderSystem.setShaderTexture(0, FREEZE_TEXTURE);
            }
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferbuilder = tesselator.getBuilder();
            bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bufferbuilder.vertex(0.0D, height / scale, -90.0D).uv(0.0F, 1.0F).endVertex();
            bufferbuilder.vertex(width / scale, height / scale, -90.0D).uv(1.0F, 1.0F).endVertex();
            bufferbuilder.vertex(width / scale, 0.0D, -90.0D).uv(1.0F, 0.0F).endVertex();
            bufferbuilder.vertex(0.0D, 0.0D, -90.0D).uv(0.0F, 0.0F).endVertex();
            tesselator.end();
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
        if (ConfigSettings.DISTORTION_EFFECTS.get() && playerTemp >= 50 && HOT_IMMUNITY < 4
        && mc.player != null && !EntityTempManager.immuneToTempEffects(mc.player))
        {
            float blur = CSMath.blend(0f, 12f, playerTemp, 50, 100) / (HOT_IMMUNITY + 1);
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
