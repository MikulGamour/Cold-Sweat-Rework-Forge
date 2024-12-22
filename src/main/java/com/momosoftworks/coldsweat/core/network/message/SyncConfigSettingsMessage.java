package com.momosoftworks.coldsweat.core.network.message;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.config.spec.EntitySettingsConfig;
import com.momosoftworks.coldsweat.config.spec.ItemSettingsConfig;
import com.momosoftworks.coldsweat.config.spec.MainSettingsConfig;
import com.momosoftworks.coldsweat.config.spec.WorldSettingsConfig;
import com.momosoftworks.coldsweat.util.ClientOnlyHelper;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;
import java.util.UUID;

public class SyncConfigSettingsMessage implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<SyncConfigSettingsMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "sync_config_settings"));
    public static final StreamCodec<FriendlyByteBuf, SyncConfigSettingsMessage> CODEC = CustomPacketPayload.codec(SyncConfigSettingsMessage::encode, SyncConfigSettingsMessage::decode);

    CompoundTag configValues;
    UUID menuOpener;

    public SyncConfigSettingsMessage(RegistryAccess registryAccess)
    {   this(null, registryAccess);
    }

    public SyncConfigSettingsMessage(UUID menuOpener, RegistryAccess registryAccess)
    {   this(ConfigSettings.encode(registryAccess), menuOpener);
    }

    private SyncConfigSettingsMessage(CompoundTag values, UUID menuOpener)
    {   this.configValues = values;
        this.menuOpener = menuOpener;
    }

    public static void encode(SyncConfigSettingsMessage message, FriendlyByteBuf buffer)
    {
        buffer.writeNbt(message.configValues);
        buffer.writeOptional(Optional.ofNullable(message.menuOpener), (buf, id) -> buf.writeUUID(id));
    }

    public static SyncConfigSettingsMessage decode(FriendlyByteBuf buffer)
    {
        return new SyncConfigSettingsMessage(buffer.readNbt(), buffer.readOptional(buf -> buf.readUUID()).orElse(null));
    }

    public static void handle(SyncConfigSettingsMessage message, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();

            if (context.flow().isServerbound())
            {
                if (context.player().hasPermissions(2))
                {
                    ConfigSettings.decode(message.configValues, registryAccess);
                    ConfigSettings.saveValues(registryAccess);
                    MainSettingsConfig.save();
                    WorldSettingsConfig.save();
                    ItemSettingsConfig.save();
                    EntitySettingsConfig.save();
                    PacketDistributor.sendToAllPlayers(new SyncConfigSettingsMessage(null, registryAccess));
                }
            }
            else
            {
                try
                {   ConfigSettings.decode(message.configValues, registryAccess);
                }
                catch (Exception e)
                {   ColdSweat.LOGGER.error("Failed to decode config settings from server: ", e);
                }
                if (message.menuOpener != null && message.menuOpener.equals(ClientOnlyHelper.getClientPlayer().getUUID()))
                {   ClientOnlyHelper.openConfigScreen();
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {   return TYPE;
    }
}