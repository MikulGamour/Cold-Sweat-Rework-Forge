package com.momosoftworks.coldsweat.client.renderer.model.armor;

import com.momosoftworks.coldsweat.ColdSweat;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class ChameleonChestplateModel<T extends LivingEntity> extends HumanoidModel<T>
{
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ColdSweat.MOD_ID, "chameleon_chestplate"), "main");

	public ChameleonChestplateModel(ModelPart root)
    {   super(root);
	}

	public static LayerDefinition createArmorLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(new CubeDeformation(0f), 1.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();
        float scale = 0.8f;

        PartDefinition body = partdefinition.getChild("body");
        PartDefinition right_arm = partdefinition.getChild("right_arm");
        PartDefinition left_arm = partdefinition.getChild("left_arm");

        PartDefinition chest_armor = body.addOrReplaceChild("chest_armor", CubeListBuilder.create().texOffs(0, 64).addBox(-4.0F, -24.5F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(scale)), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition right_arm_armor = right_arm.addOrReplaceChild("right_arm_armor", CubeListBuilder.create().texOffs(0, 80).addBox(-3.25F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(scale, scale*2, scale)), PartPose.offset(0.0F, -1.5F, 0.0F));

        PartDefinition left_arm_armor = left_arm.addOrReplaceChild("left_arm_armor", CubeListBuilder.create().texOffs(0, 80).mirror().addBox(-0.75F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(scale, scale*2, scale)).mirror(false), PartPose.offset(0.0F, -1.5F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 128);
	}
}