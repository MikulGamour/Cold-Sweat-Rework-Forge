package com.momosoftworks.coldsweat.common.item;

import com.momosoftworks.coldsweat.api.event.util.ClickAction;
import com.momosoftworks.coldsweat.api.event.common.ItemSwappedInInventoryEvent;
import com.momosoftworks.coldsweat.api.temperature.modifier.SoulLampTempModifier;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Placement;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.advancement.trigger.ModAdvancementTriggers;
import com.momosoftworks.coldsweat.core.itemgroup.ColdSweatGroup;
import com.momosoftworks.coldsweat.core.network.message.ParticleBatchMessage;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import com.momosoftworks.coldsweat.data.codec.configuration.FuelData;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModSounds;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.block.BlockState;
import net.minecraft.client.settings.ParticleStatus;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.CreatureAttribute;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber
public class SoulspringLampItem extends Item
{
    public SoulspringLampItem()
    {   super(new Properties().tab(ColdSweatGroup.COLD_SWEAT).stacksTo(1).fireResistant().rarity(Rarity.UNCOMMON));
    }

    @Override
    public void inventoryTick(ItemStack stack, World level, Entity entity, int itemSlot, boolean isSelected)
    {
        if (entity instanceof LivingEntity && entity.tickCount % 5 == 0)
        {
            LivingEntity living = (LivingEntity) entity;
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
                    if (!(living instanceof PlayerEntity && ((PlayerEntity) living).isCreative() || living.isSpectator()))
                    {   addFuel(stack, -0.005 * CSMath.clamp(temp - max, 1, 3));
                    }

                    // Affect nearby players
                    double radius = 5d;
                    AxisAlignedBB bb = new AxisAlignedBB(living.getX() - radius, living.getY() + (living.getBbHeight() / 2) - radius, living.getZ() - radius,
                                                         living.getX() + radius, living.getY() + (living.getBbHeight() / 2) + radius, living.getZ() + radius);

                    if (Math.random() < 0.6)
                    {
                        AxisAlignedBB bb2 = bb.inflate(-3);
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
                if (!level.isClientSide)
                {
                    CompoundNBT itemTag = stack.getOrCreateTag();
                    // If the conditions are not met, turn off the lamp
                    if (itemTag.getInt("stateChangeTimer") <= 0
                            && itemTag.getBoolean("Lit") != shouldBeOn)
                    {
                        itemTag.putInt("stateChangeTimer", 2);
                        itemTag.putBoolean("Lit", shouldBeOn);

                        if (getFuel(stack) < 0.5)
                            setFuel(stack, 0);

                        WorldHelper.playEntitySound(shouldBeOn ? ModSounds.NETHER_LAMP_ON : ModSounds.NETHER_LAMP_OFF, living, living.getSoundSource(), 1.5f, (float) Math.random() / 5f + 0.9f);
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
    {   stack.getOrCreateTag().putDouble("Fuel", fuel);
    }

    private static void addFuel(ItemStack stack, double amount)
    {   setFuel(stack, Math.min(64, getFuel(stack) + amount));
    }

    private static void addFuel(ItemStack stack, ItemStack fuelStack)
    {   addFuel(stack, getFuelForStack(fuelStack) * fuelStack.getCount());
    }

    private static double getFuel(ItemStack stack)
    {   return stack.getOrCreateTag().getDouble("Fuel");
    }

    public static double getFuelForStack(ItemStack item)
    {
        return ConfigHelper.findFirstFuelMatching(ConfigSettings.SOULSPRING_LAMP_FUEL, item)
                .map(FuelData::fuel).orElse(0d).intValue();
    }

    // Restore fuel if player hits an enemy
    @SubscribeEvent
    public static void onEntityHit(LivingAttackEvent event)
    {
        if (event.getSource().getEntity() instanceof PlayerEntity && !(event.getEntityLiving() instanceof PlayerEntity))
        {
            PlayerEntity attacker = (PlayerEntity) event.getSource().getEntity();
            ItemStack stack = attacker.getMainHandItem();
            if (!(stack.getItem() instanceof SoulspringLampItem)) return;

            LivingEntity target = event.getEntityLiving();

            // If fuel < 64 and target NOT player
            if (getFuel(stack) < 64
            && target.getMobType() != CreatureAttribute.UNDEAD
            && !target.getPersistentData().getBoolean("SoulSucked"))
            {
                target.getPersistentData().putBoolean("SoulSucked", true);

                // Add fuel
                addFuel(stack, (int) Math.min(8, target.getMaxHealth() / 2));
                float extraDamage = Math.max(0, 8 - event.getAmount());
                if (extraDamage > 0)
                    target.hurt(new EntityDamageSource(DamageSource.MAGIC.msgId, attacker), extraDamage);

                // Spawn particles
                if (!target.level.isClientSide)
                {
                    int particleCount = (int) CSMath.clamp(target.getBbWidth() * target.getBbWidth() * target.getBbHeight() * 3, 5, 50);
                    WorldHelper.spawnParticleBatch(attacker.level, ParticleTypes.SOUL, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                                                   target.getBbWidth() / 2, target.getBbHeight() / 2, target.getBbWidth() / 2, particleCount, 0.05);
                }
                // Play soul stealing sound
                if (attacker.level.isClientSide)
                    WorldHelper.playEntitySound(ModSounds.NETHER_LAMP_ON, attacker, attacker.getSoundSource(), 1f, (float) Math.random() / 5f + 1.3f);
            }
        }
    }

    @Override
    public boolean canAttackBlock(BlockState state, World level, BlockPos blockPos, PlayerEntity player)
    {   return !player.isCreative();
    }

    @Override
    public void fillItemCategory(ItemGroup tab, NonNullList<ItemStack> itemList)
    {
        if (this.allowdedIn(tab))
        {
            ItemStack stack = new ItemStack(this);
            stack.getOrCreateTag().putBoolean("Lit", true);
            stack.getOrCreateTag().putDouble("Fuel", 64);
            itemList.add(stack);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, World level, List<ITextComponent> tooltip, ITooltipFlag advanced)
    {
        if (advanced.isAdvanced())
        {   tooltip.add(new StringTextComponent("Fuel: " + (int) stack.getOrCreateTag().getDouble("Fuel") + " / " + 64));
        }
        super.appendHoverText(stack, level, tooltip, advanced);
    }

    @SubscribeEvent
    public static void onItemClickedInGUI(ItemSwappedInInventoryEvent event)
    {
        ItemStack thisStack = event.getSlotItem();
        if (thisStack.getItem() != ModItems.SOULSPRING_LAMP) return;

        ItemStack fuelStack = event.getHeldItem();
        PlayerEntity player = event.getPlayer();
        ClickAction action = event.getClickAction();
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

            if (player instanceof ServerPlayerEntity)
            {   ModAdvancementTriggers.SOUL_LAMP_FUELLED.trigger(((ServerPlayerEntity) player), fuelStack, thisStack);
            }
            event.setCanceled(true);
        }
    }
}
