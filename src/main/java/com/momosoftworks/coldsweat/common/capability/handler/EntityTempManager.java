package com.momosoftworks.coldsweat.common.capability.handler;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.event.vanilla.LivingEntityLoadAdditionalEvent;
import com.momosoftworks.coldsweat.api.event.common.temperautre.TempModifierEvent;
import com.momosoftworks.coldsweat.api.event.core.init.GatherDefaultTempModifiersEvent;
import com.momosoftworks.coldsweat.api.registry.TempModifierRegistry;
import com.momosoftworks.coldsweat.api.temperature.modifier.*;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.Placement;
import com.momosoftworks.coldsweat.api.util.Placement.Mode;
import com.momosoftworks.coldsweat.api.util.Placement.Order;
import com.momosoftworks.coldsweat.common.capability.ModCapabilities;
import com.momosoftworks.coldsweat.common.capability.SidedCapabilityCache;
import com.momosoftworks.coldsweat.common.capability.temperature.EntityTempCap;
import com.momosoftworks.coldsweat.common.capability.temperature.ITemperatureCap;
import com.momosoftworks.coldsweat.common.capability.temperature.PlayerTempCap;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.event.TaskScheduler;
import com.momosoftworks.coldsweat.data.codec.configuration.FoodData;
import com.momosoftworks.coldsweat.data.codec.configuration.InsulatorData;
import com.momosoftworks.coldsweat.data.codec.configuration.ItemCarryTempData;
import com.momosoftworks.coldsweat.data.codec.configuration.MountData;
import com.momosoftworks.coldsweat.util.ClientOnlyHelper;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.entity.DummyPlayer;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.FastMap;
import com.momosoftworks.coldsweat.util.registries.ModAttributes;
import com.momosoftworks.coldsweat.util.registries.ModBlocks;
import com.momosoftworks.coldsweat.util.registries.ModEffects;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

@Mod.EventBusSubscriber
public class EntityTempManager
{
    public static final Temperature.Trait[] VALID_TEMPERATURE_TRAITS = Arrays.stream(Temperature.Trait.values()).filter(Temperature.Trait::isForTemperature).toArray(Temperature.Trait[]::new);
    public static final Temperature.Trait[] VALID_MODIFIER_TRAITS = Arrays.stream(Temperature.Trait.values()).filter(Temperature.Trait::isForModifiers).toArray(Temperature.Trait[]::new);
    public static final Temperature.Trait[] VALID_ATTRIBUTE_TRAITS = Arrays.stream(Temperature.Trait.values()).filter(Temperature.Trait::isForAttributes).toArray(Temperature.Trait[]::new);

    public static final Set<EntityType<? extends LivingEntity>> TEMPERATURE_ENABLED_ENTITIES = new HashSet<>(List.of(EntityType.PLAYER));

    public static SidedCapabilityCache<ITemperatureCap, Entity> CAP_CACHE = new SidedCapabilityCache<>(ModCapabilities.ENTITY_TEMPERATURE);

    public static LazyOptional<ITemperatureCap> getTemperatureCap(Entity entity)
    {   return CAP_CACHE.get(entity);
    }

    /**
     * Attach temperature capability to entities
     */
    @SubscribeEvent
    public static void attachCapabilityToEntityHandler(AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof LivingEntity entity && TEMPERATURE_ENABLED_ENTITIES.contains(entity.getType()))
        {
            // Make a new capability instance to attach to the entity
            ITemperatureCap tempCap = entity instanceof Player ? new PlayerTempCap() : new EntityTempCap();
            // Optional that holds the capability instance
            LazyOptional<ITemperatureCap> capOptional = LazyOptional.of(() -> tempCap);

            // Capability provider
            ICapabilityProvider provider = new ICapabilitySerializable<CompoundTag>()
            {
                @Nonnull
                @Override
                public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction direction)
                {
                    // If the requested cap is the temperature cap, return the temperature cap
                    if (cap == ModCapabilities.ENTITY_TEMPERATURE)
                    {   return capOptional.cast();
                    }
                    return LazyOptional.empty();
                }

                @Override
                public CompoundTag serializeNBT()
                {   return tempCap.serializeNBT();
                }

                @Override
                public void deserializeNBT(CompoundTag nbt)
                {   tempCap.deserializeNBT(nbt);
                }
            };

