package com.momosoftworks.coldsweat.client.renderer.model.armor;

import com.momosoftworks.coldsweat.ColdSweat;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class EmptyArmorModel<T extends LivingEntity> extends HumanoidModel<T>
{
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ColdSweat.MOD_ID, "empty"), "main");

	public EmptyArmorModel(ModelPart root)
    {   super(root);
	}

	public static LayerDefinition createArmorLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(new CubeDeformation(0f), 1.0F);

        return LayerDefinition.create(meshdefinition, 64, 128);
	}
}