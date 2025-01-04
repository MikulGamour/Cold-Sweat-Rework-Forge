package com.momosoftworks.coldsweat.core.network.message;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.common.event.HearthSaveDataHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class DisableHearthParticlesMessage implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<DisableHearthParticlesMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "disable_hearth_particles"));
    public static final StreamCodec<FriendlyByteBuf, DisableHearthParticlesMessage> CODEC = CustomPacketPayload.codec(DisableHearthParticlesMessage::encode, DisableHearthParticlesMessage::decode);

    CompoundTag nbt;

    public DisableHearthParticlesMessage(CompoundTag nbt)
    {   this.nbt = nbt;
    }

    public void encode(FriendlyByteBuf buffer)
    {   buffer.writeNbt(this.nbt);
    }

    public static DisableHearthParticlesMessage decode(FriendlyByteBuf buffer)
    {   return new DisableHearthParticlesMessage(buffer.readNbt());
    }

    public static void handle(DisableHearthParticlesMessage message, IPayloadContext context)
    {
        if (context.flow().isServerbound())
        {
            context.enqueueWork(() ->
            {   context.player().getPersistentData().merge(message.nbt);
            });
        }
        else
        {   HearthSaveDataHandler.deserializeDisabledHearths(message.nbt);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {   return TYPE;
    }
}