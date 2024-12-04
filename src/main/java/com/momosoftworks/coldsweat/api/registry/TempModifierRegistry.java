package com.momosoftworks.coldsweat.api.registry;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.util.exceptions.RegistryFailureException;
import com.momosoftworks.coldsweat.util.math.FastBiMap;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.function.Supplier;

public class TempModifierRegistry
{
    static FastBiMap<ResourceLocation, TempModifierHolder> TEMP_MODIFIERS = new FastBiMap<>();

    public static BiMap<ResourceLocation, TempModifierHolder> getEntries()
    {   return ImmutableBiMap.copyOf(TEMP_MODIFIERS);
    }

    public static void register(ResourceLocation id, Supplier<TempModifier> supplier)
    {
        if (TEMP_MODIFIERS.containsKey(id))
        {
            throw new RegistryFailureException(id, "TempModifier", String.format("Found duplicate TempModifier entries: %s (%s) %s (%s)", supplier.get().getClass().getName(), id,
                                                             TEMP_MODIFIERS.get(id).getClass().getName(), id), null);
        }
        TEMP_MODIFIERS.put(id, new TempModifierHolder(supplier));
    }

    /**
     * Clears the registry of all items. This effectively "un-registers" all TempModifiers.
     */
    public static void flush()
    {
        TEMP_MODIFIERS.clear();
    }

    /**
     * Returns a new instance of the TempModifier with the given ID.<br>
     * If a TempModifier with this ID is not in the registry, this method returns null and logs an error.<br>
     */
    public static Optional<TempModifier> getValue(ResourceLocation id)
    {
        return Optional.ofNullable(TEMP_MODIFIERS.get(id)).map(TempModifierHolder::get);
    }

    public static ResourceLocation getKey(TempModifier modifier)
    {
        return TEMP_MODIFIERS.getKey(new TempModifierHolder(modifier));
    }

    public static class TempModifierHolder
    {
        private final Supplier<TempModifier> supplier;
        private final Class<? extends TempModifier> clazz;

        public TempModifierHolder(Supplier<TempModifier> supplier)
        {   this.supplier = supplier;
            this.clazz = supplier.get().getClass();
        }
        public TempModifierHolder(TempModifier modifier)
        {   this.supplier = () -> modifier;
            this.clazz = modifier.getClass();
        }

        public TempModifier get()
        {   return supplier.get();
        }

        public Class<? extends TempModifier> getModifierClass()
        {   return clazz;
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof TempModifierHolder holder && holder.clazz == clazz;
        }

        @Override
        public String toString()
        {   return "TempModifierHolder{" + clazz.getName() + "}";
        }
    }
}
