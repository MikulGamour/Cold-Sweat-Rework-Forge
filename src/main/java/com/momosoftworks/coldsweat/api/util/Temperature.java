package com.momosoftworks.coldsweat.api.util;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.momosoftworks.coldsweat.api.event.common.temperautre.TempModifierEvent;
import com.momosoftworks.coldsweat.api.event.common.temperautre.TemperatureChangedEvent;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.common.capability.temperature.ITemperatureCap;
import com.momosoftworks.coldsweat.common.capability.temperature.PlayerTempCap;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.core.network.message.SyncTempModifiersMessage;
import com.momosoftworks.coldsweat.core.network.message.SyncTemperatureMessage;
import com.momosoftworks.coldsweat.util.entity.DummyPlayer;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.InterruptableStreamer;
import com.momosoftworks.coldsweat.util.serialization.StringRepresentable;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * General helper class for temperature-related actions. (Previously TempHelper)<br>
 * Allows for manipulating player temperature and TempModifiers.
 */
public class Temperature
{
    private Temperature() {}

    /**
     * Converts a double temperature to a different unit. If {@code from} and {@code to} are the same, returns {@code value}.<br>
     * @param value The temperature to convert.
     * @param from The unit to convert from.
     * @param to The unit to convert to.
     * @param absolute Used when dealing with absolute temperature.
     * @return The converted temperature.
     */
    public static double convert(double value, Units from, Units to, boolean absolute)
    {
        switch (from)
        {
            case C : switch (to)
            {
                case C  : return value;
                case F  : return value * 1.8 + (absolute ? 32d : 0d);
                case MC : return value / 25d;
            }
            case F : switch (to)
            {
                case C  : return (value - (absolute ? 32d : 0d)) / 1.8;
                case F  : return value;
                case MC : return (value - (absolute ? 32d : 0d)) / 45d;
            }
            case MC : switch (to)
            {
                case C  : return value * 25d;
                case F  : return value * 45d + (absolute ? 32d : 0d);
                case MC : return value;
            }
        }
        return value;
    }

    public static double convertIfNeeded(double value, Trait trait, Units units)
    {
        if (trait.isForWorld())
        {   return convert(value, Units.MC, units, true);
        }
        return value;
    }

    /**
     * Returns the player's temperature of the specified type.
     */
    public static double get(LivingEntity entity, Trait trait)
    {   return EntityTempManager.getTemperatureCap(entity).map(cap -> cap.getTrait(trait)).orElse(0.0);
    }

    public static void set(LivingEntity entity, Trait trait, double value)
    {
        if (MinecraftForge.EVENT_BUS.post(new TemperatureChangedEvent(entity, trait, get(entity, trait), value)))
        {   return;
        }
        EntityTempManager.getTemperatureCap(entity).ifPresent(cap -> cap.setTrait(trait, value));
        updateTemperature(entity);
    }

    public static void add(LivingEntity entity, Trait trait, double value)
    {
        double oldTemp = get(entity, trait);
        if (MinecraftForge.EVENT_BUS.post(new TemperatureChangedEvent(entity, trait, oldTemp, oldTemp + value)))
        {   return;
        }
        EntityTempManager.getTemperatureCap(entity).ifPresent(cap -> cap.setTrait(trait, cap.getTrait(trait) + value));
        updateTemperature(entity);
    }

    /**
     * @return  a double representing what the Temperature would be after a TempModifier is applied.
     * @param entity the entity this modifier should use
     * @param modifiers the modifier(s) being applied to the {@code Temperature}
     */
    public static double apply(double currentTemp, LivingEntity entity, Trait trait, TempModifier... modifiers)
    {
        double temp2 = currentTemp;
        for (TempModifier modifier : modifiers)
        {
            if (modifier == null) continue;

            double newTemp = entity.tickCount % modifier.getTickRate() == 0 || modifier.getTicksExisted() == 0 || entity.tickCount <= 1
                    ? modifier.update(temp2, entity, trait)
                    : modifier.apply(temp2);
            if (!Double.isNaN(newTemp))
            {   temp2 = newTemp;
            }
        }
        return temp2;
    }

