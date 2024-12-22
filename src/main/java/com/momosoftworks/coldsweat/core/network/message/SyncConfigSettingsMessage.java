package com.momosoftworks.coldsweat.core.network.message;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.config.spec.EntitySettingsConfig;
import com.momosoftworks.coldsweat.config.spec.ItemSettingsConfig;
import com.momosoftworks.coldsweat.config.spec.MainSettingsConfig;
import com.momosoftworks.coldsweat.config.spec.WorldSettingsConfig;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.util.ClientOnlyHelper;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncConfigSettingsMessage
{
    CompoundTag configValues;
    UUID menuOpener;

    public SyncConfigSettingsMessage(RegistryAccess registryAccess)
    {   this(null, registryAccess);
    }

    public SyncConfigSettingsMessage(UUID menuOpener, RegistryAccess registryAccess)
    {   this(ConfigSettings.encode(registryAccess), menuOpener);
    }

    private SyncConfigSettingsMessage(CompoundTag values, UUID menuOpener)
    {
        this.configValues = values;
        this.menuOpener = menuOpener;
    }

    public static void encode(SyncConfigSettingsMessage message, FriendlyByteBuf buffer)
    {
        buffer.writeNbt(message.configValues);
        buffer.writeOptional(Optional.ofNullable(message.menuOpener), FriendlyByteBuf::writeUUID);
    }

    public static SyncConfigSettingsMessage decode(FriendlyByteBuf buffer)
    {
        return new SyncConfigSettingsMessage(buffer.readNbt(), buffer.readOptional(FriendlyByteBuf::readUUID).orElse(null));
    }

    public static void handle(SyncConfigSettingsMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();

            if (context.getDirection().getReceptionSide().isServer())
            {
                if (context.getSender() != null && context.getSender().hasPermissions(2))
                {
                    ConfigSettings.decode(message.configValues, registryAccess);
                    ConfigSettings.saveValues(registryAccess);
                    MainSettingsConfig.save();
                    WorldSettingsConfig.save();
                    ItemSettingsConfig.save();
                    EntitySettingsConfig.save();
                    ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new SyncConfigSettingsMessage(null, registryAccess));
                }
            }
            else if (context.getDirection().getReceptionSide().isClient())
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
        context.setPacketHandled(true);
    }
}
