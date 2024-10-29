package com.momosoftworks.coldsweat.api.event.core.registry;

import com.momosoftworks.coldsweat.api.registry.TempModifierRegistry;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.util.exceptions.RegistryFailureException;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

/**
 * Builds the {@link TempModifierRegistry}. <br>
 * The event is fired during {@link net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent}. <br>
 * <br>
 * This event is NOT {@link net.minecraftforge.eventbus.api.Cancelable}. <br>
 * <br>
 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
 */
public class TempModifierRegisterEvent extends Event
{
    /**
     * Adds a new {@link TempModifier} to the registry.
     *
     * @param modifier the {@link TempModifier} to add.
     */
    public void register(ResourceLocation id, Supplier<TempModifier> modifier)
    {   TempModifierRegistry.register(id, modifier);
    }

    /**
     * A way of indirectly registering TempModifiers by class name.<br>
     * Useful for adding compat for other mods, where loading the TempModifier's class directly would cause an error.<br>
     * The class must have a no-arg constructor for this to work.
     * @param id The ID of the TempModifier. Should use your mod ID as the namespace
     * @param classPath The path to the TempModifier class, e.g. "com.examplemod.TempModifier"
     */
    public void registerByClassName(ResourceLocation id, String classPath)
    {
        try
        {
            Constructor<?> clazz = Class.forName(classPath).getConstructor();
            this.register(id, () ->
            {
                try
                {   return (TempModifier) clazz.newInstance();
                }
                catch (Exception e)
                {   throw new RegistryFailureException(id, "TempModifier", "Failed to instantiate class " + classPath, e);
                }
            });
        }
        catch (Exception e)
        {   throw new RegistryFailureException(id, "TempModifier", e.getMessage(), e);
        }
    }
}