    /**
     * @return a double representing what the temperature would be after a collection of TempModifier(s) are applied.
     * @param entity the entity this list of modifiers should use
     * @param modifiers the list of modifiers being applied to the player's temperature
     */
    public static double apply(double temp, LivingEntity entity, Trait trait, Collection<TempModifier> modifiers)
    {   return apply(temp, entity, trait, modifiers.toArray(new TempModifier[0]));
    }

    public static double getTemperatureAt(BlockPos pos, World level)
    {
        DummyPlayer dummy = WorldHelper.getDummyPlayer(level);
        // Move the dummy to the position being tested
        Vector3d centerPos = CSMath.getCenterPos(pos);
        dummy.setPos(centerPos.x, centerPos.y, centerPos.z);
        return apply(0, dummy, Trait.WORLD, getModifiers(dummy, Trait.WORLD));
    }

    /**
     * @param modClass The class of the TempModifier to check for
     * @param trait The type of TempModifier to check for
     * @return true if the player has a TempModifier that extends the given class
     */
    public static boolean hasModifier(LivingEntity entity, Trait trait, Class<? extends TempModifier> modClass)
    {   return EntityTempManager.getTemperatureCap(entity).map(cap -> cap.hasModifier(trait, modClass)).orElse(false);
    }

    /**
     * @return The first modifier of the given class that is applied to the player.
     */
    public static <T extends TempModifier> Optional<T> getModifier(LivingEntity entity, Trait trait, Class<T> modClass)
    {   return getModifier(EntityTempManager.getTemperatureCap(entity).orElse(new PlayerTempCap()), trait, modClass);
    }

    public static <T extends TempModifier> Optional<T> getModifier(ITemperatureCap cap, Trait trait, Class<T> modClass)
    {   return (Optional<T>) cap.getModifiers(trait).stream().filter(modClass::isInstance).findFirst();
    }

    /**
     * @return The first modifier applied to the player that fits the predicate.
     */
    @Nullable
    public static TempModifier getModifier(LivingEntity entity, Trait trait, Predicate<TempModifier> condition)
    {
        for (TempModifier modifier : EntityTempManager.getTemperatureCap(entity).orElse(new PlayerTempCap()).getModifiers(trait))
        {
            if (condition.test(modifier))
            {   return modifier;
            }
        }
        return null;
    }

    /**
     * Invokes addModifier() in a way that replaces the first occurrence of the modifier, if it exists.<br>
     * Otherwise, it will add the modifier to the end of the list.<br>
     * @param entity The player to apply the modifier to
     * @param modifier The modifier to apply
     * @param trait The type of temperature to apply the modifier to
     * * @param matchPolicy The strictness of the check for finding the TempModifier to replace.
     */
    public static boolean addOrReplaceModifier(LivingEntity entity, TempModifier modifier, Trait trait, Placement.Duplicates matchPolicy)
    {   return addModifier(entity, modifier, trait, Placement.Duplicates.ALLOW, 1, Placement.of(Placement.Mode.REPLACE_OR_ADD, Placement.Order.FIRST, mod -> Placement.Duplicates.check(matchPolicy, modifier, mod)));
    }

    /**
     * Invokes addModifier() in a way that replaces the first occurrence of the modifier, if it exists.<br>
     * It will not add the modifier if a suitable match is not found.<br>
     * @param entity The player to apply the modifier to
     * @param modifier The modifier to apply
     * @param trait The type of temperature to apply the modifier to
     * @param matchPolicy The strictness of the check for finding the TempModifier to replace.
     */
    public static boolean replaceModifier(LivingEntity entity, TempModifier modifier, Trait trait, Placement.Duplicates matchPolicy)
    {   return addModifier(entity, modifier, trait, Placement.Duplicates.ALLOW, 1, Placement.of(Placement.Mode.REPLACE, Placement.Order.FIRST, mod -> Placement.Duplicates.check(matchPolicy, modifier, mod)));
    }

