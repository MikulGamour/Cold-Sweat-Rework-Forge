package com.momosoftworks.coldsweat.core.network.message;

import com.momosoftworks.coldsweat.common.capability.handler.ShearableFurManager;
import com.momosoftworks.coldsweat.util.ClientOnlyHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncShearableDataMessage
{
    private final int entityId;
    private final CompoundNBT nbt;

    public SyncShearableDataMessage(int entityId, CompoundNBT nbt)
    {   this.entityId = entityId;
        this.nbt = nbt;
    }

    public static void encode(SyncShearableDataMessage msg, PacketBuffer buffer)
    {   buffer.writeInt(msg.entityId);
        buffer.writeNbt(msg.nbt);
    }

    public static SyncShearableDataMessage decode(PacketBuffer buffer)
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
                    World world = ClientOnlyHelper.getClientWorld();
                    if (world != null)
                    {
                        Entity entity = world.getEntity(message.entityId);
                        if (entity instanceof LivingEntity)
                        {
                            ShearableFurManager.getFurCap(((LivingEntity) entity)).ifPresent(cap ->
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
