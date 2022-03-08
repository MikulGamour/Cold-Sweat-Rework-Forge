package dev.momostudios.coldsweat.core.network.message;

import dev.momostudios.coldsweat.common.capability.ModCapabilities;
import dev.momostudios.coldsweat.common.temperature.Temperature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerTempSyncMessage
{
    public double body;
    public double base;
    public double world;

    public PlayerTempSyncMessage() {
    }

    public PlayerTempSyncMessage(double body, double base, double world){
        this.body = body;
        this.base = base;
        this.world = world;
    }

    public static void encode(PlayerTempSyncMessage message, FriendlyByteBuf buffer)
    {
        buffer.writeDouble(message.body);
        buffer.writeDouble(message.base);
        buffer.writeDouble(message.world);
    }

    public static PlayerTempSyncMessage decode(FriendlyByteBuf buffer)
    {
        return new PlayerTempSyncMessage(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(PlayerTempSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> syncTemperature(message.body, message.base, message.world)));

        context.setPacketHandled(true);
    }

    public static DistExecutor.SafeRunnable syncTemperature(double body, double base, double world)
    {
        return new DistExecutor.SafeRunnable()
        {
            @Override
            public void run()
            {
                LocalPlayer player = Minecraft.getInstance().player;

                if (player != null && !player.isSpectator())
                {
                    player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(cap ->
                    {
                        cap.set(Temperature.Types.BODY, body);
                        cap.set(Temperature.Types.BASE, base);
                        cap.set(Temperature.Types.TOTAL, body + base);
                        cap.set(Temperature.Types.WORLD, world);
                    });
                }
            }
        };
    }
}
