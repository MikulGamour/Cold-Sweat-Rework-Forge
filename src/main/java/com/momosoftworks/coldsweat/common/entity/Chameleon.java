package com.momosoftworks.coldsweat.common.entity;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.client.renderer.animation.AnimationManager;
import com.momosoftworks.coldsweat.common.entity.data.edible.ChameleonEdibles;
import com.momosoftworks.coldsweat.common.entity.data.edible.Edible;
import com.momosoftworks.coldsweat.common.entity.goals.EatObjectsGoal;
import com.momosoftworks.coldsweat.common.entity.goals.LazyLookGoal;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.init.ModEntities;
import com.momosoftworks.coldsweat.core.init.ModSounds;
import com.momosoftworks.coldsweat.core.network.message.ChameleonEatMessage;
import com.momosoftworks.coldsweat.core.network.message.EntityMountMessage;
import com.momosoftworks.coldsweat.data.loot.ModLootTables;
import com.momosoftworks.coldsweat.data.tag.ModItemTags;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec2;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@EventBusSubscriber
public class Chameleon extends Animal
{
    static final EntityDataAccessor<Boolean> SHEDDING = SynchedEntityData.defineId(Chameleon.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Integer> LAST_SHED = SynchedEntityData.defineId(Chameleon.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> HURT_TIMESTAMP = SynchedEntityData.defineId(Chameleon.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<CompoundTag> TRUSTED_PLAYERS = SynchedEntityData.defineId(Chameleon.class, EntityDataSerializers.COMPOUND_TAG);
    static final EntityDataAccessor<BlockPos> TRACKING_POS = SynchedEntityData.defineId(Chameleon.class, EntityDataSerializers.BLOCK_POS);
    static final EntityDataAccessor<Integer> EAT_TIMESTAMP = SynchedEntityData.defineId(Chameleon.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Float> TEMPERATURE = SynchedEntityData.defineId(Chameleon.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<CompoundTag> EDIBLE_COOLDOWNS = SynchedEntityData.defineId(Chameleon.class, EntityDataSerializers.COMPOUND_TAG);
    static final EntityDataAccessor<Boolean> SEARCHING = SynchedEntityData.defineId(Chameleon.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Integer> AGE_SECS = SynchedEntityData.defineId(Chameleon.class, EntityDataSerializers.INT);

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

    public Chameleon(EntityType<Chameleon> type, Level Level)
    {
        super(type, Level);
    }

    @Override
    protected void registerGoals()
    {   this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new EatObjectsGoal(this, List.of(EntityType.SILVERFISH)));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.25, Ingredient.fromValues(ChameleonEdibles.EDIBLES
                                                                                     .stream()
                                                                                     .map(edible -> new Ingredient.TagValue(edible.associatedItems()))
                                                                                     .filter(ing -> ing.getItems().stream().noneMatch(ItemStack::isEmpty))), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(5, new LazyLookGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.16D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(SHEDDING, false);
        builder.define(LAST_SHED, 0);
        builder.define(HURT_TIMESTAMP, 0);
        builder.define(TRUSTED_PLAYERS, new CompoundTag());
        builder.define(TRACKING_POS, BlockPos.ZERO);
        builder.define(EAT_TIMESTAMP, 0);
        builder.define(TEMPERATURE, (float) CSMath.average(ConfigSettings.MIN_TEMP.get(), ConfigSettings.MAX_TEMP.get()));
        builder.define(EDIBLE_COOLDOWNS, new CompoundTag());
        builder.define(SEARCHING, false);
        builder.define(AGE_SECS, 0);
    }

    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource source)
    {
        if (this.getVehicle() instanceof Player player)
        {   DamageSources damageSources = player.level().damageSources();
            if (source.equals(damageSources.inWall()) || source.equals(damageSources.fall())) return true;
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
            this.getCombatTracker().recordDamage(source, 1);
            Component deathMessage = this.getCombatTracker().getDeathMessage();
            ListTag trustedPlayers = this.getTrustedPlayers().getList("Players", 8);

            if (!this.level().isClientSide && this.level().getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES))
            {   trustedPlayers.forEach(string ->
                {
                    Player player = this.level().getPlayerByUUID(UUID.fromString(string.getAsString()));
                    if (player != null)
                    {   player.sendSystemMessage(deathMessage);
                    }
                });
            }
        }
    }

    @NotNull
    @Override
    public InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        Edible edible = ChameleonEdibles.getEdible(stack).orElse(null);

        if (edible != null)
        {
            if (this.feedCooldown <= 0 && ((this.isPlayerTrusted(player) ^ this.isTamingItem(stack))
            && this.getCooldown(edible) <= 0) || this.canFallInLove() && this.isFood(stack))
            {
                if (!player.level().isClientSide)
                {
                    ItemStack dropStack = stack.copy();
                    dropStack.setCount(1);
                    ItemEntity dropped = player.drop(dropStack, true);
                    if (dropped != null)
                    {   dropped.getPersistentData().putUUID("Recipient", this.getUUID());
                    }
                    player.stopUsingItem();
                    this.usePlayerItem(player, hand, stack);
                }
                this.feedCooldown = 10;

                return InteractionResult.SUCCESS;
            }
            else
            {   player.swing(hand);
                return InteractionResult.CONSUME;
            }
        }
        else if (this.isPlayerTrusted(player) && player.getPassengers().isEmpty() && !this.level().isClientSide)
        {
            if (this.startRiding(player) && player instanceof ServerPlayer serverPlayer)
            {   serverPlayer.connection.send(new EntityMountMessage(this.getId(), player.getId(), EntityMountMessage.Action.MOUNT));
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public boolean isFood(ItemStack pStack)
    {   return pStack.is(ModItemTags.CHAMELEON_TAMING);
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
        WorldHelper.playEntitySound(ModSounds.CHAMELEON_SHED.value(), this, this.getSoundSource(), 1, this.getVoicePitch());
    }

    @SubscribeEvent
    public static void setHeight(EntityEvent.Size event)
    {
        if (event.getEntity() instanceof Chameleon chameleon)
        {
            if (chameleon.isBaby())
            {   event.setNewSize(EntityDimensions.fixed(0.65f, 0.5f).withEyeHeight(0.25F));
            }
        }
    }

    @SubscribeEvent
    public static void onHitFromOwner(LivingIncomingDamageEvent event)
    {
        if (event.getEntity() instanceof Chameleon chameleon)
        {
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

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob parent)
    {   return ModEntities.CHAMELEON.get().create(level);
    }

    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal mate)
    {
        super.spawnChildFromBreeding(level, mate);
        this.getPersistentData().putBoolean("HasBred", true);
        mate.getPersistentData().putBoolean("HasBred", true);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound()
    {   return ModSounds.CHAMELEON_AMBIENT.value();
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source)
    {   return ModSounds.CHAMELEON_HURT.value();
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound()
    {   return ModSounds.CHAMELEON_DEATH.value();
    }

    @Override
    public void playAmbientSound()
    {
        SoundEvent soundevent = this.getAmbientSound();
        if (!this.level().isClientSide && soundevent != null && !this.isSearching())
        {   // This method plays a sound that actually follows the entity
            WorldHelper.playEntitySound(soundevent, this, this.getSoundSource(), this.getSoundVolume(), this.getVoicePitch());
        }
    }

    @Override
    protected void playHurtSound(@NotNull DamageSource damageSource)
    {
        SoundEvent soundevent = this.getHurtSound(damageSource);
        if (soundevent != null)
        {   // This method plays a sound that actually follows the entity
            WorldHelper.playEntitySound(soundevent, this, this.getSoundSource(), this.getSoundVolume(), this.getVoicePitch());
        }
    }

    public boolean isWalking()
    {   return new Vec2((float) getDeltaMovement().x, (float) getDeltaMovement().z).length() > 0.01;
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
            this.eatAnimationTimer--;

        if (this.feedCooldown > 0)
            this.feedCooldown--;

        // Age
        if (!this.level().isClientSide && this.tickCount % 20 == 0)
        {   this.setAgeSecs(this.getAgeSecs() + 1);
        }

        // Tick shedding
        if (!this.level().isClientSide)
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
        if (this.getVehicle() instanceof Player player)
        {
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
        if (this.getVehicle() instanceof Player player && !this.level().isClientSide)
        {
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
                        if (player instanceof ServerPlayer serverPlayer)
                        {   serverPlayer.connection.send(new EntityMountMessage(this.getId(), player.getId(), EntityMountMessage.Action.DISMOUNT));
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
                WorldHelper.spawnParticle(this.level(), ParticleTypes.HAPPY_VILLAGER,
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
                    AdvancementHolder advancement = this.getServer().getAdvancements().get(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "chameleon_find_biome"));
                    for (ServerPlayer player : this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(20)))
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
        if (!level().isClientSide)
        {
            CompoundTag cooldowns = this.getCooldowns();
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

    @Nullable
    @Override
    public Entity changeDimension(DimensionTransition teleportData)
    {   this.clearTrackingPos();
        return super.changeDimension(teleportData);
    }

    public void onEatEntity(Entity entity)
    {
        if (!level().isClientSide)
        {
            if (entity instanceof ItemEntity itemEntity)
            {
                ItemStack item = itemEntity.getItem();
                if (this.isTamingItem(item))
                {
                    Player player = itemEntity.getOwner() != null ? this.level().getPlayerByUUID(itemEntity.getOwner().getUUID()) : null;
                    if (player != null)
                    {
                        // For taming
                        if (!this.isPlayerTrusted(player))
                        {
                            if ((player.isCreative() || Math.random() < 0.3))
                            {
                                this.setPersistenceRequired();
                                this.addTrustedPlayer(itemEntity.getOwner().getUUID());
                                WorldHelper.spawnParticleBatch(this.level(), ParticleTypes.HEART, this.getX(), this.getY() + 0.5, this.getZ(), 1, 1, 1, 6, 0.01);
                            }
                            else
                            {   WorldHelper.spawnParticleBatch(this.level(), ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 1, 1, 1, 6, 0.01);
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

    public static boolean canSpawn(EntityType<Chameleon> type, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random)
    {   return true;
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
            if (!this.level().isClientSide)
            {   PacketDistributor.sendToPlayersTrackingEntity(this, new ChameleonEatMessage(this.getId()));
            }
            else
            {   AnimationManager.ANIMATION_TIMERS.put(this, 0f);
            }
            this.eatAnimationTimer = this.getEatAnimLength();
        }
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

    public CompoundTag getTrustedPlayers()
    {
        return this.entityData.get(TRUSTED_PLAYERS);
    }

    public void addTrustedPlayer(UUID player)
    {
        CompoundTag trustedPlayers = this.getTrustedPlayers();
        ListTag players = trustedPlayers.getList("Players", 8);
        StringTag uuid = StringTag.valueOf(player.toString());
        if (!players.contains(uuid))
        {   players.add(uuid);
        }
        trustedPlayers.put("Players", players);

        this.entityData.set(TRUSTED_PLAYERS, trustedPlayers, true);
    }

    public void removeTrustedPlayer(UUID player)
    {
        CompoundTag trustedPlayers = this.getTrustedPlayers();
        trustedPlayers.getList("Players", 8).removeIf(tag -> tag.getAsString().equals(player.toString()));
        this.entityData.set(TRUSTED_PLAYERS, trustedPlayers);
    }

    public boolean isPlayerTrusted(Player player)
    {
        return this.isPlayerTrusted(player.getUUID());
    }

    public boolean isPlayerTrusted(UUID player)
    {
        return this.getTrustedPlayers().getList("Players", 8).contains(StringTag.valueOf(player.toString()));
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
    {
        CompoundTag map = this.entityData.get(EDIBLE_COOLDOWNS);
        map.putInt(edible.associatedItems().location().toString(), time);
        this.entityData.set(EDIBLE_COOLDOWNS, map);
    }

    public Integer getCooldown(Edible edible)
    {
        return this.entityData.get(EDIBLE_COOLDOWNS).getInt(edible.getName());
    }

    public CompoundTag getCooldowns()
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
        return item.is(ModItemTags.CHAMELEON_TAMING);
    }


    @Override
    public CompoundTag saveWithoutId(CompoundTag tag)
    {
        super.saveWithoutId(tag);

        tag.put("TrustedPlayers", Objects.requireNonNullElseGet(this.getTrustedPlayers().get("Players"), ListTag::new));
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
        ListTag edibleCooldowns = new ListTag();
        for (String key : this.getCooldowns().getAllKeys())
        {
            CompoundTag cooldownTag = new CompoundTag();
            cooldownTag.putString("Edible", key);
            cooldownTag.putInt("Cooldown", this.getCooldowns().getInt(key));
            edibleCooldowns.add(cooldownTag);
        }
        tag.put("EdibleCooldowns", edibleCooldowns);

        tag.putFloat("Temperature", this.getTemperature());
        return tag;
    }

    @Override
    public void load(CompoundTag nbt)
    {
        super.load(nbt);

        CompoundTag players = new CompoundTag();
        players.put("Players", Objects.requireNonNullElseGet(nbt.get("TrustedPlayers"), ListTag::new));
        this.entityData.set(TRUSTED_PLAYERS, players, true);

        this.setLastShed(nbt.getInt("LastShed"));
        this.setShedding(nbt.getBoolean("Shedding"));
        this.setHurtTimestamp(nbt.getInt("HurtTimestamp"));
        this.setAgeSecs(nbt.getInt("AgeInSeconds"));
        this.setEatTimestamp(nbt.getInt("EatTimestamp"));

        if (nbt.contains("TrackingPos"))
            this.setTrackingPos(BlockPos.of(nbt.getLong("TrackingPos")));

        ListTag edibleCooldowns = nbt.getList("EdibleCooldowns", 10);
        for (int i = 0; i < edibleCooldowns.size(); i++)
        {
            CompoundTag cooldownTag = edibleCooldowns.getCompound(i);
            ChameleonEdibles.EDIBLES.stream().filter(ed -> ed.getName().equals(cooldownTag.getString("Item"))).findFirst().ifPresent(edible ->
            {   this.setCooldown(edible, cooldownTag.getInt("Cooldown"));
            });
        }
        this.setTemperature(nbt.getFloat("Temperature"));
    }
}
