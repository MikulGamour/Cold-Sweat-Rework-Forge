package com.momosoftworks.coldsweat.common.event;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.init.ModAttributes;
import com.momosoftworks.coldsweat.core.init.ModEffects;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class TempEffectsCommon
{
    @SubscribeEvent
    public static void onPlayerMine(PlayerEvent.BreakSpeed event)
    {
        Player player = event.getEntity();
        if (EntityTempManager.isPeacefulMode(player)) return;

        float miningSpeed = 1 - ConfigSettings.COLD_MINING_IMPAIRMENT.get().floatValue();

        if (miningSpeed == 1
        || player.hasEffect(ModEffects.ICE_RESISTANCE)
        || player.hasEffect(ModEffects.GRACE)) return;

        // Get the player's temperature
        float temp = (float) Temperature.get(player, Temperature.Trait.BODY);

        // If the player is too cold, slow down their mining speed
        if (temp < -50)
        {
            float minMiningSpeed = (float) CSMath.blend(miningSpeed, 1f, Temperature.get(player, Temperature.Trait.COLD_RESISTANCE), 0, 1);
            // Get protection from armor underwear
            event.setNewSpeed(event.getNewSpeed() * CSMath.blend(minMiningSpeed, 1f, temp, -100, -50));
        }
    }

    // Decrease the player's movement speed if their temperature is below -50
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event)
    {
        Player player = event.getEntity();
        if (EntityTempManager.isPeacefulMode(player)) return;
        float temp = (float) Temperature.get(player, Temperature.Trait.BODY);
        if (temp < -50)
        {
            double movementReduction = ConfigSettings.COLD_MOVEMENT_SLOWDOWN.get();
            double movementSpeed = 1 - movementReduction;

            if (movementSpeed == 1
            || player.hasEffect(ModEffects.ICE_RESISTANCE)
            || player.hasEffect(ModEffects.GRACE)) return;

            // If not elytra flying
            if (!player.isFallFlying())
            {
                // Get protection from armor underwear
                float minMoveMultiplier = (float) CSMath.blend(player.onGround() ? movementSpeed : movementSpeed * 1.25, 1d, Temperature.get(player, Temperature.Trait.COLD_RESISTANCE), 0d, 1d);
                if (minMoveMultiplier != 1)
                {
                    float moveSpeed = CSMath.blend(minMoveMultiplier, 1, temp, -100, -50);
                    player.setDeltaMovement(player.getDeltaMovement().multiply(moveSpeed, 1, moveSpeed));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerKnockback(LivingKnockBackEvent event)
    {
        if (event.getEntity().getLastHurtByMob() instanceof Player player)
        {
            if (EntityTempManager.isPeacefulMode(player)) return;

            double knockbackReduction = ConfigSettings.COLD_KNOCKBACK_REDUCTION.get();
            if (knockbackReduction <= 0 || player.hasEffect(ModEffects.ICE_RESISTANCE) || player.hasEffect(ModEffects.GRACE)) return;

            float temp = (float) Temperature.get(player, Temperature.Trait.BODY);
            if (temp < -50f)
            {
                // Get protection from armor underwear
                float liningProtFactor = (float) CSMath.blend(1d - knockbackReduction, 1d, Temperature.get(player, Temperature.Trait.COLD_RESISTANCE), 0d, 1d);
                if (liningProtFactor != 1f)
                {   event.setStrength(event.getStrength() * CSMath.blend(liningProtFactor, 1f, temp, -100f, -50f));
                }
            }
        }
    }

    // Prevent healing as temp decreases
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event)
    {
        if (event.getEntity() instanceof Player player)
        {
            if (EntityTempManager.isPeacefulMode(player)) return;

            double frozenHeartsPercentage = ConfigSettings.HEARTS_FREEZING_PERCENTAGE.get();

            if (frozenHeartsPercentage <= 0
            || player.hasEffect(ModEffects.ICE_RESISTANCE)
            || player.hasEffect(ModEffects.GRACE)) return;

            float healing = event.getAmount();
            float temp = (float) Temperature.get(player, Temperature.Trait.BODY);
            if (temp < -50)
            {
                // Get protection from armor underwear
                float unfrozenHealth = (float) (CSMath.blend(1 - frozenHeartsPercentage, 1d, Temperature.get(player, Temperature.Trait.COLD_RESISTANCE), 0d, 1d));
                if (unfrozenHealth != 1)
                {   event.setAmount(CSMath.clamp(healing, 0, CSMath.ceil(player.getMaxHealth() * CSMath.blend(unfrozenHealth, 1f, temp, -100f, -50f)) - player.getHealth()));
                }
            }
        }
    }

    public static int getTempResistance(Player player, boolean cold)
    {
        AttributeInstance tempAttribute = cold ? player.getAttribute(ModAttributes.COLD_RESISTANCE)
                                               : player.getAttribute(ModAttributes.HEAT_RESISTANCE);
        if (tempAttribute == null) return 0;
        return (int) CSMath.blend(0, 4, tempAttribute.getValue(), 0, 1);
    }
}
