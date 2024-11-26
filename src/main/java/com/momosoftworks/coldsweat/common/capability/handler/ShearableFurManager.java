package com.momosoftworks.coldsweat.common.capability.handler;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.common.capability.shearing.IShearableCap;
import com.momosoftworks.coldsweat.common.capability.ModCapabilities;
import com.momosoftworks.coldsweat.common.capability.shearing.ShearableFurCap;
import com.momosoftworks.coldsweat.core.event.TaskScheduler;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.core.network.message.SyncShearableDataMessage;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.loot.ModLootTables;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import oshi.util.tuples.Triplet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber
public class ShearableFurManager
{
    public static Map<Entity, LazyOptional<IShearableCap>> SERVER_CAP_CACHE = new HashMap<>();
    public static Map<Entity, LazyOptional<IShearableCap>> CLIENT_CAP_CACHE = new HashMap<>();

    @SubscribeEvent
    public static void attachCapabilityToEntityHandler(AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof Goat)
        {
            // Make a new capability instance to attach to the entity
            IShearableCap cap = new ShearableFurCap();
            // Optional that holds the capability instance
            LazyOptional<IShearableCap> capOptional = LazyOptional.of(() -> cap);
            Capability<IShearableCap> capability = ModCapabilities.SHEARABLE_FUR;

            ICapabilityProvider provider = new ICapabilitySerializable<CompoundTag>()
            {
                @Nonnull
                @Override
                public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction direction)
                {
                    // If the requested cap is the temperature cap, return the temperature cap
                    if (cap == capability)
                    {   return capOptional.cast();
                    }
                    return LazyOptional.empty();
                }

                @Override
                public CompoundTag serializeNBT()
                {
                    return cap.serializeNBT();
                }

                @Override
                public void deserializeNBT(CompoundTag nbt)
                {
                    cap.deserializeNBT(nbt);
                }
            };

