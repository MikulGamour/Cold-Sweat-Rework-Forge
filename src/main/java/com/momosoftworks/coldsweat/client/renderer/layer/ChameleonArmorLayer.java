package com.momosoftworks.coldsweat.client.renderer.layer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.insulation.AdaptiveInsulation;
import com.momosoftworks.coldsweat.common.item.ChameleonArmorItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ChameleonArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> extends HumanoidArmorLayer<T, M, A>
{
    public static final ResourceLocation GREEN_LAYER_1_LOCATION = new ResourceLocation(ColdSweat.MOD_ID, "textures/models/armor/chameleon_layer_1.png");
    public static final ResourceLocation GREEN_LAYER_2_LOCATION = new ResourceLocation(ColdSweat.MOD_ID, "textures/models/armor/chameleon_layer_2.png");
    public static final ResourceLocation RED_LAYER_1_LOCATION = new ResourceLocation(ColdSweat.MOD_ID, "textures/models/armor/chameleon_layer_1_red.png");
    public static final ResourceLocation RED_LAYER_2_LOCATION = new ResourceLocation(ColdSweat.MOD_ID, "textures/models/armor/chameleon_layer_2_red.png");
    public static final ResourceLocation BLUE_LAYER_1_LOCATION = new ResourceLocation(ColdSweat.MOD_ID, "textures/models/armor/chameleon_layer_1_blue.png");
    public static final ResourceLocation BLUE_LAYER_2_LOCATION = new ResourceLocation(ColdSweat.MOD_ID, "textures/models/armor/chameleon_layer_2_blue.png");

    public ChameleonArmorLayer(RenderLayerParent<T, M> renderer, ModelManager modelManager)
    {
        super(renderer, null, null, modelManager);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, T entity,
                       float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks,
                       float pNetHeadYaw, float pHeadPitch)
    {
        this.renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.CHEST, light);
        this.renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.LEGS, light);
        this.renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.FEET, light);
        this.renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.HEAD, light);
    }

    protected void renderArmorPiece(PoseStack poseStack, MultiBufferSource buffer, T livingEntity, EquipmentSlot slot, int light)
    {
        ItemStack itemstack = livingEntity.getItemBySlot(slot);
        Item item = itemstack.getItem();
        if (item instanceof ChameleonArmorItem armorItem)
        {
            if (armorItem.getEquipmentSlot() == slot)
            {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                // Get model data
                boolean isInnerModel = this.usesInnerModel(slot);
                HumanoidModel<T> model = (HumanoidModel<T>) armorItem.getRealArmorModel(livingEntity, itemstack, slot);
                // Set transforms & visibility properties
                this.getParentModel().copyPropertiesTo(model);
                this.setPartVisibilities(model, slot);
                this.renderModel(poseStack, buffer, light, armorItem, model, isInnerModel, 1.0f, 1.0f, 1.0f, 1.0f, Color.GREEN.getLayer(slot));
                // Render overlay texture (red/blue)
                double adaptiveFactor = AdaptiveInsulation.getFactorFromNBT(itemstack);
                ResourceLocation overlay = adaptiveFactor < 0 ? Color.BLUE.getLayer(slot) : Color.RED.getLayer(slot);
                float alpha = (float) Math.abs(adaptiveFactor);
                this.renderModel(poseStack, buffer, light, armorItem, model, isInnerModel, 1.0f, 1.0f, 1.0f, alpha, overlay);
                // Render enchantment glint
                if (itemstack.hasFoil())
                {   this.renderGlint(poseStack, buffer, light, model);
                }
                RenderSystem.disableBlend();
            }
        }
    }

    protected boolean usesInnerModel(EquipmentSlot slot)
    {   return slot == EquipmentSlot.LEGS;
    }

    protected void renderModel(PoseStack poseStack, MultiBufferSource buffer, int light, ArmorItem armorItem, Model model,
                               boolean showGlint, float red, float green, float blue, float alpha, ResourceLocation armorResource)
    {
        VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.entityTranslucent(armorResource));
        model.renderToBuffer(poseStack, vertexconsumer, light, OverlayTexture.NO_OVERLAY, red, green, blue, alpha);
    }

    protected void renderGlint(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Model model)
    {
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.armorEntityGlint()), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    protected void setPartVisibilities(HumanoidModel<T> pModel, EquipmentSlot pSlot)
    {
        pModel.setAllVisible(false);
        switch (pSlot)
        {
            case HEAD ->
            {   pModel.head.visible = true;
                pModel.hat.visible = true;
            }
            case CHEST ->
            {   pModel.body.visible = true;
                pModel.rightArm.visible = true;
                pModel.leftArm.visible = true;
            }
            case LEGS ->
            {   pModel.body.visible = true;
                pModel.rightLeg.visible = true;
                pModel.leftLeg.visible = true;
            }
            case FEET ->
            {   pModel.rightLeg.visible = true;
                pModel.leftLeg.visible = true;
            }
        }
    }

    public enum Color
    {
        GREEN(GREEN_LAYER_1_LOCATION, GREEN_LAYER_2_LOCATION),
        RED(RED_LAYER_1_LOCATION, RED_LAYER_2_LOCATION),
        BLUE(BLUE_LAYER_1_LOCATION, BLUE_LAYER_2_LOCATION);

        private final ResourceLocation layer1;
        private final ResourceLocation layer2;

        Color(ResourceLocation layer1, ResourceLocation layer2)
        {
            this.layer1 = layer1;
            this.layer2 = layer2;
        }

        public ResourceLocation getLayer1()
        {   return layer1;
        }

        public ResourceLocation getLayer2()
        {   return layer2;
        }

        public ResourceLocation getLayer(EquipmentSlot slot)
        {   return slot == EquipmentSlot.LEGS ? layer2 : layer1;
        }
    }
}
