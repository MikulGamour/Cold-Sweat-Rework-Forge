package com.momosoftworks.coldsweat.common.entity;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.client.renderer.animation.AnimationManager;
import com.momosoftworks.coldsweat.common.entity.data.edible.ChameleonEdibles;
import com.momosoftworks.coldsweat.common.entity.data.edible.Edible;
import com.momosoftworks.coldsweat.common.entity.goal.EatObjectsGoal;
import com.momosoftworks.coldsweat.common.entity.goal.LazyLookGoal;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.init.EntityInit;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.core.network.message.ChameleonEatMessage;
import com.momosoftworks.coldsweat.core.network.message.EntityMountMessage;
import com.momosoftworks.coldsweat.data.loot.ModLootTables;
import com.momosoftworks.coldsweat.data.tag.ModItemTags;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import com.momosoftworks.coldsweat.util.registries.ModSounds;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.advancements.Advancement;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraft.network.datasync.DataParameter;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.*;

@Mod.EventBusSubscriber
public class ChameleonEntity extends AnimalEntity
{
    static Method GET_DATA_ITEM = ObfuscationReflectionHelper.findMethod(EntityDataManager.class, "func_187219_c", DataParameter.class);
    static
    {   GET_DATA_ITEM.setAccessible(true);
    }

    static final DataParameter<Boolean> SHEDDING = EntityDataManager.defineId(ChameleonEntity.class, DataSerializers.BOOLEAN);
    static final DataParameter<Integer> LAST_SHED = EntityDataManager.defineId(ChameleonEntity.class, DataSerializers.INT);
    static final DataParameter<Integer> HURT_TIMESTAMP = EntityDataManager.defineId(ChameleonEntity.class, DataSerializers.INT);
    static final DataParameter<CompoundNBT> TRUSTED_PLAYERS = EntityDataManager.defineId(ChameleonEntity.class, DataSerializers.COMPOUND_TAG);
    static final DataParameter<BlockPos> TRACKING_POS = EntityDataManager.defineId(ChameleonEntity.class, DataSerializers.BLOCK_POS);
    static final DataParameter<Integer> EAT_TIMESTAMP = EntityDataManager.defineId(ChameleonEntity.class, DataSerializers.INT);
    static final DataParameter<Float> TEMPERATURE = EntityDataManager.defineId(ChameleonEntity.class, DataSerializers.FLOAT);
    static final DataParameter<CompoundNBT> EDIBLE_COOLDOWNS = EntityDataManager.defineId(ChameleonEntity.class, DataSerializers.COMPOUND_TAG);
    static final DataParameter<Boolean> SEARCHING = EntityDataManager.defineId(ChameleonEntity.class, DataSerializers.BOOLEAN);
    static final DataParameter<Integer> AGE_SECS = EntityDataManager.defineId(ChameleonEntity.class, DataSerializers.INT);

    public float xRotHead = 0;
    public float yRotHead = 0;
    public float xRotLeftEye = 0;
    public float yRotLeftEye = 0;
    public float xRotRightEye = 0;
    public float yRotRightEye = 0;
    public float xRotTail = 0;
    public float tailPhase = 0;

    float eatAnimationTimer = 0;
    private int feedCooldown = 0;
    public float opacity = 1;
    float desiredTemp = 1f;

    boolean mountSneaking = false;
    int mountSneakCount = 0;
    int lastMountSneak = 0;

    public ChameleonEntity(EntityType<ChameleonEntity> type, World world)
    {
        super(type, world);
    }

    @Override
    protected void registerGoals()
    {   this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new EatObjectsGoal(this, Arrays.asList(EntityType.SILVERFISH)));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.25, Ingredient.of(ChameleonEdibles.EDIBLES.stream().map(edible -> edible.associatedItems().getValues()).flatMap(Collection::stream)
                                                                                     .map(item -> (IItemProvider) () -> item).toArray(IItemProvider[]::new)), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(5, new LazyLookGoal(this));
        this.goalSelector.addGoal(5, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
    }

