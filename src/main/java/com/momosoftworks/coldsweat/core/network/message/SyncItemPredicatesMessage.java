package com.momosoftworks.coldsweat.core.network.message;

import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.client.event.TooltipHandler;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.*;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.impl.RequirementHolder;
import com.momosoftworks.coldsweat.util.math.FastMap;
import com.momosoftworks.coldsweat.util.serialization.DynamicHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class SyncItemPredicatesMessage implements CustomPacketPayload
{
    public static final Type<SyncItemPredicatesMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "sync_item_predicates"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncItemPredicatesMessage> CODEC = CustomPacketPayload.codec(SyncItemPredicatesMessage::encode, SyncItemPredicatesMessage::decode);

    private final Map<UUID, Boolean> predicateMap = new FastMap<>();
    ItemStack stack = ItemStack.EMPTY;
    int inventorySlot = 0;
    @Nullable EquipmentSlot equipmentSlot = null;

    public static SyncItemPredicatesMessage fromClient(ItemStack stack, int inventorySlot, @Nullable EquipmentSlot equipmentSlot)
    {   return new SyncItemPredicatesMessage(stack, inventorySlot, equipmentSlot);
    }

    public static SyncItemPredicatesMessage fromServer(ItemStack stack, int inventorySlot, @Nullable EquipmentSlot equipmentSlot, Entity entity)
    {   return new SyncItemPredicatesMessage(stack, inventorySlot, equipmentSlot, entity);
    }

    public SyncItemPredicatesMessage(ItemStack stack, int inventorySlot, @Nullable EquipmentSlot equipmentSlot)
    {
        this.stack = stack;
        this.inventorySlot = inventorySlot;
        this.equipmentSlot = equipmentSlot;
    }

    public SyncItemPredicatesMessage(ItemStack stack, int inventorySlot, @Nullable EquipmentSlot equipmentSlot, Entity entity)
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

    public SyncItemPredicatesMessage(ItemStack stack, int inventorySlot, @Nullable EquipmentSlot equipmentSlot, Map<UUID, Boolean> predicateMap)
    {
        this.stack = stack;
        this.inventorySlot = inventorySlot;
        this.equipmentSlot = equipmentSlot;
        this.predicateMap.putAll(predicateMap);
    }

    public static void encode(SyncItemPredicatesMessage message, RegistryFriendlyByteBuf buffer)
    {
        ItemStack.STREAM_CODEC.encode(buffer, message.stack);
        buffer.writeInt(message.inventorySlot);
        buffer.writeOptional(Optional.ofNullable(message.equipmentSlot), FriendlyByteBuf::writeEnum);
        buffer.writeMap(message.predicateMap, (buf, uuid) -> buf.writeUUID(uuid), FriendlyByteBuf::writeBoolean);
    }

    public static SyncItemPredicatesMessage decode(RegistryFriendlyByteBuf buffer)
    {
        ItemStack stack = ItemStack.STREAM_CODEC.decode(buffer);
        int inventorySlot = buffer.readInt();
        EquipmentSlot equipmentSlot = buffer.readOptional(buf -> buf.readEnum(EquipmentSlot.class)).orElse(null);
        Map<UUID, Boolean> predicateMap = buffer.readMap(buf -> buf.readUUID(), FriendlyByteBuf::readBoolean);

        return new SyncItemPredicatesMessage(stack, inventorySlot, equipmentSlot, predicateMap);
    }

    public static void handle(SyncItemPredicatesMessage message, IPayloadContext context)
    {
        LogicalSide receivingSide = context.flow().getReceptionSide();

        // Server is telling client which insulators pass their checks
        if (receivingSide.isClient())
        {
            context.enqueueWork(() ->
            {   TooltipHandler.HOVERED_STACK_PREDICATES.putAll(message.predicateMap);
            });
        }
        // Client is asking server for insulator predicates
        else if (receivingSide.isServer() && context.player() instanceof ServerPlayer player)
        {
            context.enqueueWork(() ->
            {
                PacketDistributor.sendToPlayer(player, SyncItemPredicatesMessage.fromServer(message.stack, message.inventorySlot, message.equipmentSlot, player));
            });
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {   return TYPE;
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

    private void checkCarriedTemps(ItemStack stack, int invSlot, EquipmentSlot equipmentSlot, Entity entity)
    {
        if (ConfigSettings.CARRIED_ITEM_TEMPERATURES.get().containsKey(stack.getItem()))
        {
            Map<UUID, Boolean> insulatorMap = ConfigSettings.CARRIED_ITEM_TEMPERATURES.get().get(stack.getItem())
                                              .stream()
                                              .map(data ->
                                              {   boolean test = data.test(entity, stack, invSlot, equipmentSlot);
                                                  return Map.entry(data.getId(), test);
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
                                           return Map.entry(id, data.test(entity, stack));
                                       })
                                       .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.predicateMap.putAll(configMap);
    }
}
