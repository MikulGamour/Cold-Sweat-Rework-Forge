package com.momosoftworks.coldsweat.client.renderer.model.armor;

import com.momosoftworks.coldsweat.ColdSweat;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class ChameleonHelmetModel<T extends LivingEntity> extends HumanoidModel<T>
{
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ColdSweat.MOD_ID, "chameleon_helmet"), "main");

	public ChameleonHelmetModel(ModelPart root)
    {   super(root);
	}

	public static LayerDefinition createArmorLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(new CubeDeformation(0f), 1.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();
        float scale = 0.8f;

        PartDefinition head = partdefinition.getChild("head");

        PartDefinition head_armor = head.addOrReplaceChild("head_armor", CubeListBuilder.create().texOffs(16, 80).addBox(-4.0F, -6.5F, -7.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(scale))
                .texOffs(16, 97).addBox(4.8F, -1F, -1.52F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.075f, 0.33f, 0.33f))
                .texOffs(16, 97).mirror().addBox(-5.8F, -1F, -1.52F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.075f, 0.33f, 0.33f)).mirror(false), PartPose.offset(0.0F, -1.5F, 3.0F));

        PartDefinition right_frill = head_armor.addOrReplaceChild("right_frill",
                                                                  CubeListBuilder.create().texOffs(24, 58)
                                                                          .addBox(0.0F, -5.0F, 1.1F, 0.0F, 12.0F, 8.0F, new CubeDeformation(0, scale*0.75f, scale*1.5f)),
                                                                  PartPose.offsetAndRotation(0.0F, -6.5F, -3.0F, 0.0F, -0.3927F, 0.0F));

        PartDefinition left_frill = head_armor.addOrReplaceChild("left_frill",
                                                                 CubeListBuilder.create().texOffs(24, 58)
                                                                         .addBox(0.0F, -5.0F, 1.1F, 0.0F, 12.0F, 8.0F, new CubeDeformation(0, scale*0.75f, scale*1.5f)),
                                                                 PartPose.offsetAndRotation(0.0F, -6.5F, -3.0F, 0.0F, 0.3927F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 128);
	}
}