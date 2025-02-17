package com.momosoftworks.coldsweat;

import com.momosoftworks.coldsweat.client.renderer.entity.ChameleonEntityRenderer;
import com.momosoftworks.coldsweat.client.renderer.entity.GoatEntityRenderer;
import com.momosoftworks.coldsweat.common.capability.*;
import com.momosoftworks.coldsweat.common.capability.insulation.IInsulatableCap;
import com.momosoftworks.coldsweat.common.capability.insulation.ItemInsulationCap;
import com.momosoftworks.coldsweat.common.capability.shearing.IShearableCap;
import com.momosoftworks.coldsweat.common.capability.shearing.ShearableFurCap;
import com.momosoftworks.coldsweat.common.capability.temperature.EntityTempCap;
import com.momosoftworks.coldsweat.common.capability.temperature.ITemperatureCap;
import com.momosoftworks.coldsweat.common.command.argument.TempAttributeTraitArgument;
import com.momosoftworks.coldsweat.common.command.argument.TempModifierTraitArgument;
import com.momosoftworks.coldsweat.common.command.argument.TemperatureTraitArgument;
import com.momosoftworks.coldsweat.config.*;
import com.momosoftworks.coldsweat.config.spec.*;
import com.momosoftworks.coldsweat.core.advancement.trigger.ModAdvancementTriggers;
import com.momosoftworks.coldsweat.core.init.*;
import com.momosoftworks.coldsweat.core.itemgroup.InsulationItemsGroup;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.registries.ModBlocks;
import com.momosoftworks.coldsweat.util.registries.ModEntities;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.command.arguments.ArgumentTypes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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
        bus.addListener(this::clientSetup);
        bus.addListener(this::registerCaps);
        bus.addListener(this::updateConfigs);
        if (CompatManager.isCuriosLoaded()) bus.addListener(this::registerCurioSlots);

        // Register stuff
        BlockInit.BLOCKS.register(bus);
        ItemInit.ITEMS.register(bus);
        EntityInit.ENTITY_TYPES.register(bus);
        BlockEntityInit.BLOCK_ENTITY_TYPES.register(bus);
        ContainerInit.MENU_TYPES.register(bus);
        EffectInit.EFFECTS.register(bus);
        ParticleTypesInit.PARTICLES.register(bus);
        PotionInit.POTIONS.register(bus);
        SoundInit.SOUNDS.register(bus);
        FeatureInit.FEATURES.register(bus);
        AttributeInit.ATTRIBUTES.register(bus);
        ActivityInit.ACTIVITIES.register(bus);
        SensorTypeInit.SENSORS.register(bus);
        MemoryInit.MEMORIES.register(bus);

        // Setup configs
        WorldSettingsConfig.setup();
        ItemSettingsConfig.setup();
        MainSettingsConfig.setup();
        ClientSettingsConfig.setup();
        EntitySettingsConfig.setup();

        // Setup compat
        CompatManager.registerEventHandlers();
    }

    public void commonSetup(final FMLCommonSetupEvent event)
    {
        // Setup packets
        ColdSweatPacketHandler.init();
        event.enqueueWork(() ->
        {
            // Register advancement triggers
            CriteriaTriggers.register(ModAdvancementTriggers.TEMPERATURE_CHANGED);
            CriteriaTriggers.register(ModAdvancementTriggers.SOUL_LAMP_FUELLED);
            CriteriaTriggers.register(ModAdvancementTriggers.BLOCK_AFFECTS_TEMP);
            CriteriaTriggers.register(ModAdvancementTriggers.ARMOR_INSULATED);

            // Register insulation items tab
            InsulationItemsGroup.INSULATION_ITEMS.register();

            // Register custom command arguments
            ArgumentTypes.register("temperature", TemperatureTraitArgument.class, new TemperatureTraitArgument.Serializer());
            ArgumentTypes.register("temp_attribute", TempAttributeTraitArgument.class, new TempAttributeTraitArgument.Serializer());
            ArgumentTypes.register("temp_modifier", TempModifierTraitArgument.class, new TempModifierTraitArgument.Serializer());
        });
    }

    public void registerCaps(FMLCommonSetupEvent event)
    {
        /* Entity temperature */
        CapabilityManager.INSTANCE.register(ITemperatureCap.class, new DummyCapStorage<>(), EntityTempCap::new);

        /* Goat fur */
        CapabilityManager.INSTANCE.register(IShearableCap.class, new DummyCapStorage<>(), ShearableFurCap::new);

        /* Armor insulation */
        CapabilityManager.INSTANCE.register(IInsulatableCap.class, new DummyCapStorage<>(), ItemInsulationCap::new);
    }

    public void clientSetup(final FMLClientSetupEvent event)
    {
        RenderTypeLookup.setRenderLayer(ModBlocks.SOUL_STALK, RenderType.cutoutMipped());
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.CHAMELEON, ChameleonEntityRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.GOAT, GoatEntityRenderer::new);
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
