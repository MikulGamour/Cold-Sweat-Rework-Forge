package com.momosoftworks.coldsweat.core.network.message;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.common.capability.handler.ShearableFurManager;
import com.momosoftworks.coldsweat.common.capability.shearing.IShearableCap;
import com.momosoftworks.coldsweat.util.ClientOnlyHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SyncShearableDataMessage implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<SyncShearableDataMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "sync_shearable_data"));
    public static final StreamCodec<FriendlyByteBuf, SyncShearableDataMessage> CODEC = CustomPacketPayload.codec(SyncShearableDataMessage::encode, SyncShearableDataMessage::decode);

    private final boolean isSheared;
    private final int lastSheared;
    private final int entityId;

    public SyncShearableDataMessage(boolean isSheared, int lastSheared, int entityId)
    {   this.isSheared = isSheared;
        this.lastSheared = lastSheared;
        this.entityId = entityId;
    }

    public static void encode(SyncShearableDataMessage msg, FriendlyByteBuf buffer)
    {   buffer.writeBoolean(msg.isSheared);
        buffer.writeInt(msg.lastSheared);
        buffer.writeInt(msg.entityId);
    }

    public static SyncShearableDataMessage decode(FriendlyByteBuf buffer)
    {   return new SyncShearableDataMessage(buffer.readBoolean(), buffer.readInt(), buffer.readInt());
    }

    public static void handle(SyncShearableDataMessage message, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            Level level = ClientOnlyHelper.getClientLevel();
            if (level != null)
            {   Entity entity = level.getEntity(message.entityId);
                if (entity instanceof LivingEntity living)
                {
                    IShearableCap cap = ShearableFurManager.getFurCap(living);
                    cap.setSheared(message.isSheared);
                    cap.setLastSheared(message.lastSheared);
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {   return TYPE;
    }
}