            event.addCapability(new ResourceLocation(ColdSweat.MOD_ID, "fur"), provider);
        }
    }

    public static LazyOptional<IShearableCap> getFurCap(Entity entity)
    {
        Map<Entity, LazyOptional<IShearableCap>> cache = entity.level.isClientSide ? CLIENT_CAP_CACHE : SERVER_CAP_CACHE;
        return cache.computeIfAbsent(entity, e ->
        {   LazyOptional<IShearableCap> cap = e.getCapability(ModCapabilities.SHEARABLE_FUR);
            cap.addListener((opt) -> cache.remove(e));
            return cap;
        });
    }

    @SubscribeEvent
    public static void onShearGoat(PlayerInteractEvent.EntityInteract event)
    {
        Entity entity = event.getTarget();
        Player player = event.getPlayer();
        ItemStack stack = event.getItemStack();

        if (entity instanceof Goat goat && !goat.isBaby() && !goat.level.isClientSide && stack.is(Tags.Items.SHEARS))
        {
            getFurCap(goat).ifPresent(cap ->
            {
                if (cap.isSheared())
                {   event.setResult(PlayerInteractEvent.Result.DENY);
                    return;
                }

                // Use shears
                player.swing(event.getHand(), true);
                stack.hurtAndBreak(1, event.getPlayer(), (p) -> p.broadcastBreakEvent(event.getHand()));
                // Play sound
                goat.level.playSound(null, goat, SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 1.0F, 1.0F);

                // Spawn item(s)
                for (ItemStack item : ModLootTables.getEntityDropsLootTable(goat, player, ModLootTables.GOAT_SHEARING))
                {   WorldHelper.entityDropItem(goat, item);
                }

                // Random chance to ram the player when sheared
                stopGoals:
                if (!player.isCreative() && goat.level.getDifficulty() != Difficulty.PEACEFUL
                && !goat.level.isClientSide && goat.getRandom().nextDouble() < 0.4)
                {
                    // Set ram cooldown ticks
                    goat.getBrain().setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, 30);
                    // Stop active goals
                    for (WrappedGoal goal : goat.goalSelector.getAvailableGoals())
                    {
                        if (goal.isInterruptable())
                        {   goal.stop();
                        }
                        else break stopGoals;
                    }

                    // Start lowering head
                    TaskScheduler.scheduleServer(() ->
                    {
                        ClientboundEntityEventPacket packet = new ClientboundEntityEventPacket(goat, (byte) 58);
                        ((ServerChunkCache) goat.level.getChunkSource()).broadcastAndSend(goat, packet);
                    }, 5);

                    // Look at player
                    BehaviorUtils.lookAtEntity(goat, player);
                    // Stop walking
                    goat.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

                    // Set ram target to player pos
                    goat.getBrain().setMemory(MemoryModuleType.RAM_TARGET, player.position());
                    TaskScheduler.scheduleServer(() ->
                    {
                        if (player.distanceTo(goat) <= 10)
                        {
                            goat.playSound(goat.isScreamingGoat() ? SoundEvents.GOAT_SCREAMING_PREPARE_RAM : SoundEvents.GOAT_PREPARE_RAM, 1.0F, 1.0F);
                            goat.getBrain().setMemory(MemoryModuleType.RAM_TARGET, player.position());
                        }
                    }, 30);

                    // Trigger ram
                    goat.getBrain().setActiveActivityIfPossible(Activity.RAM);
                }

                // Set sheared
                cap.setSheared(true);
                cap.setLastSheared(goat.tickCount);
                entity.getPersistentData().putInt("FurGrowthCooldown", ConfigSettings.FUR_TIMINGS.get().getB());
                syncData(goat, null);
                event.setResult(PlayerInteractEvent.Result.ALLOW);
            });
        }
    }

    // Regrow goat fur
    @SubscribeEvent
    public static void onGoatTick(LivingEvent.LivingUpdateEvent event)
    {
        Entity entity = event.getEntity();
        if (!(entity instanceof Goat goat)) return;

        Triplet<Integer, Integer, Double> furConfig = ConfigSettings.FUR_TIMINGS.get();
        // Tick fur growth cooldown
        if (entity.getPersistentData().getInt("FurGrowthCooldown") > 0)
        {   entity.getPersistentData().putInt("FurGrowthCooldown", entity.getPersistentData().getInt("FurGrowthCooldown") - 1);
        }
        // Entity is goat, current tick is a multiple of the regrow time, and random chance succeeds
        if (!goat.level.isClientSide && goat.getAge() % Math.max(1, furConfig.getA()) == 0 && Math.random() < furConfig.getC())
        {
            getFurCap(goat).ifPresent(cap ->
            {
                // Growth cooldown has passed and goat is sheared
                if (entity.getPersistentData().getInt("FurGrowthCooldown") <= 0 && cap.isSheared())
                {
                    WorldHelper.playEntitySound(SoundEvents.WOOL_HIT, goat, goat.getSoundSource(), 0.5f, 0.6f);
                    WorldHelper.playEntitySound(SoundEvents.LLAMA_SWAG, goat, goat.getSoundSource(), 0.5f, 0.8f);

                    // Spawn particles
                    WorldHelper.spawnParticleBatch(goat.level, ParticleTypes.SPIT, goat.getX(), goat.getY() + goat.getBbHeight() / 2, goat.getZ(), 0.5f, 0.5f, 0.5f, 10, 0.05f);
                    // Set not sheared
                    cap.setSheared(false);
                    syncData(goat, null);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onEntityLoaded(PlayerEvent.StartTracking event)
    {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof Goat goat)
        {   syncData(goat, player);
        }
    }

    public static void syncData(Goat goat, ServerPlayer player)
    {
        if (!goat.level.isClientSide)
        {   getFurCap(goat).ifPresent(cap ->
            {   ColdSweatPacketHandler.INSTANCE.send(player != null ? PacketDistributor.PLAYER.with(() -> player)
                                                                    : PacketDistributor.TRACKING_ENTITY.with(() -> goat),
                                                     new SyncShearableDataMessage(cap.isSheared(), cap.lastSheared(), goat.getId()));
            });
        }
    }
}
