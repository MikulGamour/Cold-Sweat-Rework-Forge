package com.momosoftworks.coldsweat;

import com.momosoftworks.coldsweat.common.capability.insulation.ItemInsulationCap;
import com.momosoftworks.coldsweat.common.capability.shearing.ShearableFurCap;
import com.momosoftworks.coldsweat.common.capability.temperature.EntityTempCap;
import com.momosoftworks.coldsweat.common.capability.temperature.PlayerTempCap;
import com.momosoftworks.coldsweat.common.entity.Chameleon;
import com.momosoftworks.coldsweat.config.*;
import com.momosoftworks.coldsweat.config.spec.*;
import com.momosoftworks.coldsweat.core.advancement.trigger.ModAdvancementTriggers;
import com.momosoftworks.coldsweat.core.init.*;
import com.momosoftworks.coldsweat.core.itemgroup.InsulationItemsGroup;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.data.ModRegistries;
import com.momosoftworks.coldsweat.data.codec.configuration.*;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.registries.ModEntities;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.SlotTypePreset;

@Mod(ColdSweat.MOD_ID)
public class ColdSweat
{
    public static final Logger LOGGER = LogManager.getLogger("Cold Sweat");

    public static final String MOD_ID = "cold_sweat";

    public ColdSweat()
    {
        MinecraftForge.EVENT_BUS.register(this);
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        bus.addListener(this::commonSetup);
        bus.addListener(this::spawnPlacements);
        bus.addListener(this::registerCaps);
        bus.addListener(this::updateConfigs);
        if (CompatManager.isCuriosLoaded()) bus.addListener(this::registerCurioSlots);

        // Register stuff
        BlockInit.BLOCKS.register(bus);
        ItemInit.ITEMS.register(bus);
        EntityInit.ENTITY_TYPES.register(bus);
        BlockEntityInit.BLOCK_ENTITY_TYPES.register(bus);
        MenuInit.MENU_TYPES.register(bus);
        EffectInit.EFFECTS.register(bus);
        ParticleTypesInit.PARTICLES.register(bus);
        PotionInit.POTIONS.register(bus);
        SoundInit.SOUNDS.register(bus);
        FeatureInit.FEATURES.register(bus);
        BiomeCodecInit.BIOME_MODIFIER_SERIALIZERS.register(bus);
        AttributeInit.ATTRIBUTES.register(bus);
        CommandInit.ARGUMENTS.register(bus);

        ConfigUpdater.updateFileNames();

        // Setup configs
        WorldSettingsConfig.setup();
        ItemSettingsConfig.setup();
        MainSettingsConfig.setup();
        ClientSettingsConfig.setup();
        EntitySettingsConfig.setup();

        // Setup compat
        CompatManager.registerEventHandlers();

        // Setup JSON data-driven handlers
        bus.addListener((NewRegistryEvent event) ->
        {
            event.create(new RegistryBuilder<InsulatorData>().setName(ModRegistries.INSULATOR_DATA.location()).dataPackRegistry(InsulatorData.CODEC));
            event.create(new RegistryBuilder<FuelData>().setName(ModRegistries.FUEL_DATA.location()).dataPackRegistry(FuelData.CODEC));
            event.create(new RegistryBuilder<FoodData>().setName(ModRegistries.FOOD_DATA.location()).dataPackRegistry(FoodData.CODEC));
            event.create(new RegistryBuilder<BlockTempData>().setName(ModRegistries.BLOCK_TEMP_DATA.location()).dataPackRegistry(BlockTempData.CODEC));
            event.create(new RegistryBuilder<BiomeTempData>().setName(ModRegistries.BIOME_TEMP_DATA.location()).dataPackRegistry(BiomeTempData.CODEC));
            event.create(new RegistryBuilder<DimensionTempData>().setName(ModRegistries.DIMENSION_TEMP_DATA.location()).dataPackRegistry(DimensionTempData.CODEC));
            event.create(new RegistryBuilder<StructureTempData>().setName(ModRegistries.STRUCTURE_TEMP_DATA.location()).dataPackRegistry(StructureTempData.CODEC));
            event.create(new RegistryBuilder<MountData>().setName(ModRegistries.MOUNT_DATA.location()).dataPackRegistry(MountData.CODEC));
            event.create(new RegistryBuilder<SpawnBiomeData>().setName(ModRegistries.ENTITY_SPAWN_BIOME_DATA.location()).dataPackRegistry(SpawnBiomeData.CODEC));
            event.create(new RegistryBuilder<DepthTempData>().setName(ModRegistries.DEPTH_TEMP_DATA.location()).dataPackRegistry(DepthTempData.CODEC));
            event.create(new RegistryBuilder<ItemCarryTempData>().setName(ModRegistries.CARRY_TEMP_DATA.location()).dataPackRegistry(ItemCarryTempData.CODEC));
            event.create(new RegistryBuilder<EntityTempData>().setName(ModRegistries.ENTITY_TEMP_DATA.location()).dataPackRegistry(EntityTempData.CODEC));
            event.create(new RegistryBuilder<RemoveRegistryData<?>>().setName(ModRegistries.REMOVE_REGISTRY_DATA.location()).dataPackRegistry(RemoveRegistryData.CODEC));
        });
    }

    public void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(() ->
        {
            // Setup packets
            ColdSweatPacketHandler.init();

            // Register advancement triggers
            CriteriaTriggers.register(ModAdvancementTriggers.TEMPERATURE_CHANGED);
            CriteriaTriggers.register(ModAdvancementTriggers.SOUL_LAMP_FUELLED);
            CriteriaTriggers.register(ModAdvancementTriggers.BLOCK_AFFECTS_TEMP);
            CriteriaTriggers.register(ModAdvancementTriggers.ARMOR_INSULATED);

            // Register insulation items tab
            InsulationItemsGroup.INSULATION_ITEMS.register();
        });
    }

    public void spawnPlacements(SpawnPlacementRegisterEvent event)
    {
        event.register(ModEntities.CHAMELEON, SpawnPlacements.Type.ON_GROUND,
                       Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Chameleon::canSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    public void registerCaps(RegisterCapabilitiesEvent event)
    {   event.register(PlayerTempCap.class);
        event.register(EntityTempCap.class);
        event.register(ItemInsulationCap.class);
        event.register(ShearableFurCap.class);
    }

    public void updateConfigs(FMLLoadCompleteEvent event)
    {   ConfigUpdater.updateConfigs();
    }

    public void registerCurioSlots(InterModEnqueueEvent event)
    {
        event.enqueueWork(() ->
        {   InterModComms.sendTo(ColdSweat.MOD_ID, CuriosApi.MODID, SlotTypeMessage.REGISTER_TYPE,
                                 () -> SlotTypePreset.CHARM.getMessageBuilder().build());
        });
    }
}
