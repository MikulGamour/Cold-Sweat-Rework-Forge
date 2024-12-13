package com.momosoftworks.coldsweat.util.serialization;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraft.nbt.CompoundNBT;
import com.momosoftworks.coldsweat.util.exceptions.SerializationException;
import net.minecraft.client.Minecraft;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraftforge.fml.common.thread.EffectiveSide;

import java.util.function.*;

/**
 * Contains a value that updates as needed (usually when a player interacts with the config screen). <br>
 * If added to {@link ConfigSettings#CONFIG_SETTINGS}, it will be synced to the client.
 */
public class DynamicHolder<T>
{
    private T value;
    private Supplier<T> valueCreator;
    private Loader<T> loader;
    private Saver<T> saver;
    private Writer<T> encoder;
    private Reader<T> decoder;
    private SyncType syncType = SyncType.NONE;
    private boolean requireRegistries = false;

    protected DynamicHolder(Supplier<T> valueCreator, Consumer<DynamicHolder<T>> loader)
    {   this.valueCreator = valueCreator;
        this.loader = (holder, registryAccess) -> loader.accept(holder);
    }

    protected DynamicHolder(Supplier<T> valueCreator, Loader<T> loader)
    {   this.valueCreator = valueCreator;
        this.loader = loader;
    }

    /**
     * Creates a simple value holder.
     * @param valueCreator A supplier that returns the holder's value.
     * @return A value holder.
     * @param <T> The type of the value.
     */
    public static <T> DynamicHolder<T> createWithRegistries(Supplier<T> valueCreator, Loader<T> loader)
    {
        DynamicHolder<T> holder = new DynamicHolder<>(valueCreator, loader);
        holder.requireRegistries = true;
        return holder;
    }

    public static <T> DynamicHolder<T> create(Supplier<T> valueCreator, Consumer<DynamicHolder<T>> loader)
    {   return new DynamicHolder<>(valueCreator, loader);
    }

    public static <T> DynamicHolder<T> create(Supplier<T> valueCreator)
    {   return new DynamicHolder<>(valueCreator, holder -> {});
    }

    /**
     * Creates a value holder that can be synced between the server and client.
     * @param valueCreator A supplier that returns the holder's value.
     * @param encoder Writes the value to a CompoundNBT.
     * @param decoder Reads the value from a CompoundNBT.
     * @param saver Writes the value to a config file. This may be either on the server or client.
     * @return A synced value holder.
     * @param <T> The type of the value.
     */
    public static <T> DynamicHolder<T> createSyncedWithRegistries(Supplier<T> valueCreator, Loader<T> loader, Writer<T> encoder, Reader<T> decoder, Saver<T> saver, SyncType syncType)
    {
        if (syncType == SyncType.NONE)
        {   throw new IllegalArgumentException("SyncType cannot be NONE for a synced DynamicHolder.");
        }
        DynamicHolder<T> holder = new DynamicHolder<>(valueCreator, loader);
        holder.encoder = encoder;
        holder.decoder = decoder;
        holder.saver = saver;
        holder.syncType = syncType;
        holder.requireRegistries = true;
        return holder;
    }

    public static <T> DynamicHolder<T> createSynced(Supplier<T> valueCreator, Consumer<DynamicHolder<T>> loader, Function<T, CompoundNBT> encoder, Function<CompoundNBT, T> decoder, Consumer<T> saver, SyncType syncType)
    {
        if (syncType == SyncType.NONE)
        {   throw new IllegalArgumentException("SyncType cannot be NONE for a synced DynamicHolder.");
        }
        DynamicHolder<T> holder = new DynamicHolder<>(valueCreator, loader);
        holder.encoder = (val, registryAccess) -> encoder.apply(val);
        holder.decoder = (tag, registryAccess) -> decoder.apply(tag);
        holder.saver = (val, registryAccess) -> saver.accept(val);
        holder.syncType = syncType;
        return holder;
    }

