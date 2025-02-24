package com.momosoftworks.coldsweat.compat.kubejs.event;

import com.google.common.collect.Multimap;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.registry.BlockTempRegistry;
import com.momosoftworks.coldsweat.api.registry.TempModifierRegistry;
import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTemp;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.compat.kubejs.event.builder.*;
import com.momosoftworks.coldsweat.compat.kubejs.util.TempModifierDataJS;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.*;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.util.serialization.DynamicHolder;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import dev.latvian.mods.kubejs.event.StartupEventJS;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.function.Consumer;
import java.util.function.Function;

public class ModRegistriesEventJS extends StartupEventJS
{
    /*
     Block Temperature
     */

    public void addBlockTemperature(Consumer<BlockTempBuilderJS> builder, BlockTempBuilderJS.Function function)
    {
        BlockTempBuilderJS blockTempJS = new BlockTempBuilderJS();
        builder.accept(blockTempJS);
        BlockTemp blockTemp = blockTempJS.build(function);
        if (blockTemp.getAffectedBlocks().isEmpty()) return;

        BlockTempRegistry.register(blockTemp);
        ColdSweat.LOGGER.info("Registered KubeJS block temperature for blocks: {}", blockTempJS.blocks);
    }

    public void addBlockTemperature(double temperature, String units, Consumer<BlockTempBuilderJS> builder)
    {
        builder = ((Consumer<BlockTempBuilderJS>) blockTemp -> blockTemp.units(Temperature.Units.fromID(units))).andThen(builder);
        addBlockTemperature(builder, (level, entity, state, pos, distance) -> temperature);
    }

    /*
     Insulator
     */

    public void addInsulator(Consumer<InsulatorBuilderJS> builder)
    {
        InsulatorBuilderJS insulatorJS = new InsulatorBuilderJS();
        builder.accept(insulatorJS);
        InsulatorData insulator = insulatorJS.build();

        Multimap<Item, InsulatorData> map = switch (insulatorJS.slot)
        {
            case ITEM -> ConfigSettings.INSULATION_ITEMS.get();
            case ARMOR -> ConfigSettings.INSULATING_ARMORS.get();
            case CURIO -> ConfigSettings.INSULATING_CURIOS.get();
        };
        if (insulatorJS.items.isEmpty())
        {   insulatorJS.items.add(null);
        }
        for (Item item : insulatorJS.items)
        {   map.put(item, insulator);
        }
        ColdSweat.LOGGER.info("Registered KubeJS insulator for items: {}", insulatorJS.items);
    }

    /*
     Food Temperature
     */

    public void addFoodTemperature(Consumer<FoodBuilderJS> builder)
    {
        FoodBuilderJS foodJS = new FoodBuilderJS();
        builder.accept(foodJS);
        FoodData foodData = foodJS.build();
        if (!foodData.areRequiredModsLoaded()) return;

        if (foodJS.items.isEmpty())
        {   foodJS.items.add(null);
        }
        for (Item item : foodJS.items)
        {   ConfigSettings.FOOD_TEMPERATURES.get().put(item, foodData);
        }
    }

    /*
     Fuel
     */

    private void addFuel(Consumer<FuelBuilderJS> builder, DynamicHolder<Multimap<Item, FuelData>> config, FuelData.FuelType fuelType)
    {
        FuelBuilderJS fuelJS = new FuelBuilderJS();
        builder.accept(fuelJS);
        FuelData fuelData = fuelJS.build(fuelType);
        if (!fuelData.areRequiredModsLoaded()) return;

        if (fuelJS.items.isEmpty())
        {   fuelJS.items.add(null);
        }
        for (Item item : fuelJS.items)
        {   config.get().put(item, fuelData);
        }
    }

    public void addHearthFuel(Consumer<FuelBuilderJS> builder)
    {   addFuel(builder, ConfigSettings.HEARTH_FUEL, FuelData.FuelType.HEARTH);
    }

    public void addBoilerFuel(Consumer<FuelBuilderJS> builder)
    {   addFuel(builder, ConfigSettings.BOILER_FUEL, FuelData.FuelType.BOILER);
    }

    public void addIceboxFuel(Consumer<FuelBuilderJS> builder)
    {   addFuel(builder, ConfigSettings.ICEBOX_FUEL, FuelData.FuelType.ICEBOX);
    }

    public void addSoulspringLampFuel(Consumer<FuelBuilderJS> builder)
    {   addFuel(builder, ConfigSettings.SOULSPRING_LAMP_FUEL, FuelData.FuelType.SOUL_LAMP);
    }

    /*
     Carried Item Temperature
     */

