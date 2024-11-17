package com.momosoftworks.coldsweat.api.event.core.registry;

import com.google.common.collect.Multimap;
import com.momosoftworks.coldsweat.data.ModRegistries;
import com.momosoftworks.coldsweat.data.codec.configuration.*;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.eventbus.api.Event;

import java.util.HashSet;
import java.util.Set;

/**
 * Gives subscribers unrestricted access to Cold Sweat's registries as they are being loaded.<br>
 * <br>
 * Fired on the Forge event bus when Cold Sweat's registries are gathered, but before they are committed to {@link com.momosoftworks.coldsweat.config.ConfigSettings} where they become usable.<br>
 * <br>
 * This even is not {@link net.minecraftforge.eventbus.api.Cancelable}.
 */
public class CreateRegistriesEvent extends Event
{
    RegistryAccess registryAccess;
    Multimap<ResourceKey<Registry<?>>, Holder<?>> registries;

    public CreateRegistriesEvent(RegistryAccess registryAccess, Multimap<ResourceKey<Registry<?>>, Holder<?>> registries)
    {
        this.registryAccess = registryAccess;
        this.registries = registries;
    }

    public RegistryAccess getRegistryAccess()
    {   return registryAccess;
    }

    public Multimap<ResourceKey<Registry<?>>, Holder<?>> getRegistries()
    {   return registries;
    }

    public Set<Holder<InsulatorData>> getInsulators()
    {   return getRegistry(ModRegistries.INSULATOR_DATA);
    }

    public Set<Holder<FuelData>> getFuels()
    {   return getRegistry(ModRegistries.FUEL_DATA);
    }

    public Set<Holder<FoodData>> getFoods()
    {   return getRegistry(ModRegistries.FOOD_DATA);
    }

    public Set<Holder<ItemCarryTempData>> getCarryTemps()
    {   return getRegistry(ModRegistries.CARRY_TEMP_DATA);
    }

    public Set<Holder<BlockTempData>> getBlockTemps()
    {   return getRegistry(ModRegistries.BLOCK_TEMP_DATA);
    }

    public Set<Holder<BiomeTempData>> getBiomeTemps()
    {   return getRegistry(ModRegistries.BIOME_TEMP_DATA);
    }

    public Set<Holder<DimensionTempData>> getDimensionTemps()
    {   return getRegistry(ModRegistries.DIMENSION_TEMP_DATA);
    }

    public Set<Holder<StructureTempData>> getStructureTemps()
    {   return getRegistry(ModRegistries.STRUCTURE_TEMP_DATA);
    }

    public Set<Holder<DepthTempData>> getDepthTemps()
    {   return getRegistry(ModRegistries.DEPTH_TEMP_DATA);
    }

    public Set<Holder<MountData>> getMounts()
    {   return getRegistry(ModRegistries.MOUNT_DATA);
    }

    public Set<Holder<SpawnBiomeData>> getSpawnBiomes()
    {   return getRegistry(ModRegistries.ENTITY_SPAWN_BIOME_DATA);
    }

    public Set<Holder<EntityTempData>> getEntityTemps()
    {   return getRegistry(ModRegistries.ENTITY_TEMP_DATA);
    }

    public <T> Set<Holder<T>> getRegistry(ResourceKey<Registry<T>> key)
    {
        return registries.containsKey(key)
               ? new HashSet<>(registries.get((ResourceKey) key))
               : new HashSet<>();
    }
}
