package com.momosoftworks.coldsweat.client.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.temperature.modifier.WaterTempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import oshi.util.tuples.Triplet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@EventBusSubscriber(Dist.CLIENT)
public class WetnessRenderer
{
    private static final ResourceLocation WATER_DROP = ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "textures/gui/overlay/droplet.png");
    private static final ResourceLocation WATER_DROP_TRAIL = ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "textures/gui/overlay/droplet_trail.png");
    private static final List<Droplet> WATER_DROPS = new ArrayList<>();
    private static final List<Triplet<Vector2i, Float, Integer>> TRAILS = new ArrayList<>();
    private static boolean WAS_SUBMERGED = false;

    @SubscribeEvent
    public static void updateSkyBrightness(ClientTickEvent.Pre event)
    {
        Level level = Minecraft.getInstance().level;
        if (level != null)
        {   level.updateSkyBrightness();
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiEvent.Pre event)
    {
        if (!ConfigSettings.SHOW_WATER_EFFECT.get()) return;

        Minecraft mc = Minecraft.getInstance();
        float frametime = mc.getTimer().getRealtimeDeltaTicks();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        boolean paused = mc.isPaused();
        int uiScale = mc.options.guiScale().get();
        if (uiScale == 0)
        {   uiScale = mc.getWindow().calculateScale(0, mc.isEnforceUnicode());
        }

        Player player = mc.player;
        if (player == null) return;

        BlockPos playerPos = player.blockPosition();
        float playerYVelocity = (float) (player.position().y - player.yOld);
        boolean isSubmerged = player.canSwimInFluidType(player.getEyeInFluidType());

        int light = player.level().getMaxLocalRawBrightness(playerPos.above());
        if (player.hasEffect(MobEffects.NIGHT_VISION)) light = 15;
        float brightness = CSMath.blend(0, 1, light, 0, 15);

        float tempMult = (float) CSMath.blend(0.3, 6, Temperature.get(player, Temperature.Trait.WORLD), ConfigSettings.MIN_TEMP.get(), ConfigSettings.MAX_TEMP.get() * 2);

        // Clear water drops when the player submerges
        if (isSubmerged && !paused)
        {
            TRAILS.clear();
            for (Droplet drop : WATER_DROPS)
            {
                drop.alpha -= 0.6f * frametime;
                float xMoveDir = drop.position.x < screenWidth / 2f ? -1 : 1;
                float yMoveDir = drop.position.y < screenHeight / 2f ? -1 : 1;
                drop.position.add(new Vector2f(xMoveDir, yMoveDir).mul(200 * -playerYVelocity * frametime));
            }
        }

        // Get the player's wetness level
        double wetness = Temperature.getModifier(mc.player, Temperature.Trait.WORLD, WaterTempModifier.class).map(mod ->
                         {  return CSMath.blend(0, 1, mod.getWetness(), 0, mod.getMaxStrength(player));
                         }).orElse(0d);

        // Spawn a bunch of droplets when the player exits the water
        boolean justExitedWater = WAS_SUBMERGED && !isSubmerged;
        if (justExitedWater)
        {
            for (int i = 0; i < 15; i++)
            {
                Droplet newDrop = createDrop(screenWidth, screenHeight);
                newDrop.yMotion = getRandomVelocity(frametime) / 2 + 0.3f;
                newDrop.position.y = (float) Math.random() * screenHeight;
                WATER_DROPS.add(newDrop);
                int streakLength = (int) (Math.random() * 5) + 5;
                int x = (int)newDrop.position.x;
                int y = (int)newDrop.position.y;
                for (int j = 1; j < streakLength; j++)
                {
                    TRAILS.add(new Triplet<>(new Vector2i(x, y - j),
                                             CSMath.blend(newDrop.alpha * 0.8f, 0, j, 1, streakLength),
                                             newDrop.size / 2));
                }
            }
        }
        WAS_SUBMERGED = isSubmerged;

        // Spawn droplets randomly when the player is wet
        if (!paused && !isSubmerged && wetness > 0.01f && ((float) Math.random() * 0.05) < 0.0015f * wetness * (frametime * 2)
        && WATER_DROPS.size() < 5)
        {
            WATER_DROPS.add(createDrop(screenWidth, screenHeight));
        }

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, WATER_DROP);

        GuiGraphics graphics = event.getGuiGraphics();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        // Handle rendering & movement of water drops
        for (int i = 0; i < WATER_DROPS.size(); i++)
        {
            Droplet drop = WATER_DROPS.get(i);
            Vector2f pos = drop.position;
            float alpha = drop.alpha;
            int size = drop.size / uiScale * 3;

            if (alpha > 0)
            {
                // Render the water drop
                renderQuad(graphics, bufferBuilder, (int) CSMath.roundNearest(pos.x, 3f/uiScale), (int)pos.y, size, size, 0, 0, 1, 1,
                           brightness, brightness, brightness, alpha);

                // Update the drop's position and alpha
                if (!paused)
                {
                    // Fade out
                    if (wetness <= 0)
                    {   drop.alpha -= 0.08f * frametime;
                    }
                    else
                    {   drop.alpha -= 0.003f * frametime * tempMult;
                    }
                    // Recalculate the y velocity every so often
                    if (drop.yMotionUpdateCooldown <= 0)
                    {
                        drop.yMotionUpdateCooldown = (float) Math.random() * 16f + 8f;
                        drop.yMotion = getRandomVelocity(frametime);
                    }
                    else drop.yMotionUpdateCooldown -= frametime;

                    // Movement due to player motion
                    float dropMoveFromPlayerLook = -(player.yHeadRot - player.yHeadRotO) / 20;
                    dropMoveFromPlayerLook = (float) CSMath.shrink(dropMoveFromPlayerLook, 0.5f);
                    drop.xVelocity = (float) CSMath.maxAbs(dropMoveFromPlayerLook * (Math.random() * 0.2), drop.xVelocity);
                    drop.xVelocity /= 1 + 0.6f * frametime;

                    // Randomly change the x motion
                    if (drop.XMotionUpdateCooldown <= 0)
                    {
                        drop.XMotionUpdateCooldown = (float) Math.random() * 8f + 4f;
                        drop.xMotion = (float) Math.random() * 0.02f - 0.01f;
                    }
                    drop.XMotionUpdateCooldown -= frametime;

                    int oldY = (int)pos.y;
                    // Move the drop
                    if (!isSubmerged)
                    {   drop.position.add(new Vector2f(drop.xMotion * drop.yMotion * 20 + drop.xVelocity, drop.yMotion).div(uiScale).mul(3));
                    }

                    // Add a trail behind the drop
                    for (int j = 0; j < Math.max(0, (int) (pos.y - oldY)); j++)
                    {
                        TRAILS.add(new Triplet<>(new Vector2i((int)pos.x, oldY + j), alpha, size));
                    }
                }

                // Wrap drops around the screen
                if (pos.x < -20)
                {   pos.x = screenWidth + 20;
                }
                else if (pos.x > screenWidth + 20)
                {   pos.x = -20;
                }
                // Remove drops that fall off the bottom of the screen
                if (pos.y > screenHeight)
                {   WATER_DROPS.remove(drop);
                    i--;
                }
            }
            // Remove drops that have faded out
            else
            {   WATER_DROPS.remove(drop);
                i--;
            }
        }
        MeshData meshData = bufferBuilder.build();
        if (meshData != null)
        {   BufferUploader.drawWithShader(meshData);
        }

        // Render water drop trails
        bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, WATER_DROP_TRAIL);
        for (int i = 0; i < TRAILS.size(); i++)
        {
            Triplet<Vector2i, Float, Integer> trail = TRAILS.get(i);
            Vector2i pos = trail.getA();
            float alpha = trail.getB();
            int size = trail.getC();

            if (alpha > 0)
            {
                renderQuad(graphics, bufferBuilder, (int) CSMath.roundNearest(pos.x, 3f/uiScale * 4), pos.y, size, 1, 0, 0, 1, 1,
                           brightness, brightness, brightness, alpha);
                if (!paused)
                {   TRAILS.set(i, new Triplet<>(new Vector2i(pos.x, pos.y), alpha - 0.045f * frametime, size));
                }
            }
            else
            {   TRAILS.remove(trail);
                i--;
            }
        }
        meshData = bufferBuilder.build();
        if (meshData != null)
        {   BufferUploader.drawWithShader(meshData);
        }
    }

    private static float getRandomVelocity(float frametime)
    {
        return (float) Math.min(0.7f * frametime * 20, (Math.pow(Math.random() * 5 + 0.1f, 3) * frametime) / 4f);
    }

    private static Droplet createDrop(int screenWidth, int screenHeight)
    {
        int size = new Random().nextInt(40, 48);
        return new Droplet(new Vector2f((int) (Math.random() * screenWidth), -size), 1f, size);
    }

    private static void renderQuad(GuiGraphics graphics, BufferBuilder bufferBuilder, int x, int y,
                                   int width, int height, float u, float v, float uWidth, float vHeight,
                                   float r, float g, float b, float a)
    {
        Matrix4f lastPose = graphics.pose().last().pose();
        bufferBuilder.addVertex(lastPose, x, y, 0).setUv(u, v).setColor(r, g, b, a);
        bufferBuilder.addVertex(lastPose, x, y + height, 0).setUv(u, v + vHeight).setColor(r, g, b, a);
        bufferBuilder.addVertex(lastPose, x + width, y + height, 0).setUv(u + uWidth, v + vHeight).setColor(r, g, b, a);
        bufferBuilder.addVertex(lastPose, x + width, y, 0).setUv(u + uWidth, v).setColor(r, g, b, a);
    }

    protected static class Droplet
    {
        public Vector2f position;
        public float alpha;
        public int size;
        public float yMotion = (float) Math.random() * 0.05f + 0.05f;
        public float xMotion = (float) Math.random() * 0.02f - 0.01f;
        public float xVelocity = 0;
        public float yMotionUpdateCooldown = (float) Math.random() * 16f + 8f;
        public float XMotionUpdateCooldown = 16;

        public Droplet(Vector2f position, float alpha, int size)
        {
            this.position = position;
            this.alpha = alpha;
            this.size = size;
        }
    }
}