    /**
     * Adds the given modifier to the entity.<br>
     * If duplicates are disabled and the modifier already exists, this action will fail.
     * @param duplicates Disallow duplicates of the same modifier if they match this policy
     */
    public static boolean addModifier(LivingEntity entity, TempModifier modifier, Trait trait, Placement.Duplicates duplicates)
    {   return addModifier(entity, modifier, trait, duplicates, 1, Placement.AFTER_LAST);
    }

    /**
     * Adds the given modifier to the entity, with a custom placement.<br>
     */
    public static boolean addModifier(LivingEntity entity, TempModifier modifier, Trait trait, Placement.Duplicates duplicates, int maxCount, Placement placement)
    {
        TempModifierEvent.Add event = new TempModifierEvent.Add(entity, trait, modifier);
        MinecraftForge.EVENT_BUS.post(event);
        if (!event.isCanceled())
        {
            LazyOptional<ITemperatureCap> optCap = EntityTempManager.getTemperatureCap(entity);
            if (optCap.isPresent())
            {
                ITemperatureCap cap = optCap.resolve().get();
                if (addModifier(cap.getModifiers(trait), event.getModifier(), duplicates, maxCount, placement))
                {
                    updateModifiers(entity, cap);
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    public static boolean addModifier(List<TempModifier> modifiers, TempModifier modifier, Placement.Duplicates duplicatePolicy, int maxCount, Placement placement)
    {
        boolean changed = false;
        Predicate<TempModifier> predicate = placement.predicate;
        if (predicate == null) predicate = mod -> true;

        boolean isReplacing = placement.mode.isReplacing();
        boolean isForward = placement.order == Placement.Order.FIRST;

        if (!isReplacing
        && modifiers.stream().anyMatch(mod -> Placement.Duplicates.check(duplicatePolicy, modifier, mod)))
        {   return false;
        }

        // The number of TempModifiers that match the predicate
        int hits = 0;
        // Get the start of the iterator & which direction it's going
        int start = isForward ? 0 : (modifiers.size() - 1);
        // Iterate through the list (backwards if "forward" is false)
        for (int i = start; isForward ? i < modifiers.size() : i >= 0; i += isForward ? 1 : -1)
        {
            TempModifier modifierAt = modifiers.get(i);
            // If the predicate is true, inject the modifier at this position (or after it if "after" is true)
            if (predicate.test(modifierAt))
            {
                if (isReplacing)
                {   changed = !modifierAt.equals(modifier);
                    modifiers.set(i, modifier);
                }
                else
                {   modifiers.add(i + (placement.mode  == Placement.Mode.AFTER ? 1 : 0), modifier);
                    changed = true;
                }
                hits++;
                // If duplicates are not allowed, break the loop
                if (duplicatePolicy != Placement.Duplicates.ALLOW || hits >= maxCount)
                {   return true;
                }
            }
        }
        // Add the modifier if the insertion check fails
        if (placement.mode != Placement.Mode.REPLACE)
        {
            modifiers.add(modifier);
            changed = true;
        }
        return changed;
    }

    public static void addModifiers(LivingEntity entity, List<TempModifier> modifiers, Trait trait, Placement.Duplicates duplicatePolicy)
    {
        EntityTempManager.getTemperatureCap(entity).ifPresent(cap ->
        {
            boolean changed = false;
            for (TempModifier modifier : modifiers)
            {   changed |= addModifier(entity, modifier, trait, duplicatePolicy);
            }
            if (changed) updateModifiers(entity, cap);
        });
    }

    /**
     * Removes the specified number of TempModifiers of the specified type from the player
     * @param entity The entity being sampled
     * @param trait Determines which TempModifier list to pull from
     * @param maxCount The number of modifiers of the given type to be removed (can be higher than the number of modifiers on the player)
     * @param condition The predicate to determine which TempModifiers to remove
     */
    public static void removeModifiers(LivingEntity entity, Trait trait, int maxCount, Placement.Order order, Predicate<TempModifier> condition)
    {
        EntityTempManager.getTemperatureCap(entity).ifPresent(cap ->
        {
            List<TempModifier> modifiers = cap.getModifiers(trait);
            boolean forwardOrder = order == Placement.Order.FIRST;
            int removed = 0;

            for (int i = forwardOrder ? 0 : modifiers.size() - 1; i >= 0 && i < modifiers.size(); i += forwardOrder ? 1 : -1)
            {
                if (removed < maxCount)
                {
                    TempModifier modifier = modifiers.get(i);
                    if (condition.test(modifier))
                    {
                        TempModifierEvent.Remove event = new TempModifierEvent.Remove(entity, trait, modifier);
                        MinecraftForge.EVENT_BUS.post(event);
                        if (!event.isCanceled())
                        {
                            removed++;
                            modifiers.remove(i);
                            i += forwardOrder ? -1 : 1;
                        }
                    }

                }
                else break;
            }

            // Update modifiers if anything actually changed
            if (removed > 0)
            {   updateModifiers(entity, cap);
            }
        });
    }

    public static void removeModifiers(LivingEntity entity, Trait trait, Predicate<TempModifier> condition)
    {   removeModifiers(entity, trait, Integer.MAX_VALUE, Placement.Order.FIRST, condition);
    }

    /**
     * Gets all TempModifiers of the specified type on the player
     * @param entity is the entity being sampled
     * @param trait determines which TempModifier list to pull from
     * @return an immutable list of all TempModifiers for the specified trait
     */
    public static List<TempModifier> getModifiers(LivingEntity entity, Trait trait)
    {   return EntityTempManager.getTemperatureCap(entity).map(cap -> ImmutableList.copyOf(cap.getModifiers(trait))).orElse(ImmutableList.of());
    }

    /**
     * Iterates through all TempModifiers of the specified type on the player
     * @param trait determines which TempModifier list to pull from
     * @param action the action(s) to perform on each TempModifier
     */
    public static void forEachModifier(LivingEntity entity, Trait trait, Consumer<TempModifier> action)
    {
        EntityTempManager.getTemperatureCap(entity).ifPresent(cap ->
        {
            if (cap.getModifiers(trait) != null)
            {   cap.getModifiers(trait).forEach(action);
            }
        });
    }

    public static void forEachModifier(LivingEntity entity, Trait trait, BiConsumer<TempModifier, InterruptableStreamer<TempModifier>> action)
    {
        EntityTempManager.getTemperatureCap(entity).ifPresent(cap ->
        {
            if (cap.getModifiers(trait) != null)
            {   CSMath.breakableForEach(cap.getModifiers(trait), action);
            }
        });
    }

    public static void updateTemperature(LivingEntity entity, ITemperatureCap cap, boolean instant)
    {
        if (!entity.level.isClientSide)
        {
            ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
            new SyncTemperatureMessage(entity, cap.serializeTraits(), instant));
        }
    }
    public static void updateTemperature(LivingEntity entity)
    {
        EntityTempManager.getTemperatureCap(entity).ifPresent(cap ->
        {   cap.syncValues(entity);
        });
    }

    public static void updateModifiers(LivingEntity entity, ITemperatureCap cap)
    {
        if (!entity.level.isClientSide)
        {
            ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
            new SyncTempModifiersMessage(entity, cap.serializeModifiers()));
        }
    }
    public static void updateModifiers(LivingEntity entity)
    {   EntityTempManager.getTemperatureCap(entity).ifPresent(cap -> updateModifiers(entity, cap));
    }

    public static Map<Trait, Double> getTemperatures(LivingEntity entity)
    {   return EntityTempManager.getTemperatureCap(entity).map(ITemperatureCap::getTraits).orElse(new EnumMap<>(Trait.class));
    }

    public static EnumMap<Trait, List<TempModifier>> getModifiers(LivingEntity entity)
    {
        EnumMap<Trait, List<TempModifier>> map = new EnumMap<>(Trait.class);
        for (Trait trait : EntityTempManager.VALID_MODIFIER_TRAITS)
        {   map.put(trait, getModifiers(entity, trait));
        }
        return map;
    }

    /**
     * Defines all temperature stats in Cold Sweat. <br>
     * These are used to get temperature stored on the player and/or to apply modifiers to it. <br>
     * <br>
     * {@link #WORLD}: The temperature of the area around the player. Should ONLY be changed by TempModifiers. <br>
     * {@link #CORE}: The core temperature of the player (This is what "body" temperature typically refers to). <br>
     * {@link #BASE}: A static offset applied to the player's core temperature. <br>
     * {@link #BODY}: The sum of the player's core and base temperatures. (CANNOT be set) <br>
     * {@link #RATE}: Only used by TempModifiers. Affects the rate at which the player's body temperature changes. <br>
     * <br>
     * {@link #FREEZING_POINT}: The minimum temperature threshold, below which an entity starts freezing. <br>
     * {@link #BURNING_POINT}: The maximum temperature threshold, above which an entity starts overheating. <br>
     * {@link #COLD_RESISTANCE}: Resistance to cold temperature-related damage. <br>
     * {@link #HEAT_RESISTANCE}: Resistance to heat temperature-related damage. <br>
     * {@link #COLD_DAMPENING}: Changes the rate of body temperature increase. <br>
     * {@link #HEAT_DAMPENING}: Changes the rate of body temperature decrease. <br>
     */
    public enum Trait implements StringRepresentable
    {
        WORLD("world", true, true, true),
        CORE("core", true, true, false),
        BASE("base", true, true, true),
        BODY("body", false, false, false),
        RATE("rate", false, true, false),

        FREEZING_POINT("freezing_point", true, true, true),
        BURNING_POINT("burning_point", true, true, true),
        COLD_RESISTANCE("cold_resistance", true, true, true),
        HEAT_RESISTANCE("heat_resistance", true, true, true),
        COLD_DAMPENING("cold_dampening", true, true, true),
        HEAT_DAMPENING("heat_dampening", true, true, true);

        public static final Codec<Trait> CODEC = StringRepresentable.fromEnum(Trait::values);

        private final String id;
        private final boolean forTemperature;
        private final boolean forModifiers;
        private final boolean forAttributes;

        Trait(String id, boolean forTemperature, boolean forModifiers, boolean forAttributes)
        {
            this.id = id;
            this.forTemperature = forTemperature;
            this.forModifiers = forModifiers;
            this.forAttributes = forAttributes;
        }

        public boolean isForTemperature()
        {   return forTemperature;
        }

        public boolean isForModifiers()
        {   return forModifiers;
        }

        public boolean isForAttributes()
        {   return forAttributes;
        }

        public boolean isForWorld()
        {   return this == WORLD || this == BURNING_POINT || this == FREEZING_POINT;
        }

        public static Trait fromID(String id)
        {
            for (Trait trait : values())
            {
                if (trait.getSerializedName().equals(id))
                    return trait;
            }
            return null;
        }

        @Override
        public String getSerializedName()
        {   return id;
        }
    }

    /**
     * Units of measurement used by Cold Sweat.<br>
     * Most calculations are done in MC units, then converted to C or F when they are displayed.<br>
     */
    public enum Units implements StringRepresentable
    {
        F("°F", "f"),
        C("°C", "c"),
        MC("MC", "mc");

        public static final Codec<Units> CODEC = StringRepresentable.fromEnum(Units::values);

        private final String name;
        private final String id;

        Units(String name, String id)
        {   this.name = name;
            this.id = id;
        }

        public static Units fromID(String id)
        {
            for (Units unit : values())
            {
                if (unit.getSerializedName().equals(id))
                    return unit;
            }
            return null;
        }

        public String getFormattedName()
        {   return name;
        }

        @Override
        public String getSerializedName()
        {   return id;
        }
    }

}
