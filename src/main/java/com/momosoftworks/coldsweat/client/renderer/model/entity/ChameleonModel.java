package com.momosoftworks.coldsweat.client.renderer.model.entity;

import com.jozufozu.flywheel.core.model.ModelPart;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.momosoftworks.coldsweat.client.renderer.ChameleonAnimations;
import com.momosoftworks.coldsweat.client.renderer.animation.AnimationManager;
import com.momosoftworks.coldsweat.client.renderer.entity.ChameleonEntityRenderer;
import com.momosoftworks.coldsweat.common.entity.ChameleonEntity;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.model.AgeableModel;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.entity.model.IHasHead;
import net.minecraft.client.renderer.entity.model.QuadrupedModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChameleonModel<T extends ChameleonEntity> extends AgeableModel<T> implements IHasHead
{
	ChameleonEntity chameleon;
	boolean tongueVisible = false;

	public Map<String, ModelRenderer> modelParts = new HashMap<>();

	private final ModelRenderer body;
	private final ModelRenderer head;
	private final ModelRenderer topFrill;
	private final ModelRenderer jaw;
	private final ModelRenderer rightEye;
	private final ModelRenderer leftEye;
	private final ModelRenderer tongue1;
	private final ModelRenderer tongue2;
	private final ModelRenderer tongue3;
	private final ModelRenderer rightFrontLeg;
	private final ModelRenderer rightFrontLeg2;
	private final ModelRenderer rightFrontFoot1;
	private final ModelRenderer rightFrontFoot2;
	private final ModelRenderer leftFrontLeg;
	private final ModelRenderer leftFrontLeg2;
	private final ModelRenderer leftFrontFoot1;
	private final ModelRenderer leftFrontFoot2;
	private final ModelRenderer rightBackLeg;
	private final ModelRenderer rightBackLeg2;
	private final ModelRenderer rightBackFoot1;
	private final ModelRenderer rightBackFoot2;
	private final ModelRenderer leftBackLeg;
	private final ModelRenderer leftBackLeg2;
	private final ModelRenderer leftBackFoot1;
	private final ModelRenderer leftBackFoot2;
	private final ModelRenderer tail1;
	private final ModelRenderer tail2;
	private final ModelRenderer tail3;

	public ChameleonModel()
	{
		super(RenderType::entityTranslucent, true, 4.75f, 0.75f, 1.8F, 1.6F, 14.0F);

		texWidth = 48;
		texHeight = 32;

		modelParts.put("Body", body = new ModelRenderer(this));
		body.setPos(0.0F, 24.0F, 0.0F);
		body.texOffs(1, 16).addBox(-2.0F, -8.0F, -3.0F, 4.0F, 7.0F, 9.0F, 0.0F, false);

		modelParts.put("Head", head = new ModelRenderer(this));
		head.setPos(0.0F, -5.0F, -3.0F);
		head.texOffs(0, 0).addBox(-2.0F, -2.0F, -7.0F, 4.0F, 4.0F, 7.0F, 0.0F, false);

		modelParts.put("TopFrill", topFrill = new ModelRenderer(this));
		topFrill.setPos(0.0F, -3.0F, -5.0F);
		head.addChild(topFrill);
		topFrill.xRot = 0.7418F;
		topFrill.texOffs(30, 0).addBox(-1.0F, 1.4F, 0.05F, 2.0F, 4.0F, 7.0F, 0.0F, false);

		modelParts.put("Jaw", jaw = new ModelRenderer(this));
		jaw.setPos(0.0F, 2.0F, 0.0F);
		head.addChild(jaw);
		jaw.texOffs(15, 7).addBox(-2.0F, 0.0F, -7.0F, 4.0F, 1.0F, 7.0F, 0.0F, false);
		jaw.texOffs(18, 15).addBox(0.0F, 1.0F, -6.0F, 0.0F, 1.0F, 6.0F, 0.0F, false);

		modelParts.put("RightEye", rightEye = new ModelRenderer(this));
		rightEye.setPos(-1.5F, 0.0F, -3.5F);
		head.addChild(rightEye);
		rightEye.texOffs(15, 1).addBox(-1.5F, -1.5F, -1.5F, 2.0F, 3.0F, 3.0F, 0.0F, false);

		modelParts.put("LeftEye", leftEye = new ModelRenderer(this));
		leftEye.setPos(1.0005F, -0.0218F, -3.366F);
		head.addChild(leftEye);
		leftEye.texOffs(15, 1).addBox(-0.0005F, -1.4782F, -1.634F, 2.0F, 3.0F, 3.0F, 0.0F, true);

		modelParts.put("Tongue1", tongue1 = new ModelRenderer(this));
		tongue1.setPos(0.0F, 2.0F, 0.0F);
		head.addChild(tongue1);
		tongue1.texOffs(27, 25).addBox(-1.0F, 0.0F, -7.0F, 2.0F, 0.0F, 7.0F, 0.0F, false);

		modelParts.put("Tongue2", tongue2 = new ModelRenderer(this));
		tongue2.setPos(0.0F, 3.0F, 3.0F);
		tongue1.addChild(tongue2);
		tongue2.texOffs(27, 25).addBox(-1.0F, -3.0F, -10.0F, 2.0F, 0.0F, 7.0F, 0.0F, false);

		modelParts.put("Tongue3", tongue3 = new ModelRenderer(this));
		tongue3.setPos(0.0F, 0.0F, 0.0F);
		tongue2.addChild(tongue3);
		tongue3.texOffs(27, 25).addBox(-1.0F, -3.0F, -10.0F, 2.0F, 0.0F, 7.0F, 0.0F, false);

		modelParts.put("RightFrontLeg", rightFrontLeg = new ModelRenderer(this));
		rightFrontLeg.setPos(-2.0F, -3.0F, -1.0F);
		body.addChild(rightFrontLeg);
		rightFrontLeg.texOffs(38, 25).addBox(-4.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, 0.0F, true);

		modelParts.put("RightFrontLeg2", rightFrontLeg2 = new ModelRenderer(this));
		rightFrontLeg2.setPos(-4.0F, 0.0F, 0.0F);
		rightFrontLeg.addChild(rightFrontLeg2);
		rightFrontLeg2.texOffs(38, 29).addBox(-4.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, 0.0F, true);

		modelParts.put("RightFrontFoot1", rightFrontFoot1 = new ModelRenderer(this));
		rightFrontFoot1.setPos(-4.0F, 0.0F, 0.0F);
		rightFrontLeg2.addChild(rightFrontFoot1);
		rightFrontFoot1.texOffs(28, 27).addBox(0.0F, -3.0F, -1.0F, 0.0F, 3.0F, 2.0F, 0.0F, true);

		modelParts.put("RightFrontFoot2", rightFrontFoot2 = new ModelRenderer(this));
		rightFrontFoot2.setPos(-4.0F, 0.0F, 0.0F);
		rightFrontLeg2.addChild(rightFrontFoot2);
		rightFrontFoot2.texOffs(28, 24).addBox(0.0F, 0.0F, -1.0F, 0.0F, 2.0F, 1.0F, 0.0F, true);

		modelParts.put("LeftFrontLeg", leftFrontLeg = new ModelRenderer(this));
		leftFrontLeg.setPos(2.0F, -3.0F, -1.0F);
		body.addChild(leftFrontLeg);
		leftFrontLeg.texOffs(38, 25).addBox(0.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, 0.0F, false);

		modelParts.put("LeftFrontLeg2", leftFrontLeg2 = new ModelRenderer(this));
		leftFrontLeg2.setPos(4.0F, 0.0F, 0.0F);
		leftFrontLeg.addChild(leftFrontLeg2);
		leftFrontLeg2.texOffs(38, 29).addBox(0.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, 0.0F, false);

		modelParts.put("LeftFrontFoot1", leftFrontFoot1 = new ModelRenderer(this));
		leftFrontFoot1.setPos(4.0F, 0.0F, 0.0F);
		leftFrontLeg2.addChild(leftFrontFoot1);
		leftFrontFoot1.texOffs(28, 27).addBox(0.0F, -3.0F, -1.0F, 0.0F, 3.0F, 2.0F, 0.0F, false);

		modelParts.put("LeftFrontFoot2", leftFrontFoot2 = new ModelRenderer(this));
		leftFrontFoot2.setPos(4.0F, 0.0F, 0.0F);
		leftFrontLeg2.addChild(leftFrontFoot2);
		leftFrontFoot2.texOffs(28, 24).addBox(0.0F, 0.0F, -1.0F, 0.0F, 2.0F, 1.0F, 0.0F, false);

		modelParts.put("RightBackLeg", rightBackLeg = new ModelRenderer(this));
		rightBackLeg.setPos(-2.0F, -3.0F, 4.0F);
		body.addChild(rightBackLeg);
		rightBackLeg.texOffs(38, 25).addBox(-4.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, 0.0F, true);

		modelParts.put("RightBackLeg2", rightBackLeg2 = new ModelRenderer(this));
		rightBackLeg2.setPos(-4.0F, 0.0F, 0.0F);
		rightBackLeg.addChild(rightBackLeg2);
		rightBackLeg2.texOffs(38, 29).addBox(-4.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, 0.0F, true);

		modelParts.put("RightBackFoot1", rightBackFoot1 = new ModelRenderer(this));
		rightBackFoot1.setPos(-4.0F, 0.0F, 0.0F);
		rightBackLeg2.addChild(rightBackFoot1);
		rightBackFoot1.texOffs(28, 27).addBox(0.0F, -3.0F, -1.0F, 0.0F, 3.0F, 2.0F, 0.0F, true);

		modelParts.put("RightBackFoot2", rightBackFoot2 = new ModelRenderer(this));
		rightBackFoot2.setPos(-4.0F, 0.0F, 0.0F);
		rightBackLeg2.addChild(rightBackFoot2);
		rightBackFoot2.texOffs(28, 24).addBox(0.0F, 0.0F, -1.0F, 0.0F, 2.0F, 1.0F, 0.0F, true);

		modelParts.put("LeftBackLeg", leftBackLeg = new ModelRenderer(this));
		leftBackLeg.setPos(2.0F, -3.0F, 4.0F);
		body.addChild(leftBackLeg);
		leftBackLeg.texOffs(38, 25).addBox(0.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, 0.0F, false);

		modelParts.put("LeftBackLeg2", leftBackLeg2 = new ModelRenderer(this));
		leftBackLeg2.setPos(4.0F, 0.0F, 0.0F);
		leftBackLeg.addChild(leftBackLeg2);
		leftBackLeg2.texOffs(38, 29).addBox(0.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, 0.0F, false);

		modelParts.put("LeftBackFoot1", leftBackFoot1 = new ModelRenderer(this));
		leftBackFoot1.setPos(4.0F, 0.0F, 0.0F);
		leftBackLeg2.addChild(leftBackFoot1);
		leftBackFoot1.texOffs(28, 27).addBox(0.0F, -3.0F, -1.0F, 0.0F, 3.0F, 2.0F, 0.0F, false);

		modelParts.put("LeftBackFoot2", leftBackFoot2 = new ModelRenderer(this));
		leftBackFoot2.setPos(4.0F, 0.0F, 0.0F);
		leftBackLeg2.addChild(leftBackFoot2);
		leftBackFoot2.texOffs(27, 24).addBox(0.0F, 0.0F, -1.0F, 0.0F, 2.0F, 1.0F, 0.0F, false);

		modelParts.put("Tail", tail1 = new ModelRenderer(this));
		tail1.setPos(0.0F, -3.5F, 6.0F);
		body.addChild(tail1);
		tail1.texOffs(34, 18).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 4.0F, 0.0F, false);

		modelParts.put("Tail2", tail2 = new ModelRenderer(this));
		tail2.setPos(0.0F, 0.0F, 4.0F);
		tail1.addChild(tail2);
		tail2.texOffs(38, 13).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F, 0.0F, false);

		modelParts.put("Tail3", tail3 = new ModelRenderer(this));
		tail3.setPos(0.0F, 0.0F, 3.0F);
		tail2.addChild(tail3);
		tail3.texOffs(0, 17).addBox(-0.5F, -1.0F, 0.0F, 1.0F, 4.0F, 4.0F, 0.0F, false);

		head.y = 19.2f;
		body.y -= 0.8f;

		AnimationManager.storeDefaultPoses(ModEntities.CHAMELEON, modelParts);
	}

	@Override
	public ModelRenderer getHead()
	{	return head;
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
	{
		chameleon = entity;
		AnimationManager.loadAnimationStates(entity, modelParts);

		float tickDelta = Minecraft.getInstance().getDeltaFrameTime();
		float partialTick = Minecraft.getInstance().getFrameTime();
		ModelRenderer head = modelParts.get("Head");
        ModelRenderer rightEye = modelParts.get("RightEye");
        ModelRenderer leftEye = modelParts.get("LeftEye");

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
			Map<String, ModelRenderer> animatedParts = new HashMap<>(modelParts);
			animatedParts.remove("Head");
			animatedParts.remove("LeftEye");
			animatedParts.remove("RightEye");
            animatedParts.remove("Tail");
            animatedParts.remove("Tail2");
            animatedParts.remove("Tail3");

			// Riding player animation
			if (this.riding && entity.getVehicle() instanceof PlayerEntity)
			{
                PlayerEntity player = (PlayerEntity) entity.getVehicle();
				float playerYaw = CSMath.blend(player.yHeadRotO, player.yHeadRot, partialTick, 0, 1);
				animTime += frameTime;

				ChameleonAnimations.RIDE.animateAll(animatedParts, animTime, false);

				// Free up the tail if the chameleon is pointing toward a biome
				if (!chameleon.isTracking())
				{
					ChameleonAnimations.RIDE.animate("Tail",  tail1,  0, false);
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
				float walkSpeed = Math.min(0.15f, (float) new Vector3d((float) entity.getDeltaMovement().x, 0, (float) entity.getDeltaMovement().z).length());
                animTime += (frameTime * walkSpeed * (young ? 50 : 30));

				ChameleonAnimations.WALK.animateAll(animatedParts, animTime, true);
				if (!chameleon.isTracking())
				{
					ChameleonAnimations.WALK.animate("Tail",  tail1,  animTime, true);
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
					ChameleonAnimations.WALK.animate("Tail",  tail1,  0, true);
					ChameleonAnimations.WALK.animate("Tail2", tail2, 0, true);
					ChameleonAnimations.WALK.animate("Tail3", tail3, 0, true);
				}
			}

			// Point the tail toward the biome the Chameleon is tracking (if present)
			if (chameleon.isTracking())
			{
				BlockPos trackingPos = chameleon.getTrackingPos();

                Vector3d entityPos = entity.getPosition(partialTick);
                Vector3d trackingDirection = new Vector3d(trackingPos.getX() - entityPos.x, 0, trackingPos.getZ() - entityPos.z);
				float playerRotX = 0;
				float rotY;

				if (entity.getVehicle() instanceof PlayerEntity)
				{	PlayerEntity player = (PlayerEntity) entity.getVehicle();
                    playerRotX = player.getViewXRot(partialTick);
					rotY = CSMath.blend(player.yHeadRotO, player.yHeadRot, partialTick, 0, 1);
				}
				else rotY = CSMath.blend(entity.yBodyRotO, entity.yBodyRot, partialTick, 0, 1);

				// Get the angle between the entity's position and the tracking position (radians)
				float angle = (float) Math.atan2(trackingDirection.z, trackingDirection.x) - CSMath.toRadians(MathHelper.wrapDegrees(rotY));

				float desiredTailRot = angle + CSMath.toRadians(90);

				// Side-to-side tail movement
				tail2.yRot = (float) Math.sin(desiredTailRot) / 1.3f;
				tail1.yRot  = tail2.yRot;
				tail3.yRot = tail2.yRot;

				// Up-and-down tail movement
				tail2.xRot = Math.max(0, (float) Math.sin(desiredTailRot - Math.PI / 2) + 0.2f);
				tail3.xRot = tail2.xRot / 1.5f;
				tail1.xRot = tail3.xRot - CSMath.toRadians(playerRotX) / 1.25f + 0.1f;
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
			if (entity.getVehicle() instanceof PlayerEntity)
			{	PlayerEntity player = (PlayerEntity) entity.getVehicle();
                playerXHead = CSMath.toRadians(player.getViewXRot(partialTick));
			}

			Vector3d velocity = playerXHead != 0 ? entity.getVehicle().getDeltaMovement() : entity.getDeltaMovement();
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

				float tailRotation = (1 + Math.abs(tail1.xRot - 0.2f) * 1);

				tail1.yRot  = tailRot1 / tailRotation;
				tail2.yRot = tailRot2 / tailRotation;
				tail3.yRot = tailRot3 / tailRotation;
			}

			// Up/down tail movement (takes into account player head rotation)
			float playerYVel = (float) velocity.y;
			float tailRot = entity.xRotTail += (CSMath.clamp(playerYVel, -0.5, 0.5) - entity.xRotTail) * frameTime * 8;
			tailRot *= CSMath.clamp(Math.abs(tail1.xRot + tail2.xRot + tail3.xRot + playerXHead) - 2.3, -1, 1);
			tail1.xRot += tailRot;
			tail2.xRot += tailRot;
			tail3.xRot += tailRot;

			return animTime;
		});
	}

	@Override
	public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
	{	renderToBuffer(matrixStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, false);
	}

    @Override
    protected Iterable<ModelRenderer> headParts()
    {
        return Arrays.asList(head);
    }

    @Override
    protected Iterable<ModelRenderer> bodyParts()
    {
        return Arrays.asList(body);
    }

	public void renderToBuffer(MatrixStack poseStack, IVertexBuilder vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, boolean isOverlay)
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

		tongue1.visible = false;

		super.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, isOverlay ? alpha : chameleon.opacity);

		// Render the tongue with a different VertexConsumer that culls backfaces
		if (tongueVisible && !isOverlay)
		{
			poseStack.pushPose();

			// vertex consumer for entityTranslucentCull
			IVertexBuilder tongueConsumer = Minecraft.getInstance().renderBuffers().bufferSource()
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