    public void addCarriedItemTemperature(Consumer<CarriedItemBuilderJS> builder)
    {
        CarriedItemBuilderJS carriedItemJS = new CarriedItemBuilderJS();
        builder.accept(carriedItemJS);
        ItemCarryTempData carryData = carriedItemJS.build();
        if (!carryData.areRequiredModsLoaded()) return;

        if (carriedItemJS.items.isEmpty())
        {   carriedItemJS.items.add(null);
        }
        for (Item item : carriedItemJS.items)
        {   ConfigSettings.CARRIED_ITEM_TEMPERATURES.get().put(item, carryData);
        }
    }

    /*
     Drying Item
     */

    public void addDryingItem(Consumer<DryingItemBuilderJS> builder)
    {
        DryingItemBuilderJS dryingItemJS = new DryingItemBuilderJS();
        builder.accept(dryingItemJS);
        DryingItemData dryingData = dryingItemJS.build();
        if (!dryingData.areRequiredModsLoaded()) return;

        if (dryingItemJS.items.isEmpty())
        {   dryingItemJS.items.add(null);
        }
        for (Item item : dryingItemJS.items)
        {   ConfigSettings.DRYING_ITEMS.get().put(item, dryingData);
        }
    }

    /*
     Biome Temperature
     */

    public void addBiomeTemperature(String biomeId, double minTemp, double maxTemp, String units)
    {
        RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();
        if (registryAccess == null) return;
        Holder<Biome> biome = RegistryHelper.getBiome(new ResourceLocation(biomeId), registryAccess);
        if (biome == null)
        {   ColdSweat.LOGGER.error("Failed to find biome with ID: {}", biomeId);
            return;
        }
        BiomeTempData biomeData = new BiomeTempData(biome, minTemp, maxTemp, Temperature.Units.fromID(units), false);
        biomeData.setType(ConfigData.Type.KUBEJS);
        if (!biomeData.areRequiredModsLoaded()) return;

        ConfigSettings.BIOME_TEMPS.get().put(biome, biomeData);
    }

    public void addBiomeTemperature(String biomeId, double minTemp, double maxTemp)
    {   addBiomeTemperature(biomeId, minTemp, maxTemp, "mc");
    }

    public void addBiomeOffset(String biomeId, double minTemp, double maxTemp, String units)
    {
        RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();
        if (registryAccess == null) return;
        Holder<Biome> biome = RegistryHelper.getBiome(new ResourceLocation(biomeId), registryAccess);
        if (biome == null)
        {   ColdSweat.LOGGER.error("Failed to find biome with ID: {}", biomeId);
            return;
        }
        BiomeTempData biomeData = new BiomeTempData(biome, minTemp, maxTemp, Temperature.Units.fromID(units), true);
        biomeData.setType(ConfigData.Type.KUBEJS);
        if (!biomeData.areRequiredModsLoaded()) return;

        ConfigSettings.BIOME_OFFSETS.get().put(biome, biomeData);
    }

    public void addBiomeOffset(String biomeId, double minTemp, double maxTemp)
    {   addBiomeOffset(biomeId, minTemp, maxTemp, "mc");
    }

    /*
     Dimension Temperature
     */

    public void addDimensionTemperature(String dimensionId, double temperature, String units)
    {
        RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();
        if (registryAccess == null) return;
        Holder<DimensionType> dimension = RegistryHelper.getDimension(new ResourceLocation(dimensionId), registryAccess);
        if (dimension == null)
        {   ColdSweat.LOGGER.error("Failed to find dimension with ID: {}", dimensionId);
            return;
        }
        DimensionTempData dimensionData = new DimensionTempData(dimension, temperature, Temperature.Units.fromID(units), false);
        dimensionData.setType(ConfigData.Type.KUBEJS);
        if (!dimensionData.areRequiredModsLoaded()) return;

        ConfigSettings.DIMENSION_TEMPS.get().put(dimension, dimensionData);
    }

    public void addDimensionTemperature(String dimensionId, double temperature)
    {   addDimensionTemperature(dimensionId, temperature, "mc");
    }

    public void addDimensionOffset(String dimensionId, double temperature, String units)
    {
        RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();
        if (registryAccess == null) return;
        Holder<DimensionType> dimension = RegistryHelper.getDimension(new ResourceLocation(dimensionId), registryAccess);
        if (dimension == null)
        {   ColdSweat.LOGGER.error("Failed to find dimension with ID: {}", dimensionId);
            return;
        }
        DimensionTempData dimensionData = new DimensionTempData(dimension, temperature, Temperature.Units.fromID(units), true);
        dimensionData.setType(ConfigData.Type.KUBEJS);
        if (!dimensionData.areRequiredModsLoaded()) return;

        ConfigSettings.DIMENSION_OFFSETS.get().put(dimension, dimensionData);
    }

