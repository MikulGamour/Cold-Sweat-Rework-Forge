package com.momosoftworks.coldsweat.common.capability.handler;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.common.capability.ModCapabilities;
import com.momosoftworks.coldsweat.common.capability.shearing.IShearableCap;
import com.momosoftworks.coldsweat.common.capability.shearing.ShearableFurCap;
import com.momosoftworks.coldsweat.common.entity.GoatEntity;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.core.network.message.SyncShearableDataMessage;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.EntityDropData;
import com.momosoftworks.coldsweat.data.loot.ModLootTables;
import com.momosoftworks.coldsweat.util.serialization.Triplet;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.*;
import net.minecraft.world.Difficulty;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

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
        if (event.getObject() instanceof GoatEntity || CompatManager.isGoat(event.getObject()))
        {
            // Make a new capability instance to attach to the entity
            IShearableCap cap = new ShearableFurCap();
            // Optional that holds the capability instance
            LazyOptional<IShearableCap> capOptional = LazyOptional.of(() -> cap);
            Capability<IShearableCap> capability = ModCapabilities.SHEARABLE_FUR;

            ICapabilityProvider provider = new ICapabilitySerializable<CompoundNBT>()
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
                public CompoundNBT serializeNBT()
                {   return cap.serializeNBT();
                }

                @Override
                public void deserializeNBT(CompoundNBT nbt)
                {   cap.deserializeNBT(nbt);
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
        PlayerEntity player = event.getPlayer();
        ItemStack stack = event.getItemStack();

        if (entity instanceof LivingEntity && (!(entity instanceof AgeableEntity) || !((AgeableEntity) entity).isBaby())
        && !entity.level.isClientSide && Tags.Items.SHEARS.contains(stack.getItem()))
        {
            getFurCap(entity).ifPresent(cap ->
            {
                if (cap.isSheared())
                {   event.setResult(PlayerInteractEvent.Result.DENY);
                    return;
                }

                // Use shears
                player.swing(event.getHand(), true);
                stack.hurtAndBreak(1, event.getPlayer(), (p) -> p.broadcastBreakEvent(event.getHand()));
                // Play sound
                entity.level.playSound(null, entity, SoundEvents.SHEEP_SHEAR, SoundCategory.NEUTRAL, 1.0F, 1.0F);

                // Spawn item(s)
                for (ItemStack item : ModLootTables.getEntityDropsLootTable(entity, player, ModLootTables.GOAT_SHEARING))
                {   WorldHelper.entityDropItem(entity, item);
                }

                // Set sheared
                cap.setSheared(true);
                cap.setFurGrowthCooldown(ConfigSettings.FUR_TIMINGS.get().cooldown());
                syncData(((LivingEntity) entity), null);
                event.setResult(PlayerInteractEvent.Result.ALLOW);
            });
        }
    }

    // Regrow goat fur
    @SubscribeEvent
    public static void onShearableEntityTick(LivingEvent.LivingUpdateEvent event)
    {
        LivingEntity entity = event.getEntityLiving();
        getFurCap(entity).ifPresent(cap ->
        {
            EntityDropData furConfig = ConfigSettings.FUR_TIMINGS.get();
            // Tick fur growth cooldown
            if (cap.furGrowthCooldown() > 0)
            {   cap.setFurGrowthCooldown(Math.min(cap.furGrowthCooldown() - 1, furConfig.cooldown()));
            }
            cap.setAge(cap.age() + 1);

            // Entity is goat, current tick is a multiple of the regrow time, and random chance succeeds
            if (!entity.level.isClientSide
            && cap.isSheared()
            && cap.age() % Math.max(1, furConfig.interval()) == 0
            && cap.furGrowthCooldown() == 0
            && entity.getRandom().nextDouble() < furConfig.chance())
            {
                WorldHelper.playEntitySound(SoundEvents.WOOL_HIT, entity, entity.getSoundSource(), 0.5f, 0.6f);
                WorldHelper.playEntitySound(SoundEvents.LLAMA_SWAG, entity, entity.getSoundSource(), 0.5f, 0.8f);

                // Spawn particles
                WorldHelper.spawnParticleBatch(entity.level, ParticleTypes.SPIT, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 0.5f, 0.5f, 0.5f, 10, 0.05f);
                // Set not sheared
                cap.setSheared(false);
                syncData(entity, null);
            }
        });
    }

    @SubscribeEvent
    public static void onEntityLoaded(PlayerEvent.StartTracking event)
    {
        if (event.getEntity() instanceof ServerPlayerEntity && event.getTarget() instanceof GoatEntity)
        {   syncData((LivingEntity) event.getTarget(), (ServerPlayerEntity) event.getEntity());
        }
    }

    public static void syncData(LivingEntity entity, ServerPlayerEntity player)
    {
        if (!entity.level.isClientSide)
        {   getFurCap(entity).ifPresent(cap ->
            {   ColdSweatPacketHandler.INSTANCE.send(player != null ? PacketDistributor.PLAYER.with(() -> player)
                                                                    : PacketDistributor.TRACKING_ENTITY.with(() -> entity),
                                                     new SyncShearableDataMessage(entity.getId(), cap.serializeNBT()));
            });
        }
    }
}
