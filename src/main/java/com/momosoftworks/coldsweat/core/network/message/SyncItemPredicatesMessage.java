package com.momosoftworks.coldsweat.core.network.message;

import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.client.event.TooltipHandler;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.network.BufferHelper;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.data.ModRegistries;
import com.momosoftworks.coldsweat.data.codec.configuration.*;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.impl.RequirementHolder;
import com.momosoftworks.coldsweat.util.math.FastMap;
import com.momosoftworks.coldsweat.util.serialization.DynamicHolder;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SyncItemPredicatesMessage
{
    private final Map<UUID, Boolean> predicateMap = new FastMap<>();
    ItemStack stack = ItemStack.EMPTY;
    int inventorySlot = 0;
    @Nullable
    EquipmentSlotType equipmentSlot = null;

    public static SyncItemPredicatesMessage fromClient(ItemStack stack, int inventorySlot, @Nullable EquipmentSlotType equipmentSlot)
    {   return new SyncItemPredicatesMessage(stack, inventorySlot, equipmentSlot);
    }

    public static SyncItemPredicatesMessage fromServer(ItemStack stack, int inventorySlot, @Nullable EquipmentSlotType equipmentSlot, Entity entity)
    {   return new SyncItemPredicatesMessage(stack, inventorySlot, equipmentSlot, entity);
    }

    public SyncItemPredicatesMessage(ItemStack stack, int inventorySlot, @Nullable EquipmentSlotType equipmentSlot)
    {
        this.stack = stack;
        this.inventorySlot = inventorySlot;
        this.equipmentSlot = equipmentSlot;
    }

    public SyncItemPredicatesMessage(ItemStack stack, int inventorySlot, @Nullable EquipmentSlotType equipmentSlot, Entity entity)
    {
        this.stack = stack;
        this.checkInsulator(stack, entity);
        this.checkInsulatingArmor(stack, entity);
        this.checkInsulatingCurio(stack, entity);
        this.checkArmorInsulation(stack, entity);

        this.checkFood(stack, entity);

        this.checkBoilerFuel(stack);
        this.checkIceboxFuel(stack);
        this.checkHearthFuel(stack);
        this.checkSoulLampFuel(stack);

        this.checkCarriedTemps(stack, inventorySlot, equipmentSlot, entity);
    }

    public SyncItemPredicatesMessage(ItemStack stack, int inventorySlot, @Nullable EquipmentSlotType equipmentSlot, Map<UUID, Boolean> predicateMap)
    {
        this.stack = stack;
        this.inventorySlot = inventorySlot;
        this.equipmentSlot = equipmentSlot;
        this.predicateMap.putAll(predicateMap);
    }

    public static void encode(SyncItemPredicatesMessage message, PacketBuffer buffer)
    {
        buffer.writeItem(message.stack);
        buffer.writeInt(message.inventorySlot);
        BufferHelper.writeOptional(buffer, Optional.ofNullable(message.equipmentSlot), PacketBuffer::writeEnum);

        if (message.predicateMap.isEmpty())
        {   buffer.writeBoolean(false);
            return;
        }
        else buffer.writeBoolean(true);
        BufferHelper.writeMap(buffer, message.predicateMap, PacketBuffer::writeUUID, PacketBuffer::writeBoolean);
    }

    public static SyncItemPredicatesMessage decode(PacketBuffer buffer)
    {
        ItemStack stack = buffer.readItem();
        int inventorySlot = buffer.readInt();
        EquipmentSlotType equipmentSlot = BufferHelper.readOptional(buffer, buf -> buf.readEnum(EquipmentSlotType.class)).orElse(null);

        Map<UUID, Boolean> predicateMap = buffer.readBoolean()
                                          ? BufferHelper.readMap(buffer, PacketBuffer::readUUID, PacketBuffer::readBoolean)
                                          : new FastMap<>();

        return new SyncItemPredicatesMessage(stack, inventorySlot, equipmentSlot, predicateMap);
    }

    public static void handle(SyncItemPredicatesMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        LogicalSide receivingSide = context.getDirection().getReceptionSide();

        // Server is telling client which insulators pass their checks
        if (receivingSide.isClient())
        {
            context.enqueueWork(() ->
            {   TooltipHandler.HOVERED_STACK_PREDICATES.clear();
                TooltipHandler.HOVERED_STACK_PREDICATES.putAll(message.predicateMap);
            });
        }
        // Client is asking server for insulator predicates
        else if (receivingSide.isServer() && context.getSender() != null)
        {
            context.enqueueWork(() ->
            {
                ServerPlayerEntity player = context.getSender();
                if (player != null)
                {
                    ColdSweatPacketHandler.INSTANCE.sendTo(SyncItemPredicatesMessage.fromServer(message.stack, message.inventorySlot, message.equipmentSlot, player),
                                                           player.connection.connection,
                                                           NetworkDirection.PLAY_TO_CLIENT);
                }
            });
        }
        context.setPacketHandled(true);
    }

    private void checkInsulator(ItemStack stack, Entity entity)
    {   this.checkItemRequirement(stack, entity, (DynamicHolder) ConfigSettings.INSULATION_ITEMS);
    }

    private void checkInsulatingArmor(ItemStack stack, Entity entity)
    {   this.checkItemRequirement(stack, entity, (DynamicHolder) ConfigSettings.INSULATING_ARMORS);
    }

    private void checkInsulatingCurio(ItemStack stack, Entity entity)
    {   this.checkItemRequirement(stack, entity, (DynamicHolder) ConfigSettings.INSULATING_CURIOS);
    }

    private void checkArmorInsulation(ItemStack stack, Entity entity)
    {
        if (ItemInsulationManager.isInsulatable(stack))
        {
            ItemInsulationManager.getInsulationCap(stack).ifPresent(cap ->
            {
                for (Pair<ItemStack, Multimap<InsulatorData, Insulation>> pair : cap.getInsulation())
                {
                    for (InsulatorData insulatorData : pair.getSecond().keySet())
                    {   this.predicateMap.put(insulatorData.getId(), insulatorData.test(entity, stack));
                    }
                }
            });
        }
    }

    private void checkFood(ItemStack stack, Entity entity)
    {   this.checkItemRequirement(stack, entity, (DynamicHolder) ConfigSettings.FOOD_TEMPERATURES);
    }

    private void checkBoilerFuel(ItemStack stack)
    {   this.checkItemRequirement(stack, null, (DynamicHolder) ConfigSettings.BOILER_FUEL);
    }

    private void checkIceboxFuel(ItemStack stack)
    {   this.checkItemRequirement(stack, null, (DynamicHolder) ConfigSettings.ICEBOX_FUEL);
    }

    private void checkHearthFuel(ItemStack stack)
    {   this.checkItemRequirement(stack, null, (DynamicHolder) ConfigSettings.HEARTH_FUEL);
    }

    private void checkSoulLampFuel(ItemStack stack)
    {   this.checkItemRequirement(stack, null, (DynamicHolder) ConfigSettings.SOULSPRING_LAMP_FUEL);
    }

    private void checkCarriedTemps(ItemStack stack, int invSlot, EquipmentSlotType equipmentSlot, Entity entity)
    {
        if (ConfigSettings.CARRIED_ITEM_TEMPERATURES.get().containsKey(stack.getItem()))
        {
            Map<UUID, Boolean> insulatorMap = ConfigSettings.CARRIED_ITEM_TEMPERATURES.get().get(stack.getItem())
                                              .stream()
                                              .map(data ->
                                              {   boolean test = data.test(entity, stack, invSlot, equipmentSlot);
                                                  return new AbstractMap.SimpleEntry<>(data.getId(), test);
                                              })
                                              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            this.predicateMap.putAll(insulatorMap);
        }
    }

    private void checkItemRequirement(ItemStack stack, Entity entity, DynamicHolder<Multimap<Item, RequirementHolder>> configSetting)
    {
        Map<UUID, Boolean> configMap = configSetting.get().get(stack.getItem())
                                       .stream()
                                       .map(data ->
                                       {   UUID id = ((ConfigData<?>) data).getId();
                                           return new AbstractMap.SimpleEntry<>(id, data.test(entity, stack));
                                       })
                                       .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.predicateMap.putAll(configMap);
    }
}
