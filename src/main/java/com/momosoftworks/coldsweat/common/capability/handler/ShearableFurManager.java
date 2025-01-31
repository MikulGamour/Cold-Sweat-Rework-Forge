package com.momosoftworks.coldsweat.common.capability.handler;

import com.momosoftworks.coldsweat.common.capability.ModCapabilities;
import com.momosoftworks.coldsweat.common.capability.shearing.IShearableCap;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.event.TaskScheduler;
import com.momosoftworks.coldsweat.core.network.message.SyncShearableDataMessage;
import com.momosoftworks.coldsweat.data.codec.configuration.EntityDropData;
import com.momosoftworks.coldsweat.data.loot.ModLootTables;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import oshi.util.tuples.Triplet;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber
public class ShearableFurManager
{
    public static Map<Entity, IShearableCap> SERVER_CAP_CACHE = new HashMap<>();
    public static Map<Entity, IShearableCap> CLIENT_CAP_CACHE = new HashMap<>();

    public static Optional<IShearableCap> getFurCap(Entity entity)
    {
        Map<Entity, IShearableCap> cache = entity.level().isClientSide ? CLIENT_CAP_CACHE : SERVER_CAP_CACHE;
        return Optional.ofNullable(cache.computeIfAbsent(entity, e -> e.getCapability(ModCapabilities.SHEARABLE_FUR)));
    }

    @SubscribeEvent
    public static void onShearGoat(PlayerInteractEvent.EntityInteract event)
    {
        Entity entity = event.getTarget();
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (entity instanceof LivingEntity living && (!(living instanceof AgeableMob ageable) || !ageable.isBaby())
        && !entity.level().isClientSide && stack.is(Tags.Items.TOOLS_SHEAR))
        {
            IShearableCap cap = getFurCap(living).orElse(null);
            if (cap == null) return;
            if (cap.isSheared())
            {   event.setCancellationResult(InteractionResult.PASS);
                return;
            }

            // Use shears
            player.swing(event.getHand(), true);
            stack.hurtAndBreak(1, ((ServerLevel) event.getEntity().level()), event.getEntity(), (item) -> {});


            // Spawn item(s)
            shear(living, stack, player);

            // Random chance to ram the player when sheared

            if (living instanceof Goat goat && !player.getAbilities().instabuild && goat.level().getDifficulty() != Difficulty.PEACEFUL
            && !goat.level().isClientSide && goat.getRandom().nextDouble() < 0.4)
            {
                // Set ram cooldown ticks
                goat.getBrain().setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, 30);
                // Stop active goals
                for (WrappedGoal goal : goat.goalSelector.getAvailableGoals())
                {
                    if (goal.isInterruptable())
                    {   goal.stop();
                    }

                }

                // Start lowering head
                TaskScheduler.scheduleServer(() ->
                {
                    ClientboundEntityEventPacket packet = new ClientboundEntityEventPacket(goat, (byte) 58);
                    ((ServerChunkCache) goat.level().getChunkSource()).broadcastAndSend(goat, packet);
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


            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    // Regrow goat fur
    @SubscribeEvent
    public static void onGoatTick(EntityTickEvent.Post event)
    {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity living)) return;
        Optional<IShearableCap> icap = getFurCap(entity);
        icap.ifPresent(cap ->
        {
            EntityDropData furConfig = ConfigSettings.FUR_TIMINGS.get();
            // Tick fur growth cooldown
            if (cap.furGrowthCooldown() > 0)
            {   cap.setFurGrowthCooldown(Math.min(cap.furGrowthCooldown() - 1, furConfig.cooldown()));
            }
            cap.setAge(cap.age() + 1);

            // Entity is goat, current tick is a multiple of the regrow time, and random chance succeeds
            if (!entity.level().isClientSide
            && cap.isSheared()
            && cap.age() % Math.max(1, furConfig.interval()) == 0
            && cap.furGrowthCooldown() == 0
            && entity.getRandom().nextDouble() < furConfig.chance())
            {
                WorldHelper.playEntitySound(SoundEvents.WOOL_HIT, entity, entity.getSoundSource(), 0.5f, 0.6f);
                WorldHelper.playEntitySound(SoundEvents.LLAMA_SWAG.value(), entity, entity.getSoundSource(), 0.5f, 0.8f);

                // Spawn particles
                WorldHelper.spawnParticleBatch(entity.level(), ParticleTypes.SPIT, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 0.5f, 0.5f, 0.5f, 10, 0.05f);
                // Set not sheared
                cap.setSheared(false);
                syncData(living, null);
            }
        });
    }

    @SubscribeEvent
    public static void onEntityLoaded(PlayerEvent.StartTracking event)
    {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof Goat goat)
        {   syncData(goat, player);
        }
    }

    public static void shear(LivingEntity entity, ItemStack shears, @Nullable Player player)
    {
        getFurCap(entity).ifPresent(cap ->
        {
            if (!cap.isSheared() && !entity.level().isClientSide())
            {
                // Set sheared flag & cooldown
                cap.setSheared(true);
                cap.setFurGrowthCooldown(ConfigSettings.FUR_TIMINGS.get().cooldown());
                // Drop items
                for (ItemStack item : ModLootTables.getEntityDropsLootTable(entity, player, ModLootTables.GOAT_SHEARING))
                {   WorldHelper.entityDropItem(entity, item);
                }
                // Play sound
                entity.level().playSound(null, entity, SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 1.0F, 1.0F);
                // Damage shears
                if (player == null || !player.getAbilities().instabuild)
                {   shears.hurtAndBreak(1, ((ServerLevel) entity.level()), entity, (item) -> {});
                }
                // Sync shear data
                syncData(entity, null);
            }
        });
    }

    public static void syncData(LivingEntity entity, ServerPlayer player)
    {
        if (!entity.level().isClientSide)
        {
            getFurCap(entity).ifPresent(cap ->
            {
                if (player != null)
                {   PacketDistributor.sendToPlayer(player, new SyncShearableDataMessage(entity.getId(), cap.serializeNBT()));
                }
                else
                {   PacketDistributor.sendToPlayersTrackingEntity(entity, new SyncShearableDataMessage(entity.getId(), cap.serializeNBT()));
                }
            });
        }
    }
}
