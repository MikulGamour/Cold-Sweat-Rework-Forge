package com.momosoftworks.coldsweat.data.codec.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.serialization.Codec;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class AttributeModifierMap implements NbtSerializable
{
    public static final Codec<AttributeModifierMap> CODEC = Codec.unboundedMap(AttributeCodecs.ATTRIBUTE_CODEC, AttributeCodecs.MODIFIER_CODEC.listOf())
            .xmap(AttributeModifierMap::new,
                  map -> map.getMap().asMap().entrySet()
                            .stream()
                            .collect(HashMap::new,
                                     (mp, ent) -> mp.put(ent.getKey(), new ArrayList<>(ent.getValue())),
                                     HashMap::putAll));

    private final Multimap<Attribute, AttributeModifier> map = new FastMultiMap<>();

    public AttributeModifierMap()
    {
    }

    public AttributeModifierMap(Map<Attribute, ?> attributeListMap)
    {   attributeListMap.forEach((attribute, list) ->
        {   if (list instanceof Collection)
            {   map.putAll(attribute, (Collection<AttributeModifier>) list);
            }
            else if (list instanceof AttributeModifier)
            {   map.put(attribute, (AttributeModifier) list);
            }
        });
    }

    public AttributeModifierMap(Multimap<Attribute, AttributeModifier> map)
    {   this.map.putAll(map);
    }

    public void put(Attribute attribute, AttributeModifier modifier)
    {   map.put(attribute, modifier);
    }

    public Collection<AttributeModifier> get(Attribute attribute)
    {   return map.get(attribute);
    }

    public Multimap<Attribute, AttributeModifier> getMap()
    {   return map;
    }

    public AttributeModifierMap putAll(AttributeModifierMap other)
    {   map.putAll(other.map);
        return this;
    }

    public AttributeModifierMap putAll(Attribute attribute, Collection<AttributeModifier> modifiers)
    {   map.putAll(attribute, modifiers);
        return this;
    }

    public boolean isEmpty()
    {   return map.isEmpty();
    }

    @Override
    public CompoundNBT serialize()
    {
        CompoundNBT tag = new CompoundNBT();
        map.asMap().forEach((attribute, modifier) ->
        {   String key = ForgeRegistries.ATTRIBUTES.getKey(attribute).toString();
            ListNBT list = new ListNBT();
            modifier.forEach(mod -> list.add(mod.save()));
            tag.put(key, list);
        });
        return tag;
    }

    public static AttributeModifierMap deserialize(CompoundNBT tag)
    {
        if (tag.isEmpty()) return new AttributeModifierMap();

        Multimap<Attribute, AttributeModifier> map = HashMultimap.create();
        tag.getAllKeys().forEach(key ->
        {   Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(key));
            ListNBT list = tag.getList(key, 10);
            list.forEach(mod -> map.put(attribute, AttributeModifier.load(((CompoundNBT) mod))));
        });
        return new AttributeModifierMap(map);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        AttributeModifierMap that = (AttributeModifierMap) obj;
        for (Map.Entry<Attribute, Collection<AttributeModifier>> entry : map.asMap().entrySet())
        {
            if (!that.map.containsKey(entry.getKey())) return false;

            Collection<AttributeModifier> other = that.map.get(entry.getKey());
            if (entry.getValue().size() != other.size()) return false;

            Iterator<AttributeModifier> thatIterator = other.iterator();
            for (AttributeModifier modifier : entry.getValue())
            {
                if (!thatIterator.hasNext()) return false;
                AttributeModifier thatModifier = thatIterator.next();
                if (!(Double.compare(modifier.getAmount(), thatModifier.getAmount()) == 0
                      && modifier.getOperation() == thatModifier.getOperation()
                      && modifier.getName().equals(thatModifier.getName())))
                {
                    return false;
                }
            }
        }
        return true;
    }
}
