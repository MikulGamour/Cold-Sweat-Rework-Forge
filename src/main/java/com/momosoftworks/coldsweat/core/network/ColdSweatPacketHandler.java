package com.momosoftworks.coldsweat.core.network;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.core.network.message.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ColdSweatPacketHandler
{
    private static final String PROTOCOL_VERSION = "0.1.2";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ColdSweat.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void init()
    {
        INSTANCE.registerMessage(0, SyncTemperatureMessage.class, SyncTemperatureMessage::encode, SyncTemperatureMessage::decode, SyncTemperatureMessage::handle);
        INSTANCE.registerMessage(1, SyncTempModifiersMessage.class, SyncTempModifiersMessage::encode, SyncTempModifiersMessage::decode, SyncTempModifiersMessage::handle);
        INSTANCE.registerMessage(2, SyncConfigSettingsMessage.class, SyncConfigSettingsMessage::encode, SyncConfigSettingsMessage::decode, SyncConfigSettingsMessage::handle);
        INSTANCE.registerMessage(3, ClientConfigAskMessage.class, ClientConfigAskMessage::encode, ClientConfigAskMessage::decode, ClientConfigAskMessage::handle);
        INSTANCE.registerMessage(4, PlayEntityAttachedSoundMessage.class, PlayEntityAttachedSoundMessage::encode, PlayEntityAttachedSoundMessage::decode, PlayEntityAttachedSoundMessage::handle);
        INSTANCE.registerMessage(5, BlockDataUpdateMessage.class, BlockDataUpdateMessage::encode, BlockDataUpdateMessage::decode, BlockDataUpdateMessage::handle);
        INSTANCE.registerMessage(6, HearthResetMessage.class, HearthResetMessage::encode, HearthResetMessage::decode, HearthResetMessage::handle);
        INSTANCE.registerMessage(7, DisableHearthParticlesMessage.class, DisableHearthParticlesMessage::encode, DisableHearthParticlesMessage::decode, DisableHearthParticlesMessage::handle);
        INSTANCE.registerMessage(8, ParticleBatchMessage.class, ParticleBatchMessage::encode, ParticleBatchMessage::decode, ParticleBatchMessage::handle);
        INSTANCE.registerMessage(9, SyncShearableDataMessage.class, SyncShearableDataMessage::encode, SyncShearableDataMessage::decode, SyncShearableDataMessage::handle);
        INSTANCE.registerMessage(10, ChameleonEatMessage.class, ChameleonEatMessage::encode, ChameleonEatMessage::decode, ChameleonEatMessage::handle);
        INSTANCE.registerMessage(11, SyncForgeDataMessage.class, SyncForgeDataMessage::encode, SyncForgeDataMessage::decode, SyncForgeDataMessage::handle);
        INSTANCE.registerMessage(12, SyncPreferredUnitsMessage.class, SyncPreferredUnitsMessage::encode, SyncPreferredUnitsMessage::decode, SyncPreferredUnitsMessage::handle);
        INSTANCE.registerMessage(13, SyncContainerSlotMessage.class, SyncContainerSlotMessage::encode, SyncContainerSlotMessage::decode, SyncContainerSlotMessage::handle);
        INSTANCE.registerMessage(14, EntityMountMessage.class, EntityMountMessage::encode, EntityMountMessage::decode, EntityMountMessage::handle);
        INSTANCE.registerMessage(15, SyncItemPredicatesMessage.class, SyncItemPredicatesMessage::encode, SyncItemPredicatesMessage::decode, SyncItemPredicatesMessage::handle);
    }
}
