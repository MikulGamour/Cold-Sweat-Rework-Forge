package com.momosoftworks.coldsweat.core.init;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.event.core.registry.BlockTempRegisterEvent;
import com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent;
import com.momosoftworks.coldsweat.api.registry.BlockTempRegistry;
import com.momosoftworks.coldsweat.api.registry.TempModifierRegistry;
import com.momosoftworks.coldsweat.api.temperature.block_temp.*;
import com.momosoftworks.coldsweat.api.temperature.modifier.*;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.config.ConfigLoadingHandler;
import com.momosoftworks.coldsweat.config.spec.WorldSettingsConfig;
import com.momosoftworks.coldsweat.data.ModRegistries;
import com.momosoftworks.coldsweat.data.codec.configuration.BlockTempData;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@EventBusSubscriber
public class TempModifierInit
{
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void fireRegisterModifiers(ServerAboutToStartEvent event)
    {   buildModifierRegistries();
    }

    // Trigger registry events
    public static void buildModifierRegistries()
    {
        TempModifierRegistry.flush();

        try { NeoForge.EVENT_BUS.post(new TempModifierRegisterEvent()); }
        catch (Exception e)
        {
            ColdSweat.LOGGER.error("Registering TempModifiers failed!");
            throw e;
        }
    }

    public static void buildBlockRegistries()
    {
        try { NeoForge.EVENT_BUS.post(new BlockTempRegisterEvent()); }
        catch (Exception e)
        {
            ColdSweat.LOGGER.error("Registering BlockTemps failed!");
            throw e;
        }
    }

    public static void buildBlockConfigs()
    {
        // Auto-generate BlockTemps from config
        List<BlockTempData> blockTemps = WorldSettingsConfig.BLOCK_TEMPERATURES.get().stream()
                                         .map(BlockTempData::fromToml)
                                         .filter(Objects::nonNull).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        // Handle entries removed by configs
        ConfigLoadingHandler.removeEntries(blockTemps, ModRegistries.BLOCK_TEMP_DATA);

        for (BlockTempData blockConfig : blockTemps)
        {
            Block[] blocks = RegistryHelper.mapBuiltinRegistryTagList(BuiltInRegistries.BLOCK, blockConfig.blocks()).toArray(new Block[0]);

            BlockTemp blockTemp = new BlockTempConfig(-blockConfig.getMaxEffect(), blockConfig.getMaxEffect(),
                                                      blockConfig.getMinTemp(), blockConfig.getMaxTemp(),
                                                      blockConfig.range(), true, blockConfig.conditions(), blocks)
            {
                @Override
                public double getTemperature(Level level, LivingEntity entity, BlockState state, BlockPos pos, double distance)
                {   return blockConfig.getTemperature();
                }
            };

            BlockTempRegistry.register(blockTemp);
        }
    }

    // Register BlockTemps
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void registerBlockTemps(BlockTempRegisterEvent event)
    {
        long startMS = System.currentTimeMillis();

        event.register(new LavaBlockTemp());
        event.register(new FurnaceBlockTemp());
        event.register(new NetherPortalBlockTemp());
        event.registerFirst(new SoulFireBlockTemp());
        ColdSweat.LOGGER.debug("Registered BlockTemps in {}ms", System.currentTimeMillis() - startMS);
    }

    // Register TempModifiers
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void registerTempModifiers(TempModifierRegisterEvent event)
    {
        long startMS = System.currentTimeMillis();
        String compatPath = "com.momosoftworks.coldsweat.api.temperature.modifier.compat.";
        String sereneSeasons = compatPath + "SereneSeasonsTempModifier";
        String armorUnder = compatPath + "ArmorUnderTempModifier";
        String weatherStorms = compatPath + "StormTempModifier";
        String curios = compatPath + "CuriosTempModifier";

        event.register(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "blocks"), BlockTempModifier::new);
        event.register(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "biomes"), BiomeTempModifier::new);
        event.register(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "elevation"), ElevationTempModifier::new);
        event.register(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "depth_biomes"), DepthBiomeTempModifier::new);
        event.register(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "armor"), ArmorInsulationTempModifier::new);
        event.register(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "mount"), MountTempModifier::new);
        event.register(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "waterskin"), WaterskinTempModifier::new);
        event.register(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "soulspring_lamp"), SoulLampTempModifier::new);
        event.register(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "water"), WaterTempModifier::new);
        event.register(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "air_conditioning"), BlockInsulationTempModifier::new);
        event.register(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "food"), FoodTempModifier::new);
        event.register(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "freezing"), FreezingTempModifier::new);
        event.register(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "on_fire"), FireTempModifier::new);
        event.register(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "soul_sprout"), SoulSproutTempModifier::new);
        event.register(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "inventory_items"), InventoryItemsTempModifier::new);
        event.register(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "entities"), EntitiesTempModifier::new);

        // Compat
        if (CompatManager.isSereneSeasonsLoaded())
        {   event.registerByClassName(ResourceLocation.fromNamespaceAndPath("sereneseasons", "season"), sereneSeasons);
        }
        if (CompatManager.isArmorUnderwearLoaded())
        {   event.registerByClassName(ResourceLocation.fromNamespaceAndPath("armorunder", "lining"), armorUnder);
        }
        if (CompatManager.isWeather2Loaded())
        {   event.registerByClassName(ResourceLocation.fromNamespaceAndPath("weather2", "storm"), weatherStorms);
        }
        if (CompatManager.isCuriosLoaded())
        {   event.registerByClassName(ResourceLocation.fromNamespaceAndPath("curios", "curios"), curios);
        }

        ColdSweat.LOGGER.debug("Registered TempModifiers in {}ms", System.currentTimeMillis() - startMS);
    }
}