    public void addDimensionOffset(String dimensionId, double temperature)
    {   addDimensionOffset(dimensionId, temperature, "mc");
    }

    /*
     Structure Temperature
     */

    public void addStructureTemperature(String structureId, double temperature, String units)
    {
        RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();
        if (registryAccess == null) return;
        Holder<Structure> structure = RegistryHelper.getStructure(new ResourceLocation(structureId), registryAccess);
        if (structure == null)
        {   ColdSweat.LOGGER.error("Failed to find structure with ID: {}", structureId);
            return;
        }
        StructureTempData structureData = new StructureTempData(structure, temperature, Temperature.Units.fromID(units), false);
        structureData.setType(ConfigData.Type.KUBEJS);
        if (!structureData.areRequiredModsLoaded()) return;

        ConfigSettings.STRUCTURE_TEMPS.get().put(structure, structureData);
    }

    public void addStructureTemperature(String structureId, double temperature)
    {   addStructureTemperature(structureId, temperature, "mc");
    }

    public void addStructureOffset(String structureId, double temperature, String units)
    {
        RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();
        if (registryAccess == null) return;
        Holder<Structure> structure = RegistryHelper.getStructure(new ResourceLocation(structureId), registryAccess);
        if (structure == null)
        {   ColdSweat.LOGGER.error("Failed to find structure with ID: {}", structureId);
            return;
        }
        StructureTempData structureData = new StructureTempData(structure, temperature, Temperature.Units.fromID(units), false);
        structureData.setType(ConfigData.Type.KUBEJS);
        if (!structureData.areRequiredModsLoaded()) return;

        ConfigSettings.STRUCTURE_OFFSETS.get().put(structure, structureData);
    }

    public void addStructureOffset(String structureId, double temperature)
    {   addStructureOffset(structureId, temperature, "mc");
    }

    /*
     Entity Temperature
     */

    public void addEntityTemperature(Consumer<EntityTempBuilderJS> builder)
    {
        EntityTempBuilderJS entityTempJS = new EntityTempBuilderJS();
        builder.accept(entityTempJS);
        EntityTempData entityTempData = entityTempJS.build();
        if (!entityTempData.areRequiredModsLoaded()) return;

        if (entityTempJS.entities.isEmpty())
        {   entityTempJS.entities.add(null);
        }
        for (EntityType<?> item : entityTempJS.entities)
        {   ConfigSettings.ENTITY_TEMPERATURES.get().put(item, entityTempData);
        }
    }

    /*
     Insulating Mounts
     */

    public void addInsulatingMount(Consumer<InsulatingMountBuilderJS> builder)
    {
        InsulatingMountBuilderJS insulatingMountJS = new InsulatingMountBuilderJS();
        builder.accept(insulatingMountJS);
        MountData mountData = insulatingMountJS.build();
        if (!mountData.areRequiredModsLoaded()) return;

        if (insulatingMountJS.entities.isEmpty())
        {   insulatingMountJS.entities.add(null);
        }
        for (EntityType<?> item : insulatingMountJS.entities)
        {   ConfigSettings.INSULATED_MOUNTS.get().put(item, mountData);
        }
    }

    /*
     Spawn Biomes
     */

    public void addSpawnBiomes(Consumer<SpawnBiomeBuilderJS> builder)
    {
        SpawnBiomeBuilderJS spawnBiomeJS = new SpawnBiomeBuilderJS();
        builder.accept(spawnBiomeJS);
        SpawnBiomeData spawnBiomeData = spawnBiomeJS.build();
        if (!spawnBiomeData.areRequiredModsLoaded()) return;

        if (spawnBiomeJS.biomes.isEmpty())
        {   spawnBiomeJS.biomes.add(null);
        }
        for (Holder<Biome> biome : spawnBiomeJS.biomes)
        {   ConfigSettings.ENTITY_SPAWN_BIOMES.get().put(biome, spawnBiomeData);
        }
    }

    /*
     TempModifier
     */

    public void addTempModifier(String id, Function<TempModifierDataJS, Function<Double, Double>> constructor)
    {
        ResourceLocation key = new ResourceLocation(id);
        if (key.getNamespace().equals("minecraft"))
        {   ColdSweat.LOGGER.error("KubeJS: Non-Minecraft namespace required for TempModifier IDs (i.e. mymod:my_modifier)");
            return;
        }
        class TempModifierJS extends TempModifier
        {
            public TempModifierJS()
            {}

            @Override
            protected Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait)
            {
                return constructor.apply(new TempModifierDataJS(entity, trait));
            }
        }

        TempModifierRegistry.register(key, TempModifierJS::new);
    }
}