    public T get()
    {
        if (this.requireRegistries)
        {   throw new RuntimeException("DynamicRegistries is required for this DynamicHolder, yet none was provided.");
        }
        if (this.value == null)
        {   this.load(true);
        }
        return value;
    }

    public T get(DynamicRegistries registryAccess)
    {
        if (this.value == null)
        {   this.load(registryAccess, true);
        }
        return value;
    }

    public void set(T value)
    {
        this.value = value;
    }

    public void setUnsafe(Object value)
    {
        this.value = (T) value;
    }

    public void load(boolean replace)
    {
        if (replace)
        {   this.value = this.valueCreator.get();
        }
        this.loader.load(this, null);
    }

    public void load(DynamicRegistries registryAccess, boolean replace)
    {
        if (replace)
        {   this.value = this.valueCreator.get();
        }
        this.loader.load(this, registryAccess);
    }

    public CompoundNBT encode(DynamicRegistries registryAccess)
    {
        if (!isSynced())
        {  throw ColdSweat.LOGGER.throwing(SerializationException.serialize(this.value, "Tried to encode non-synced DynamicHolder", null));
        }
        try
        {   return encoder.write(this.get(registryAccess), registryAccess);
        }
        catch (Exception e)
        {   throw ColdSweat.LOGGER.throwing(SerializationException.serialize(this.value, "Failed to encode DynamicHolder for type " + this.value.getClass().getSimpleName(), e));
        }
    }

    public void decode(CompoundNBT tag, DynamicRegistries registryAccess)
    {
        if (!isSynced())
        {  throw ColdSweat.LOGGER.throwing(new SerializationException("Tried to decode non-synced DynamicHolder", null));
        }
        try
        {   this.value = decoder.read(tag, registryAccess);
        }
        catch (Exception e)
        {   throw ColdSweat.LOGGER.throwing(new SerializationException("Failed to decode DynamicHolder for type " + this.value.getClass().getSimpleName(), e));
        }
    }

    public void save(DynamicRegistries registryAccess)
    {
        if (!isSynced())
        {  throw ColdSweat.LOGGER.throwing(new SerializationException("Tried to save non-synced DynamicHolder", null));
        }
        try
        {   saver.save(this.get(registryAccess), registryAccess);
        }
        catch (Exception e)
        {   throw ColdSweat.LOGGER.throwing(new SerializationException("Failed to save DynamicHolder for type " + this.value.getClass().getSimpleName(), e));
        }
    }

    public void reset()
    {   this.value = this.valueCreator.get();
    }

    public boolean isSynced()
    {   return syncType != SyncType.NONE;
    }

    public SyncType getSyncType()
    {   return syncType;
    }

    public boolean requiresRegistries()
    {   return requireRegistries;
    }

    @FunctionalInterface
    public interface Loader<T>
    {
        void load(DynamicHolder<T> holder, DynamicRegistries registryAccess);
    }

    @FunctionalInterface
    public interface Writer<T>
    {
        CompoundNBT write(T value, DynamicRegistries registryAccess);
    }

    @FunctionalInterface
    public interface Reader<T>
    {
        T read(CompoundNBT tag, DynamicRegistries registryAccess);
    }

    @FunctionalInterface
    public interface Saver<T>
    {
        void save(T value, DynamicRegistries registryAccess);
    }

    public enum SyncType
    {
        // The value is not synced between the server and client.
        NONE,
        // The value is synced server -> client AND client -> server (when the config menu is used)
        BOTH_WAYS,
        // The value is synced server -> client only
        ONE_WAY;

        public boolean canSend()
        {
            return this == BOTH_WAYS || (this == ONE_WAY && EffectiveSide.get().isServer());
        }

        public boolean canReceive()
        {   return this == BOTH_WAYS || (this == ONE_WAY && EffectiveSide.get().isClient() && !Minecraft.getInstance().isLocalServer());
        }
    }
}
