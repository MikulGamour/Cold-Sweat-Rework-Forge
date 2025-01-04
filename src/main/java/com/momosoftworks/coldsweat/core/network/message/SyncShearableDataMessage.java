package com.momosoftworks.coldsweat.core.network.message;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.common.capability.handler.ShearableFurManager;
import com.momosoftworks.coldsweat.common.capability.shearing.IShearableCap;
import com.momosoftworks.coldsweat.util.ClientOnlyHelper;
import net.minecraft.nbt.CompoundTag;
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

    private final int entityId;
    private final CompoundTag nbt;

    public SyncShearableDataMessage(int entityId, CompoundTag nbt)
    {   this.entityId = entityId;
        this.nbt = nbt;
    }

    public static void encode(SyncShearableDataMessage msg, FriendlyByteBuf buffer)
    {   buffer.writeInt(msg.entityId);
        buffer.writeNbt(msg.nbt);
    }

    public static SyncShearableDataMessage decode(FriendlyByteBuf buffer)
    {   return new SyncShearableDataMessage(buffer.readInt(), buffer.readNbt());
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
                    ShearableFurManager.getFurCap(living).ifPresent(cap -> cap.deserializeNBT(message.nbt));
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {   return TYPE;
    }
}