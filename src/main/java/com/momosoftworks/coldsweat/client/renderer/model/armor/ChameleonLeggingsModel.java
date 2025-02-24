package com.momosoftworks.coldsweat.client.renderer.model.armor;

import com.momosoftworks.coldsweat.ColdSweat;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class ChameleonLeggingsModel<T extends LivingEntity> extends HumanoidModel<T>
{
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ColdSweat.MOD_ID, "chameleon_leggings"), "main");

	public ChameleonLeggingsModel(ModelPart root)
    {   super(root);
	}

	public static LayerDefinition createArmorLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(new CubeDeformation(0f), 1.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();
        float scale = 0.6f;

        PartDefinition body = partdefinition.getChild("body");
        PartDefinition right_leg = partdefinition.getChild("right_leg");
        PartDefinition left_leg = partdefinition.getChild("left_leg");

        PartDefinition waist_armor = body.addOrReplaceChild("waist_armor", CubeListBuilder.create().texOffs(16, 112).addBox(-4.0F, -24.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(scale)), PartPose.offset(0.0F, 23.0F, 0.0F));

        PartDefinition right_legging = right_leg.addOrReplaceChild("right_legging", CubeListBuilder.create().texOffs(0, 112).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(scale)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition left_legging = left_leg.addOrReplaceChild("left_legging", CubeListBuilder.create().texOffs(0, 112).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(scale)).mirror(false), PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 128);
	}
}