package com.momosoftworks.coldsweat.common.item;

import com.momosoftworks.coldsweat.api.temperature.modifier.SoulLampTempModifier;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Placement;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.init.ModAdvancementTriggers;
import com.momosoftworks.coldsweat.core.init.ModItemComponents;
import com.momosoftworks.coldsweat.core.init.ModSounds;
import com.momosoftworks.coldsweat.core.network.message.ParticleBatchMessage;
import com.momosoftworks.coldsweat.data.codec.configuration.FuelData;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;
import java.util.Optional;

@EventBusSubscriber
public class SoulspringLampItem extends Item
{
    public SoulspringLampItem()
    {
        super(new Properties().stacksTo(1).fireResistant().rarity(Rarity.UNCOMMON)
                              .component(ModItemComponents.SOULSPRING_LAMP_LIT, false)
                              .component(ModItemComponents.SOULSPRING_LAMP_FUEL, 0d)
                              .component(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag())));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int itemSlot, boolean isSelected)
    {
        if (entity instanceof LivingEntity living && living.tickCount % 5 == 0)
        {
            boolean shouldBeOn = false;
            try
            {
                if (!(isSelected || living.getOffhandItem() == stack))
                {   return;
                }
                double max = Temperature.get(living, Temperature.Trait.BURNING_POINT);

                double temp = Temperature.getModifier(living, Temperature.Trait.WORLD, SoulLampTempModifier.class)
                              .map(TempModifier::getLastInput).orElseGet(() -> Temperature.get(living, Temperature.Trait.WORLD));

                // Is in valid dimension
                if ((ConfigSettings.LAMP_DIMENSIONS.get(level.registryAccess()).contains(level.dimensionType()))
                // Is world temp more than max
                && temp > max && getFuel(stack) > 0)
                {
                    // Drain fuel
                    if (!(living instanceof Player player && player.isCreative() || living.isSpectator()))
                    {   addFuel(stack, -0.005 * CSMath.clamp(temp - max, 1, 3));
                    }

                    // Affect nearby players
                    double radius = 5d;
                    AABB bb = new AABB(living.getX() - radius, living.getY() + (living.getBbHeight() / 2) - radius, living.getZ() - radius,
                                       living.getX() + radius, living.getY() + (living.getBbHeight() / 2) + radius, living.getZ() + radius);

                    if (Math.random() < 0.6)
                    {
                        AABB bb2 = bb.inflate(-3);
                        double x = bb2.minX + (bb2.maxX - bb2.minX) * Math.random();
                        double y = bb2.minY + (bb2.maxY - bb2.minY) * Math.random();
                        double z = bb2.minZ + (bb2.maxZ - bb2.minZ) * Math.random();
                        double xSpeed = (Math.random() - 0.5) * 0.02;
                        double zSpeed = (Math.random() - 0.5) * 0.02;
                        new ParticleBatchMessage(0).addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, xSpeed, 0, zSpeed)
                                                   .sendEntity(living);
                    }

                    for (LivingEntity ent : level.getEntitiesOfClass(LivingEntity.class, bb))
                    {
                        if (!EntityTempManager.isTemperatureEnabled(ent))
                        {   continue;
                        }
                        // Extend modifier time if it is present
                        Optional<SoulLampTempModifier> mod = Temperature.getModifier(ent, Temperature.Trait.WORLD, SoulLampTempModifier.class);
                        if (mod.isPresent())
                        {   mod.get().setTicksExisted(0);
                        }
                        else
                        {   Temperature.addOrReplaceModifier(ent, new SoulLampTempModifier().expires(5).tickRate(5), Temperature.Trait.WORLD, Placement.Duplicates.BY_CLASS);
                        }
                    }
                    shouldBeOn = true;
                }
            }
            finally
            {
                if (!level.isClientSide())
                {
                    CustomData itemTag = NBTHelper.getOrCreateTag(stack);
                    // If the conditions are not met, turn off the lamp
                    if (itemTag.copyTag().getInt("stateChangeTimer") <= 0
                    && CSMath.orElse(stack.get(ModItemComponents.SOULSPRING_LAMP_LIT), false) != shouldBeOn)
                    {
                        stack.set(DataComponents.CUSTOM_DATA, itemTag.update(tag -> tag.putInt("stateChangeTimer", 2)));
                        stack.set(ModItemComponents.SOULSPRING_LAMP_LIT, shouldBeOn);

                        if (getFuel(stack) < 0.5)
                            setFuel(stack, 0);

                        WorldHelper.playEntitySound(shouldBeOn ? ModSounds.SOUL_LAMP_ON.value() : ModSounds.SOUL_LAMP_OFF.value(), living, living.getSoundSource(), 1.5f, (float) Math.random() / 5f + 0.9f);
                    }
                    else
                    {   // Decrement the state change timer
                        NBTHelper.incrementTag(stack, "stateChangeTimer", -1, tag -> tag > 0);
                    }
                }
            }
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {   return slotChanged;
    }

    private static void setFuel(ItemStack stack, double fuel)
    {   stack.set(ModItemComponents.SOULSPRING_LAMP_FUEL, fuel);
    }

    private static void addFuel(ItemStack stack, double amount)
    {   setFuel(stack, Math.min(64, getFuel(stack) + amount));
    }

    private static void addFuel(ItemStack stack, ItemStack fuelStack)
    {   addFuel(stack, getFuelForStack(fuelStack) * fuelStack.getCount());
    }

    private static double getFuel(ItemStack stack)
    {   return stack.getOrDefault(ModItemComponents.SOULSPRING_LAMP_FUEL, 0d);
    }

    public static double getFuelForStack(ItemStack item)
    {
        return ConfigHelper.findFirstFuelMatching(ConfigSettings.SOULSPRING_LAMP_FUEL, item)
                .map(FuelData::fuel).orElse(0d).intValue();
    }

    // Restore fuel if player hits an enemy
    @SubscribeEvent
    public static void onEntityHit(LivingIncomingDamageEvent event)
    {
        if (event.getSource().getEntity() instanceof Player attacker && !(event.getEntity() instanceof Player))
        {
            Level level = attacker.level();
            ItemStack stack = attacker.getMainHandItem();
            if (!(stack.getItem() instanceof SoulspringLampItem)) return;

            LivingEntity target = event.getEntity();

            // If fuel < 64 and target NOT player
            if (getFuel(stack) < 64
            && !target.getType().is(EntityTypeTags.UNDEAD)
            && !target.getPersistentData().getBoolean("SoulSucked"))
            {
                target.getPersistentData().putBoolean("SoulSucked", true);

                // Add fuel
                addFuel(stack, (int) Math.min(8, target.getMaxHealth() / 2));
                float extraDamage = Math.max(0, 8 - event.getAmount());
                if (extraDamage > 0)
                    target.hurt(level.damageSources().playerAttack(attacker), extraDamage);

                // Spawn particles
                if (!target.level().isClientSide)
                {
                    int particleCount = (int) CSMath.clamp(target.getBbWidth() * target.getBbWidth() * target.getBbHeight() * 3, 5, 50);
                    WorldHelper.spawnParticleBatch(attacker.level(), ParticleTypes.SOUL, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                                                   target.getBbWidth() / 2, target.getBbHeight() / 2, target.getBbWidth() / 2, particleCount, 0.05);
                }
                // Play soul stealing sound
                if (attacker.level().isClientSide)
                {   WorldHelper.playEntitySound(ModSounds.SOUL_LAMP_ON.value(), attacker, attacker.getSoundSource(), 1f, (float) Math.random() / 5f + 1.3f);
                }
            }
        }
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos blockPos, Player player)
    {   return !player.isCreative();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag advanced)
    {
        if (advanced.isAdvanced())
        {   tooltip.add(Component.literal("Fuel: " + (int) getFuel(stack) + " / " + 64));
        }
        super.appendHoverText(stack, context, tooltip, advanced);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack thisStack, ItemStack fuelStack, Slot slot, ClickAction action, Player player, SlotAccess slotAccess)
    {
        FuelData fuel = ConfigHelper.findFirstFuelMatching(ConfigSettings.SOULSPRING_LAMP_FUEL, fuelStack).orElse(null);
        if (fuel != null && fuel.test(fuelStack) && getFuel(thisStack) < 64)
        {
            double currentFuel = getFuel(thisStack);
            if (action == ClickAction.PRIMARY)
            {
                addFuel(thisStack, fuelStack);
                fuelStack.shrink((int) ((64 - currentFuel) / getFuelForStack(fuelStack)));
            }
            else if (action == ClickAction.SECONDARY)
            {
                ItemStack singleFuelItem = fuelStack.copy();
                singleFuelItem.setCount(1);
                addFuel(thisStack, singleFuelItem);
                fuelStack.shrink(1);
            }

            if (player instanceof ServerPlayer serverPlayer)
            {   ModAdvancementTriggers.SOUL_LAMP_FUELED.value().trigger(serverPlayer, fuelStack, thisStack);
            }

            return true;
        }
        return false;
    }
}
