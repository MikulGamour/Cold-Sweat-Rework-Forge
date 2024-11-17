package com.momosoftworks.coldsweat.api.event.core.registry;

import com.google.common.collect.Multimap;
import com.momosoftworks.coldsweat.data.ModRegistries;
import com.momosoftworks.coldsweat.data.codec.configuration.*;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
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
    DynamicRegistries registryAccess;
    Multimap<RegistryKey<Registry<?>>, ?> registries;

    public CreateRegistriesEvent(DynamicRegistries registryAccess, Multimap<RegistryKey<Registry<?>>, ?> registries)
    {
        this.registryAccess = registryAccess;
        this.registries = registries;
    }

    public DynamicRegistries getRegistryAccess()
    {   return registryAccess;
    }

    public Multimap<RegistryKey<Registry<?>>, ?> getRegistries()
    {   return registries;
    }

    public Set<InsulatorData> getInsulators()
    {   return getRegistry(ModRegistries.INSULATOR_DATA);
    }

    public Set<FuelData> getFuels()
    {   return getRegistry(ModRegistries.FUEL_DATA);
    }

    public Set<FoodData> getFoods()
    {   return getRegistry(ModRegistries.FOOD_DATA);
    }

    public Set<ItemCarryTempData> getCarryTemps()
    {   return getRegistry(ModRegistries.CARRY_TEMP_DATA);
    }

    public Set<BlockTempData> getBlockTemps()
    {   return getRegistry(ModRegistries.BLOCK_TEMP_DATA);
    }

    public Set<BiomeTempData> getBiomeTemps()
    {   return getRegistry(ModRegistries.BIOME_TEMP_DATA);
    }

    public Set<DimensionTempData> getDimensionTemps()
    {   return getRegistry(ModRegistries.DIMENSION_TEMP_DATA);
    }

    public Set<StructureTempData> getStructureTemps()
    {   return getRegistry(ModRegistries.STRUCTURE_TEMP_DATA);
    }

    public Set<DepthTempData> getDepthTemps()
    {   return getRegistry(ModRegistries.DEPTH_TEMP_DATA);
    }

    public Set<MountData> getMounts()
    {   return getRegistry(ModRegistries.MOUNT_DATA);
    }

    public Set<SpawnBiomeData> getSpawnBiomes()
    {   return getRegistry(ModRegistries.ENTITY_SPAWN_BIOME_DATA);
    }

    public Set<EntityTempData> getEntityTemps()
    {   return getRegistry(ModRegistries.ENTITY_TEMP_DATA);
    }

    public <T> Set<T> getRegistry(RegistryKey<Registry<T>> key)
    {
        return registries.containsKey(key)
               ? new HashSet<>(registries.get((RegistryKey) key))
               : new HashSet<>();
    }
}