            // Attach the capability to the entity
            event.addCapability(new ResourceLocation(ColdSweat.MOD_ID, "temperature"), provider);
        }
    }

    /**
     * Add modifiers to the player and valid entities when they join the world
     */
    @SubscribeEvent
    public static void initModifiersOnEntity(EntityJoinLevelEvent event)
    {
        if (event.getEntity() instanceof LivingEntity living && !living.level.isClientSide()
        && isTemperatureEnabled(living.getType()))
        {
            getTemperatureCap(living).ifPresent(cap ->
            {
                // Add default modifiers every time the entity joins the world
                for (Temperature.Trait trait : VALID_MODIFIER_TRAITS)
                {
                    GatherDefaultTempModifiersEvent gatherEvent = new GatherDefaultTempModifiersEvent(living, trait);
                    MinecraftForge.EVENT_BUS.post(gatherEvent);

                    cap.getModifiers(trait).clear();
                    cap.getModifiers(trait).addAll(gatherEvent.getModifiers());
                }
            });
        }
    }

    @SubscribeEvent
    public static void fixOldAttributeData(LivingEntityLoadAdditionalEvent event)
    {
        if (isTemperatureEnabled(event.getEntity().getType())
        && event.getNBT().getList("Attributes", 10).stream().anyMatch(attribute -> ((CompoundTag) attribute).getString("Name").equals("cold_sweat:world_temperature_offset")))
        {
            TaskScheduler.scheduleServer(() ->
            {
                for (Temperature.Trait attributeType : VALID_ATTRIBUTE_TRAITS)
                {
                    CSMath.doIfNotNull(getAttribute(attributeType, event.getEntity()),
                    attribute ->
                    {
                        attribute.removeModifiers();
                        attribute.setBaseValue(attribute.getAttribute().getDefaultValue());
                    });
                }
            }, 1);
        }
    }

    /**
     * Tick TempModifiers and update temperature for living entities
     */
    @SubscribeEvent
    public static void tickTemperature(LivingEvent.LivingTickEvent event)
    {
        LivingEntity entity = event.getEntity();
        if (!TEMPERATURE_ENABLED_ENTITIES.contains(entity.getType())) return;

        getTemperatureCap(entity).ifPresent(cap ->
        {
            if (!entity.level.isClientSide)
            {   // Tick modifiers serverside
                cap.tick(entity);
            }
            else
            {   // Tick modifiers clientside
                cap.tickDummy(entity);
            }

            // Remove expired modifiers
            AtomicBoolean sync = new AtomicBoolean(false);
            for (Temperature.Trait trait : VALID_MODIFIER_TRAITS)
            {
                cap.getModifiers(trait).removeIf(modifier ->
                {
                    int expireTime = modifier.getExpireTime();
                    if (modifier.isDirty())
                    {   sync.set(true);
                        modifier.markClean();
                    }
                    return (modifier.setTicksExisted(modifier.getTicksExisted() + 1) > expireTime && expireTime != -1);
                });
            }
            if (sync.get())
            {   Temperature.updateModifiers(entity, cap);
            }
        });
    }

    @SubscribeEvent
    public static void tickInventoryTempItems(LivingEvent.LivingTickEvent event)
    {
        LivingEntity entity = event.getEntity();
        if (entity.tickCount % 10 != 0 || !isTemperatureEnabled(event.getEntity().getType())) return;

        Map<Temperature.Trait, Double> effectsPerTrait = Arrays.stream(VALID_MODIFIER_TRAITS).collect(
                () -> new EnumMap<>(Temperature.Trait.class),
                (map, type) -> map.put(type, 0.0),
                EnumMap::putAll);
        Map<ItemCarryTempData, Double> effectsPerCarriedTemp = new FastMap<>();

        // Get temperature of equipped items
        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty())
            {
                Item item = stack.getItem();
                ConfigSettings.CARRIED_ITEM_TEMPERATURES.get().get(item).forEach(
                carried ->
                {   checkAndAddCarriedTemp(entity, stack, null, slot, carried, effectsPerCarriedTemp);
                });
            }
        }

        // Get temperature of main inventory items
        if (entity instanceof Player player)
        {
            for (Slot slot : player.inventoryMenu.slots)
            {
                ItemStack stack = slot.getItem();
                if (!stack.isEmpty())
                {
                    Item item = stack.getItem();
                    ConfigSettings.CARRIED_ITEM_TEMPERATURES.get().get(item).forEach(
                    carried ->
                    {   checkAndAddCarriedTemp(entity, stack, slot.index, null, carried, effectsPerCarriedTemp);
                    });
                }
            }
        }

        for (Map.Entry<ItemCarryTempData, Double> entry : effectsPerCarriedTemp.entrySet())
        {
            Temperature.Trait trait = entry.getKey().trait();
            double temp = entry.getValue();

            effectsPerTrait.put(trait, effectsPerTrait.get(trait) + temp);
        }

        effectsPerTrait.forEach((trait, temp) ->
        {
            Optional<InventoryItemsTempModifier> modifier = Temperature.getModifier(entity, trait, InventoryItemsTempModifier.class);
            if (modifier.isEmpty())
            {   Temperature.addModifier(entity, new InventoryItemsTempModifier(temp), trait, Placement.Duplicates.BY_CLASS);
            }
            else
            {   modifier.get().getNBT().putDouble("Effect", temp);
            }
        });
    }

    private static void checkAndAddCarriedTemp(LivingEntity entity, ItemStack stack, Integer slot, EquipmentSlot equipmentSlot,
                                               ItemCarryTempData carried, Map<ItemCarryTempData, Double> effectsPerCarriedTemp)
    {
        if (carried.test(entity, stack, slot, equipmentSlot))
        {
            double temp = carried.temperature() * stack.getCount();
            double currentEffect = effectsPerCarriedTemp.getOrDefault(carried, 0.0);
            double newEffect = Math.min(carried.maxEffect(), Math.abs(currentEffect + temp)) * CSMath.sign(currentEffect + temp);

            effectsPerCarriedTemp.put(carried, newEffect);
        }
    }

    @SubscribeEvent
    public static void clearClientCapCache(TickEvent.ClientTickEvent event)
    {
        if (event.side == LogicalSide.CLIENT && event.phase == TickEvent.Phase.END
        && ClientOnlyHelper.getClientLevel() != null
        && ClientOnlyHelper.getClientLevel().getGameTime() % 5 == 0)
        {   CAP_CACHE.clearClient();
        }
    }

    /**
     * Transfer the player's capability when traveling from the End
     */
    @SubscribeEvent
    public static void carryOverPersistentAttributes(PlayerEvent.Clone event)
    {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();

        if (!newPlayer.level.isClientSide)
        {
            // Get the old player's capability
            oldPlayer.reviveCaps();
            getTemperatureCap(oldPlayer).map(ITemperatureCap::getPersistentAttributes).orElse(new HashSet<>())
            .forEach(attr ->
            {   newPlayer.getAttribute(attr).setBaseValue(oldPlayer.getAttribute(attr).getBaseValue());
            });
            oldPlayer.invalidateCaps();
        }
    }

    /**
     * Reset the player's temperature upon respawning
     */
    @SubscribeEvent
    public static void handlePlayerReset(PlayerEvent.Clone event)
    {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();

        CAP_CACHE.ifLazyPresent(oldPlayer, LazyOptional::invalidate);

        getTemperatureCap(newPlayer).ifPresent(cap ->
        {
            if (!event.isWasDeath())
            {
                oldPlayer.reviveCaps();
                getTemperatureCap(oldPlayer).ifPresent(cap::copy);
                oldPlayer.invalidateCaps();
            }
            if (!newPlayer.level.isClientSide)
            {   Temperature.updateTemperature(newPlayer, cap, true);
            }
        });
    }

    /**
     * Add default modifiers to players and temperature-enabled entities
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void defineDefaultModifiers(GatherDefaultTempModifiersEvent event)
    {
        // Default TempModifiers for players
        if (event.getEntity() instanceof Player)
        {
            if (event.getTrait() == Temperature.Trait.WORLD)
            {
                event.addModifier(new BiomeTempModifier(25).tickRate(10), Placement.Duplicates.BY_CLASS, Placement.BEFORE_FIRST);
                event.addModifier(new UndergroundTempModifier().tickRate(10), Placement.Duplicates.BY_CLASS, Placement.of(Mode.AFTER, Order.FIRST, mod -> mod instanceof BiomeTempModifier));
                event.addModifier(new BlockTempModifier().tickRate(4), Placement.Duplicates.BY_CLASS, Placement.AFTER_LAST);
                event.addModifier(new EntitiesTempModifier().tickRate(10), Placement.Duplicates.BY_CLASS, Placement.AFTER_LAST);

                // Serene Seasons compat
                event.addModifierById(new ResourceLocation("sereneseasons:season"),
                                      mod -> mod.tickRate(60),
                                      Placement.Duplicates.BY_CLASS,
                                      Placement.of(Mode.BEFORE, Order.FIRST, mod2 -> mod2 instanceof UndergroundTempModifier));
                // Weather2 Compat
                event.addModifierById(new ResourceLocation("weather2:storm"),
                                      mod -> mod.tickRate(60),
                                      Placement.Duplicates.BY_CLASS,
                                      Placement.of(Mode.BEFORE, Order.FIRST, mod2 -> mod2 instanceof UndergroundTempModifier));
                // Valkyrien Skies Compat
                event.addModifierById(new ResourceLocation("valkyrienskies:ship_blocks"),
                                      mod -> mod.tickRate(10),
                                      Placement.Duplicates.BY_CLASS,
                                      Placement.of(Mode.AFTER, Order.FIRST, mod2 -> mod2 instanceof BlockTempModifier));
            }
            if (event.getTrait().isForModifiers())
            {   event.addModifier(new InventoryItemsTempModifier(), Placement.Duplicates.BY_CLASS, Placement.AFTER_LAST);
            }
        }
        // Default TempModifiers for other temperature-enabled entities
        else if (event.getTrait() == Temperature.Trait.WORLD && TEMPERATURE_ENABLED_ENTITIES.contains(event.getEntity().getType()))
        {   // Basic modifiers
            event.addModifier(new BiomeTempModifier(16).tickRate(40), Placement.Duplicates.BY_CLASS, Placement.BEFORE_FIRST);
            event.addModifier(new UndergroundTempModifier(16).tickRate(40), Placement.Duplicates.BY_CLASS, Placement.of(Mode.AFTER, Order.FIRST, mod -> mod instanceof BiomeTempModifier));
            event.addModifier(new BlockTempModifier(4).tickRate(20), Placement.Duplicates.BY_CLASS, Placement.AFTER_LAST);
            event.addModifier(new EntitiesTempModifier().tickRate(10), Placement.Duplicates.BY_CLASS, Placement.AFTER_LAST);

            // Serene Seasons compat
            if (CompatManager.isSereneSeasonsLoaded())
            {   TempModifierRegistry.getValue(new ResourceLocation("sereneseasons:season")).ifPresent(mod -> event.addModifier(mod.tickRate(60), Placement.Duplicates.BY_CLASS, Placement.of(Mode.BEFORE, Order.FIRST,
                                                                                                                                          mod2 -> mod2 instanceof UndergroundTempModifier)));
            }
            // Weather2 Compat
            if (CompatManager.isWeather2Loaded())
            {   TempModifierRegistry.getValue(new ResourceLocation("weather2:storm")).ifPresent(mod -> event.addModifier(mod.tickRate(60), Placement.Duplicates.BY_CLASS, Placement.of(Mode.BEFORE, Order.FIRST,
                                                                                                                                    mod2 -> mod2 instanceof UndergroundTempModifier)));
            }
        }
    }

    /**
     * Used to grant the player the sewing table recipe when they get an insulation item
     */
    @SubscribeEvent
    public static void addSewingIngredientListener(EntityJoinLevelEvent event)
    {
        // Add listener for granting the sewing table recipe when the player gets an insulation item
        if (event.getEntity() instanceof Player player)
        {
            player.containerMenu.addSlotListener(new ContainerListener()
            {
                public void slotChanged(AbstractContainerMenu menu, int slotIndex, ItemStack stack)
                {
                    Slot slot = menu.getSlot(slotIndex);
                    if (!(slot instanceof ResultSlot))
                    {
                        if (slot.container == player.getInventory()
                        && (ConfigSettings.INSULATION_ITEMS.get().containsKey(stack.getItem())))
                        {
                            player.awardRecipesByKey(new ResourceLocation[]{new ResourceLocation(ColdSweat.MOD_ID, "sewing_table")});
                        }
                    }
                }
                public void dataChanged(AbstractContainerMenu menu, int slot, int value) {}
            });
        }
    }

    @SubscribeEvent
    public static void cancelImmuneModifiers(TempModifierEvent.Calculate.Pre event)
    {
        TempModifier modifier = event.getModifier();
        LivingEntity entity = event.getEntity();

        ResourceLocation modifierKey = TempModifierRegistry.getKey(modifier);

        if (ConfigSettings.DISABLED_MODIFIERS.get().contains(modifierKey))
        {
            if (modifier instanceof BiomeTempModifier)
            {   event.setFunction(temp -> temp + ((Temperature.get(entity, Temperature.Trait.FREEZING_POINT) + Temperature.get(entity, Temperature.Trait.BURNING_POINT)) / 2));
            }
            else
            {   event.setFunction(temp -> temp);
            }
            event.setCanceled(true);
        }
    }

    /**
     * Check the player's immunity level to temperature modifiers when they tick
     */
    @SubscribeEvent
    public static void checkModifierImmunity(TempModifierEvent.Calculate.Post event)
    {
        if (event.getEntity() instanceof DummyPlayer) return;
        if (!event.getTrait().isForAttributes()) return;

        TempModifier modifier = event.getModifier();
        ResourceLocation modifierKey = TempModifierRegistry.getKey(modifier);
        LivingEntity entity = event.getEntity();

        double lastInput = modifier instanceof BiomeTempModifier
                           ? (Temperature.get(entity, Temperature.Trait.FREEZING_POINT) + Temperature.get(entity, Temperature.Trait.BURNING_POINT)) / 2
                           : modifier.getLastInput();

        // Calculate modifier immunity from equipped insulators
        for (Map.Entry<ItemStack, InsulatorData> entry : getInsulatorsOnEntity(entity).entrySet())
        {
            InsulatorData insulator = entry.getValue();
            ItemStack stack = entry.getKey();

            Double immunity = insulator.immuneTempModifiers().get(modifierKey);
            if (immunity != null && insulator.test(entity, stack))
            {
                Function<Double, Double> func = event.getFunction();
                event.setFunction(temp -> CSMath.blend(func.apply(temp), lastInput, immunity, 0, 1));
            }
        }
    }

    /**
     * Handle modifiers for freezing, burning, and being wet
     */
    @SubscribeEvent
    public static void handleWaterAndFreezing(TickEvent.PlayerTickEvent event)
    {
        Player player = event.player;

        // Water / Rain
        if (!player.level.isClientSide && event.phase == TickEvent.Phase.START)
        {
            if (player.tickCount % 5 == 0)
            {
                if (!player.isSpectator() && (WorldHelper.isInWater(player) || player.tickCount % 40 == 0
                && WorldHelper.isRainingAt(player.level, player.blockPosition())))
                {   Temperature.addModifier(player, new WaterTempModifier(0.01f).tickRate(10), Temperature.Trait.WORLD, Placement.Duplicates.BY_CLASS);
                }

                if (player.isFreezing())
                {   Temperature.addOrReplaceModifier(player, new FreezingTempModifier(player.getTicksFrozen() / 13.5f).expires(5), Temperature.Trait.BASE, Placement.Duplicates.BY_CLASS);
                }

                if (player.isOnFire())
                {   Temperature.addOrReplaceModifier(player, new FireTempModifier().expires(5), Temperature.Trait.BASE, Placement.Duplicates.BY_CLASS);
                }
            }

            if (player.isFreezing() && player.getTicksFrozen() > 0)
            {
                AtomicReference<Double> insulation = new AtomicReference<>((double) 0);
                boolean hasIcePotion = player.hasEffect(ModEffects.ICE_RESISTANCE) && ConfigSettings.ICE_RESISTANCE_ENABLED.get();

                if (!hasIcePotion)
                {
                    Temperature.getModifier(player, Temperature.Trait.RATE, ArmorInsulationTempModifier.class).ifPresent(insulModifier ->
                    {   insulation.updateAndGet(v -> (v + insulModifier.getNBT().getDouble("Hot") + insulModifier.getNBT().getDouble("Cold")));
                    });
                }

                if (!(hasIcePotion || insulation.get() > 0) && (player.tickCount % Math.max(1, 37 - insulation.get())) == 0)
                {   player.setTicksFrozen(player.getTicksFrozen() - 1);
                }
            }
        }
    }

    @SubscribeEvent
    public static void updateAttributeModifiersOnSlotChange(LivingEquipmentChangeEvent event)
    {
        updateInsulationAttributeModifiers(event.getEntity());
    }

    public static void updateInsulationAttributeModifiers(LivingEntity entity)
    {
        Stream.of(ConfigSettings.INSULATION_ITEMS.get().values(),
                  ConfigSettings.INSULATING_ARMORS.get().values(),
                  ConfigSettings.INSULATING_CURIOS.get().values())
        .flatMap(Collection::stream)
        .forEach(insulator ->
        {
            for (Map.Entry<Attribute, AttributeModifier> entry : insulator.attributes().getMap().entries())
            {
                Attribute attribute = entry.getKey();
                AttributeModifier modifier = entry.getValue();
                AttributeInstance instance = entity.getAttribute(attribute);
                if (instance != null)
                {   instance.removeModifier(modifier);
                }
            }
        });

        for (Map.Entry<ItemStack, InsulatorData> insulationItem : getInsulatorsOnEntity(entity).entrySet())
        {
            InsulatorData insulator = insulationItem.getValue();
            ItemStack stack = insulationItem.getKey();
            if (insulator.test(entity, stack))
            {
                for (Map.Entry<Attribute, AttributeModifier> entry : insulator.attributes().getMap().entries())
                {
                    Attribute attribute = entry.getKey();
                    AttributeModifier modifier = entry.getValue();
                    AttributeInstance instance = entity.getAttribute(attribute);
                    if (instance != null)
                    {   instance.addTransientModifier(modifier);
                    }
                }
            }
        }
    }

    /**
     * Cancel freezing damage when the player has the Ice Resistance effect
     */
    @SubscribeEvent
    public static void cancelFreezingDamage(LivingAttackEvent event)
    {
        if (event.getSource() == DamageSource.FREEZE && event.getEntity().hasEffect(ModEffects.ICE_RESISTANCE) && ConfigSettings.ICE_RESISTANCE_ENABLED.get())
        {   event.setCanceled(true);
        }
    }

    /**
     * Handle HearthTempModifier when the player has the Insulation effect
     */
    @SubscribeEvent
    public static void onInsulationAdded(MobEffectEvent.Added event)
    {
        LivingEntity entity = event.getEntity();
        MobEffectInstance effect = event.getEffectInstance();

        if (!entity.level.isClientSide && isTemperatureEnabled(entity.getType())
        && (effect.getEffect() == ModEffects.CHILL || effect.getEffect() == ModEffects.WARMTH))
        {
            boolean isWarmth = effect.getEffect() == ModEffects.WARMTH;
            int warming = isWarmth ? effect.getAmplifier() + 1 : 0;
            int cooling = !isWarmth ? effect.getAmplifier() + 1 : 0;
            // Add TempModifier on potion effect added
            Optional<BlockInsulationTempModifier> oldMod = Temperature.getModifier(entity, Temperature.Trait.WORLD, BlockInsulationTempModifier.class);
            if (oldMod.isPresent())
            {
                CompoundTag nbt = oldMod.get().getNBT();
                nbt.putInt("Warming", Math.max(nbt.getInt("Warming"), warming));
                nbt.putInt("Cooling", Math.max(nbt.getInt("Cooling"), cooling));
                oldMod.get().setTicksExisted(0);
                oldMod.get().expires(effect.getDuration());
            }
            else
            {
                TempModifier newMod = new BlockInsulationTempModifier(cooling, warming).expires(effect.getDuration());
                Temperature.addOrReplaceModifier(entity, newMod, Temperature.Trait.WORLD, Placement.Duplicates.BY_CLASS);
            }
        }
    }

    @SubscribeEvent
    public static void onInsulationRemoved(MobEffectEvent.Remove event)
    {
        LivingEntity entity = event.getEntity();
        MobEffectInstance effect = event.getEffectInstance();

        if (effect != null && !entity.level.isClientSide && isTemperatureEnabled(entity.getType())
        && (effect.getEffect() == ModEffects.CHILL || effect.getEffect() == ModEffects.WARMTH))
        {
            Optional<BlockInsulationTempModifier> modifier = Temperature.getModifier(entity, Temperature.Trait.WORLD, BlockInsulationTempModifier.class);
            if (modifier.isPresent())
            {
                boolean isWarmth = effect.getEffect() == ModEffects.WARMTH;
                CompoundTag nbt = modifier.get().getNBT();

                if (isWarmth)
                {   nbt.putInt("Warming", 0);
                }
                else
                {   nbt.putInt("Cooling", 0);
                }
                if (isWarmth ? !entity.hasEffect(ModEffects.CHILL) : !entity.hasEffect(ModEffects.WARMTH))
                {   Temperature.removeModifiers(entity, Temperature.Trait.WORLD, mod -> mod instanceof BlockInsulationTempModifier);
                }
            }
        }
    }

    /**
     * Improve the player's temperature when they sleep
     */
    @SubscribeEvent
    public static void onSleep(SleepFinishedTimeEvent event)
    {
        if (!event.getLevel().isClientSide())
        {
            event.getLevel().players().forEach(player ->
            {
                if (player.isSleeping())
                {
                    // Divide the player's current temperature by 4
                    double temp = Temperature.get(player, Temperature.Trait.CORE);
                    Temperature.set(player, Temperature.Trait.CORE, temp / 4f);
                }
            });
        }
    }

    /**
     * Handle insulation on mounted entity
     */
    @SubscribeEvent
    public static void playerRiding(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START && !event.player.level.isClientSide() && event.player.tickCount % 5 == 0)
        {
            Player player = event.player;
            if (player.getVehicle() != null)
            {
                Entity mount = player.getVehicle();
                // If insulated minecart
                if (mount instanceof Minecart minecart && minecart.getDisplayBlockState().getBlock() == ModBlocks.MINECART_INSULATION)
                {   Temperature.addOrReplaceModifier(player, new MountTempModifier(1, 1).tickRate(5).expires(5), Temperature.Trait.RATE, Placement.Duplicates.BY_CLASS);
                }
                // If insulated entity (defined in config)
                else
                {
                    MountData entityInsul = ConfigSettings.INSULATED_MOUNTS.get().get(mount.getType())
                                                  .stream().filter(mnt -> mnt.test(mount)).findFirst().orElse(null);
                    if (entityInsul != null && entityInsul.test(mount))
                    {   Temperature.addOrReplaceModifier(player, new MountTempModifier(entityInsul.coldInsulation(), entityInsul.heatInsulation()).tickRate(5).expires(5), Temperature.Trait.RATE, Placement.Duplicates.BY_CLASS);
                    }
                }
            }
        }
    }

    /**
     * Handle TempModifiers for consumables
     */
    @SubscribeEvent
    public static void onEatFood(LivingEntityUseItemEvent.Finish event)
    {
        if (event.getEntity() instanceof Player player
        && (event.getItem().getUseAnimation() == UseAnim.DRINK || event.getItem().getUseAnimation() == UseAnim.EAT)
        && !event.getEntity().level.isClientSide)
        {
            // If food item defined in config
            for (FoodData foodData : ConfigSettings.FOOD_TEMPERATURES.get().get(event.getItem().getItem()))
            {
                if (foodData != null && foodData.test(event.getItem()))
                {
                    double effect = foodData.temperature();
                    if (foodData.duration() > 0)
                    {
                        // Special case for soul sprouts
                        FoodTempModifier foodModifier = event.getItem().getItem() == ModItems.SOUL_SPROUT
                                                        ? new SoulSproutTempModifier(effect)
                                                        : new FoodTempModifier(effect);
                        // Store the duration of the TempModifier
                        foodModifier.getNBT().putInt("duration", foodData.duration());
                        // Add the TempModifier
                        Temperature.addModifier(player, foodModifier.expires(foodData.duration()), Temperature.Trait.BASE, Placement.Duplicates.BY_CLASS);
                    }
                    else
                    {   Temperature.addModifier(player, new FoodTempModifier(effect).expires(0), Temperature.Trait.CORE, Placement.Duplicates.EXACT);
                    }
                }
            }
        }
    }

    public static Set<EntityType<? extends LivingEntity>> getEntitiesWithTemperature()
    {   return ImmutableSet.copyOf(TEMPERATURE_ENABLED_ENTITIES);
    }

    public static boolean isTemperatureEnabled(EntityType<?> type)
    {   return TEMPERATURE_ENABLED_ENTITIES.contains(type);
    }

    public static boolean immuneToTempEffects(LivingEntity entity)
    {   return entity.level.getDifficulty() == Difficulty.PEACEFUL && ConfigSettings.USE_PEACEFUL_MODE.get();
    }

    public static Map<ItemStack, InsulatorData> getInsulatorsOnEntity(LivingEntity entity)
    {
        Map<ItemStack, InsulatorData> insulators = new HashMap<>();
        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty())
            {
                ConfigSettings.INSULATING_ARMORS.get().get(stack.getItem()).forEach(insul -> insulators.put(stack, insul));
                ItemInsulationManager.getInsulationCap(stack).ifPresent(cap ->
                {
                    cap.getInsulation().stream().map(Pair::getFirst).forEach(item ->
                    {
                        ConfigSettings.INSULATION_ITEMS.get().get(item.getItem()).forEach(insul -> insulators.put(item, insul));
                    });
                });
            }
        }
        for (ItemStack curio : CompatManager.getCurios(entity))
        {   ConfigSettings.INSULATING_CURIOS.get().get(curio.getItem()).forEach(insul -> insulators.put(curio, insul));
        }
        return insulators;
    }

    /**
     * Sets the corresponding attribute value for the given {@link Temperature.Trait}.
     * @param trait the type or ability to get the attribute for
     */
    public static void setAttribute(Temperature.Trait trait, LivingEntity entity, double value)
    {
        switch (trait)
        {
            case WORLD -> CSMath.doIfNotNull(entity.getAttribute(ModAttributes.WORLD_TEMPERATURE), att -> att.setBaseValue(value));
            case BASE  -> CSMath.doIfNotNull(entity.getAttribute(ModAttributes.BASE_BODY_TEMPERATURE), att -> att.setBaseValue(value));
            case HEAT_RESISTANCE -> CSMath.doIfNotNull(entity.getAttribute(ModAttributes.HEAT_RESISTANCE), att -> att.setBaseValue(value));
            case COLD_RESISTANCE -> CSMath.doIfNotNull(entity.getAttribute(ModAttributes.COLD_RESISTANCE), att -> att.setBaseValue(value));
            case HEAT_DAMPENING  -> CSMath.doIfNotNull(entity.getAttribute(ModAttributes.HEAT_DAMPENING), att -> att.setBaseValue(value));
            case COLD_DAMPENING  -> CSMath.doIfNotNull(entity.getAttribute(ModAttributes.COLD_DAMPENING), att -> att.setBaseValue(value));
            case FREEZING_POINT -> CSMath.doIfNotNull(entity.getAttribute(ModAttributes.FREEZING_POINT), att -> att.setBaseValue(value));
            case BURNING_POINT  -> CSMath.doIfNotNull(entity.getAttribute(ModAttributes.BURNING_POINT), att -> att.setBaseValue(value));
        }
    }

    /**
     * Gets the corresponding attribute value for the given {@link Temperature.Trait}.
     * @param trait the type or ability to get the attribute for
     */
    @Nullable
    public static AttributeInstance getAttribute(Temperature.Trait trait, LivingEntity entity)
    {
        return switch (trait)
        {
            case WORLD -> entity.getAttribute(ModAttributes.WORLD_TEMPERATURE);
            case BASE  -> entity.getAttribute(ModAttributes.BASE_BODY_TEMPERATURE);
            case FREEZING_POINT  -> entity.getAttribute(ModAttributes.FREEZING_POINT);
            case BURNING_POINT   -> entity.getAttribute(ModAttributes.BURNING_POINT);
            case HEAT_RESISTANCE -> entity.getAttribute(ModAttributes.HEAT_RESISTANCE);
            case COLD_RESISTANCE -> entity.getAttribute(ModAttributes.COLD_RESISTANCE);
            case HEAT_DAMPENING  -> entity.getAttribute(ModAttributes.HEAT_DAMPENING);
            case COLD_DAMPENING  -> entity.getAttribute(ModAttributes.COLD_DAMPENING);

            default -> throw ColdSweat.LOGGER.throwing(new IllegalArgumentException("\"" + trait + "\" is not a valid trait!"));
        };
    }

    public static Collection<AttributeModifier> getAllAttributeModifiers(LivingEntity entity, AttributeInstance attribute, @Nullable AttributeModifier.Operation operation)
    {
        Collection<AttributeModifier> modifiers = new ArrayList<>(operation == null
                                                                  ? attribute.getModifiers()
                                                                  : attribute.getModifiers(operation));
        modifiers.addAll(getAllEquipmentAttributeModifiers(entity, attribute, operation));

        return modifiers;
    }

    public static Collection<AttributeModifier> getAllEquipmentAttributeModifiers(LivingEntity entity, AttributeInstance attribute, @Nullable AttributeModifier.Operation operation)
    {
        Collection<AttributeModifier> modifiers = new ArrayList<>();

        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty())
            {   modifiers.addAll(ItemInsulationManager.getAttributeModifiersForSlot(stack, attribute.getAttribute(), slot, operation, entity));
            }
        }
        return modifiers;
    }

    public static AttributeModifier makeAttributeModifier(Temperature.Trait trait, double value, AttributeModifier.Operation operation)
    {
        return switch (trait)
        {
            case WORLD -> new AttributeModifier("World Temperature Modifier", value, operation);
            case BASE  -> new AttributeModifier("Base Body Temperature Modifier", value, operation);

            case FREEZING_POINT -> new AttributeModifier("Freezing Point Modifier", value, operation);
            case BURNING_POINT  -> new AttributeModifier("Burning Point Modifier", value, operation);
            case HEAT_RESISTANCE -> new AttributeModifier("Heat Resistance Modifier", value, operation);
            case COLD_RESISTANCE -> new AttributeModifier("Cold Resistance Modifier", value, operation);
            case HEAT_DAMPENING  -> new AttributeModifier("Heat Dampening Modifier", value, operation);
            case COLD_DAMPENING  -> new AttributeModifier("Cold Dampening Modifier", value, operation);
            default -> throw ColdSweat.LOGGER.throwing(new IllegalArgumentException("\"" + trait + "\" is not a valid trait!"));
        };
    }

    public static boolean isTemperatureAttribute(Attribute attribute)
    {
        return CSMath.containsAny(ForgeRegistries.ATTRIBUTES.getKey(attribute).toString(),
                                  Arrays.stream(EntityTempManager.VALID_ATTRIBUTE_TRAITS)
                                        .map(Temperature.Trait::getSerializedName).toArray(String[]::new));
    }

    public static List<TempModifier> getAllModifiers(LivingEntity entity)
    {
        List<TempModifier> allModifiers = new ArrayList<>();
        getTemperatureCap(entity).ifPresent(cap ->
        {
            for (Temperature.Trait trait : VALID_MODIFIER_TRAITS)
            {   allModifiers.addAll(cap.getModifiers(trait));
            }
        });
        return allModifiers;
    }
}