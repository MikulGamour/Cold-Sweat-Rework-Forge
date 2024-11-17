package com.momosoftworks.coldsweat.util.entity;

import com.momosoftworks.coldsweat.util.ClientOnlyHelper;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import com.momosoftworks.coldsweat.util.serialization.ObjectBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameType;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class EntityHelper
{
    private EntityHelper() {}

    public static ItemStack getItemInHand(LivingEntity player, HandSide hand)
    {   return player.getItemInHand(hand == player.getMainArm() ? Hand.MAIN_HAND : Hand.OFF_HAND);
    }

    public static HandSide getArmFromHand(Hand hand, PlayerEntity player)
    {   return hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm() == HandSide.RIGHT ? HandSide.LEFT : HandSide.RIGHT;
    }

    public static boolean holdingLamp(LivingEntity player, HandSide arm)
    {   return getItemInHand(player, arm).getItem() == ModItems.SOULSPRING_LAMP;
    }

    public static Vector3d getCenterOf(Entity entity)
    {   return entity.position().add(0, entity.getBbHeight() / 2, 0);
    }

    public static HandSide getHandSide(Hand hand, PlayerEntity player)
    {   return hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm() == HandSide.RIGHT ? HandSide.LEFT : HandSide.RIGHT;
    }

    static final Method GET_VOICE_PITCH;
    static
    {   GET_VOICE_PITCH = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "func_70647_i");
        GET_VOICE_PITCH.setAccessible(true);
    }
    public static float getVoicePitch(LivingEntity entity)
    {   try
        {   return (float) GET_VOICE_PITCH.invoke(entity);
        }
        catch (Exception e)
        {   return 1f;
        }
    }

    public static GameType getGameModeForPlayer(PlayerEntity player)
    {
        return player instanceof ServerPlayerEntity
               ? ObjectBuilder.build((() ->
                 {
                     ServerPlayerEntity serverPlayer = ((ServerPlayerEntity) player);
                     Field gameMode = ObfuscationReflectionHelper.findField(ServerPlayerEntity.class, "field_71134_c");
                     gameMode.setAccessible(true);
                     try
                     {   return (GameType) gameMode.get(serverPlayer);
                     }
                     catch (IllegalAccessException e)
                     {   throw new RuntimeException(e);
                     }
                 }))
               : ClientOnlyHelper.getGameMode();
    }

    public static ServerPlayerEntity getServerPlayer(PlayerEntity player)
    {   return ((MinecraftServer) LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER)).getPlayerList().getPlayer(player.getUUID());
    }

    @Nullable
    public static EquipmentSlotType getEquipmentSlot(int index)
    {
        if (index == 100 + EquipmentSlotType.HEAD.getIndex())
        {   return EquipmentSlotType.HEAD;
        }
        else if (index == 100 + EquipmentSlotType.CHEST.getIndex())
        {   return EquipmentSlotType.CHEST;
        }
        else if (index == 100 + EquipmentSlotType.LEGS.getIndex())
        {   return EquipmentSlotType.LEGS;
        }
        else if (index == 100 + EquipmentSlotType.FEET.getIndex())
        {   return EquipmentSlotType.FEET;
        }
        else if (index == 98)
        {   return EquipmentSlotType.MAINHAND;
        }
        else
        {   return index == 99 ? EquipmentSlotType.OFFHAND : null;
        }
    }
}