    public static AttributeModifierMap.MutableAttribute createAttributes()
    {
        return TameableEntity.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.16D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        this.entityData.define(SHEDDING, false);
        this.entityData.define(LAST_SHED, 0);
        this.entityData.define(HURT_TIMESTAMP, 0);
        this.entityData.define(TRUSTED_PLAYERS, new CompoundNBT());
        this.entityData.define(TRACKING_POS, BlockPos.ZERO);
        this.entityData.define(EAT_TIMESTAMP, 0);
        this.entityData.define(TEMPERATURE, (float) CSMath.average(ConfigSettings.MIN_TEMP.get(), ConfigSettings.MAX_TEMP.get()));
        this.entityData.define(EDIBLE_COOLDOWNS, new CompoundNBT());
        this.entityData.define(SEARCHING, false);
        this.entityData.define(AGE_SECS, 0);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source)
    {
        if (this.getVehicle() instanceof PlayerEntity)
        {
            PlayerEntity player = (PlayerEntity) this.getVehicle();
            if (source.equals(DamageSource.IN_WALL) || source.equals(DamageSource.FALL)) return true;
            return player.isCreative() || player.isInvulnerableTo(source);
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public void die(DamageSource source)
    {
        super.die(source);

        if (this.dead)
        {
            this.getCombatTracker().recordDamage(source, this.getHealth(), 1);
            ITextComponent deathMessage = this.getCombatTracker().getDeathMessage();
            ListNBT trustedPlayers = this.getTrustedPlayers().getList("Players", 8);

            if (!this.level.isClientSide && this.level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES))
            {   trustedPlayers.forEach(string ->
                {
                    PlayerEntity player = this.level.getPlayerByUUID(UUID.fromString(string.getAsString()));
                    if (player != null)
                    {   player.sendMessage(deathMessage, Util.NIL_UUID);
                    }
                });
            }
        }
    }

    @Override
    public ActionResultType mobInteract(PlayerEntity player, Hand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        Edible edible = ChameleonEdibles.getEdible(stack).orElse(null);

        if (edible != null)
        {
            if (this.feedCooldown <= 0 && ((this.isPlayerTrusted(player) ^ this.isTamingItem(stack))
            && this.getCooldown(edible) <= 0) || this.canFallInLove() && this.isFood(stack))
            {
                if (!player.level.isClientSide)
                {
                    ItemStack dropStack = stack.copy();
                    dropStack.setCount(1);
                    ItemEntity dropped = player.drop(dropStack, true);
                    if (dropped != null)
                    {   dropped.getPersistentData().putUUID("Recipient", this.getUUID());
                    }
                    player.stopUsingItem();
                    this.usePlayerItem(player, stack);
                }
                this.feedCooldown = 10;

                return ActionResultType.SUCCESS;
            }
            else
            {   player.swing(hand);
                return ActionResultType.CONSUME;
            }
        }
        else if (this.isPlayerTrusted(player) && player.getPassengers().isEmpty() && !this.level.isClientSide)
        {
            if (this.startRiding(player) && player instanceof ServerPlayerEntity)
            {   ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), new EntityMountMessage(this.getId(), player.getId(), EntityMountMessage.Action.MOUNT));
            }
            return ActionResultType.SUCCESS;
        }

