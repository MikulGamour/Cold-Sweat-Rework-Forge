package com.momosoftworks.coldsweat.common.event;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModAttributes;
import com.momosoftworks.coldsweat.util.registries.ModEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

@Mod.EventBusSubscriber
public class TempEffectsCommon
{
    @SubscribeEvent
    public static void onPlayerMine(PlayerEvent.BreakSpeed event)
    {
        Player player = event.getEntity();
        if (EntityTempManager.immuneToTempEffects(player)) return;

        double miningSpeedReduction = ConfigSettings.COLD_MINING_IMPAIRMENT.get();

        if (miningSpeedReduction <= 0
        || player.hasEffect(ModEffects.ICE_RESISTANCE)
        || player.hasEffect(ModEffects.GRACE)) return;

        // Get the player's temperature
        float temp = (float) Temperature.get(player, Temperature.Trait.BODY);
        float miningSpeed = (float) (1d - miningSpeedReduction);

        // If the player is too cold, slow down their mining speed
        if (temp < -50)
        {
            float minMiningSpeed = CSMath.blend(miningSpeed, 1f, getTempResistance(player, true), 0, 4);
            // Get protection from armor underwear
            event.setNewSpeed(event.getNewSpeed() * CSMath.blend(minMiningSpeed, 1f, temp, -100, -50));
        }
    }

    // Decrease the player's movement speed if their temperature is below -50
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        Player player = event.player;
        if (EntityTempManager.immuneToTempEffects(player)) return;

        if (event.phase == TickEvent.Phase.END)
        {
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
                    float minMoveMultiplier = (float) CSMath.blend(player.isOnGround() ? movementSpeed : movementSpeed * 1.25, 1d, getColdResistance(player), 0d, 4d);
                    if (minMoveMultiplier != 1)
                    {
                        float moveSpeed = CSMath.blend(minMoveMultiplier, 1, temp, -100, -50);
                        player.setDeltaMovement(player.getDeltaMovement().multiply(moveSpeed, 1, moveSpeed));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerKnockback(LivingKnockBackEvent event)
    {
        if (event.getEntity().getLastHurtByMob() instanceof Player player)
        {
            if (EntityTempManager.immuneToTempEffects(player)) return;

            double knockbackReduction = ConfigSettings.COLD_KNOCKBACK_REDUCTION.get();
            if (knockbackReduction <= 0 || player.hasEffect(ModEffects.ICE_RESISTANCE) || player.hasEffect(ModEffects.GRACE)) return;

            float temp = (float) Temperature.get(player, Temperature.Trait.BODY);
            if (temp < -50f)
            {
                // Get protection from armor underwear
                float liningProtFactor = (float) CSMath.blend(1d - knockbackReduction, 1d, getColdResistance(player), 0d, 4d);
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
            if (EntityTempManager.immuneToTempEffects(player)) return;

            double frozenHeartsPercentage = ConfigSettings.HEARTS_FREEZING_PERCENTAGE.get();

            if (frozenHeartsPercentage <= 0
            || player.hasEffect(ModEffects.ICE_RESISTANCE)
            || player.hasEffect(ModEffects.GRACE)) return;

            float healing = event.getAmount();
            float temp = (float) Temperature.get(player, Temperature.Trait.BODY);
            if (temp < -50)
            {
                // Get protection from armor underwear
                float unfrozenHealth = (float) (CSMath.blend(1 - frozenHeartsPercentage, 1d, getColdResistance(player), 0d, 4d));
                if (unfrozenHealth != 1)
                {   event.setAmount(CSMath.clamp(healing, 0, CSMath.ceil(player.getMaxHealth() * CSMath.blend(unfrozenHealth, 1f, temp, -100f, -50f)) - player.getHealth()));
                }
            }
        }
    }

    public static int getColdResistance(Player player)
    {   return getTempResistance(player, true);
    }

    public static int getHeatResistance(Player player)
    {   return getTempResistance(player, false);
    }

    public static int getTempResistance(Player player, boolean cold)
    {
        int strength = 0;
        if (CompatManager.isArmorUnderwearLoaded())
        {
            strength += ((Collection<ItemStack>) player.getArmorSlots()).stream()
                    .map(stack -> cold ? CompatManager.ArmorUnderwear.hasOttoLiner(stack) : CompatManager.ArmorUnderwear.hasOllieLiner(stack))
                    .filter(Boolean::booleanValue)
                    .mapToInt(i -> 1).sum();
        }
        strength += CSMath.blend(0, 4, CSMath.getIfNotNull(cold ? player.getAttribute(ModAttributes.COLD_RESISTANCE)
                                                                : player.getAttribute(ModAttributes.HEAT_RESISTANCE), att -> att.getValue(), 0).floatValue(), 0, 1);
        return CSMath.clamp(strength, 0, 4);
    }
}
