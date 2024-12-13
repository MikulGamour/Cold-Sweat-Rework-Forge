package com.momosoftworks.coldsweat.api.temperature.modifier;

import com.momosoftworks.coldsweat.api.event.common.temperautre.TempModifierEvent;
import com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent;
import com.momosoftworks.coldsweat.api.registry.TempModifierRegistry;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.core.init.TempModifierInit;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.MinecraftForge;

import java.util.function.Function;

/**
 * TempModifiers are applied to entities to dynamically change their temperature.<br>
 * For example, biome temperature, time of day, depth, and waterskins are all TempModifiers<br>
 *<br>
 * It is up to you to apply and remove these modifiers manually.<br>
 *<br>
 * TempModifiers must be REGISTERED using {@link TempModifierRegisterEvent}<br>
 * (see {@link TempModifierInit} for an example)<br>
 */
public abstract class TempModifier
{
    private CompoundNBT nbt = new CompoundNBT();
    private int expireTicks = -1;
    private int ticksExisted = 0;
    private int tickRate = 1;
    private double lastInput = 0;
    private double lastOutput = 0;
    private Function<Double, Double> function = temp -> temp;
    private boolean changed = false;

    /**
     * Default constructor (REQUIRED for proper registration).<br>
     */
    public TempModifier() {}

    /**
     * Determines what the provided temperature would be, given the player it is being applied to.<br>
     * This is basically a simple in-out system. It is given a temperature, and returns a new temperature based on the PlayerEntity.<br>
     * <br>
     * @param entity the entity that is being affected by the modifier.<br>
     * @return the new temperature.<br>
     */
    protected abstract Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait);

    /**
     * Posts this TempModifier's {@link #calculate(LivingEntity, Temperature.Trait)} to the Forge event bus.<br>
     * Returns the stored value if this TempModifier has a tickRate set, and it is not the right tick.<br>
     * <br>
     * @param temp the Temperature being fed into the {@link #calculate(LivingEntity, Temperature.Trait)} method.
     * @param entity the entity that is being affected by the modifier.
     */
    public final double update(double temp, LivingEntity entity, Temperature.Trait trait)
    {
        TempModifierEvent.Calculate.Pre pre = new TempModifierEvent.Calculate.Pre(this, entity, temp, trait);
        MinecraftForge.EVENT_BUS.post(pre);
        if (pre.isCanceled())
        {
            this.function = pre.getFunction();
            return this.apply(pre.getTemperature());
        }

        TempModifierEvent.Calculate.Post post = new TempModifierEvent.Calculate.Post(this, entity, pre.getTemperature(), this.calculate(entity, trait), trait);
        MinecraftForge.EVENT_BUS.post(post);

        this.function = post.getFunction();

        return this.apply(post.getTemperature());
    }

    /**
     * @param temp the Temperature to calculate with
     * @return The result of this TempModifier's unique stored function. Stores the input and output.
     */
    public double apply(double temp)
    {
        lastInput = temp;
        return lastOutput = function.apply(temp);
    }

    /**
     * Sets the number of ticks this TempModifier will exist before it is automatically removed.<br>
     * @param ticks the number of ticks this modifier will last.
     * @return this TempModifier instance (allows for in-line building).
     */
    public final TempModifier expires(int ticks)
    {
        expireTicks = ticks;
        return this;
    }
    public final int getExpireTime()
    {   return expireTicks;
    }
    public final int getTicksExisted()
    {   return ticksExisted;
    }
    public final int setTicksExisted(int ticks)
    {   return ticksExisted = ticks;
    }

    /**
     * TempModifiers can be configured to run {@link TempModifier#calculate(LivingEntity, Temperature.Trait)} at a specified interval.<br>
     * This is useful if the TempModifier is expensive to calculate, and you want to avoid it being called each tick.<br>
     * <br>
     * Every X ticks, the TempModifier's {@code getResult()} function will be called, then stored internally.<br>
     * Every other time {@code calculate()} is called, the stored value will be returned until X ticks have passed.<br>
     * (new TempModifiers ALWAYS run {@code getResult()} when they are called for the first time).<br>
     * <br>
     * @param ticks the number of ticks between each call to {@code getResult()}.
     * @return this TempModifier instance (allows for in-line building).
     */
    public final TempModifier tickRate(int ticks)
    {   tickRate = Math.max(1, ticks);
        return this;
    }

    public final int getTickRate()
    {   return tickRate;
    }

    /**
     * @return The Temperature this TempModifier was last given
     */
    public final double getLastInput()
    {   return lastInput;
    }

    /**
     * @return The Temperature this TempModifier's function last returned
     */
    public final double getLastOutput()
    {   return lastOutput;
    }

    public final CompoundNBT getNBT()
    {
        return nbt;
    }

    public void setNBT(CompoundNBT data)
    {
        this.nbt = data;
    }

    @Override
    public String toString()
    {   return TempModifierRegistry.getKey(this).toString();
    }

    public void markDirty()
    {   this.changed = true;
    }

    public boolean isDirty()
    {   return this.changed;
    }

    public void markClean()
    {   this.changed = false;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj instanceof TempModifier)
        {
            TempModifier mod = ((TempModifier) obj);
            return this.getClass().equals(mod.getClass())
                && mod.getNBT().equals(this.getNBT());
        }
        return false;
    }
}
