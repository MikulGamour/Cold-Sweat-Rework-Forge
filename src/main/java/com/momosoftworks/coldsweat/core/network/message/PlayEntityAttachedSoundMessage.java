package com.momosoftworks.coldsweat.core.network.message;

import com.momosoftworks.coldsweat.util.ClientOnlyHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class PlayEntityAttachedSoundMessage
{
    String sound;
    int soundChars;
    SoundSource source;
    float volume;
    float pitch;
    int entityID;

    public PlayEntityAttachedSoundMessage(SoundEvent sound, SoundSource source, float volume, float pitch, int entityID)
    {   this(ForgeRegistries.SOUND_EVENTS.getKey(sound).toString(), source, volume, pitch, entityID);
    }

    PlayEntityAttachedSoundMessage(String sound, SoundSource source, float volume, float pitch, int entityID)
    {
        this.sound = sound;
        this.source = source;
        soundChars = sound.length();
        this.volume = volume;
        this.pitch = pitch;
        this.entityID = entityID;
    }

    public static void encode(PlayEntityAttachedSoundMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.soundChars);
        buffer.writeCharSequence(message.sound, StandardCharsets.UTF_8);
        buffer.writeEnum(message.source);
        buffer.writeFloat(message.volume);
        buffer.writeFloat(message.pitch);
        buffer.writeInt(message.entityID);
    }

    public static PlayEntityAttachedSoundMessage decode(FriendlyByteBuf buffer)
    {
        int soundChars = buffer.readInt();
        return new PlayEntityAttachedSoundMessage(buffer.readCharSequence(soundChars, StandardCharsets.UTF_8).toString(), buffer.readEnum(SoundSource.class), buffer.readFloat(), buffer.readFloat(), buffer.readInt());
    }

    public static void handle(PlayEntityAttachedSoundMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            if (context.getDirection().getReceptionSide().isClient())
            {
                SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(message.sound));
                Entity entity = Minecraft.getInstance().level.getEntity(message.entityID);

                if (entity != null && sound != null)
                {
                    ClientOnlyHelper.playEntitySound(sound, message.source, message.volume, message.pitch, entity);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
