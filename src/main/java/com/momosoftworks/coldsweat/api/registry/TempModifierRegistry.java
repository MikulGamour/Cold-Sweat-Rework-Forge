package com.momosoftworks.coldsweat.api.registry;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.util.exceptions.RegistryFailureException;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.FastBiMap;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
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
        TempModifierHolder holder = new TempModifierHolder(supplier, id);
        if (TEMP_MODIFIERS.containsKey(id) || TEMP_MODIFIERS.values().stream().anyMatch(holder::equals))
        {
            throw ColdSweat.LOGGER.throwing(new RegistryFailureException(id, "TempModifier", String.format("Found duplicate TempModifier entries: %s (%s) %s (%s)", holder.getModifierClass().getName(), id,
                                                                           TEMP_MODIFIERS.get(id).getModifierClass().getName(), id), null));
        }
        TEMP_MODIFIERS.put(id, holder);
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
    {   return CSMath.getIfNotNull(getHolder(modifier), TempModifierHolder::getId, null);
    }

    @Nullable
    public static TempModifierHolder getHolder(TempModifier modifier)
    {
        for (TempModifierHolder holder : TEMP_MODIFIERS.values())
        {
            if (holder.getModifierClass() == modifier.getClass())
            {   return holder;
            }
        }
        return null;
    }

    public static class TempModifierHolder
    {
        private final Supplier<TempModifier> supplier;
        private final Class<? extends TempModifier> clazz;
        private final ResourceLocation id;

        public TempModifierHolder(Supplier<TempModifier> supplier, ResourceLocation id)
        {   this.supplier = supplier;
            this.clazz = supplier.get().getClass();
            this.id = id;
        }

        public TempModifier get()
        {   return supplier.get();
        }

        public Class<? extends TempModifier> getModifierClass()
        {   return clazz;
        }

        public ResourceLocation getId()
        {   return id;
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
