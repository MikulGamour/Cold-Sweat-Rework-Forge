package com.momosoftworks.coldsweat.common.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import com.momosoftworks.coldsweat.common.entity.task.GoatTasks;
import com.momosoftworks.coldsweat.core.event.TaskScheduler;
import com.momosoftworks.coldsweat.core.init.EntityInit;
import com.momosoftworks.coldsweat.core.init.MemoryInit;
import com.momosoftworks.coldsweat.core.init.SensorTypeInit;
import com.momosoftworks.coldsweat.core.init.SoundInit;
import com.momosoftworks.coldsweat.data.tag.ModBlockTags;
import com.momosoftworks.coldsweat.compat.CompatManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.DebugPacketSender;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.*;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.*;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.Random;

public class GoatEntity extends AnimalEntity
{
    public static final EntitySize LONG_JUMPING_DIMENSIONS = EntitySize.scalable(0.9F, 1.3F).scale(0.7F);
    protected static final ImmutableList<SensorType<? extends Sensor<? super GoatEntity>>> SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.NEAREST_ITEMS, SensorType.NEAREST_ADULT, SensorType.HURT_BY, SensorTypeInit.GOAT_TEMPTATIONS.get());
    protected static final ImmutableList<MemoryModuleType<?>> MEMORY_MODULES = ImmutableList.of(MemoryModuleType.LOOK_TARGET, MemoryModuleType.VISIBLE_LIVING_ENTITIES, MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.PATH, MemoryModuleType.ATE_RECENTLY, MemoryModuleType.BREED_TARGET, MemoryInit.LONG_JUMP_COOLING_DOWN.get(), MemoryInit.LONG_JUMP_MID_JUMP.get(), MemoryInit.TEMPTING_PLAYER.get(), MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryInit.TEMPTATION_COOLDOWN_TICKS.get(), new MemoryModuleType[] {(MemoryModuleType) MemoryInit.IS_TEMPTED.get(), (MemoryModuleType) MemoryInit.RAM_COOLDOWN_TICKS.get(), (MemoryModuleType) MemoryInit.RAM_TARGET.get()});
    private static final DataParameter<Boolean> SCREAMING = EntityDataManager.defineId(GoatEntity.class, DataSerializers.BOOLEAN);
    private boolean preparingRam;
    private int rammingTicks;

    public GoatEntity(EntityType<? extends GoatEntity> type, World worldIn)
    {
        super(type, worldIn);
        this.getNavigation().setCanFloat(true);
    }

    protected Brain.BrainCodec<GoatEntity> brainProvider()
    {   return Brain.provider(MEMORY_MODULES, SENSOR_TYPES);
    }

    protected Brain<?> makeBrain(Dynamic<?> dynamicIn)
    {   return GoatTasks.makeBrain(this.brainProvider().makeBrain(dynamicIn));
    }

    public static AttributeModifierMap.MutableAttribute createAttributes()
    {
        return MobEntity.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0)
                                              .add(Attributes.MOVEMENT_SPEED, 0.20000000298023224)
                                              .add(Attributes.ATTACK_DAMAGE, 2.0)
                                              .add(Attributes.FOLLOW_RANGE, 8.0);
    }

    protected void ageBoundaryReached()
    {
        if (this.isBaby())
        {   this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(1.0);
        }
        else
        {   this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2.0);
        }
    }

    protected int calculateFallDamage(float distance, float damageMultiplier)
    {   return super.calculateFallDamage(distance, damageMultiplier) - 10;
    }

    @Nullable
    protected SoundEvent getAmbientSound()
    {
        return this.isScreaming()
               ? SoundInit.ENTITY_GOAT_SCREAMING_AMBIENT.get()
               : SoundInit.ENTITY_GOAT_AMBIENT.get();
    }

    @Nullable
    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return this.isScreaming()
               ? SoundInit.ENTITY_GOAT_SCREAMING_HURT.get()
               : SoundInit.ENTITY_GOAT_HURT.get();
    }

    @Nullable
    protected SoundEvent getDeathSound()
    {
        return this.isScreaming()
               ? SoundInit.ENTITY_GOAT_SCREAMING_DEATH.get()
               : SoundInit.ENTITY_GOAT_DEATH.get();
    }

    protected void playStepSound(BlockPos pos, BlockState blockIn)
    {   this.playSound(SoundInit.ENTITY_GOAT_STEP.get(), 0.15F, 1.0F);
    }

    protected SoundEvent getMilkingSound()
    {
        return this.isScreaming()
               ? SoundInit.ENTITY_GOAT_SCREAMING_MILK.get()
               : SoundInit.ENTITY_GOAT_MILK.get();
    }

    @Nullable
    public AgeableEntity getBreedOffspring(ServerWorld world, AgeableEntity entity)
    {
        GoatEntity goat = (GoatEntity) ((EntityType) EntityInit.GOAT.get()).create(world);
        if (goat != null)
        {
            GoatTasks.resetLongJumpCooldown(goat);
            boolean isScreamingGoat = entity instanceof GoatEntity && ((GoatEntity) entity).isScreaming();
            goat.setScreaming(isScreamingGoat || world.getRandom().nextDouble() < 0.02);
        }

        return goat;
    }

    public Brain<GoatEntity> getBrain()
    {   return (Brain<GoatEntity>) super.getBrain();
    }

    protected void customServerAiStep()
    {
        this.level.getProfiler().push("goatBrain");
        this.getBrain().tick((ServerWorld) this.level, this);
        this.level.getProfiler().pop();
        this.level.getProfiler().push("goatActivityUpdate");
        GoatTasks.updateActivities(this);
        this.level.getProfiler().pop();
        super.customServerAiStep();
    }

    public int getMaxHeadYRot()
    {   return 15;
    }

    public void setYHeadRot(float rotation)
    {
        int horizontalFaceSpeed = this.getMaxHeadYRot();
        float angleRotation = MathHelper.degreesDifference(this.yBodyRot, rotation);
        float yawRotation = MathHelper.clamp(angleRotation, (float) (-horizontalFaceSpeed), (float) horizontalFaceSpeed);
        super.setYHeadRot(this.yBodyRot + yawRotation);
    }

    public SoundEvent getEatingSound(ItemStack itemStackIn)
    {
        return this.isScreaming()
               ? SoundInit.ENTITY_GOAT_SCREAMING_EAT.get()
               : SoundInit.ENTITY_GOAT_EAT.get();
    }

    public ActionResultType mobInteract(PlayerEntity player, Hand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem().equals(Items.BUCKET) && !this.isBaby())
        {
            player.playSound(this.getMilkingSound(), 1.0F, 1.0F);
            ItemStack filledBucket = Items.MILK_BUCKET.getDefaultInstance();
            if (!player.isCreative())
            {   stack.shrink(1);
            }
            if (stack.isEmpty())
            {   player.setItemInHand(hand, filledBucket);
            }
            else if (!player.addItem(filledBucket))
            {   player.drop(filledBucket, false);
            }
            return ActionResultType.sidedSuccess(this.level.isClientSide());
        }
        else
        {
            ActionResultType actionResultType = super.mobInteract(player, hand);
            if (actionResultType.shouldSwing() && this.isFood(stack))
            {
                this.level.playSound(null, this, this.getEatingSound(stack), SoundCategory.NEUTRAL, 1.0F, this.level.getRandom().nextFloat() * 0.4f + 0.8F);
            }

            return actionResultType;
        }
    }

    @Override
    public void onAddedToWorld()
    {
        // Convert this entity to a Goat from Caves & Cliffs if loaded
        if (CompatManager.isCavesAndCliffsLoaded())
        {
            AnimalEntity convertedGoat = CompatManager.createGoatFrom(this);
            if (convertedGoat != null)
            {
                TaskScheduler.scheduleServer(() ->
                {
                    this.remove();
                    if (this.level instanceof ServerWorld)
                    {   ((ServerWorld) this.level).despawn(this);
                    }
                    this.level.addFreshEntity(convertedGoat);
                }, 0);
            }
        }
        super.onAddedToWorld();
    }

    public ILivingEntityData finalizeSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag)
    {
        GoatTasks.resetLongJumpCooldown(this);
        this.setScreaming(worldIn.getRandom().nextDouble() < 0.02);
        return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    protected void sendDebugPackets()
    {
        super.sendDebugPackets();
        DebugPacketSender.sendEntityBrain(this);
    }

    public EntitySize getDimensions(Pose poseIn)
    {
        return poseIn == Pose.SPIN_ATTACK
               ? LONG_JUMPING_DIMENSIONS.scale(this.getScale())
               : super.getDimensions(poseIn);
    }

    public void addAdditionalSaveData(CompoundNBT compound)
    {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("IsScreamingGoat", this.isScreaming());
    }

    public void readAdditionalSaveData(CompoundNBT compound)
    {
        super.readAdditionalSaveData(compound);
        this.setScreaming(compound.getBoolean("IsScreamingGoat"));
    }

    public void handleEntityEvent(byte id)
    {
        if (id == 58)
        {   this.preparingRam = true;
        }
        else if (id == 59)
        {   this.preparingRam = false;
        }
        else
        {   super.handleEntityEvent(id);
        }
    }

    public void aiStep()
    {
        if (this.preparingRam)
        {   ++this.rammingTicks;
        }
        else
        {   this.rammingTicks -= 2;
        }

        this.rammingTicks = MathHelper.clamp(this.rammingTicks, 0, 20);
        super.aiStep();
    }

    protected void defineSynchedData()
    {
        super.defineSynchedData();
        this.entityData.define(SCREAMING, false);
    }

    public boolean isScreaming()
    {   return this.entityData.get(SCREAMING);
    }

    public void setScreaming(boolean screaming)
    {   this.entityData.set(SCREAMING, screaming);
    }

    public float getHeadPitch()
    {   return (float) this.rammingTicks / 20.0F * 30.0F * 0.02F;
    }

    protected PathNavigator createNavigation(World worldIn)
    {   return new GoatPathNavigator(this, worldIn);
    }

    public static boolean canSpawn(EntityType<? extends AnimalEntity> entityType, IWorld worldIn, SpawnReason reason, BlockPos pos, Random rand)
    {
        BlockState state = worldIn.getBlockState(pos.below());
        return state.is(ModBlockTags.GOATS_SPAWNABLE_ON) && worldIn.getRawBrightness(pos, 0) > 8;
    }

    private static class GoatPathNodeProcessor extends WalkNodeProcessor
    {
        private final BlockPos.Mutable pos;

        private GoatPathNodeProcessor()
        {   this.pos = new BlockPos.Mutable();
        }

        public PathNodeType getBlockPathType(IBlockReader reader, int x, int y, int z)
        {
            this.pos.set(x, y, z);
            return getBlockPathTypeStatic(reader, this.pos);
        }
    }

    private static class GoatPathNavigator extends GroundPathNavigator
    {
        public GoatPathNavigator(MobEntity entityIn, World worldIn)
        {   super(entityIn, worldIn);
        }

        protected PathFinder createPathFinder(int range)
        {
            this.nodeEvaluator = new GoatPathNodeProcessor();
            return new PathFinder(this.nodeEvaluator, range);
        }
    }
}
