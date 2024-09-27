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
import com.momosoftworks.coldsweat.config.type.Insulator;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public record ItemInsulationCap(List<Pair<ItemStack, Multimap<Insulator, Insulation>>> insulation)
{
    public static final Codec<List<Insulation>> INSULATION_CODEC = CompoundTag.CODEC.xmap(
    tag ->
    {
        List<Insulation> insulList = new ArrayList<>();
        for (Tag insulTag : tag.getList("Insulation", 10))
        {   insulList.add(Insulation.deserialize(((CompoundTag) insulTag)));
        }
        return insulList;
    },
    list ->
    {
        CompoundTag tag = new CompoundTag();
        ListTag listTag = new ListTag();
        for (Insulation value : list)
        {   listTag.add(value.serialize());
        }
        tag.put("Insulation", listTag);
        return tag;
    });

    public static final Codec<ItemInsulationCap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.pair(ItemStack.CODEC, INSULATION_CODEC).listOf().fieldOf("Insulation").forGetter(armorInsulation -> armorInsulation.insulation)
    ).apply(instance, ItemInsulationCap::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemInsulationCap> STREAM_CODEC = StreamCodec.of((buf, insul) -> insul.serialize(buf),
                                                                                                              (buf) -> ItemInsulationCap.deserialize(buf));

    public ItemInsulationCap()
    {   this(new ArrayList<>());
    }

    public List<Pair<ItemStack, Multimap<Insulator, Insulation>>> getInsulation()
    {   return ImmutableList.copyOf(this.insulation());
    }

    public ItemInsulationCap calcAdaptiveInsulation(double worldTemp, double minTemp, double maxTemp)
    {
        var insulation = new ArrayList<>(this.insulation());
        for (Pair<ItemStack, Multimap<Insulator, Insulation>> entry : insulation)
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

        Multimap<Insulator, Insulation> newInsulation = ConfigSettings.INSULATION_ITEMS.get().get(stack.getItem()).stream()
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
        Optional<Pair<ItemStack, Multimap<Insulator, Insulation>>> toRemove = insulation.stream().filter(entry -> entry.getFirst().equals(stack)).findFirst();
        toRemove.ifPresent(insulation::remove);

        return new ItemInsulationCap(insulation);
    }

    public ItemStack getInsulationItem(int index)
    {   return this.insulation().get(index).getFirst();
    }

    public boolean canAddInsulationItem(ItemStack armorItem, ItemStack insulationItem)
    {
        AtomicInteger positiveInsul = new AtomicInteger();

        Multimap<Insulator, Insulation> insulation = ConfigSettings.INSULATION_ITEMS.get().get(insulationItem.getItem())
                                                     .stream().filter(insulator -> insulator.test(null, insulationItem))
                                                     .map(insulator -> Map.entry(insulator, insulator.insulation().split()))
                                                     .collect(FastMultiMap::new, (map, o) -> map.putAll(o.getKey(), o.getValue()), FastMultiMap::putAll);
        if (insulation.isEmpty())
        {   return false;
        }

        List<Pair<ItemStack, Multimap<Insulator, Insulation>>> insulList = new ArrayList<>(this.insulation);
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
            Pair<ItemStack, Multimap<Insulator, Insulation>> entry = this.insulation().get(i);

            Multimap<Insulator, Insulation> insulList = entry.getSecond();
            // Store ItemStack data
            ItemStack.STREAM_CODEC.encode(buffer, entry.getFirst());
            // Store insulation data
            buffer.writeInt(insulList.size());
            for (Map.Entry<Insulator, Collection<Insulation>> insulMapping : insulList.asMap().entrySet())
            {
                Insulator
                mappingNBT.put("Insulator", insulMapping.getKey().serialize());
                mappingNBT.put("Insulation", serializeInsulation(insulMapping.getValue()));
                entryInsulList.add(mappingNBT);
            }
            entryNBT.put("Values", entryInsulList);
            // Add the item to the list
            insulNBT.add(entryNBT);
        }
    }

    public static ItemInsulationCap deserialize(RegistryFriendlyByteBuf buffer)
    {
        int size = buffer.readInt();
        List<Pair<ItemStack, List<Insulation>>> insulation = new ArrayList<>();
        for (int i = 0; i < size; i++)
        {
            ItemStack stack = ItemStack.STREAM_CODEC.decode(buffer);
            List<Insulation> insulList = buffer.readList(buf -> Insulation.getNetworkCodec().decode(buf));
            insulation.add(Pair.of(stack, insulList));
        }
        return new ItemInsulationCap(insulation);
    }

    @Override
    public void deserializeNBT(CompoundTag tag)
    {
        this.insulation.clear();

        // Load the insulation items
        ListTag insulNBT = tag.getList("Insulation", 10);

        for (int i = 0; i < insulNBT.size(); i++)
        {
            CompoundTag entryNBT = insulNBT.getCompound(i);

            ItemStack stack = ItemStack.of(entryNBT.getCompound("Item"));
            Multimap<Insulator, Insulation> insulMap = new FastMultiMap<>();
            ListTag pairListNBT = entryNBT.getList("Values", 10);
            // Handle legacy insulation
            if (!pairListNBT.isEmpty() && !pairListNBT.getCompound(0).contains("Insulator"))
            {
                for (Insulator insulator : ConfigSettings.INSULATION_ITEMS.get().get(stack.getItem()))
                {   insulMap.putAll(insulator, insulator.insulation().split());
                }
            }
            // Handle normal insulation
            else for (int j = 0; j < pairListNBT.size(); j++)
            {
                // Legacy insulation handling
                CompoundTag mappingNBT = pairListNBT.getCompound(j);
                Insulator insulator = Insulator.deserialize(mappingNBT.getCompound("Insulator"));
                ListTag insulListNBT = mappingNBT.getList("Insulation", 10);
                List<Insulation> insulList = new ArrayList<>();
                for (int k = 0; k < insulListNBT.size(); k++)
                {   insulList.add(AdaptiveInsulation.deserialize(insulListNBT.getCompound(k)));
                }
                insulMap.putAll(insulator, insulList);
            }
            this.insulation.add(Pair.of(stack, insulMap));
        }
    }
}