        return ActionResultType.PASS;
    }

    @Override
    public boolean isFood(ItemStack pStack)
    {   return ModItemTags.CHAMELEON_TAMING.contains(pStack.getItem());
    }

    @Override
    public boolean canFallInLove()
    {   return super.canFallInLove() && !this.isBaby() && !this.isInLove()
            && !this.getPersistentData().getBoolean("HasBred");
    }

    @Override
    protected void ageBoundaryReached()
    {
        super.ageBoundaryReached();
        if (!this.isBaby())
        {   this.shedItems();
        }
    }

    private void shedItems()
    {
        for (ItemStack stack : ModLootTables.getEntityDropsLootTable(this, null, ModLootTables.CHAMELEON_SHEDDING))
        {   WorldHelper.entityDropItem(this, stack, 40000);
        }
        WorldHelper.playEntitySound(ModSounds.CHAMELEON_SHED, this, this.getSoundSource(), 1, this.getVoicePitch());
    }

    @Override
    public float getEyeHeight(Pose pose)
    {   return this.isBaby() ? 0.25F : 0.35F;
    }

    @SubscribeEvent
    public static void setHeight(EntityEvent.Size event)
    {
        if (event.getEntity() instanceof ChameleonEntity)
        {
            ChameleonEntity chameleon = ((ChameleonEntity) event.getEntity());
            if (chameleon.isBaby())
            {   event.setNewSize(EntitySize.fixed(0.65f, 0.5f));
            }
        }
    }

    @SubscribeEvent
    public static void onHitFromOwner(LivingHurtEvent event)
    {
        if (event.getEntity() instanceof ChameleonEntity)
        {
            ChameleonEntity chameleon = ((ChameleonEntity) event.getEntity());
            if (chameleon.getVehicle() != null && chameleon.getVehicle() == event.getSource().getEntity())
            {   event.setCanceled(true);
            }
        }
    }

    public int getTimeToShed()
    {   return 600;
    }

    public int getEatAnimLength()
    {
        return 6;
    }

    @Override
    public AgeableEntity getBreedOffspring(ServerWorld world, AgeableEntity parent)
    {   return EntityInit.CHAMELEON.get().create(world);
    }

    @Override
    public void spawnChildFromBreeding(ServerWorld level, AnimalEntity mate)
    {
        super.spawnChildFromBreeding(level, mate);
        this.getPersistentData().putBoolean("HasBred", true);
        mate.getPersistentData().putBoolean("HasBred", true);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound()
    {   return ModSounds.CHAMELEON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source)
    {   return ModSounds.CHAMELEON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound()
    {   return ModSounds.CHAMELEON_DEATH;
    }

    @Override
    public void playAmbientSound()
    {
        SoundEvent soundevent = this.getAmbientSound();
        if (!this.level.isClientSide && soundevent != null && !this.isSearching())
        {   // This method plays a sound that actually follows the entity
            WorldHelper.playEntitySound(soundevent, this, this.getSoundSource(), this.getSoundVolume(), this.getVoicePitch());
        }
    }

    @Override
    protected void playHurtSound(DamageSource damageSource)
    {
        SoundEvent soundevent = this.getHurtSound(damageSource);
        if (soundevent != null)
        {   // This method plays a sound that actually follows the entity
            WorldHelper.playEntitySound(soundevent, this, this.getSoundSource(), this.getSoundVolume(), this.getVoicePitch());
        }
    }

    public boolean isWalking()
    {   return new Vector3d((float) getDeltaMovement().x, 0, (float) getDeltaMovement().z).length() > 0.01;
    }

    @Override
    public int getHeadRotSpeed()
    {   return 20;
    }

    @Override
    public void tick()
    {
        super.tick();

        // Tick eat animation
        if (this.eatAnimationTimer > 0)
        {   this.eatAnimationTimer--;
        }

        if (this.feedCooldown > 0)
            this.feedCooldown--;

        // Age
        if (!this.level.isClientSide && this.tickCount % 20 == 0)
        {   this.setAgeSecs(this.getAgeSecs() + 1);
        }

        // Tick shedding
        if (!this.level.isClientSide)
        {
            boolean shedding = this.isShedding();
            int shedCheckInterval = ConfigSettings.SHED_TIMINGS.get().interval();
            int shedCooldown = ConfigSettings.SHED_TIMINGS.get().cooldown();
            double shedChance = ConfigSettings.SHED_TIMINGS.get().chance();
            if (this.tickCount % shedCheckInterval == 0 && !shedding
            && this.random.nextDouble() < shedChance && this.getAgeSecs() * 20 - this.getLastShed() > shedCooldown)
            {
                this.setShedding(true);
                this.setLastShed(this.getAgeSecs() * 20);
            }

            if (shedding && this.getAgeSecs() * 20 - this.getLastShed() > this.getTimeToShed())
            {
                this.shedItems();
                this.setLastShed(this.getAgeSecs() * 20);
                this.setShedding(false);
            }
        }

        // Follow the player's movements when riding
        if (this.getVehicle() instanceof PlayerEntity)
        {   PlayerEntity player = (PlayerEntity) this.getVehicle();
            float playerHeadYaw = player.yHeadRot;
            this.yHeadRot = CSMath.clamp(this.yHeadRot, playerHeadYaw - 50, playerHeadYaw + 50);
            this.yBodyRot = playerHeadYaw;
        }

        // Control temperature
        if (this.tickCount % 20 == 0)
        {   // Use the player's temperature if mounted
            this.desiredTemp = (float) CSMath.clamp(Temperature.get(this, Temperature.Trait.WORLD), ConfigSettings.MIN_TEMP.get(), ConfigSettings.MAX_TEMP.get());
        }
        this.setTemperature(this.getTemperature() + (this.desiredTemp - this.getTemperature()) * 0.03f);

        // Handle dismounting
        if (this.getVehicle() instanceof PlayerEntity && !this.level.isClientSide)
        {
            PlayerEntity player = (PlayerEntity) this.getVehicle();
            if (player.isCrouching())
            {
                if (!mountSneaking)
                {
                    if (player.tickCount - lastMountSneak < 8 || mountSneakCount == 0)
                    {   mountSneakCount++;
                    }
                    else
                    {   mountSneakCount = 0;
                    }
                    lastMountSneak = player.tickCount;
                    mountSneaking = true;

                    if (mountSneakCount >= 2)
                    {
                        this.stopRiding();
                        if (player instanceof ServerPlayerEntity)
                        {   ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), new EntityMountMessage(this.getId(), player.getId(), EntityMountMessage.Action.DISMOUNT));
                        }
                        this.boardingCooldown = 10;
                        mountSneakCount = 0;
                    }
                }
            }
            else mountSneaking = false;
        }

        // Spawn particles if tracking a position. After 5 minutes, clear the position
        if (this.tickCount % 5 == 0 && this.isTracking())
        {
            if (this.random.nextDouble() < 0.3)
            {
                WorldHelper.spawnParticle(this.level, ParticleTypes.HAPPY_VILLAGER,
                                          this.getX() + Math.random() - 0.5, this.getY() + Math.random() - 0.5 + this.getBbHeight() / 2, this.getZ() + Math.random() - 0.5,
                                          0.01, 0.01, 0.01);
            }

            if (this.tickCount % 20 == 0)
            {
                if (this.getAgeSecs() * 20L - this.getEatTimestamp() > 6000)
                {   this.clearTrackingPos();
                }
                // Award nearby players the "chameleon_find_biome" advancement
                if ((Math.sqrt(Math.pow(this.getX() - this.getTrackingPos().getX(), 2)
                             + Math.pow(this.getZ() - this.getTrackingPos().getZ(), 2)) < 20
                || this.getTrackingPos().equals(BlockPos.ZERO))
                && this.getServer() != null)
                {
                    Advancement advancement = this.getServer().getAdvancements().getAdvancement(new ResourceLocation(ColdSweat.MOD_ID, "chameleon_find_biome"));
                    for (ServerPlayerEntity player : this.level.getEntitiesOfClass(ServerPlayerEntity.class, this.getBoundingBox().inflate(20)))
                    {
                        if (advancement != null)
                        {
                            if (player.getAdvancements().getOrStartProgress(advancement).isDone())
                                continue;
                            player.getAdvancements().award(advancement, "requirement");
                        }
                    }
                    this.clearTrackingPos();
                }
            }
        }

        // Tick cooldowns
        if (!level.isClientSide)
        {
            CompoundNBT cooldowns = this.getCooldowns();
            for (String tag : cooldowns.getAllKeys())
            {
                int time = cooldowns.getInt(tag);
                if (time > 0)
                    cooldowns.putInt(tag, time - 1);
            }
            this.entityData.set(EDIBLE_COOLDOWNS, cooldowns);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        if (source.getEntity() != null && !this.isInvulnerableTo(source))
        {   this.setHurtTimestamp(this.tickCount);
        }
        return super.hurt(source, amount);
    }

    @Override
    public Entity changeDimension(ServerWorld world, ITeleporter teleporter)
    {   this.clearTrackingPos();
        return super.changeDimension(world, teleporter);
    }

    public void onEatEntity(Entity entity)
    {
        if (!level.isClientSide)
        {
            if (entity instanceof ItemEntity)
            {
                ItemEntity itemEntity = (ItemEntity) entity;
                ItemStack item = itemEntity.getItem();
                if (this.isTamingItem(item))
                {
                    PlayerEntity player = itemEntity.getThrower() != null ? this.level.getPlayerByUUID(itemEntity.getThrower()) : null;
                    if (player != null)
                    {
                        // For taming
                        if (!this.isPlayerTrusted(player))
                        {
                            if ((player.isCreative() || Math.random() < 0.3))
                            {
                                this.setPersistenceRequired();
                                this.addTrustedPlayer(itemEntity.getThrower());
                                WorldHelper.spawnParticleBatch(this.level, ParticleTypes.HEART, this.getX(), this.getY() + 0.5, this.getZ(), 1, 1, 1, 6, 0.01);
                            }
                            else
                            {   WorldHelper.spawnParticleBatch(this.level, ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 1, 1, 1, 6, 0.01);
                            }
                        }
                        // For breeding
                        else if (this.canFallInLove())
                        {   this.setInLove(player);
                        }
                    }
                }
                ChameleonEdibles.getEdible(item).ifPresent(edible ->
                {
                    if (edible.onEaten(this, itemEntity) == Edible.Result.SUCCESS)
                    {   this.setCooldown(edible, edible.getCooldown());
                    }
                    else
                    {   this.setCooldown(edible, edible.getCooldown() / 4);
                    }
                });
            }
            this.setEatTimestamp(this.getAgeSecs() * 20);
        }
    }

    @Override
    public double getMyRidingOffset()
    {
        return this.getVehicle() instanceof PlayerEntity
               ? ((PlayerEntity) this.getVehicle()).getItemBySlot(EquipmentSlotType.HEAD).getItem() == ModItems.HOGLIN_HEADPIECE
                    ? 0.65 : 0.5
               : 0;
    }

    @Override
    public int getMaxHeadYRot()
    {
        return 60;
    }

    public void eatAnimation()
    {
        if (this.eatAnimationTimer <= 0)
        {
            if (!this.level.isClientSide)
            {   ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new ChameleonEatMessage(this.getId()));
            }
            else
            {   AnimationManager.ANIMATION_TIMERS.put(this, 0f);
            }
            this.eatAnimationTimer = this.getEatAnimLength();
        }
    }

    public void manualSync(DataParameter<?> param)
    {
        // Forge refuses to sync some DataItems, like CompoundNBTs
        try
        {   ((EntityDataManager.DataEntry<?>) GET_DATA_ITEM.invoke(this.entityData, param)).setDirty(true);
        } catch (Exception ignored) {}
    }

    public float getEatTimer()
    {
        return this.eatAnimationTimer;
    }

    public boolean isShedding()
    {
        return this.entityData.get(SHEDDING);
    }

    public void setShedding(boolean shedding)
    {
        this.entityData.set(SHEDDING, shedding);
    }

    public CompoundNBT getTrustedPlayers()
    {
        return this.entityData.get(TRUSTED_PLAYERS);
    }

    public void addTrustedPlayer(UUID player)
    {
        CompoundNBT trustedPlayers = this.getTrustedPlayers();
        ListNBT players = trustedPlayers.getList("Players", 8);
        StringNBT uuid = StringNBT.valueOf(player.toString());
        if (!players.contains(uuid))
        {   players.add(uuid);
        }
        trustedPlayers.put("Players", players);

        this.entityData.set(TRUSTED_PLAYERS, trustedPlayers);
        this.manualSync(TRUSTED_PLAYERS);
    }

    public void removeTrustedPlayer(UUID player)
    {
        CompoundNBT trustedPlayers = this.getTrustedPlayers();
        trustedPlayers.getList("Players", 8).removeIf(tag -> tag.getAsString().equals(player.toString()));
        this.entityData.set(TRUSTED_PLAYERS, trustedPlayers);
    }

    public boolean isPlayerTrusted(PlayerEntity player)
    {
        return this.isPlayerTrusted(player.getUUID());
    }

    public boolean isPlayerTrusted(UUID player)
    {
        return this.getTrustedPlayers().getList("Players", 8).contains(StringNBT.valueOf(player.toString()));
    }

    public int getLastShed()
    {
        return this.entityData.get(LAST_SHED);
    }

    public void setLastShed(int lastShed)
    {
        this.entityData.set(LAST_SHED, lastShed);
    }

    public float getTemperature()
    {
        return this.entityData.get(TEMPERATURE);
    }

    public void setTemperature(float temperature)
    {
        this.entityData.set(TEMPERATURE, temperature);
    }

    public void setSearching(boolean searching)
    {
        this.entityData.set(SEARCHING, searching);
    }

    public boolean isSearching()
    {
        return this.entityData.get(SEARCHING);
    }

    public int getHurtTimestamp()
    {
        return this.entityData.get(HURT_TIMESTAMP);
    }

    public void setHurtTimestamp(int hurtTimestamp)
    {
        this.entityData.set(HURT_TIMESTAMP, hurtTimestamp);
    }

    public void setTrackingPos(BlockPos pos)
    {
        this.entityData.set(TRACKING_POS, pos);
    }

    public void clearTrackingPos()
    {
        this.entityData.set(TRACKING_POS, BlockPos.ZERO);
    }

    public BlockPos getTrackingPos()
    {
        return this.entityData.get(TRACKING_POS);
    }

    public boolean isTracking()
    {
        return !this.entityData.get(TRACKING_POS).equals(BlockPos.ZERO);
    }

    public long getEatTimestamp()
    {
        return this.entityData.get(EAT_TIMESTAMP);
    }

    public void setEatTimestamp(int eatTimestamp)
    {
        this.entityData.set(EAT_TIMESTAMP, eatTimestamp);
    }

    public void setCooldown(Edible edible, Integer time)
    {   CompoundNBT map = this.entityData.get(EDIBLE_COOLDOWNS);
        map.putInt(edible.getName(), time);
        this.entityData.set(EDIBLE_COOLDOWNS, map);
    }

    public Integer getCooldown(Edible edible)
    {
        return this.entityData.get(EDIBLE_COOLDOWNS).getInt(edible.getName());
    }

    public CompoundNBT getCooldowns()
    {
        return this.entityData.get(EDIBLE_COOLDOWNS);
    }

    public int getAgeSecs()
    {
        return this.entityData.get(AGE_SECS);
    }

    public void setAgeSecs(int ageSecs)
    {
        this.entityData.set(AGE_SECS, ageSecs);
    }

    public boolean isTamingItem(ItemStack item)
    {
        return ModItemTags.CHAMELEON_TAMING.contains(item.getItem());
    }


    @Override
    public CompoundNBT saveWithoutId(CompoundNBT tag)
    {
        super.saveWithoutId(tag);

        tag.put("TrustedPlayers", this.getTrustedPlayers().getList("Players", 8));
        tag.putInt("LastShed", this.getLastShed());
        tag.putBoolean("Shedding", this.isShedding());
        tag.putInt("HurtTimestamp", this.getHurtTimestamp());
        tag.putInt("AgeInSeconds", this.getAgeSecs());
        tag.putLong("EatTimestamp", this.getEatTimestamp());

        // Tracking pos
        if (this.isTracking())
            tag.putLong("TrackingPos", this.getTrackingPos().asLong());
        else tag.remove("TrackingPos");

        // Edible cooldowns
        ListNBT edibleCooldowns = new ListNBT();
        for (String key : this.getCooldowns().getAllKeys())
        {   CompoundNBT cooldownTag = new CompoundNBT();
            cooldownTag.putString("Edible", key);
            cooldownTag.putInt("Cooldown", this.getCooldowns().getInt(key));
            edibleCooldowns.add(cooldownTag);
        }
        tag.put("EdibleCooldowns", edibleCooldowns);

        tag.putFloat("Temperature", this.getTemperature());
        return tag;
    }

    @Override
    public void load(CompoundNBT nbt)
    {
        super.load(nbt);

        CompoundNBT players = new CompoundNBT();
        players.put("Players", nbt.getList("TrustedPlayers", 8));
        this.entityData.set(TRUSTED_PLAYERS, players);

        this.setLastShed(nbt.getInt("LastShed"));
        this.setShedding(nbt.getBoolean("Shedding"));
        this.setHurtTimestamp(nbt.getInt("HurtTimestamp"));
        this.setAgeSecs(nbt.getInt("AgeInSeconds"));
        this.setEatTimestamp(nbt.getInt("EatTimestamp"));

        if (nbt.contains("TrackingPos"))
            this.setTrackingPos(BlockPos.of(nbt.getLong("TrackingPos")));

        ListNBT edibleCooldowns = nbt.getList("EdibleCooldowns", 10);
        for (int i = 0; i < edibleCooldowns.size(); i++)
        {
            CompoundNBT cooldownTag = edibleCooldowns.getCompound(i);
            ChameleonEdibles.EDIBLES.stream().filter(ed -> ed.getName().equals(cooldownTag.getString("Item"))).findFirst().ifPresent(edible ->
            {   this.setCooldown(edible, cooldownTag.getInt("Cooldown"));
            });
        }
        this.setTemperature(nbt.getFloat("Temperature"));
    }
}
