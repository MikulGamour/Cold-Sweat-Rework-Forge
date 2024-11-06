package com.momosoftworks.coldsweat.core.init;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.event.core.registry.BlockTempRegisterEvent;
import com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent;
import com.momosoftworks.coldsweat.api.registry.BlockTempRegistry;
import com.momosoftworks.coldsweat.api.registry.TempModifierRegistry;
import com.momosoftworks.coldsweat.api.temperature.block_temp.*;
import com.momosoftworks.coldsweat.api.temperature.modifier.*;
import com.momosoftworks.coldsweat.config.spec.WorldSettingsConfig;
import com.momosoftworks.coldsweat.data.codec.requirement.NbtRequirement;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.function.Predicate;

@Mod.EventBusSubscriber
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

        try { MinecraftForge.EVENT_BUS.post(new TempModifierRegisterEvent()); }
        catch (Exception e)
        {
            ColdSweat.LOGGER.error("Registering TempModifiers failed!");
            throw e;
        }
    }

    public static void buildBlockRegistries()
    {
        try { MinecraftForge.EVENT_BUS.post(new BlockTempRegisterEvent()); }
        catch (Exception e)
        {
            ColdSweat.LOGGER.error("Registering BlockTemps failed!");
            throw e;
        }
    }

    public static void buildBlockConfigs()
    {
        // Auto-generate BlockTemps from config
        for (List<?> effectBuilder : WorldSettingsConfig.getInstance().getBlockTemps())
        {
            if (effectBuilder.size() < 3)
            {   ColdSweat.LOGGER.error("Malformed configuration for block temperature: {}", effectBuilder);
                continue;
            }
            try
            {
                // Get IDs associated with this config entry
                String[] blockIDs = ((String) effectBuilder.get(0)).split(",");

                // Parse block IDs into blocks
                Block[] effectBlocks = Arrays.stream(blockIDs).map(ConfigHelper::getBlocks).flatMap(List::stream).toArray(Block[]::new);

                // Temp of block
                final double blockTemp = ((Number) effectBuilder.get(1)).doubleValue();
                // Range of effect
                final double blockRange = ((Number) effectBuilder.get(2)).doubleValue();

                // Get min/max effect
                final double maxChange = effectBuilder.size() > 3 && effectBuilder.get(3) instanceof Number
                                         ? ((Number) effectBuilder.get(3)).doubleValue()
                                         : Double.MAX_VALUE;

                // Get block predicate
                Map<String, Predicate<BlockState>> blockPredicates = effectBuilder.size() > 4 && effectBuilder.get(4) instanceof String str && !str.isEmpty()
                                                                     ? ConfigHelper.getBlockStatePredicates(effectBlocks[0], str)
                                                                     : new HashMap<>();

                NbtRequirement tag = effectBuilder.size() > 5 && effectBuilder.get(5) instanceof String str && !str.isEmpty()
                                            ? new NbtRequirement(NBTHelper.parseCompoundNbt(str))
                                            : new NbtRequirement();

                double tempLimit = effectBuilder.size() > 6
                                   ? ((Number) effectBuilder.get(6)).doubleValue()
                                   : Double.MAX_VALUE;

                double maxEffect = blockTemp > 0 ?  maxChange :  Double.MAX_VALUE;
                double minEffect = blockTemp < 0 ? -maxChange : -Double.MAX_VALUE;

                double maxTemperature = blockTemp > 0 ? tempLimit : Double.MAX_VALUE;
                double minTemperature = blockTemp < 0 ? tempLimit : -Double.MAX_VALUE;

                BlockTempRegistry.register(new BlockTempConfig(minEffect, maxEffect, minTemperature, maxTemperature, blockRange, true, blockPredicates, effectBlocks)
                {
                    @Override
                    public double getTemperature(Level level, LivingEntity entity, BlockState state, BlockPos pos, double distance)
                    {
                        // Check the list of predicates first
                        BlockEntity blockEntity = level.getBlockEntity(pos);
                        if (blockEntity != null)
                        {
                            CompoundTag blockTag = blockEntity.saveWithFullMetadata();
                            if (!tag.test(blockTag))
                            {   return 0;
                            }
                        }
                        return blockTemp;
                    }
                });
            }
            catch (Exception e)
            {   ColdSweat.LOGGER.error("Invalid configuration for BlockTemps", e);
                break;
            }
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
        event.register(new SoulFireBlockTemp());
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
        String valkyrienSkies = compatPath + "ValkShipBlockTempModifier";

        event.register(new ResourceLocation(ColdSweat.MOD_ID, "blocks"), BlockTempModifier::new);
        event.register(new ResourceLocation(ColdSweat.MOD_ID, "biomes"), BiomeTempModifier::new);
        event.register(new ResourceLocation(ColdSweat.MOD_ID, "underground"), UndergroundTempModifier::new);
        event.register(new ResourceLocation(ColdSweat.MOD_ID, "armor"), ArmorInsulationTempModifier::new);
        event.register(new ResourceLocation(ColdSweat.MOD_ID, "mount"), MountTempModifier::new);
        event.register(new ResourceLocation(ColdSweat.MOD_ID, "waterskin"), WaterskinTempModifier::new);
        event.register(new ResourceLocation(ColdSweat.MOD_ID, "soulspring_lamp"), SoulLampTempModifier::new);
        event.register(new ResourceLocation(ColdSweat.MOD_ID, "water"), WaterTempModifier::new);
        event.register(new ResourceLocation(ColdSweat.MOD_ID, "air_conditioning"), BlockInsulationTempModifier::new);
        event.register(new ResourceLocation(ColdSweat.MOD_ID, "food"), FoodTempModifier::new);
        event.register(new ResourceLocation(ColdSweat.MOD_ID, "freezing"), FreezingTempModifier::new);
        event.register(new ResourceLocation(ColdSweat.MOD_ID, "on_fire"), FireTempModifier::new);
        event.register(new ResourceLocation(ColdSweat.MOD_ID, "soul_sprout"), SoulSproutTempModifier::new);
        event.register(new ResourceLocation(ColdSweat.MOD_ID, "inventory_items"), InventoryItemsTempModifier::new);
        event.register(new ResourceLocation(ColdSweat.MOD_ID, "entities"), EntitiesTempModifier::new);

        // Compat
        if (CompatManager.isSereneSeasonsLoaded())
        {   event.registerByClassName(new ResourceLocation("sereneseasons", "season"), sereneSeasons);
        }
        if (CompatManager.isArmorUnderwearLoaded())
        {   event.registerByClassName(new ResourceLocation("armorunder", "lining"), armorUnder);
        }
        if (CompatManager.isWeather2Loaded())
        {   event.registerByClassName(new ResourceLocation("weather2", "storm"), weatherStorms);
        }
        if (CompatManager.isCuriosLoaded())
        {   event.registerByClassName(new ResourceLocation("curios", "curios"), curios);
        }
        if (CompatManager.isValkyrienSkiesLoaded())
        {   event.registerByClassName(new ResourceLocation("valkyrienskies", "ship_blocks"), valkyrienSkies);
        }

        ColdSweat.LOGGER.debug("Registered TempModifiers in {}ms", System.currentTimeMillis() - startMS);
    }
}
