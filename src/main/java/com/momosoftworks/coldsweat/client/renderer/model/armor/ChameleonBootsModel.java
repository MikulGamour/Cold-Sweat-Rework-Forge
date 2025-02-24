package com.momosoftworks.coldsweat.client.renderer.model.armor;

import com.momosoftworks.coldsweat.ColdSweat;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class ChameleonBootsModel<T extends LivingEntity> extends HumanoidModel<T>
{
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ColdSweat.MOD_ID, "chameleon_boots"), "main");

	public ChameleonBootsModel(ModelPart root)
    {   super(root);
	}

	public static LayerDefinition createArmorLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(new CubeDeformation(0f), 1.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();
        float scale = 0.8f;

        PartDefinition right_leg = partdefinition.getChild("right_leg");
        PartDefinition left_leg = partdefinition.getChild("left_leg");

        PartDefinition right_boot = right_leg.addOrReplaceChild("right_boot", CubeListBuilder.create().texOffs(48, 80).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(scale)), PartPose.offset(0.0F, -0.5F, 0.0F));

        PartDefinition left_boot = left_leg.addOrReplaceChild("left_boot", CubeListBuilder.create().texOffs(48, 80).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(scale)).mirror(false), PartPose.offset(0.0F, -0.5F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 128);
	}
}