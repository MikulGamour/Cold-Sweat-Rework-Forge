package com.momosoftworks.coldsweat.core.network.message;

import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.client.event.TooltipHandler;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.network.BufferHelper;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.data.codec.configuration.*;
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
    private final Map<String, Object> predicateMap = new FastMap<>();
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

    public SyncItemPredicatesMessage(ItemStack stack, int inventorySlot, @Nullable EquipmentSlotType equipmentSlot, Map<String, Object> predicateMap)
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

        BufferHelper.writeMap(buffer, message.predicateMap,
        PacketBuffer::writeUtf,
        (buf, value) ->
        {
            if (value instanceof Map)
            {
                buf.writeUtf("Map");
                BufferHelper.writeMap(buf, (Map<?, ?>) value,
                (buf1, key) ->
                {
                    if (key instanceof NbtSerializable)
                    {
                        buf1.writeUtf(key.getClass().getSimpleName());
                        buf1.writeNbt(((NbtSerializable) key).serialize());
                    }
                    else throw new IllegalStateException("Invalid key type: " + key.getClass());
                },
                (buf1, value1) ->
                {
                    if (value1 instanceof Boolean)
                        buf1.writeBoolean((Boolean) value1);
                    else throw new IllegalStateException("Invalid value type: " + value1.getClass());
                });
            }
            else if (value instanceof List)
            {
                buf.writeUtf("List");
                List<?> list = (List<?>) value;
                BufferHelper.writeCollection(buf, list, (buf1, element) ->
                {
                    if (element instanceof Pair<?,?>)
                    {
                        Pair<?,?> pair = (Pair<?,?>) element;
                        if (pair.getFirst() instanceof ItemStack)
                        {
                            buf1.writeItem((ItemStack) pair.getFirst());
                            BufferHelper.writeMap(buf1, (Map<?, ?>) pair.getSecond(),
                            (buf2, key) ->
                            {
                                if (key instanceof NbtSerializable)
                                {
                                    buf2.writeUtf(key.getClass().getSimpleName());
                                    buf2.writeNbt(((NbtSerializable) key).serialize());
                                }
                                else throw new IllegalStateException("Invalid key type: " + key.getClass());
                            },
                            (buf2, value1) ->
                            {
                                if (value1 instanceof Boolean)
                                    buf2.writeBoolean((Boolean) value1);
                                else throw new IllegalStateException("Invalid value type: " + value1.getClass());
                            });
                        }
                        else throw new IllegalStateException("Invalid pair first type: " + pair.getFirst().getClass());
                    }
                    else throw new IllegalStateException("Invalid element type: " + element.getClass());
                });
            }
        });
    }

    public static SyncItemPredicatesMessage decode(PacketBuffer buffer)
    {
        ItemStack stack = buffer.readItem();
        int inventorySlot = buffer.readInt();
        EquipmentSlotType equipmentSlot = BufferHelper.readOptional(buffer, buf -> buf.readEnum(EquipmentSlotType.class)).orElse(null);

        Map<String, Object> predicateMap;
        if (!buffer.readBoolean())
        {   predicateMap = new FastMap<>();
        }
        else predicateMap = BufferHelper.readMap(buffer, PacketBuffer::readUtf,
        buf ->
        {
            String type = buf.readUtf();
            if (type.equals("Map"))
            {
                return BufferHelper.readMap(buf,
                FastMap::new,
                buf1 ->
                {
                    String className = buf1.readUtf();
                    return getDeserializer(className).apply(buf1.readNbt());
                },
                PacketBuffer::readBoolean);
            }
            else if (type.equals("List"))
            {
                return BufferHelper.readCollection(buffer,
                ArrayList::new,
                buf1 ->
                {
                    ItemStack itemStack = buf1.readItem();
                    Map<Object, Boolean> map = BufferHelper.readMap(buf1,
                    FastMap::new,
                    buf2 ->
                    {
                        String className = buf2.readUtf();
                        return getDeserializer(className).apply(buf2.readNbt());
                    },
                    PacketBuffer::readBoolean);
                    return Pair.of(itemStack, map);
                });
            }
            else throw new IllegalStateException("Invalid type: " + type);
        });

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
            {   TooltipHandler.HOVERED_STACK_PREDICATES = message.predicateMap;
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
                List<Pair<ItemStack, Map<InsulatorData, Boolean>>> insulatorMap =
                cap.getInsulation().stream()
                   .map(pair -> Pair.of(pair.getFirst(),
                                        pair.getSecond().entries().stream()
                                            .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getKey().test(entity, pair.getFirst())))
                                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))))
                   .collect(Collectors.toList());

                this.predicateMap.put("armor_insulation", insulatorMap);
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
            Map<ItemCarryTempData, Boolean> insulatorMap = ConfigSettings.CARRIED_ITEM_TEMPERATURES.get().get(stack.getItem()).stream()
                    .map(data -> new AbstractMap.SimpleEntry<>(data, data.test(entity, stack, invSlot, equipmentSlot)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            String configId = ConfigSettings.CONFIG_SETTINGS.inverse().get(ConfigSettings.CARRIED_ITEM_TEMPERATURES);
            this.predicateMap.put(configId, insulatorMap);
        }
    }

    private void checkItemRequirement(ItemStack stack, Entity entity, DynamicHolder<Multimap<Item, ? extends RequirementHolder>> configSetting)
    {
        if (configSetting.get().containsKey(stack.getItem()))
        {
            Map<? extends RequirementHolder, Boolean> configMap = configSetting.get().get(stack.getItem()).stream()
                    .map(data -> new AbstractMap.SimpleEntry<>(data, data.test(entity, stack)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            String configId = ConfigSettings.CONFIG_SETTINGS.inverse().get(configSetting);
            this.predicateMap.put(configId, configMap);
        }
    }

    private static Function<CompoundNBT, Object> getDeserializer(String className)
    {
        switch (className)
        {
            case "ItemCarryTempData" : return ItemCarryTempData::deserialize;
            case "InsulatorData" : return InsulatorData::deserialize;
            case "FuelData" : return FuelData::deserialize;
            default : throw new IllegalStateException("Invalid class name: " + className);
        }
    }
}
