package com.momosoftworks.coldsweat;

import com.momosoftworks.coldsweat.common.blockentity.HearthBlockEntity;
import com.momosoftworks.coldsweat.common.capability.ModCapabilities;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.mojang.serialization.Codec;
import com.momosoftworks.coldsweat.common.capability.shearing.ShearableFurCap;
import com.momosoftworks.coldsweat.common.capability.temperature.EntityTempCap;
import com.momosoftworks.coldsweat.common.capability.temperature.PlayerTempCap;
import com.momosoftworks.coldsweat.common.entity.Chameleon;
import com.momosoftworks.coldsweat.config.ConfigUpdater;
import com.momosoftworks.coldsweat.config.spec.*;
import com.momosoftworks.coldsweat.core.init.*;
import com.momosoftworks.coldsweat.data.ModRegistries;
import com.momosoftworks.coldsweat.compat.CompatManager;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ColdSweat.MOD_ID)
public class ColdSweat
{
    public static final Logger LOGGER = LogManager.getLogger("Cold Sweat");

    public static final String MOD_ID = "cold_sweat";

    public ColdSweat(IEventBus bus, ModContainer modContainer)
    {
        //NeoForge.EVENT_BUS.register(this);

        bus.addListener(this::commonSetup);
        bus.addListener(this::spawnPlacements);
        bus.addListener(this::registerCaps);
        bus.addListener(this::updateConfigs);

        // Register stuff
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITY_TYPES.register(bus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(bus);
        ModMenus.MENU_TYPES.register(bus);
        ModEffects.EFFECTS.register(bus);
        ModParticleTypes.PARTICLES.register(bus);
        ModPotions.POTIONS.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModFeatures.FEATURES.register(bus);
        ModBiomeModifiers.BIOME_MODIFIER_SERIALIZERS.register(bus);
        ModCreativeTabs.ITEM_GROUPS.register(bus);
        ModAttributes.ATTRIBUTES.register(bus);
        ModCommands.ARGUMENTS.register(bus);
        ModArmorMaterials.ARMOR_MATERIALS.register(bus);
        ModAdvancementTriggers.TRIGGERS.register(bus);
        ModItemComponents.DATA_COMPONENTS.register(bus);

        ConfigUpdater.updateFileNames();

        // Setup configs
        WorldSettingsConfig.setup(modContainer);
        ItemSettingsConfig.setup(modContainer);
        MainSettingsConfig.setup(modContainer);
        ClientSettingsConfig.setup(modContainer);
        EntitySettingsConfig.setup(modContainer);

        // Setup compat
        CompatManager.registerEventHandlers();

        // Setup JSON data-driven handlers
        bus.addListener((DataPackRegistryEvent.NewRegistry event) ->
        {
            for (ModRegistries.RegistryHolder<?> holder : ModRegistries.getRegistries().values())
            {   event.dataPackRegistry((ResourceKey) holder.registry(), (Codec) holder.codec());
            }
        });
    }

    public static String getVersion()
    {   return FMLLoader.getLoadingModList().getModFileById(ColdSweat.MOD_ID).versionString();
    }

    public void commonSetup(final FMLCommonSetupEvent event)
    {
        // Load configs to memory
        //ConfigSettings.load(null);
    }

    public void spawnPlacements(RegisterSpawnPlacementsEvent event)
    {
        event.register(ModEntities.CHAMELEON.value(), SpawnPlacementTypes.ON_GROUND,
                       Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Chameleon::canSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    public void registerCaps(RegisterCapabilitiesEvent event)
    {
        // Register temperature for temperature-enabled entities
        for (EntityType<?> type : EntityTempManager.getEntitiesWithTemperature())
        {
            if (type == EntityType.PLAYER)
            {
                event.registerEntity(ModCapabilities.PLAYER_TEMPERATURE, type, (entity, context) ->
                {   return new PlayerTempCap();
                });
            }
            else
            {   event.registerEntity(ModCapabilities.ENTITY_TEMPERATURE, type, (entity, context) ->
                {   return new EntityTempCap();
                });
            }
        }

        // Register shearable fur for goats
        event.registerEntity(ModCapabilities.SHEARABLE_FUR, EntityType.GOAT, (entity, context) ->
        {   return new ShearableFurCap();
        });

        // Register fluid handlers for hearth-like blocks
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.HEARTH.value(), (hearth, facing) ->
        {
            return facing == Direction.DOWN
                           ? new HearthBlockEntity.BottomFluidHandler(hearth)
                 : facing != Direction.UP
                           ? new HearthBlockEntity.SidesFluidHandler(hearth)
                 : null;
        });
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.BOILER.value(), (boiler, facing) -> new HearthBlockEntity.BottomFluidHandler(boiler));
        //event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.ICEBOX.value(), (icebox, facing) -> new HearthBlockEntity.SidesFluidHandler(icebox));
    }

    public void updateConfigs(FMLLoadCompleteEvent event)
    {   ConfigUpdater.updateConfigs();
    }
}
