package com.momosoftworks.coldsweat.common.capability.insulation;

import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.api.insulation.AdaptiveInsulation;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.InsulatorData;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTDynamicOps;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;

public class ItemInsulationCap implements IInsulatableCap
{
    private final List<Pair<ItemStack, Multimap<InsulatorData, Insulation>>> insulation = new ArrayList<>();
    private boolean changed = false;
    private CompoundNBT oldSerialized = null;

    @Override
    public List<Pair<ItemStack, Multimap<InsulatorData, Insulation>>> getInsulation()
    {   return this.insulation;
    }

    public void calcAdaptiveInsulation(double worldTemp, double minTemp, double maxTemp)
    {
        for (Pair<ItemStack, Multimap<InsulatorData, Insulation>> entry : insulation)
        {
            Collection<Insulation> entryInsul = entry.getSecond().values();
            for (Insulation pair : entryInsul)
            {
                if (pair instanceof AdaptiveInsulation)
                {
                    AdaptiveInsulation insul = (AdaptiveInsulation) pair;
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
        this.changed = true;
    }

    public void addInsulationItem(ItemStack stack)
    {
        Multimap<InsulatorData, Insulation> insulation = ConfigSettings.INSULATION_ITEMS.get().get(stack.getItem()).stream()
                                                     .map(insulator -> new AbstractMap.SimpleEntry<>(insulator, insulator.insulation().split()))
                                                     .collect(FastMultiMap::new, (map, o) -> map.putAll(o.getKey(), o.getValue()), FastMultiMap::putAll);
        if (!insulation.isEmpty())
        {   this.insulation.add(Pair.of(stack, insulation));
            this.changed = true;
        }
    }

    public ItemStack removeInsulationItem(ItemStack stack)
    {
        Optional<Pair<ItemStack, Multimap<InsulatorData, Insulation>>> toRemove = this.insulation.stream().filter(entry -> entry.getFirst().equals(stack)).findFirst();
        toRemove.ifPresent(pair ->
        {
            this.insulation.remove(pair);
            this.changed = true;
        });
        return stack;
    }

    public ItemStack getInsulationItem(int index)
    {   return this.insulation.get(index).getFirst();
    }

    public boolean canAddInsulationItem(ItemStack armorItem, ItemStack insulationItem)
    {
        AtomicInteger positiveInsul = new AtomicInteger();

        Multimap<InsulatorData, Insulation> insulation = ConfigSettings.INSULATION_ITEMS.get().get(insulationItem.getItem())
                                                     .stream().filter(insulator -> insulator.test(null, insulationItem))
                                                     .map(insulator -> new AbstractMap.SimpleEntry<>(insulator, insulator.insulation().split()))
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

    @Override
    public CompoundNBT serializeNBT()
    {
        if (!this.changed && this.oldSerialized != null)
        {   return this.oldSerialized;
        }
        // Save the insulation items
        ListNBT insulNBT = new ListNBT();
        // Iterate over insulation items
        for (int i = 0; i < insulation.size(); i++)
        {
            Pair<ItemStack, Multimap<InsulatorData, Insulation>> entry = insulation.get(i);

            CompoundNBT entryNBT = new CompoundNBT();
            Multimap<InsulatorData, Insulation> pairList = entry.getSecond();
            // Store ItemStack data
            entryNBT.put("Item", entry.getFirst().save(new CompoundNBT()));
            // Store insulation data
            ListNBT entryInsulList = new ListNBT();
            for (Map.Entry<InsulatorData, Collection<Insulation>> insulMapping : pairList.asMap().entrySet())
            {
                CompoundNBT mappingNBT = new CompoundNBT();
                mappingNBT.put("Insulator", insulMapping.getKey().serialize());
                mappingNBT.put("Insulation", serializeInsulation(insulMapping.getValue()));
                entryInsulList.add(mappingNBT);
            }
            entryNBT.put("Values", entryInsulList);
            // Add the item to the list
            insulNBT.add(entryNBT);
        }

        CompoundNBT tag = new CompoundNBT();
        tag.put("Insulation", insulNBT);

        this.oldSerialized = tag;
        this.changed = false;
        return tag;
    }

    private static ListNBT serializeInsulation(Collection<Insulation> pairList)
    {
        ListNBT insulList = new ListNBT();
        // Store insulation values for the item
        for (Insulation insulation : pairList)
        {   insulList.add(insulation.serialize());
        }
        return insulList;
    }

    @Override
    public void deserializeNBT(CompoundNBT tag)
    {
        this.insulation.clear();

        // Load the insulation items
        ListNBT insulNBT = tag.getList("Insulation", 10);

        for (int i = 0; i < insulNBT.size(); i++)
        {
            CompoundNBT entryNBT = insulNBT.getCompound(i);
            ItemStack stack = ItemStack.of(entryNBT.getCompound("Item"));
            Multimap<InsulatorData, Insulation> insulMap = new FastMultiMap<>();
            ListNBT pairListNBT = entryNBT.getList("Values", 10);
            // Handle legacy insulation
            if (!pairListNBT.isEmpty() && !pairListNBT.getCompound(0).contains("Insulator"))
            {
                for (InsulatorData insulator : ConfigSettings.INSULATION_ITEMS.get().get(stack.getItem()))
                {   insulMap.putAll(insulator, insulator.insulation().split());
                }
            }
            // Handle normal insulation
            else for (int j = 0; j < pairListNBT.size(); j++)
            {
                // Legacy insulation handling
                CompoundNBT mappingNBT = pairListNBT.getCompound(j);
                InsulatorData.CODEC.decode(NBTDynamicOps.INSTANCE, mappingNBT.getCompound("Insulator")).map(Pair::getFirst).result()
                .ifPresent(insulator ->
                {
                    ListNBT insulListNBT = mappingNBT.getList("Insulation", 10);
                    List<Insulation> insulList = new ArrayList<>();
                    for (int k = 0; k < insulListNBT.size(); k++)
                    {   insulList.add(Insulation.deserialize(insulListNBT.getCompound(k)));
                    }
                    insulMap.putAll(insulator, insulList);
                });
            }
            this.insulation.add(Pair.of(stack, insulMap));
        }

        if (!tag.equals(this.oldSerialized))
        {   this.changed = true;
        }
    }

    @Override
    public void copy(IInsulatableCap cap)
    {
        this.insulation.clear();
        this.insulation.addAll(cap.getInsulation());
    }
}
