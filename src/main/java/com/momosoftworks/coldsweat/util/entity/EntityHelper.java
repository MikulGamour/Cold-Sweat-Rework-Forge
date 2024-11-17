package com.momosoftworks.coldsweat.util.entity;

import com.momosoftworks.coldsweat.util.ClientOnlyHelper;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import com.momosoftworks.coldsweat.util.serialization.ObjectBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

public class EntityHelper
{
    private EntityHelper() {}

    public static ItemStack getItemInHand(LivingEntity player, HumanoidArm hand)
    {   return player.getItemInHand(hand == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }

    public static HumanoidArm getArmFromHand(InteractionHand hand, Player player)
    {   return hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm() == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }

    public static boolean holdingLamp(LivingEntity player, HumanoidArm arm)
    {   return getItemInHand(player, arm).getItem() == ModItems.SOULSPRING_LAMP;
    }

    public static Vec3 getCenterOf(Entity entity)
    {   return entity.position().add(0, entity.getBbHeight() / 2, 0);
    }

    public static GameType getGameModeForPlayer(Player player)
    {
        return player instanceof ServerPlayer serverPlayer
               ? ObjectBuilder.build((() ->
                 {
                     Field gameMode = ObfuscationReflectionHelper.findField(ServerPlayer.class, "f_8941_");
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

    public static ServerPlayer getServerPlayer(Player player)
    {   return ((MinecraftServer) LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER)).getPlayerList().getPlayer(player.getUUID());
    }

    @Nullable
    public static EquipmentSlot getEquipmentSlot(int index)
    {
        if (index == 100 + EquipmentSlot.HEAD.getIndex())
        {   return EquipmentSlot.HEAD;
        }
        else if (index == 100 + EquipmentSlot.CHEST.getIndex())
        {   return EquipmentSlot.CHEST;
        }
        else if (index == 100 + EquipmentSlot.LEGS.getIndex())
        {   return EquipmentSlot.LEGS;
        }
        else if (index == 100 + EquipmentSlot.FEET.getIndex())
        {   return EquipmentSlot.FEET;
        }
        else if (index == 98)
        {   return EquipmentSlot.MAINHAND;
        }
        else
        {   return index == 99 ? EquipmentSlot.OFFHAND : null;
        }
    }
}
