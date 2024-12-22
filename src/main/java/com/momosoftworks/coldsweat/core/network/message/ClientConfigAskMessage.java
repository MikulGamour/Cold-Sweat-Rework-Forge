package com.momosoftworks.coldsweat.core.network.message;

import com.momosoftworks.coldsweat.core.network.BufferHelper;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class ClientConfigAskMessage
{
    UUID openerUUID;

    public ClientConfigAskMessage(UUID openerUUID)
    {   this.openerUUID = openerUUID;
    }

    public ClientConfigAskMessage()
    {   this(null);
    }

    public static void encode(ClientConfigAskMessage message, PacketBuffer buffer)
    {   BufferHelper.writeOptional(buffer, Optional.ofNullable(message.openerUUID), PacketBuffer::writeUUID);
    }

    public static ClientConfigAskMessage decode(PacketBuffer buffer)
    {   return new ClientConfigAskMessage(BufferHelper.readOptional(buffer, PacketBuffer::readUUID).orElse(null));
    }

    public static void handle(ClientConfigAskMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            if (context.getDirection().getReceptionSide().isServer())
            {
                ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(context::getSender), new SyncConfigSettingsMessage(message.openerUUID, context.getSender().level.registryAccess()));
            }
        });
        context.setPacketHandled(true);
    }
}