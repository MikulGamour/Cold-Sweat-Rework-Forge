package com.momosoftworks.coldsweat.core.network.message;

import com.momosoftworks.coldsweat.common.capability.handler.ShearableFurManager;
import com.momosoftworks.coldsweat.util.ClientOnlyHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncShearableDataMessage
{
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

    public static void handle(SyncShearableDataMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient())
        {
            context.enqueueWork(() ->
            {
                try
                {
                    Level level = ClientOnlyHelper.getClientLevel();
                    if (level != null)
                    {
                        Entity entity = level.getEntity(message.entityId);
                        if (entity instanceof LivingEntity living)
                        {
                            ShearableFurManager.getFurCap(living).ifPresent(cap ->
                            {   cap.deserializeNBT(message.nbt);
                            });
                        }
                    }
                } catch (Exception ignored) {}
            });
        }
        context.setPacketHandled(true);
    }
}
