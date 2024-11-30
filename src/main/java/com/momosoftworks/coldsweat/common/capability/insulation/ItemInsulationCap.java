package com.momosoftworks.coldsweat.common.capability.insulation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.api.insulation.AdaptiveInsulation;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.InsulatorData;
import com.momosoftworks.coldsweat.data.codec.util.CommonStreamCodecs;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.FastMap;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public record ItemInsulationCap(List<Pair<ItemStack, Multimap<InsulatorData, Insulation>>> insulation)
{
    public static final Codec<Pair<InsulatorData, List<Insulation>>> INSULATION_PAIR_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            InsulatorData.CODEC.fieldOf("insulator").forGetter(Pair::getFirst),
            Codec.list(Insulation.getCodec()).fieldOf("insulation").forGetter(Pair::getSecond)
    ).apply(instance, Pair::new));

    public static final Codec<Multimap<InsulatorData, Insulation>> INSULATOR_INSULATION_MULTIMAP_CODEC = Codec.unboundedMap(Codec.STRING, INSULATION_PAIR_CODEC).
    xmap(
        map ->
        {
            Multimap<InsulatorData, Insulation> multimap = new FastMultiMap<>();
            map.forEach((key, pair) -> multimap.putAll(pair.getFirst(), pair.getSecond()));
            return multimap;
        },
        multimap ->
        {
            Map<String, Pair<InsulatorData, List<Insulation>>> map = new HashMap<>();
            int i = 0;
            for (Map.Entry<InsulatorData, Collection<Insulation>> entry : multimap.asMap().entrySet())
            {   map.put(String.valueOf(i++), Pair.of(entry.getKey(), new ArrayList<>(entry.getValue())));
            }
            return map;
        }
    );

    public static final Codec<Pair<ItemStack, Multimap<InsulatorData, Insulation>>> ITEM_INSULATION_PAIR_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("item").forGetter(Pair::getFirst),
            INSULATOR_INSULATION_MULTIMAP_CODEC.fieldOf("insulation").forGetter(Pair::getSecond)
    ).apply(instance, Pair::new));

    public static final Codec<ItemInsulationCap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITEM_INSULATION_PAIR_CODEC.listOf().fieldOf("insulation").forGetter(ItemInsulationCap::insulation)
    ).apply(instance, ItemInsulationCap::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemInsulationCap> STREAM_CODEC = StreamCodec.of((buf, insul) -> insul.serialize(buf),
                                                                                                              (buf) -> ItemInsulationCap.deserialize(buf));

    public ItemInsulationCap()
    {   this(new ArrayList<>());
    }

    public List<Pair<ItemStack, Multimap<InsulatorData, Insulation>>> getInsulation()
    {   return ImmutableList.copyOf(this.insulation());
    }

    public ItemInsulationCap calcAdaptiveInsulation(double worldTemp, double minTemp, double maxTemp)
    {
        var insulation = new ArrayList<>(this.insulation());
        for (Pair<ItemStack, Multimap<InsulatorData, Insulation>> entry : insulation)
        {
            Collection<Insulation> entryInsul = entry.getSecond().values();
            for (Insulation pair : entryInsul)
            {
                if (pair instanceof AdaptiveInsulation insul)
                {
                    double factor = insul.getFactor();
                    double adaptSpeed = insul.getSpeed();

                    double newFactor;
                    if (CSMath.betweenInclusive(CSMath.blend(-1, 1, worldTemp, minTemp, maxTemp), -0.25, 0.25))
                    {   newFactor = CSMath.shrink(factor, adaptSpeed);
                    }
                    else
                    {   newFactor = CSMath.clamp(factor + CSMath.blend(-adaptSpeed, adaptSpeed, worldTemp, minTemp, maxTemp), -1, 1);
                    }
                    insul.setFactor(newFactor);
                }
            }
        }
        return new ItemInsulationCap(insulation);
    }

    public ItemInsulationCap addInsulationItem(ItemStack stack)
    {
        var insulation = new ArrayList<>(this.insulation());

        Multimap<InsulatorData, Insulation> newInsulation = ConfigSettings.INSULATION_ITEMS.get().get(stack.getItem()).stream()
                                                        .map(insulator -> Map.entry(insulator, insulator.insulation().split()))
                                                        .collect(FastMultiMap::new, (map, o) -> map.putAll(o.getKey(), o.getValue()), FastMultiMap::putAll);
        if (!newInsulation.isEmpty())
        {   insulation.add(Pair.of(stack, newInsulation));
        }
        return new ItemInsulationCap(insulation);
    }

    public ItemInsulationCap removeInsulationItem(ItemStack stack)
    {
        var insulation = new ArrayList<>(this.insulation());
        Optional<Pair<ItemStack, Multimap<InsulatorData, Insulation>>> toRemove = insulation.stream().filter(entry -> entry.getFirst().equals(stack)).findFirst();
        toRemove.ifPresent(insulation::remove);

        return new ItemInsulationCap(insulation);
    }

    public ItemStack getInsulationItem(int index)
    {   return this.insulation().get(index).getFirst();
    }

    public boolean canAddInsulationItem(ItemStack armorItem, ItemStack insulationItem)
    {
        AtomicInteger positiveInsul = new AtomicInteger();

        Multimap<InsulatorData, Insulation> insulation = ConfigSettings.INSULATION_ITEMS.get().get(insulationItem.getItem())
                                                     .stream().filter(insulator -> insulator.test(null, insulationItem))
                                                     .map(insulator -> Map.entry(insulator, insulator.insulation().split()))
                                                     .collect(FastMultiMap::new, (map, o) -> map.putAll(o.getKey(), o.getValue()), FastMultiMap::putAll);
        if (insulation.isEmpty())
        {   return false;
        }

        List<Pair<ItemStack, Multimap<InsulatorData, Insulation>>> insulList = new ArrayList<>(this.insulation);
        insulList.add(Pair.of(insulationItem, insulation));

        // Get the total positive/negative insulation of the armor
        insulList.stream().map(Pair::getSecond).flatMap(map -> map.values().stream()).forEach(insul ->
        {
            if (insul.getHeat() >= 0 || insul.getCold() >= 0)
            {   positiveInsul.getAndIncrement();
            }
        });
        return positiveInsul.get() <= ItemInsulationManager.getInsulationSlots(armorItem);
    }

    public void serialize(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeInt(this.insulation().size());
        // Iterate over insulation items
        for (int i = 0; i < this.insulation().size(); i++)
        {
            Pair<ItemStack, Multimap<InsulatorData, Insulation>> entry = this.insulation().get(i);
            Multimap<InsulatorData, Insulation> insulList = entry.getSecond();
            // Store ItemStack data
            ItemStack.STREAM_CODEC.encode(buffer, entry.getFirst());
            // Store insulation data
            CommonStreamCodecs.writeMap(buffer, insulList.asMap(), InsulatorData.STREAM_CODEC, CommonStreamCodecs.listCodec(Insulation.getNetworkCodec()));
        }
    }

    public static ItemInsulationCap deserialize(RegistryFriendlyByteBuf buffer)
    {
        List<Pair<ItemStack, Multimap<InsulatorData, Insulation>>> insulation = new ArrayList<>();
        int size = buffer.readInt();
        for (int i = 0; i < size; i++)
        {
            ItemStack stack = ItemStack.STREAM_CODEC.decode(buffer);
            Multimap<InsulatorData, Insulation> insulList = new FastMultiMap<>(CommonStreamCodecs.readMap(buffer, InsulatorData.STREAM_CODEC, CommonStreamCodecs.listCodec(Insulation.getNetworkCodec())));
            insulation.add(Pair.of(stack, insulList));
        }
        return new ItemInsulationCap(insulation);
    }
}
