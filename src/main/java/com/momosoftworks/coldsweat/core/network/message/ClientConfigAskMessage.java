package com.momosoftworks.coldsweat.core.network.message;

import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

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

    public static void encode(ClientConfigAskMessage message, FriendlyByteBuf buffer)
    {   buffer.writeOptional(Optional.ofNullable(message.openerUUID), FriendlyByteBuf::writeUUID);
    }

    public static ClientConfigAskMessage decode(FriendlyByteBuf buffer)
    {   return new ClientConfigAskMessage(buffer.readOptional(FriendlyByteBuf::readUUID).orElse(null));
    }

    public static void handle(ClientConfigAskMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            if (context.getDirection().getReceptionSide().isServer()
            && context.getSender() != null)
            {
                ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(context::getSender), new SyncConfigSettingsMessage(message.openerUUID, context.getSender().level.registryAccess()));
            }
        });
        context.setPacketHandled(true);
    }
}