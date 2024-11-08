package com.momosoftworks.coldsweat.util.serialization;

import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.insulation.AdaptiveInsulation;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.insulation.StaticInsulation;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.type.Insulator;
import com.momosoftworks.coldsweat.config.type.PredicateItem;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemComponentsRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.exceptions.ArgumentCountException;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import oshi.util.tuples.Triplet;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class ConfigHelper
{
    private ConfigHelper() {}

    public static <T> List<T> parseRegistryItems(ResourceKey<Registry<T>> registry, RegistryAccess registryAccess, String objects)
    {
        List<T> biomeList = new ArrayList<>();
        Optional<Registry<T>> optReg = registryAccess.registry(registry);
        if (!optReg.isPresent()) return biomeList;

        Registry<T> reg = optReg.get();

        for (String objString : objects.split(","))
        {
            if (objString.startsWith("#"))
            {
                final String tagID = objString.replace("#", "");
                Optional<HolderSet.Named<T>> tag = reg.getTag(TagKey.create(registry, ResourceLocation.parse(tagID)));
                tag.ifPresent(tg -> biomeList.addAll(tg.stream().map(Holder::value).toList()));
            }
            else
            {
                ResourceLocation id = ResourceLocation.parse(objString);
                Optional<T> obj = Optional.ofNullable(reg.get(id));
                if (obj.isEmpty())
                {
                    ColdSweat.LOGGER.error("Error parsing config: \"{}\" does not exist", objString);
                    continue;
                }
                biomeList.add(obj.get());
            }
        }
        return biomeList;
    }

    public static List<Block> getBlocks(String... ids)
    {
        List<Block> blocks = new ArrayList<>();
        for (String id : ids)
        {
            if (id.startsWith("#"))
            {
                final String tagID = id.replace("#", "");
                CSMath.doIfNotNull(BuiltInRegistries.BLOCK.getTags(), tags ->
                {   Optional<Pair<TagKey<Block>, HolderSet.Named<Block>>> optionalTag = tags.filter(tag -> tag != null && tag.getFirst().location().toString().equals(tagID)).findFirst();
                    optionalTag.ifPresent(blockITag -> blocks.addAll(optionalTag.get().getSecond().stream().map(Holder::value).toList()));
                });
            }
            else
            {
                ResourceLocation blockId = ResourceLocation.parse(id);
                if (BuiltInRegistries.BLOCK.containsKey(blockId))
                {   blocks.add(BuiltInRegistries.BLOCK.get(blockId));
                }
                else
                {   ColdSweat.LOGGER.error("Error parsing block config: block \"{}\" does not exist", id);
                }
            }
        }
        return blocks;
    }

    public static List<Item> getItems(String... ids)
    {
        List<Item> items = new ArrayList<>();
        for (String itemId : ids)
        {
            if (itemId.startsWith("#"))
            {
                final String tagID = itemId.replace("#", "");
                CSMath.doIfNotNull(BuiltInRegistries.ITEM.getTags(), tags ->
                {   Optional<Pair<TagKey<Item>, HolderSet.Named<Item>>> optionalTag = tags.filter(tag -> tag != null && tag.getFirst().location().toString().equals(tagID)).findFirst();
                    optionalTag.ifPresent(itemITag -> items.addAll(optionalTag.get().getSecond().stream().map(Holder::value).toList()));
                });
            }
            else
            {
                ResourceLocation itemID = ResourceLocation.parse(itemId);
                if (BuiltInRegistries.ITEM.containsKey(itemID))
                {   items.add(BuiltInRegistries.ITEM.get(itemID));
                }
                else
                {   ColdSweat.LOGGER.error("Error parsing item config: item \"{}\" does not exist", itemId);
                }
            }
        }
        return items;
    }

    public static Map<Biome, Triplet<Double, Double, Temperature.Units>> getBiomesWithValues(List<? extends List<?>> source, boolean absolute, RegistryAccess registryAccess)
    {
        Map<Biome, Triplet<Double, Double, Temperature.Units>> map = new HashMap<>();
        for (List<?> entry : source)
        {
            try
            {
                String biomeIdString = (String) entry.get(0);
                for (Biome biome : parseRegistryItems(Registries.BIOME, registryAccess, biomeIdString))
                {
                    if (biome == null)
                    {   ColdSweat.LOGGER.error("Error parsing biome config: string \"{}\" contains a biome that does not exist or is not loaded yet", biomeIdString);
                        continue;
                    }

                    double min;
                    double max;
                    Temperature.Units units;
                    // The config defines a min and max value, with optional unit conversion
                    if (entry.size() > 2)
                    {   units = entry.size() == 4 ? Temperature.Units.valueOf(((String) entry.get(3)).toUpperCase()) : Temperature.Units.MC;
                        min = Temperature.convert(((Number) entry.get(1)).doubleValue(), units, Temperature.Units.MC, absolute);
                        max = Temperature.convert(((Number) entry.get(2)).doubleValue(), units, Temperature.Units.MC, absolute);
                    }
                    // The config only defines a mid-temperature
                    else
                    {   double mid = ((Number) entry.get(1)).doubleValue();
                        double variance = 1 / Math.max(1, 2 + biome.getModifiedClimateSettings().downfall() * 2);
                        min = mid - variance;
                        max = mid + variance;
                        units = Temperature.Units.MC;
                    }

                    // Maps the biome ID to the temperature (and variance if present)
                    map.put(biome, new Triplet<>(min, max, units));
                }
            }
            catch (Exception e)
            {
                ColdSweat.LOGGER.error("Error parsing biome config \"{}\"", entry.toString());
                e.printStackTrace();
            }
        }
        return map;
    }

    public static Map<DimensionType, Pair<Double, Temperature.Units>> getDimensionsWithValues(List<? extends List<?>> source, boolean absolute, RegistryAccess registryAccess)
    {
        Map<DimensionType, Pair<Double, Temperature.Units>> map = new HashMap<>();
        for (List<?> entry : source)
        {
            try
            {
                String dimensionIdString = (String) entry.get(0);
                for (DimensionType dimension : parseRegistryItems(Registries.DIMENSION_TYPE, registryAccess, dimensionIdString))
                {
                    if (dimension == null)
                    {   ColdSweat.LOGGER.error("Error parsing dimension config: string \"{}\" contains a dimension that does not exist or is not loaded yet", dimensionIdString);
                        continue;
                    }
                    double temp = ((Number) entry.get(1)).doubleValue();
                    Temperature.Units units = entry.size() == 3 ? Temperature.Units.valueOf(((String) entry.get(2)).toUpperCase()) : Temperature.Units.MC;
                    map.put(dimension, Pair.of(Temperature.convert(temp, units, Temperature.Units.MC, absolute), units));

                }
            }
            catch (Exception e)
            {
                ColdSweat.LOGGER.error("Error parsing dimension config \"{}\"", entry.toString());
                e.printStackTrace();
            }
        }
        return map;
    }

    public static Map<StructureType<?>, Pair<Double, Temperature.Units>> getStructuresWithValues(List<? extends List<?>> source, boolean absolute, RegistryAccess registryAccess)
    {
        Map<StructureType<?>, Pair<Double, Temperature.Units>> map = new HashMap<>();
        for (List<?> entry : source)
        {
            try
            {
                String structureIdString = (String) entry.get(0);
                for (Structure structure : parseRegistryItems(Registries.STRUCTURE, registryAccess, structureIdString))
                {
                    if (structure == null)
                    {   ColdSweat.LOGGER.error("Error parsing structure config: string \"{}\" contains a structure that does not exist or is not loaded yet", structureIdString);
                        continue;
                    }
                    double temp = ((Number) entry.get(1)).doubleValue();
                    Temperature.Units units = entry.size() == 3 ? Temperature.Units.valueOf(((String) entry.get(2)).toUpperCase()) : Temperature.Units.MC;
                    map.put(structure.type(), Pair.of(Temperature.convert(temp, units, Temperature.Units.MC, absolute), units));
                }
            }
            catch (Exception e)
            {   ColdSweat.LOGGER.error("Error parsing structure config \"{}\"", entry.toString());
                e.printStackTrace();
            }
        }
        return map;
    }

    public static Map<String, Predicate<BlockState>> getBlockStatePredicates(Block block, String predicates)
    {
        Map<String, Predicate<BlockState>> blockPredicates = new HashMap<>();
        // Separate comma-delineated predicates
        String[] predicateList = predicates.split(",");

        // Iterate predicates
        for (String predicate : predicateList)
        {
            // Split predicate into key-value pairs separated by "="
            String[] pair = predicate.split("=");
            String key = pair[0];
            String value = pair[1];

            // Get the property with the given name
            Property<?> property = block.getStateDefinition().getProperty(key);
            if (property != null)
            {
                // Parse the desired value for this property
                property.getValue(value).ifPresent(propertyValue ->
                {
                    // Add a new predicate to the list
                    blockPredicates.put(predicate, state ->
                    {   // If the value matches, this predicate returns true
                        return state.getValue(property).equals(propertyValue);
                    });
                });
            }
        }
        return blockPredicates;
    }

    public static List<EntityType<?>> getEntityTypes(String... entities)
    {
        List<EntityType<?>> entityList = new ArrayList<>();
        for (String entity : entities)
        {
            if (entity.startsWith("#"))
            {
                final String tagID = entity.replace("#", "");
                CSMath.doIfNotNull(BuiltInRegistries.ENTITY_TYPE.getTags(), tags ->
                {   Optional<Pair<TagKey<EntityType<?>>, HolderSet.Named<EntityType<?>>>> optionalTag = tags.filter(tag -> tag != null && tag.getFirst().location().toString().equals(tagID)).findFirst();
                    optionalTag.ifPresent(entityITag -> entityList.addAll(optionalTag.get().getSecond().stream().map(Holder::value).toList()));
                });
            }
            else
            {
                ResourceLocation entityId = ResourceLocation.parse(entity);
                if (BuiltInRegistries.ENTITY_TYPE.containsKey(entityId))
                {   entityList.add(BuiltInRegistries.ENTITY_TYPE.get(entityId));
                }
                else
                {   ColdSweat.LOGGER.error("Error parsing entity config: entity \"{}\" does not exist", entity);
                }
            }
        }
        return entityList;
    }

    public static CompoundTag serializeNbtBool(boolean value, String key)
    {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(key, value);
        return tag;
    }

    public static CompoundTag serializeNbtInt(int value, String key)
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt(key, value);
        return tag;
    }

    public static CompoundTag serializeNbtDouble(double value, String key)
    {
        CompoundTag tag = new CompoundTag();
        tag.putDouble(key, value);
        return tag;
    }

    public static CompoundTag serializeNbtString(String value, String key)
    {
        CompoundTag tag = new CompoundTag();
        tag.putString(key, value);
        return tag;
    }

    public static CompoundTag serializeBiomeTemps(Map<Biome, Triplet<Double, Double, Temperature.Units>> map, String key, RegistryAccess registryAccess)
    {
        CompoundTag tag = new CompoundTag();
        CompoundTag mapTag = new CompoundTag();
        for (Map.Entry<Biome, Triplet<Double, Double, Temperature.Units>> entry : map.entrySet())
        {
            CompoundTag biomeTag = new CompoundTag();
            ResourceLocation biomeId = RegistryHelper.getBiomeId(entry.getKey(), registryAccess);
            if (biomeId == null)
            {   ColdSweat.LOGGER.error("Error serializing biome temperatures: biome \"{}\" does not exist", entry.getKey());
                continue;
            }
            biomeTag.putDouble("Min", entry.getValue().getA());
            biomeTag.putDouble("Max", entry.getValue().getB());
            biomeTag.putString("Units", entry.getValue().getC().toString());
            mapTag.put(biomeId.toString(), biomeTag);
        }
        tag.put(key, mapTag);
        return tag;
    }

    public static Map<Biome, Triplet<Double, Double, Temperature.Units>> deserializeBiomeTemps(CompoundTag tag, String key, RegistryAccess registryAccess)
    {
        Map<Biome, Triplet<Double, Double, Temperature.Units>> map = new HashMap<>();
        CompoundTag mapTag = tag.getCompound(key);
        for (String biomeID : mapTag.getAllKeys())
        {
            CompoundTag biomeTag = mapTag.getCompound(biomeID);
            Biome biome = RegistryHelper.getBiome(ResourceLocation.parse(biomeID), registryAccess);
            if (biome == null)
            {   ColdSweat.LOGGER.error("Error deserializing biome temperatures: biome \"{}\" does not exist", biomeID);
                continue;
            }
            map.put(biome, new Triplet<>(biomeTag.getDouble("Min"), biomeTag.getDouble("Max"), Temperature.Units.valueOf(biomeTag.getString("Units"))));
        }
        return map;
    }

    public static CompoundTag serializeDimensionTemps(Map<DimensionType, Pair<Double, Temperature.Units>> map, String key, RegistryAccess registryAccess)
    {
        CompoundTag tag = new CompoundTag();
        CompoundTag mapTag = new CompoundTag();
        for (Map.Entry<DimensionType, Pair<Double, Temperature.Units>> entry : map.entrySet())
        {
            CompoundTag dimensionTag = new CompoundTag();
            ResourceLocation dimensionId = RegistryHelper.getDimensionId(entry.getKey(), registryAccess);
            if (dimensionId == null)
            {   ColdSweat.LOGGER.error("Error serializing dimension temperatures: dimension \"{}\" does not exist", entry.getKey());
                continue;
            }
            mapTag.put(dimensionId.toString(), dimensionTag);
            dimensionTag.putDouble("Temp", entry.getValue().getFirst());
            dimensionTag.putString("Units", entry.getValue().getSecond().toString());
            mapTag.put(dimensionId.toString(), dimensionTag);
        }
        tag.put(key, mapTag);
        return tag;
    }

    public static Map<DimensionType, Pair<Double, Temperature.Units>> deserializeDimensionTemps(CompoundTag tag, String key, RegistryAccess registryAccess)
    {
        Map<DimensionType, Pair<Double, Temperature.Units>> map = new HashMap<>();
        CompoundTag mapTag = tag.getCompound(key);
        for (String dimensionId : mapTag.getAllKeys())
        {
            CompoundTag biomeTag = mapTag.getCompound(dimensionId);
            DimensionType dimension = RegistryHelper.getDimension(ResourceLocation.parse(dimensionId), registryAccess);
            if (dimension == null)
            {   ColdSweat.LOGGER.error("Error deserializing dimension temperatures: dimension \"{}\" does not exist", dimensionId);
                continue;
            }
            map.put(dimension, Pair.of(biomeTag.getDouble("Temp"), Temperature.Units.valueOf(biomeTag.getString("Units"))));
        }
        return map;
    }

    public static CompoundTag serializeStructureTemps(Map<StructureType<?>, Pair<Double, Temperature.Units>> map, String key, RegistryAccess registryAccess)
    {
        CompoundTag tag = new CompoundTag();
        CompoundTag mapTag = new CompoundTag();
        for (Map.Entry<StructureType<?>, Pair<Double, Temperature.Units>> entry : map.entrySet())
        {
            CompoundTag structureTag = new CompoundTag();
            ResourceLocation structureId = RegistryHelper.getStructureId(entry.getKey(), registryAccess);
            if (structureId == null)
            {   ColdSweat.LOGGER.error("Error serializing structure temperatures: structure \"{}\" does not exist", entry.getKey());
                continue;
            }
            mapTag.put(structureId.toString(), structureTag);
            structureTag.putDouble("Temp", entry.getValue().getFirst());
            structureTag.putString("Units", entry.getValue().getSecond().toString());
            mapTag.put(structureId.toString(), structureTag);
        }
        tag.put(key, mapTag);

        return tag;
    }

    public static Map<StructureType<?>, Pair<Double, Temperature.Units>> deserializeStructureTemps(CompoundTag tag, String key, RegistryAccess registryAccess)
    {
        Map<StructureType<?>, Pair<Double, Temperature.Units>> map = new HashMap<>();
        CompoundTag mapTag = tag.getCompound(key);
        for (String structureId : mapTag.getAllKeys())
        {
            CompoundTag biomeTag = mapTag.getCompound(structureId);
            StructureType<?> structure = RegistryHelper.getStructure(ResourceLocation.parse(structureId), registryAccess);
            if (structure == null)
            {   ColdSweat.LOGGER.error("Error deserializing structure temperatures: structure \"{}\" does not exist", structureId);
                continue;
            }
            map.put(structure, Pair.of(biomeTag.getDouble("Temp"), Temperature.Units.valueOf(biomeTag.getString("Units"))));
        }
        return map;
    }

    public static <T> CompoundTag serializeItemMap(Map<Item, T> map, String key, Function<T, CompoundTag> serializer)
    {
        CompoundTag tag = new CompoundTag();
        CompoundTag mapTag = new CompoundTag();
        for (Map.Entry<Item, T> entry : map.entrySet())
        {
            if (!BuiltInRegistries.ITEM.containsValue(entry.getKey()))
            {
                ColdSweat.LOGGER.error("Error serializing item map: item \"{}\" does not exist", entry.getKey());
                continue;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(entry.getKey());
            mapTag.put(itemId.toString(), serializer.apply(entry.getValue()));
        }
        tag.put(key, mapTag);

        return tag;
    }

    public static <T> CompoundTag serializeItemMultimap(Multimap<Item, T> map, String key, Function<T, CompoundTag> serializer)
    {
        CompoundTag tag = new CompoundTag();
        ListTag mapTag = new ListTag();
        for (Map.Entry<Item, T> entry : map.entries())
        {
            CompoundTag entryTag = new CompoundTag();
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(entry.getKey());
            if (itemId == null)
            {
                ColdSweat.LOGGER.error("Error serializing item map: item \"{}\" does not exist", entry.getKey());
                continue;
            }
            entryTag.putString("Item", itemId.toString());
            entryTag.put("Value", serializer.apply(entry.getValue()));
            mapTag.add(entryTag);
        }
        tag.put(key, mapTag);

        return tag;
    }

    public static <T> Map<Item, T> deserializeItemMap(CompoundTag tag, String key, Function<CompoundTag, T> deserializer)
    {
        Map<Item, T> map = new HashMap<>();
        CompoundTag mapTag = tag.getCompound(key);
        for (String itemID : mapTag.getAllKeys())
        {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemID));
            T value = deserializer.apply(mapTag.getCompound(itemID));
            if (value != null)
            {   map.put(item, value);
            }
        }
        return map;
    }

    public static <T> Multimap<Item, T> deserializeItemMultimap(CompoundTag tag, String key, Function<CompoundTag, T> deserializer)
    {
        Multimap<Item, T> map = new FastMultiMap<>();
        ListTag mapTag = tag.getList(key, 10);
        for (int i = 0; i < mapTag.size(); i++)
        {
            CompoundTag entryTag = mapTag.getCompound(i);
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entryTag.getString("Item")));
            T value = deserializer.apply(entryTag.getCompound("Value"));
            if (value != null)
            {   map.put(item, value);
            }
        }
        return map;
    }

    public static <T> Map<Item, T> readItemMap(List<? extends List<?>> source, BiFunction<Item, List<?>, T> valueParser)
    {   return readItemMapLike(source, valueParser).getFirst();
    }

    public static <T> Multimap<Item, T> readItemMultimap(List<? extends List<?>> source, BiFunction<Item, List<?>, T> valueParser)
    {   return readItemMapLike(source, valueParser).getSecond();
    }

    private static <T> Pair<Map<Item, T>, Multimap<Item, T>> readItemMapLike(List<? extends List<?>> source, BiFunction<Item, List<?>, T> valueParser)
    {
        Map<Item, T> map = new HashMap<>();
        Multimap<Item, T> multimap = new FastMultiMap<>();
        for (List<?> entry : source)
        {
            String itemId = (String) entry.get(0);
            for (Item item : getItems(itemId))
            {
                T value = valueParser.apply(item, entry.subList(1, entry.size()));
                if (value != null)
                {   map.put(item, value);
                    multimap.put(item, value);
                }
            }
        }
        return Pair.of(map, multimap);
    }

    public static <T> void writeItemMap(Map<Item, T> map, Consumer<List<? extends List<?>>> saver, Function<T, List<?>> valueWriter)
    {   writeItemMapLike(Either.left(map), saver, valueWriter);
    }

    public static <T> void writeItemMultimap(Multimap<Item, T> map, Consumer<List<? extends List<?>>> saver, Function<T, List<?>> valueWriter)
    {   writeItemMapLike(Either.right(map), saver, valueWriter);
    }

    private static <T> void writeItemMapLike(Either<Map<Item, T>, Multimap<Item, T>> map, Consumer<List<? extends List<?>>> saver, Function<T, List<?>> valueWriter)
    {
        List<List<?>> list = new ArrayList<>();
        for (Map.Entry<Item, T> entry : map.map(Map::entrySet, Multimap::entries))
        {
            Item item = entry.getKey();
            T value = entry.getValue();
            List<Object> itemData = new ArrayList<>();
            if (!BuiltInRegistries.ITEM.containsValue(item))
            {   ColdSweat.LOGGER.error("Error writing item map: item \"{}\" does not exist", item);
                continue;
            }
            ResourceLocation itemID = BuiltInRegistries.ITEM.getKey(item);
            itemData.add(itemID.toString());
            List<?> args = valueWriter.apply(value);
            if (args == null) continue;
            itemData.addAll(args);
            list.add(itemData);
        }
        saver.accept(list);
    }

    public static CompoundTag serializeItemInsulations(Multimap<Item, Insulator> map, String key)
    {
        CompoundTag tag = new CompoundTag();
        ListTag mapTag = new ListTag();
        for (Map.Entry<Item, Insulator> entry : map.entries())
        {
            Item item = entry.getKey();
            Insulator insulator = entry.getValue();
            if (!BuiltInRegistries.ITEM.containsValue(item))
            {   ColdSweat.LOGGER.error("Error serializing item insulations: item \"{}\" does not exist", item);
                continue;
            }
            ResourceLocation itemID = BuiltInRegistries.ITEM.getKey(item);
            if (insulator == null)
            {   ColdSweat.LOGGER.error("Error serializing item insulations: insulation value for item \"{}\" is null", item);
                continue;
            }

            CompoundTag insulatorTag = new CompoundTag();
            insulatorTag.put("Insulator", insulator.serialize());
            insulatorTag.putString("Item", itemID.toString());

            mapTag.add(insulatorTag);
        }
        tag.put(key, mapTag);

        return tag;
    }

    public static Multimap<Item, Insulator> deserializeItemInsulations(CompoundTag tag, String key)
    {
        Multimap<Item, Insulator> map = new FastMultiMap<>();
        ListTag mapTag = tag.getList(key, 10);
        for (int i = 0; i < mapTag.size(); i++)
        {
            CompoundTag insulatorTag = mapTag.getCompound(i);
            String itemID = insulatorTag.getString("Item");
            ResourceLocation itemLocation = ResourceLocation.parse(itemID);
            if (!BuiltInRegistries.ITEM.containsKey(itemLocation))
            {   ColdSweat.LOGGER.error("Error deserializing item insulations: item \"{}\" does not exist", itemID);
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(itemLocation);
            Insulator insulator = Insulator.deserialize(insulatorTag.getCompound("Insulator"));

            if (item != null && insulator != null)
            {   map.put(item, insulator);
            }
        }
        return map;
    }

    public static Multimap<Item, Insulator> readItemInsulations(List<? extends List<?>> items, Insulation.Slot slot)
    {
        return readItemMultimap(items, (item, args) ->
        {
            if (args.size() < 2)
            {   ColdSweat.LOGGER.error(new ArgumentCountException(args.size(), 2, String.format("Error parsing insulation config for item %s", item)).getMessage());
                return null;
            }
            double value1 = ((Number) args.get(0)).doubleValue();
            double value2 = ((Number) args.get(1)).doubleValue();
            String type = args.size() > 2 ? (String) args.get(2) : "static";
            ItemComponentsRequirement components = args.size() > 3 ? ItemComponentsRequirement.parse(((String) args.get(3))) : new ItemComponentsRequirement();
            Insulation insulation = type.equals("static")
                                    ? new StaticInsulation(value1, value2)
                                    : new AdaptiveInsulation(value1, value2);
            ItemRequirement requirement = new ItemRequirement(Optional.of(List.of(Either.right(item))),
                                                              Optional.empty(), Optional.empty(),
                                                              Optional.empty(), Optional.empty(),
                                                              Optional.empty(), Optional.empty(),
                                                              components);

            return new Insulator(insulation, slot, requirement, EntityRequirement.NONE, new AttributeModifierMap(), new HashMap<>());
        });
    }

    public static void writeItemInsulations(Multimap<Item, Insulator> items, Consumer<List<? extends List<?>>> saver)
    {
        writeItemMultimap(items, saver, insulator ->
        {
            if (insulator == null)
            {   ColdSweat.LOGGER.error("Error writing item insulations: insulator value is null");
                return List.of();
            }
            if (!insulator.predicate().equals(EntityRequirement.NONE) || !insulator.attributes().getMap().isEmpty())
            {   return List.of();
            }
            List<Object> itemData = new ArrayList<>();
            itemData.add(insulator.insulation() instanceof StaticInsulation
                         ? insulator.insulation().getCold()
                         : ((AdaptiveInsulation) insulator.insulation()).getInsulation());
            itemData.add(insulator.insulation() instanceof StaticInsulation
                         ? insulator.insulation().getHeat()
                         : ((AdaptiveInsulation) insulator.insulation()).getSpeed());
            itemData.add(insulator.insulation() instanceof StaticInsulation
                         ? "static"
                         : "adaptive");
            itemData.add(insulator.data().components().serialize().toString());

            return itemData;
        });
    }

    public static <T> Codec<Either<TagKey<T>, T>> tagOrBuiltinCodec(ResourceKey<Registry<T>> vanillaRegistry, DefaultedRegistry<T> forgeRegistry)
    {
        return Codec.either(Codec.STRING.comapFlatMap(str ->
                                                      {
                                                          if (!str.startsWith("#"))
                                                          {   return DataResult.<TagKey<T>>error(() -> String.format("Not a tag key for builtin registry %s: %s", vanillaRegistry.location(), str));
                                                          }
                                                          ResourceLocation itemLocation = ResourceLocation.parse(str.replace("#", ""));
                                                          return DataResult.success(TagKey.create(vanillaRegistry, itemLocation));
                                                      },
                                                      key -> "#" + key.location()),
                            Codec.STRING.comapFlatMap(str ->
                                                      {
                                                          ResourceLocation itemLocation = ResourceLocation.parse(str);
                                                          Optional<T> obj = forgeRegistry.getOptional(itemLocation);
                                                          if (obj.isEmpty())
                                                          {
                                                              if (CompatManager.modLoaded(itemLocation.getNamespace()))
                                                              {
                                                                  ColdSweat.LOGGER.error("Error deserializing config: object \"{}\" does not exist", str);
                                                                  return DataResult.error(() -> "Object does not exist");
                                                              }
                                                              else return DataResult.success(forgeRegistry.get(forgeRegistry.getDefaultKey()));
                                                          }
                                                          return DataResult.success(obj.get());
                                                      },
                                                      obj ->
                                                      {
                                                          ResourceLocation itemLocation = forgeRegistry.getKey(obj);
                                                          return itemLocation.toString();
                                                      }));
    }

    public static <T> Codec<Either<TagKey<T>, Holder<T>>> tagOrHolderCodec(ResourceKey<Registry<T>> vanillaRegistry)
    {
        return Codec.either(Codec.STRING.comapFlatMap(str ->
                                                      {
                                                          if (!str.startsWith("#"))
                                                          {   return DataResult.error(() -> String.format("Not a tag key for dynamic holder registry %s: %s", vanillaRegistry.location(), str));
                                                          }
                                                          ResourceLocation itemLocation = ResourceLocation.parse(str.replace("#", ""));
                                                          return DataResult.success(TagKey.create(vanillaRegistry, itemLocation));
                                                      },
                                                      key -> "#" + key.location()),
                            Codec.STRING.comapFlatMap(str ->
                                                      {
                                                          RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();
                                                          if (registryAccess == null)
                                                          {   ColdSweat.LOGGER.error("Error deserializing config: registry access is null");
                                                              return DataResult.error(() -> "Registry access is null");
                                                          }
                                                          ResourceLocation itemLocation = ResourceLocation.parse(str);
                                                          Registry<T> registry = registryAccess.registry(vanillaRegistry).orElse(null);
                                                          if (registry == null)
                                                          {   ColdSweat.LOGGER.error("Error deserializing config: registry \"{}\" does not exist", vanillaRegistry.location());
                                                              return DataResult.error(() -> "Registry does not exist");
                                                          }
                                                          Optional<Holder.Reference<T>> holder = registry.getHolder(itemLocation);
                                                          if (holder.isEmpty())
                                                          {
                                                              if (CompatManager.modLoaded(itemLocation.getNamespace()))
                                                              {
                                                                  ColdSweat.LOGGER.error("Error deserializing config: object \"{}\" does not exist", str);
                                                                  return DataResult.error(() -> "Object does not exist");
                                                              }
                                                              else return DataResult.success(Holder.Reference.createIntrusive(new HolderOwner<>(){}, registry.stream().findFirst().get()));
                                                          }
                                                          return DataResult.success(holder.get());
                                                      },
                                                      holder ->
                                                      {
                                                          RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();
                                                          if (registryAccess == null)
                                                          {   ColdSweat.LOGGER.error("Error serializing config: registry access is null");
                                                              return "null";
                                                          }
                                                          Registry<T> registry = registryAccess.registry(vanillaRegistry).orElse(null);
                                                          if (registry == null)
                                                          {   ColdSweat.LOGGER.error("Error serializing config: registry \"{}\" does not exist", vanillaRegistry.location());
                                                              return "null";
                                                          }
                                                          return registry.getKey(holder.value()).toString();
                                                      }));
    }

    public static <T> Codec<Either<TagKey<T>, Holder<T>>> tagOrBuiltinHolderCodec(ResourceKey<Registry<T>> vanillaRegistry, Registry<T> registry)
    {
        return Codec.either(Codec.STRING.comapFlatMap(str ->
                                                      {
                                                          if (!str.startsWith("#"))
                                                          {   return DataResult.error(() -> String.format("Not a tag key for builtin holder registry %s: %s", vanillaRegistry.location(), str));
                                                          }
                                                          ResourceLocation itemLocation = ResourceLocation.parse(str.replace("#", ""));
                                                          return DataResult.success(TagKey.create(vanillaRegistry, itemLocation));
                                                      },
                                                      key -> "#" + key.location()),
                            Codec.STRING.comapFlatMap(str ->
                                                      {
                                                          ResourceLocation itemLocation = ResourceLocation.parse(str);
                                                          Optional<Holder.Reference<T>> holder = registry.getHolder(itemLocation);
                                                          if (holder.isEmpty())
                                                          {
                                                              if (CompatManager.modLoaded(itemLocation.getNamespace()))
                                                              {
                                                                  ColdSweat.LOGGER.error("Error deserializing config: object \"{}\" does not exist", str);
                                                                  return DataResult.error(() -> "Object does not exist");
                                                              }
                                                              else return DataResult.success(Holder.Reference.createIntrusive(new HolderOwner<>(){}, registry.stream().findFirst().get()));
                                                          }
                                                          return DataResult.success(registry.getHolder(itemLocation).get());
                                                      },
                                                      holder ->
                                                      {
                                                          ResourceLocation itemLocation = registry.getKey(holder.value());
                                                          return itemLocation.toString();
                                                      }));
    }

    public static <T> Codec<Either<TagKey<T>, T>> tagOrVanillaRegistryCodec(ResourceKey<Registry<T>> vanillaRegistry, Codec<Holder<T>> codec)
    {
        return Codec.either(Codec.STRING.comapFlatMap(str ->
                                                      {
                                                          if (!str.startsWith("#"))
                                                          {   return DataResult.error(() -> "Not a tag key: " + str);
                                                          }
                                                          ResourceLocation itemLocation = ResourceLocation.parse(str.replace("#", ""));
                                                          return DataResult.success(TagKey.create(vanillaRegistry, itemLocation));
                                                      },
                                                      key -> "#" + key.location()),
                            codec.xmap(Holder::value, Holder::direct));
    }

    public static <T> Codec<Either<TagKey<T>, ResourceKey<T>>> tagOrResourceKeyCodec(ResourceKey<Registry<T>> vanillaRegistry)
    {
        return Codec.either(Codec.STRING.comapFlatMap(str ->
                                                      {
                                                          if (!str.startsWith("#"))
                                                          {   return DataResult.error(() -> String.format("Not a tag key for dynamic resource registry %s: %s", vanillaRegistry.location(), str));
                                                          }
                                                          ResourceLocation itemLocation = ResourceLocation.parse(str.replace("#", ""));
                                                          return DataResult.success(TagKey.create(vanillaRegistry, itemLocation));
                                                      },
                                                      key -> "#" + key.location()),
                            ResourceKey.codec(vanillaRegistry));
    }

    public static <T> String serializeTagOrResourceKey(Either<TagKey<T>, ResourceKey<T>> obj)
    {
        return obj.map(tag -> "#" + tag.location(),
                       key -> key.location().toString());
    }

    public static <T> String serializeTagOrBuiltin(Registry<T> forgeRegistry, Either<TagKey<T>, T> obj)
    {
        return obj.map(tag -> "#" + tag.location(),
                       regObj -> forgeRegistry.getKey(regObj).toString());
    }

    public static <T> String serializeTagOrRegistryObject(ResourceKey<Registry<T>> registry, Either<TagKey<T>, Holder<T>> obj)
    {
        RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();
        if (registryAccess == null)
        {   ColdSweat.LOGGER.error("Error serializing config: registry access is null");
            return null;
        }
        Registry<T> reg = registryAccess.registry(registry).orElse(null);
        return obj.map(tag -> "#" + tag.location(),
                       regObj -> reg.getKey(regObj.value()).toString());
    }

    public static <T> Either<TagKey<T>, ResourceKey<T>> deserializeTagOrResourceKey(ResourceKey<Registry<T>> registry, String key)
    {
        if (key.startsWith("#"))
        {
            ResourceLocation tagID = ResourceLocation.parse(key.replace("#", ""));
            return Either.left(TagKey.create(registry, tagID));
        }
        else
        {
            ResourceKey<T> biomeKey = ResourceKey.create(registry, ResourceLocation.parse(key));
            return Either.right(biomeKey);
        }
    }

    public static <T> Either<TagKey<T>, T> deserializeTagOrBuiltin(String tagOrRegistryObject, ResourceKey<Registry<T>> vanillaRegistry, Registry<T> forgeRegistry)
    {
        if (tagOrRegistryObject.startsWith("#"))
        {
            ResourceLocation tagID = ResourceLocation.parse(tagOrRegistryObject.replace("#", ""));
            return Either.left(TagKey.create(vanillaRegistry, tagID));
        }
        else
        {
            ResourceLocation id = ResourceLocation.parse(tagOrRegistryObject);
            T obj = forgeRegistry.get(id);
            if (obj == null)
            {   ColdSweat.LOGGER.error("Error deserializing config: object \"{}\" does not exist", tagOrRegistryObject);
                return null;
            }
            return Either.right(obj);
        }
    }

    public static <T> Either<TagKey<T>, Holder<T>> deserializeTagOrRegistryObject(String tagOrRegistryObject, ResourceKey<Registry<T>> vanillaRegistry)
    {
        if (tagOrRegistryObject.startsWith("#"))
        {
            ResourceLocation tagID = ResourceLocation.parse(tagOrRegistryObject.replace("#", ""));
            return Either.left(TagKey.create(vanillaRegistry, tagID));
        }
        else
        {
            RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();
            if (registryAccess == null)
            {   ColdSweat.LOGGER.error("Error deserializing config: registry access is null");
                return null;
            }
            Registry<T> reg = registryAccess.registry(vanillaRegistry).orElse(null);
            if (reg == null)
            {   ColdSweat.LOGGER.error("Error deserializing config: registry \"{}\" does not exist", vanillaRegistry.location());
                return null;
            }
            ResourceLocation id = ResourceLocation.parse(tagOrRegistryObject);
            Holder<T> obj = reg.getHolder(id).orElse(null);
            if (obj == null)
            {   ColdSweat.LOGGER.error("Error deserializing config: object \"{}\" does not exist", tagOrRegistryObject);
                return null;
            }
            return Either.right(obj);
        }
    }

    public static Optional<PredicateItem> findFirstItemMatching(DynamicHolder<Multimap<Item, PredicateItem>> predicates, ItemStack stack)
    {
        for (PredicateItem predicate : predicates.get().get(stack.getItem()))
        {
            if (predicate.test(stack))
            {   return Optional.of(predicate);
            }
        }
        return Optional.empty();
    }

    public static <T> Optional<T> parseResource(ResourceManager resourceManager, ResourceLocation location, Codec<T> codec)
    {
        if (resourceManager == null)
        {
            return Optional.empty();
        }
        try
        {
            Resource resource = resourceManager.getResource(location).orElseThrow();
            try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8))
            {
                JsonObject json = GsonHelper.parse(reader);
                return codec.parse(JsonOps.INSTANCE, json).result();
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to load JSON file: " + location, e);
        }
    }
}
