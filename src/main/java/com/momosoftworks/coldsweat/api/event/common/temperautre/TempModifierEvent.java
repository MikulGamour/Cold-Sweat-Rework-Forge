package com.momosoftworks.coldsweat.api.event.common.temperautre;

import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;

import java.util.function.Function;

/**
 * These events are fired when dealing with {@link TempModifier}s. <br>
 * They should not be side-specific. Do not limit them to run on any one side as it will cause desyncs.
 */
public class TempModifierEvent extends Event
{
    protected LivingEntity entity;
    protected Temperature.Trait trait;
    protected TempModifier modifier;

    protected TempModifierEvent(LivingEntity entity, Temperature.Trait trait, TempModifier modifier)
    {
        this.entity = entity;
        this.trait = trait;
        this.modifier = modifier;
    }

    public final LivingEntity getEntity()
    {   return entity;
    }

    public final Temperature.Trait getTrait()
    {   return trait;
    }

    public final TempModifier getModifier()
    {   return modifier;
    }

    /**
     * Fired when a {@link TempModifier} is about to be added to an entity. <br>
     * <br>
     * {@link #entity} - The player the TempModifier is being applied to. <br>
     * {@link #trait} - The {@link Temperature.Trait} the modifier will be applied to. <br>
     * <br>
     * This event is {@link net.minecraftforge.eventbus.api.Cancelable}. <br>
     * Canceling this event will prevent the TempModifier from being added.<br>
     * <br>
     * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
     */
    @Cancelable
    public static class Add extends TempModifierEvent
    {
        public Add(LivingEntity entity, Temperature.Trait trait, TempModifier modifier)
        {   super(entity, trait, modifier);
        }

        public void setTrait(Temperature.Trait newTrait)
        {   this.trait = newTrait;
        }

        public void setModifier(TempModifier modifier)
        {   this.modifier = modifier;
        }
    }

    /**
     * Fired when a {@link TempModifier} is about to be removed from an entity. <br>
     * <br>
     * {@link #entity} - The player the TempModifier is being removed from. <br>
     * {@link #trait} - The modifier's {@link Temperature.Trait}. It will never be {@link Temperature.Trait#BODY}. <br>
     * {@link #modifier} - The TempModifier being removed. <br>
     * <br>
     * This event is {@link net.minecraftforge.eventbus.api.Cancelable}. <br>
     * Canceling this event will prevent the TempModifier from being removed. <br>
     * <br>
     * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
     */
    @Cancelable
    public static class Remove extends TempModifierEvent
    {
        public Remove(LivingEntity entity, Temperature.Trait trait, TempModifier modifier)
        {
            super(entity, trait, modifier);
        }
    }


    /**
     * Fired when a TempModifier runs the {@code calculate()} method. <br>
     * {@code Pre} and {@code Post} are fired on the {@link MinecraftForge#EVENT_BUS} before/after the calculation respectively. <br>
     */
    public static class Calculate extends TempModifierEvent
    {
        protected double temperature;

        public Calculate(TempModifier modifier, LivingEntity entity, double temperature, Temperature.Trait trait)
        {
            super(entity, trait, modifier);
            this.temperature = temperature;
        }

        public double getTemperature()
        {   return temperature;
        }

        public void setTemperature(double temperature)
        {   this.temperature = temperature;
        }

        /**
         * Fired at the beginning of {@link  TempModifier#update(double, LivingEntity, Temperature.Trait)} before the TempModifier calculates its function. <br>
         * <br>
         * {@link #entity} - The player the TempModifier is attached to. <br>
         * {@link #modifier} - The TempModifier running the method. <br>
         * {@link #temperature} - The Temperature being passed into the {@code getValue()} method. <br>
         * {@link #newFunction} - The function that will be used by the TempModifier to calculate temperature. <br><strong>The event must be canceled for this to be used.</strong> <br>
         * <br>
         * This event is {@link Cancelable}. <br>
         * Cancelling this event results in the modifier not calculating its function and using the one provided by the event instead.
         */
        @Cancelable
        public static class Pre extends Calculate
        {
            private Function<Double, Double> newFunction = temp -> temp;

            public Function<Double, Double> getFunction()
            {   return newFunction;
            }

            public void setFunction(Function<Double, Double> newFunction)
            {   this.newFunction = newFunction;
            }

            public Pre(TempModifier modifier, LivingEntity entity, double temperature, Temperature.Trait trait)
            {   super(modifier, entity, temperature, trait);
            }
        }

        /**
         * Fired by {@link TempModifier#update(double, LivingEntity, Temperature.Trait)} after the new function is created.<br>
         * This allows for the function to be modified before it is stored.<br>
         * <br>
         * {@link #entity} is the player the TempModifier is attached to. <br>
         * {@link #modifier} is the TempModifier running the method. <br>
         * {@link #temperature} is the Temperature after the {@code getValue())} method has been called. <br>
         * {@link #newFunction} is the function that will be used by the TempModifier to calculate temperature. <br>
         * <br>
         * This event is NOT {@link Cancelable}. <br>
         */
        public static class Post extends Calculate
        {
            private Function<Double, Double> newFunction;

            public Post(TempModifier modifier, LivingEntity entity, double temp, Function<Double, Double> newFunction, Temperature.Trait trait)
            {   super(modifier, entity, temp, trait);
                this.newFunction = newFunction;
            }

            public Function<Double, Double> getFunction()
            {   return newFunction;
            }

            public void setFunction(Function<Double, Double> newFunction)
            {   this.newFunction = newFunction;
            }
        }
    }
}
