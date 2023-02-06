package dev.momostudios.coldsweat.client.event;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.api.util.Temperature;
import dev.momostudios.coldsweat.client.gui.Overlays;
import dev.momostudios.coldsweat.config.ColdSweatConfig;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.List;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class TempEffectsClient
{
    static float BLEND_TEMP = 0;

    static float PREV_X_SWAY = 0;
    static float PREV_Y_SWAY = 0;
    static float X_SWAY_SPEED = 0;
    static float X_SWAY_BLEND = 0;
    static float Y_SWAY_SPEED = 0;
    static float Y_SWAY_BLEND = 0;

    static Uniform BLUR_DIR = null;
    static Uniform BLUR_RADIUS = null;
    static Field POST_PASSES = null;
    static boolean BLUR_APPLIED = false;

    // Sway the player's camera when the player is too hot; swaying is more drastic at higher temperatures
    @SubscribeEvent
    public static void setCamera(EntityViewRenderEvent.CameraSetup event)
    {
        Player player = Minecraft.getInstance().player;
        if (!Minecraft.getInstance().isPaused() && player != null)
        {
            // Get the FPS of the game
            float frameTime = Minecraft.getInstance().getDeltaFrameTime() * 3;
            float temp = (float) Temperature.get(player, Temperature.Type.BODY);
            // Get a blended version of the player's temperature
            // More important for fog stuff
            BLEND_TEMP += (temp - BLEND_TEMP) * frameTime / 20;

            // Camera "shivers" when temp is < -50
            if (BLEND_TEMP <= -50)
            {
                float factor = CSMath.blend(0.05f, 0f, BLEND_TEMP, -100, -50);
                double tickTime = player.tickCount + event.getPartialTicks();
                float shiverAmount = (float) (Math.sin((tickTime) * 3) * factor * (10 * frameTime) / 3);
                player.setYRot(player.getYRot() + shiverAmount);
            }
            else if (BLEND_TEMP > 50)
            {
                float factor = CSMath.blend(0, 8, BLEND_TEMP, 50, 100);
                double tickTime = player.tickCount + event.getPartialTicks();

                // Set random sway speed every once in a while
                if (Math.abs(X_SWAY_BLEND - X_SWAY_SPEED) < 0.1f)
                    X_SWAY_SPEED = (float) (Math.random() * 0.5f);
                if (Math.abs(Y_SWAY_BLEND - Y_SWAY_SPEED) < 0.1f)
                    Y_SWAY_SPEED = (float) (Math.random() * 0.5f);

                // Blend to the new sway speed
                X_SWAY_BLEND += (X_SWAY_SPEED - X_SWAY_BLEND) * frameTime / 1000;
                Y_SWAY_BLEND += (Y_SWAY_SPEED - Y_SWAY_BLEND) * frameTime / 1000;

                // Apply the sway speed to a sin function
                float xOffs = (float) (Math.sin(tickTime / 500 * X_SWAY_BLEND) * factor);
                float yOffs = (float) (Math.sin(tickTime / 500 * Y_SWAY_BLEND) * factor);

                // Apply the sway
                player.setXRot(player.getXRot() + xOffs - PREV_X_SWAY);
                player.setYRot(player.getYRot() + yOffs - PREV_Y_SWAY);

                // Save the previous sway
                PREV_X_SWAY = xOffs;
                PREV_Y_SWAY = yOffs;
            }
        }
    }

    @SubscribeEvent
    public static void renderFog(EntityViewRenderEvent event)
    {
        Player player = Minecraft.getInstance().player;
        if (player != null && BLEND_TEMP >= 50 && ColdSweatConfig.getInstance().heatstrokeFog())
        {
            if (event instanceof EntityViewRenderEvent.RenderFogEvent fog)
            {
                fog.setFarPlaneDistance(CSMath.blendLog(fog.getFarPlaneDistance(), 6f, BLEND_TEMP, 50f, 90f));
                fog.setNearPlaneDistance(CSMath.blendLog(fog.getNearPlaneDistance(), 2f, BLEND_TEMP, 50f, 90f));
                fog.setCanceled(true);
            }
            else if (event instanceof EntityViewRenderEvent.FogColors fogColor)
            {
                fogColor.setRed(CSMath.blend(fogColor.getRed(), 0.01f, BLEND_TEMP, 50, 90));
                fogColor.setGreen(CSMath.blend(fogColor.getGreen(), 0.01f, BLEND_TEMP, 50, 90));
                fogColor.setBlue(CSMath.blend(fogColor.getBlue(), 0.05f, BLEND_TEMP, 50, 90));
            }
        }
    }

    static ResourceLocation HAZE_TEXTURE = new ResourceLocation(ColdSweat.MOD_ID, "textures/gui/overlay/haze.png");

    @SubscribeEvent
    public static void vignette(RenderGameOverlayEvent.PreLayer event)
    {
        Player player = Minecraft.getInstance().player;
        if (player != null && event.getOverlay() == ForgeIngameGui.VIGNETTE_ELEMENT)
        {
            float opacity = CSMath.blend(0f, 1f, Math.abs(BLEND_TEMP), 50, 100);
            float tickTime = player.tickCount + event.getPartialTicks();
            if (opacity == 0) return;
            double width = event.getWindow().getWidth();
            double height = event.getWindow().getHeight();
            double scale = event.getWindow().getGuiScale();

            float vignetteBrightness = opacity + ((float) Math.sin((tickTime + 3) / (Math.PI * 1.0132f)) / 5f - 0.2f) * opacity;
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            if (BLEND_TEMP < 0)
                RenderSystem.setShaderColor(0.690f, 0.894f, 0.937f, vignetteBrightness);
            else
                RenderSystem.setShaderColor(0.231f, 0f, 0f, vignetteBrightness);

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, HAZE_TEXTURE);
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

    static
    {
        try
        {
            POST_PASSES = ObfuscationReflectionHelper.findField(PostChain.class, "f_110009_");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @SubscribeEvent
    public static void onRenderBlur(RenderLevelStageEvent event)
    {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER)
        {
            Minecraft mc = Minecraft.getInstance();
            try
            {
                float playerTemp = (float) Overlays.BODY_TEMP;
                if (playerTemp >= 50)
                {
                    if (!BLUR_APPLIED)
                    {
                        mc.gameRenderer.loadEffect(new ResourceLocation("shaders/post/blobs2.json"));
                        BLUR_RADIUS = ((List<PostPass>) POST_PASSES.get(mc.gameRenderer.currentEffect())).get(0).getEffect().getUniform("Radius");
                        BLUR_APPLIED = true;
                    }
                    float blur = CSMath.blend(0f, 7f, playerTemp, 50, 100);
                    BLUR_RADIUS.set(blur);
                }
                else if (BLUR_APPLIED)
                {
                    BLUR_RADIUS.set(0f);
                    BLUR_APPLIED = false;
                }
            } catch (Exception ignored) { ignored.printStackTrace(); }
        }
    }
}
