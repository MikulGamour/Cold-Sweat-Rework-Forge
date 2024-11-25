package com.momosoftworks.coldsweat.core.network.message;

import com.momosoftworks.coldsweat.config.spec.ItemSettingsConfig;
import com.momosoftworks.coldsweat.config.spec.MainSettingsConfig;
import com.momosoftworks.coldsweat.config.spec.WorldSettingsConfig;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.util.ClientOnlyHelper;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncConfigSettingsMessage
{
    public static final UUID EMPTY_UUID = new UUID(0, 0);

    CompoundNBT configValues;
    UUID menuOpener;

    public SyncConfigSettingsMessage(DynamicRegistries registryAccess)
    {   this(EMPTY_UUID, registryAccess);
    }

    public SyncConfigSettingsMessage(UUID menuOpener, DynamicRegistries registryAccess)
    {   this(ConfigSettings.encode(registryAccess), menuOpener);
    }

    private SyncConfigSettingsMessage(CompoundNBT values, UUID menuOpener)
    {
        this.configValues = values;
        this.menuOpener = menuOpener;
    }

    public static void encode(SyncConfigSettingsMessage message, PacketBuffer buffer)
    {
        buffer.writeNbt(message.configValues);
        buffer.writeUUID(message.menuOpener);
    }

    public static SyncConfigSettingsMessage decode(PacketBuffer buffer)
    {
        return new SyncConfigSettingsMessage(buffer.readNbt(), buffer.readUUID());
    }

    public static void handle(SyncConfigSettingsMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            DynamicRegistries registryAccess = RegistryHelper.getDynamicRegistries();

            if (context.getDirection().getReceptionSide().isServer())
            {
                if (context.getSender() != null && context.getSender().hasPermissions(2))
                {
                    ConfigSettings.decode(message.configValues, registryAccess);
                    ConfigSettings.saveValues(registryAccess);
                    MainSettingsConfig.getInstance().save();
                    WorldSettingsConfig.getInstance().save();
                    ItemSettingsConfig.getInstance().save();
                }

                ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new SyncConfigSettingsMessage(EMPTY_UUID, registryAccess));
            }
            else if (context.getDirection().getReceptionSide().isClient())
            {
                ConfigSettings.decode(message.configValues, registryAccess);
                if (message.menuOpener.equals(ClientOnlyHelper.getClientPlayer().getUUID()))
                {   ClientOnlyHelper.openConfigScreen();
                }
            }
        });
        context.setPacketHandled(true);
    }
}
