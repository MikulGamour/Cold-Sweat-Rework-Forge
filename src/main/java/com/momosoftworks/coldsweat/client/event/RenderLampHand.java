package com.momosoftworks.coldsweat.client.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.momosoftworks.coldsweat.core.init.ModItems;
import com.momosoftworks.coldsweat.util.entity.EntityHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.event.RenderHandEvent;

import java.lang.reflect.Method;

@EventBusSubscriber(Dist.CLIENT)
public class RenderLampHand
{
    static Method RENDER_ITEM = ObfuscationReflectionHelper.findMethod(ItemInHandRenderer.class, "m_109371_",
                                                                       AbstractClientPlayer.class, float.class, float.class,
                                                                       InteractionHand.class, float.class, ItemStack.class,
                                                                       float.class, PoseStack.class, MultiBufferSource.class, int.class);
    static
    {
        RENDER_ITEM.setAccessible(true);
    }

    @SubscribeEvent
    public static void onHandRender(RenderHandEvent event)
    {
        if (event.getItemStack().getItem() == ModItems.SOULSPRING_LAMP.value())
        {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;

            PoseStack ms = event.getPoseStack();
            boolean isRightHand = EntityHelper.getArmFromHand(event.getHand(), player) == HumanoidArm.RIGHT;

            event.setCanceled(true);

            ms.pushPose();
            ms.mulPose(Axis.YP.rotationDegrees(-((float) Math.cos(Math.min(event.getSwingProgress() * 1.3, 1) * Math.PI * 2) * 5 - 5)));
            ms.mulPose(Axis.ZP.rotationDegrees(-((float) Math.cos(Math.min(event.getSwingProgress() * 1.3, 1) * Math.PI * 2) * 10 - 10)));

            ms.translate
            (
                0.0d,
                Math.cos(Math.min(event.getSwingProgress() * 1.1, 1) * Math.PI * 2 - Math.PI * 0.5) * 0.1
                    + (event.getEquipProgress() == 0 ? (Math.cos(event.getSwingProgress() * Math.PI * 2) - 1) * 0.2 : 0),
                Math.cos(Math.min(event.getSwingProgress() * 1.1, 1) * Math.PI * 2) * -0.0 - 0
            );

            ms.pushPose();
            ms.translate(isRightHand ? 0.75 : -0.75, -0.3, -0.36);
            ms.scale(0.75f, 0.8f, 0.72f);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            PlayerRenderer handRenderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
            VertexConsumer buffer = event.getMultiBufferSource().getBuffer(handRenderer.getModel().renderType(handRenderer.getTextureLocation(player)));
            if (isRightHand)
            {
                ms.mulPose(Axis.ZP.rotationDegrees(98));
                ms.mulPose(Axis.YP.rotationDegrees(170.0F));
                ms.mulPose(Axis.XP.rotationDegrees(90.0F));
                ms.translate(event.getEquipProgress() * 1, -event.getEquipProgress() * 0.2, -event.getEquipProgress() * 0.2);

                if (player.isInvisible()) buffer.setColor(1, 1, 1, 0.25f);
                handRenderer.renderRightHand(ms, event.getMultiBufferSource(), event.getPackedLight(), player);
                RenderSystem.setShaderColor(1, 1, 1, 1);
            }
            else
            {
                ms.mulPose(Axis.ZP.rotationDegrees(-98));
                ms.mulPose(Axis.YP.rotationDegrees(190.0F));
                ms.mulPose(Axis.XP.rotationDegrees(90.0F));
                ms.translate(-event.getEquipProgress() * 1, -event.getEquipProgress() * 0.2, -event.getEquipProgress() * 0.2);

                if (player.isInvisible()) buffer.setColor(1, 1, 1, 0.25f);
                handRenderer.renderLeftHand(ms, event.getMultiBufferSource(), event.getPackedLight(), player);
                RenderSystem.setShaderColor(1, 1, 1, 1);
            }
            RenderSystem.disableBlend();
            ms.popPose();

            ms.pushPose();
            ms.translate(-event.getEquipProgress() * 0.05, -event.getEquipProgress() * 0.15 + (isRightHand ? 0 : -0.075), -0.05);
            try
            {
                RENDER_ITEM.invoke(Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer(),
                                  player,
                                  event.getInterpolatedPitch(),
                                  event.getPartialTick(),
                                  event.getHand(),
                                  0,
                                  event.getItemStack(),
                                  event.getEquipProgress(),
                                  ms,
                                  event.getMultiBufferSource(),
                                  event.getPackedLight());
            }
            catch (Exception ignored) {}

            ms.popPose();
            ms.popPose();
        }
    }
}
