package com.momosoftworks.coldsweat.client.renderer.model.entity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.client.renderer.ChameleonAnimations;
import com.momosoftworks.coldsweat.client.renderer.entity.ChameleonEntityRenderer;
import com.momosoftworks.coldsweat.client.renderer.animation.AnimationManager;
import com.momosoftworks.coldsweat.common.entity.Chameleon;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChameleonModel<T extends Chameleon> extends AgeableListModel<T>
{
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ColdSweat.MOD_ID, "chameleon"), "main");

	final Map<String, ModelPart> modelParts;
	final ModelPart body;
	final ModelPart head;
	Chameleon chameleon;
	boolean tongueVisible = false;

	public ChameleonModel(ModelPart root)
	{
		super(RenderType::entityTranslucent, true, 4.75f, 0.75f, 1.8F, 1.6F, 14.0F);
		this.body = root.getChild("Body");
		this.head = root.getChild("Head");
		head.y = 19.2f;
		body.y -= 0.8f;
		modelParts = AnimationManager.getChildrenMap(root);

		AnimationManager.storeDefaultPoses(ModEntities.CHAMELEON, modelParts);
	}

	public static LayerDefinition createBodyLayer()
	{
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Body = partdefinition.addOrReplaceChild("Body", CubeListBuilder.create().texOffs(1, 16).addBox(-2.0F, -8.0F, -3.0F, 4.0F, 7.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition Head = partdefinition.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -7.0F, 4.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -5.0F, -3.0F));

		PartDefinition TopFrill = Head.addOrReplaceChild("TopFrill", CubeListBuilder.create().texOffs(30, 0).addBox(-1.0F, 1.4F, 0.05F, 2.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.0F, -5.0F, 0.7418F, 0.0F, 0.0F));

		PartDefinition Jaw = Head.addOrReplaceChild("Jaw", CubeListBuilder.create().texOffs(15, 7).addBox(-2.0F, 0.0F, -7.0F, 4.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(18, 15).addBox(0.0F, 1.0F, -6.0F, 0.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 2.0F, 0.0F));

		PartDefinition RightEye = Head.addOrReplaceChild("RightEye", CubeListBuilder.create().texOffs(15, 1).addBox(-1.5F, -1.5F, -1.5F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.5F, 0.0F, -3.5F));

		PartDefinition LeftEye = Head.addOrReplaceChild("LeftEye", CubeListBuilder.create().texOffs(15, 1).mirror().addBox(-0.0005F, -1.4782F, -1.634F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(1.0005F, -0.0218F, -3.366F));

		PartDefinition Tongue1 = Head.addOrReplaceChild("Tongue1", CubeListBuilder.create().texOffs(27, 25).addBox(-1.0F, 0.0F, -7.0F, 2.0F, 0.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 2.0F, 0.0F));

		PartDefinition Tongue2 = Tongue1.addOrReplaceChild("Tongue2", CubeListBuilder.create().texOffs(27, 25).addBox(-1.0F, -3.0F, -10.0F, 2.0F, 0.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 3.0F, 3.0F));

		PartDefinition Tongue3 = Tongue2.addOrReplaceChild("Tongue3", CubeListBuilder.create().texOffs(27, 25).addBox(-1.0F, -3.0F, -10.0F, 2.0F, 0.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition RightFrontLeg = Body.addOrReplaceChild("RightFrontLeg", CubeListBuilder.create().texOffs(38, 25).mirror().addBox(-4.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-2.0F, -3.0F, -1.0F));

		PartDefinition RightFrontLeg2 = RightFrontLeg.addOrReplaceChild("RightFrontLeg2", CubeListBuilder.create().texOffs(38, 29).mirror().addBox(-4.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-4.0F, 0.0F, 0.0F));

		PartDefinition RightFrontFoot1 = RightFrontLeg2.addOrReplaceChild("RightFrontFoot1", CubeListBuilder.create().texOffs(28, 27).mirror().addBox(0.0F, -3.0F, -1.0F, 0.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-4.0F, 0.0F, 0.0F));

		PartDefinition RightFrontFoot2 = RightFrontLeg2.addOrReplaceChild("RightFrontFoot2", CubeListBuilder.create().texOffs(28, 24).mirror().addBox(0.0F, 0.0F, -1.0F, 0.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-4.0F, 0.0F, 0.0F));

		PartDefinition LeftFrontLeg = Body.addOrReplaceChild("LeftFrontLeg", CubeListBuilder.create().texOffs(38, 25).addBox(0.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, -3.0F, -1.0F));

		PartDefinition LeftFrontLeg2 = LeftFrontLeg.addOrReplaceChild("LeftFrontLeg2", CubeListBuilder.create().texOffs(38, 29).addBox(0.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, 0.0F, 0.0F));

		PartDefinition LeftFrontFoot1 = LeftFrontLeg2.addOrReplaceChild("LeftFrontFoot1", CubeListBuilder.create().texOffs(28, 27).addBox(0.0F, -3.0F, -1.0F, 0.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, 0.0F, 0.0F));

		PartDefinition LeftFrontFoot2 = LeftFrontLeg2.addOrReplaceChild("LeftFrontFoot2", CubeListBuilder.create().texOffs(28, 24).addBox(0.0F, 0.0F, -1.0F, 0.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, 0.0F, 0.0F));

		PartDefinition RightBackLeg = Body.addOrReplaceChild("RightBackLeg", CubeListBuilder.create().texOffs(38, 25).mirror().addBox(-4.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-2.0F, -3.0F, 4.0F));

		PartDefinition RightBackLeg2 = RightBackLeg.addOrReplaceChild("RightBackLeg2", CubeListBuilder.create().texOffs(38, 29).mirror().addBox(-4.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-4.0F, 0.0F, 0.0F));

		PartDefinition RightBackFoot1 = RightBackLeg2.addOrReplaceChild("RightBackFoot1", CubeListBuilder.create().texOffs(28, 27).mirror().addBox(0.0F, -3.0F, -1.0F, 0.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-4.0F, 0.0F, 0.0F));

		PartDefinition RightBackFoot2 = RightBackLeg2.addOrReplaceChild("RightBackFoot2", CubeListBuilder.create().texOffs(28, 24).mirror().addBox(0.0F, 0.0F, -1.0F, 0.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-4.0F, 0.0F, 0.0F));

		PartDefinition LeftBackLeg = Body.addOrReplaceChild("LeftBackLeg", CubeListBuilder.create().texOffs(38, 25).addBox(0.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, -3.0F, 4.0F));

		PartDefinition LeftBackLeg2 = LeftBackLeg.addOrReplaceChild("LeftBackLeg2", CubeListBuilder.create().texOffs(38, 29).addBox(0.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, 0.0F, 0.0F));

		PartDefinition LeftBackFoot1 = LeftBackLeg2.addOrReplaceChild("LeftBackFoot1", CubeListBuilder.create().texOffs(28, 27).addBox(0.0F, -3.0F, -1.0F, 0.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, 0.0F, 0.0F));

		PartDefinition LeftBackFoot2 = LeftBackLeg2.addOrReplaceChild("LeftBackFoot2", CubeListBuilder.create().texOffs(27, 24).addBox(0.0F, 0.0F, -1.0F, 0.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, 0.0F, 0.0F));

		PartDefinition Tail = Body.addOrReplaceChild("Tail", CubeListBuilder.create().texOffs(34, 18).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -3.5F, 6.0F));

		PartDefinition Tail2 = Tail.addOrReplaceChild("Tail2", CubeListBuilder.create().texOffs(38, 13).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 4.0F));

		PartDefinition Tail3 = Tail2.addOrReplaceChild("Tail3", CubeListBuilder.create().texOffs(0, 17).addBox(-0.5F, -1.0F, 0.0F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 3.0F));

		return LayerDefinition.create(meshdefinition, 48, 32);
	}

	@Override
	public void setupAnim(@NotNull T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
	{
		chameleon = entity;
		AnimationManager.loadAnimationStates(entity, modelParts);

		float tickDelta = Minecraft.getInstance().getDeltaFrameTime();
		float partialTick = Minecraft.getInstance().getFrameTime();
		ModelPart head = modelParts.get("Head");
		ModelPart rightEye = modelParts.get("RightEye");
		ModelPart leftEye = modelParts.get("LeftEye");

		// get the pitch (up/down) of the head in Radians
		float desiredXHead = CSMath.toRadians(headPitch);
		boolean hasLookTarget = entity.isWalking() || entity.getEatTimer() > 0;
		// Blend smoothly to the desired pitch using frameTime
		// There is no slop in head rotation if the Chameleon is moving
		entity.xRotHead += ((hasLookTarget ? desiredXHead : CSMath.clamp(entity.xRotHead, desiredXHead - 0.4f, desiredXHead + 0.4f)) - entity.xRotHead) * tickDelta;

		// Get the yaw (left/right) of the head in Radians
		float desiredYHead = CSMath.toRadians(netHeadYaw);
		// Blend smoothly to the desired yaw using frameTime
		// There is no slop in head rotation if the Chameleon is moving
		entity.yRotHead += ((hasLookTarget ? desiredYHead : CSMath.clamp(entity.yRotHead, desiredYHead - 0.6f, desiredYHead + 0.6f)) - entity.yRotHead) * tickDelta;

		// Move the right eye if the Chameleon is looking at an object to its right
		if (head.yRot < desiredYHead)
		{
			// If there is slop in the head rotation, make up the difference with the eye
			entity.yRotRightEye += (-CSMath.clamp((desiredYHead - head.yRot), -0.5f, 0.5f) - entity.yRotRightEye) * tickDelta;
			entity.xRotRightEye += (-CSMath.clamp((desiredXHead - head.xRot), -0.5f, 0.5f) - entity.xRotRightEye) * tickDelta;
		}
		else
		{
			// Reset the eye rotation
			entity.yRotRightEye += (0 - entity.yRotRightEye) * tickDelta;
			entity.xRotRightEye += (0 - entity.xRotRightEye) * tickDelta;
		}
		// Move the left eye if the Chameleon is looking at an object to its left
		if (head.yRot > desiredYHead)
		{
			// If there is slop in the head rotation, make up the difference with the eye
			entity.yRotLeftEye += (CSMath.clamp((head.yRot - desiredYHead), -0.5f, 0.5f) - entity.yRotLeftEye) * tickDelta;
			entity.xRotLeftEye += (CSMath.clamp((desiredXHead - head.xRot), -0.5f, 0.5f) - entity.xRotLeftEye) * tickDelta;
		}
		else
		{
			// Reset the eye rotation
			entity.yRotLeftEye += (-entity.yRotLeftEye) * tickDelta;
			entity.xRotLeftEye += (-entity.xRotLeftEye) * tickDelta;
		}

		// Set the model part rotations to the values stored in the Chameleon entity
		head.xRot = entity.xRotHead;
		head.yRot = entity.yRotHead;
		rightEye.yRot = entity.yRotRightEye;
		rightEye.zRot = entity.xRotRightEye;
		leftEye.yRot = entity.yRotLeftEye;
		leftEye.zRot = entity.xRotLeftEye;

		AnimationManager.saveAnimationStates(entity, modelParts);

		AnimationManager.animateEntity(entity, (animTime, frameTime) ->
		{
			float prevAnimTime = animTime;
			Map<String, ModelPart> animatedParts = new HashMap<>(modelParts);
			animatedParts.remove("Head");
			animatedParts.remove("LeftEye");
			animatedParts.remove("RightEye");
			ModelPart tail  = animatedParts.remove("Tail");
			ModelPart tail2 = animatedParts.remove("Tail2");
			ModelPart tail3 = animatedParts.remove("Tail3");

			// Riding player animation
			if (this.riding && entity.getVehicle() instanceof Player player)
			{
				float playerYaw = CSMath.blend(player.yHeadRotO, player.yHeadRot, partialTick, 0, 1);
				animTime += frameTime;

				ChameleonAnimations.RIDE.animateAll(animatedParts, animTime, false);

				// Free up the tail if the chameleon is pointing toward a biome
				if (!chameleon.isTracking())
				{
					ChameleonAnimations.RIDE.animate("Tail",  tail,  0, false);
					ChameleonAnimations.RIDE.animate("Tail2", tail2, 0, false);
					ChameleonAnimations.RIDE.animate("Tail3", tail3, 0, false);
				}

				if (young)
				{   body.y -= 14;
					head.y -= 4;
				}
				body.y -= (player.getBbHeight() / 2) * 16 - 4;
				head.y -= (player.getBbHeight() / 2) * 16 + 11;
				body.yRot = CSMath.toRadians(playerYaw) - CSMath.toRadians(CSMath.blend(player.yBodyRotO, player.yBodyRot, partialTick, 0, 1));
				head.yRot = CSMath.toRadians(entity.getViewYRot(partialTick) - player.getViewYRot(partialTick)) + 0.2f;
				head.xRot = CSMath.clamp(CSMath.toRadians(entity.getViewXRot(partialTick) - player.getViewXRot(partialTick)) + 0.2f, -1f, 1f);
			}
			// Walk animation
			else if (entity.isWalking())
			{
				float walkSpeed = Math.min(0.15f, new Vec2((float) entity.getDeltaMovement().x, (float) entity.getDeltaMovement().z).length());
				animTime += (frameTime * walkSpeed * (young ? 50 : 30));

				ChameleonAnimations.WALK.animateAll(animatedParts, animTime, true);
				if (!chameleon.isTracking())
				{
					ChameleonAnimations.WALK.animate("Tail",  tail,  animTime, true);
					ChameleonAnimations.WALK.animate("Tail2", tail2, animTime, true);
					ChameleonAnimations.WALK.animate("Tail3", tail3, animTime, true);
				}
			}
			// Idle animation
			else
			{
				animTime += frameTime;
				ChameleonAnimations.IDLE.animateAll(animatedParts, animTime, true);

				// Free up the tail if the chameleon is pointing toward a biome
				if (!chameleon.isTracking())
				{
					ChameleonAnimations.WALK.animate("Tail",  tail,  0, true);
					ChameleonAnimations.WALK.animate("Tail2", tail2, 0, true);
					ChameleonAnimations.WALK.animate("Tail3", tail3, 0, true);
				}
			}

			// Point the tail toward the biome the Chameleon is tracking (if present)
			if (chameleon.isTracking())
			{
				BlockPos trackingPos = chameleon.getTrackingPos();

				Vec3 entityPos = entity.getPosition(partialTick);
				Vec3 trackingDirection = new Vec3(trackingPos.getX() - entityPos.x, 0, trackingPos.getZ() - entityPos.z);
				float playerRotX = 0;
				float rotY;

				if (entity.getVehicle() instanceof Player player)
				{	playerRotX = player.getViewXRot(partialTick);
					rotY = CSMath.blend(player.yHeadRotO, player.yHeadRot, partialTick, 0, 1);
				}
				else rotY = CSMath.blend(entity.yBodyRotO, entity.yBodyRot, partialTick, 0, 1);

				// Get the angle between the entity's position and the tracking position (radians)
				float angle = (float) Math.atan2(trackingDirection.z, trackingDirection.x) - CSMath.toRadians(Mth.wrapDegrees(rotY));

				float desiredTailRot = angle + CSMath.toRadians(90);

				// Side-to-side tail movement
				tail2.yRot = (float) Math.sin(desiredTailRot) / 1.3f;
				tail.yRot  = tail2.yRot;
				tail3.yRot = tail2.yRot;

				// Up-and-down tail movement
				tail2.xRot = Math.max(0, (float) Math.sin(desiredTailRot - Math.PI / 2) + 0.2f);
				tail3.xRot = tail2.xRot / 1.5f;
				tail.xRot = tail3.xRot - CSMath.toRadians(playerRotX) / 1.25f + 0.1f;
			}

			// Eat animation (applied on top of other anims)
			if (entity.getEatTimer() > 0)
			{	tongueVisible = true;
				ChameleonAnimations.EAT.animateAll(modelParts, CSMath.blend(0.5f, 0f, entity.getEatTimer() - Minecraft.getInstance().getFrameTime(), 0, entity.getEatAnimLength()), true);
			}
			else tongueVisible = false;

			if (Minecraft.getInstance().isPaused())
				return prevAnimTime;

			// Move the tail in accordance with the velocity of the player it's riding
			float playerXHead = 0;
			if (entity.getVehicle() instanceof Player player)
			{	playerXHead = CSMath.toRadians(player.getViewXRot(partialTick));
			}

			Vec3 velocity = playerXHead != 0 ? entity.getVehicle().getDeltaMovement() : entity.getDeltaMovement();
			// Side-to-side tail movement (disabled if the chameleon is tracking a biome)
			if (!chameleon.isTracking())
			{
				float speed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
				float tailSpeed = Math.min(0.1f, speed / 2) + 0.01f;

				float deltaTime = Minecraft.getInstance().getDeltaFrameTime();
				chameleon.tailPhase += 2 * Math.PI * deltaTime * tailSpeed;

				// Calculate sine wave value with phase shift
				float speedStraightFactor = (3 + speed * 100);
				float tailRot1 = (float) Math.sin(chameleon.tailPhase - 0) / speedStraightFactor;
				float tailRot2 = (float) Math.sin(chameleon.tailPhase - 1) / speedStraightFactor;
				float tailRot3 = (float) Math.sin(chameleon.tailPhase - 2) / speedStraightFactor;

				float tailRotation = (1 + Math.abs(tail.xRot - 0.2f) * 1);

				tail.yRot  = tailRot1 / tailRotation;
				tail2.yRot = tailRot2 / tailRotation;
				tail3.yRot = tailRot3 / tailRotation;
			}

			// Up/down tail movement (takes into account player head rotation)
			float playerYVel = (float) velocity.y;
			float tailRot = entity.xRotTail += (CSMath.clamp(playerYVel, -0.5, 0.5) - entity.xRotTail) * frameTime * 8;
			tailRot *= CSMath.clamp(Math.abs(tail.xRot + tail2.xRot + tail3.xRot + playerXHead) - 2.3, -1, 1);
			tail.xRot += tailRot;
			tail2.xRot += tailRot;
			tail3.xRot += tailRot;

			return animTime;
		});
	}

	@Override
	public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
	{
		renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, false);
	}

	@Override
	protected Iterable<ModelPart> headParts()
	{
		return List.of(head);
	}

	@Override
	protected Iterable<ModelPart> bodyParts()
	{
		return List.of(body);
	}

	public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, boolean isOverlay)
	{
		if (chameleon == null) return;

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		float partialTick = Minecraft.getInstance().getFrameTime();
		long tickCount = chameleon.tickCount;
		long hurtTime = chameleon.getHurtTimestamp();

		// Make the chameleon invisible after it gets hurt
		// Don't do this if alpha is overridden
		if (!isOverlay && chameleon.isAlive())
		{
			if (CSMath.betweenInclusive(tickCount - hurtTime, 0, 40) && hurtTime != 0)
			{	chameleon.opacity = CSMath.blend(alpha, alpha * 0.15f, tickCount + partialTick - hurtTime, 0, 40);
			}
			else if (chameleon.opacity < alpha)
			{	chameleon.opacity = CSMath.blend(alpha * 0.15f, alpha, tickCount + partialTick - hurtTime, 120, 180);
			}
		}

		ModelPart tongue1 = modelParts.get("Tongue1");
		tongue1.visible = false;

		super.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, isOverlay ? alpha : chameleon.opacity);

		// Render the tongue with a different VertexConsumer that culls backfaces
		if (tongueVisible && !isOverlay)
		{
			poseStack.pushPose();
			ModelPart jaw = modelParts.get("Jaw");

			// vertex consumer for entityTranslucentCull
			VertexConsumer tongueConsumer = Minecraft.getInstance().renderBuffers().bufferSource()
													 .getBuffer(RenderType.entityCutout(ChameleonEntityRenderer.CHAMELEON_GREEN));
			tongue1.visible = true;
			poseStack.translate(0, 1.1555, -0.18755);
			tongue1.xRot = head.xRot + jaw.xRot / 2;
			tongue1.yRot = head.yRot;
			tongue1.render(poseStack, tongueConsumer, packedLight, packedOverlay, red, green, blue, chameleon.opacity);
			poseStack.popPose();
		}
		RenderSystem.disableBlend();
	}
}