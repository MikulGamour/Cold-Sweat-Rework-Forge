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
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.NbtRequirement;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import com.momosoftworks.coldsweat.util.exceptions.ArgumentCountException;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.block.Block;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.state.Property;
import net.minecraft.tags.*;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistryEntry;

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

    public static <T> List<T> parseRegistryItems(RegistryKey<Registry<T>> registry, DynamicRegistries registryAccess, String objects)
    {
        List<T> parsedObjects = new ArrayList<>();
        Optional<MutableRegistry<T>> optReg = registryAccess.registry(registry);
        if (!optReg.isPresent()) return parsedObjects;

        Registry<T> reg = optReg.get();

        for (String objString : objects.split(","))
        {
            if (objString.startsWith("#"))
            {
                ITagCollection<T> tags = getTagsForRegistry(registry);
                if (tags == null) continue;

                final String tagID = objString.replace("#", "");
                ITag<T> itemTag = tags.getTag(new ResourceLocation(tagID));
                if (itemTag != null)
                {   parsedObjects.addAll(itemTag.getValues());
                }
            }
            else
            {
                ResourceLocation id = new ResourceLocation(objString);
                Optional<T> obj = Optional.ofNullable(reg.get(id));
                if (!obj.isPresent())
                {
                    ColdSweat.LOGGER.error("Error parsing config: \"{}\" does not exist", objString);
                    continue;
                }
                parsedObjects.add(obj.get());
            }
        }
        return parsedObjects;
    }

    private static <T>ITagCollection<T> getTagsForRegistry(RegistryKey<Registry<T>> registry)
    {
        if (registry.equals(Registry.ITEM_REGISTRY))
        {   return ((ITagCollection<T>) ItemTags.getAllTags());
        }
        else if (registry.equals(Registry.BLOCK_REGISTRY))
        {   return ((ITagCollection<T>) BlockTags.getAllTags());
        }
        else if (registry.equals(Registry.FLUID_REGISTRY))
        {   return ((ITagCollection<T>) FluidTags.getAllTags());
        }
        else if (registry.equals(Registry.ENTITY_TYPE_REGISTRY))
        {   return ((ITagCollection<T>) EntityTypeTags.getAllTags());
        }
        return null;
    }

    public static List<Block> getBlocks(String... ids)
    {
        List<Block> blocks = new ArrayList<>();
        for (String id : ids)
        {
            if (id.startsWith("#"))
            {
                final String tagID = id.replace("#", "");
                ITag<Block> blockTag = BlockTags.getAllTags().getTag(new ResourceLocation(tagID));
                if (blockTag != null)
                {   blocks.addAll(blockTag.getValues());
                }
            }
            else
            {
                ResourceLocation blockId = new ResourceLocation(id);
                if (ForgeRegistries.BLOCKS.containsKey(blockId))
                {   blocks.add(ForgeRegistries.BLOCKS.getValue(blockId));
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
                ITag<Item> itemTag = ItemTags.getAllTags().getTag(new ResourceLocation(tagID));
                if (itemTag != null)
                {   items.addAll(itemTag.getValues());
                }
            }
            else
            {
                ResourceLocation itemID = new ResourceLocation(itemId);
                if (ForgeRegistries.ITEMS.containsKey(itemID))
                {   items.add(ForgeRegistries.ITEMS.getValue(itemID));
                }
                else
                {   ColdSweat.LOGGER.error("Error parsing item config: item \"{}\" does not exist", itemId);
                }
            }
        }
        return items;
    }

    public static Map<Biome, Triplet<Double, Double, Temperature.Units>> getBiomesWithValues(List<? extends List<?>> source, boolean absolute, DynamicRegistries registryAccess)
    {
        Map<Biome, Triplet<Double, Double, Temperature.Units>> map = new HashMap<>();
        for (List<?> entry : source)
        {
            try
            {
                String biomeIdString = (String) entry.get(0);
                for (Biome biome : parseRegistryItems(Registry.BIOME_REGISTRY, registryAccess, biomeIdString))
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
                        double variance = 1 / Math.max(1, 2 + biome.getDownfall() * 2);
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

    public static Map<DimensionType, Pair<Double, Temperature.Units>> getDimensionsWithValues(List<? extends List<?>> source, boolean absolute, DynamicRegistries registryAccess)
    {
        Map<DimensionType, Pair<Double, Temperature.Units>> map = new HashMap<>();
        for (List<?> entry : source)
        {
            try
            {
                String dimensionIdString = (String) entry.get(0);
                for (DimensionType dimension : parseRegistryItems(Registry.DIMENSION_TYPE_REGISTRY, registryAccess, dimensionIdString))
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

    public static Map<Structure<?>, Pair<Double, Temperature.Units>> getStructuresWithValues(List<? extends List<?>> source, boolean absolute, DynamicRegistries registryAccess)
    {
        Map<Structure<?>, Pair<Double, Temperature.Units>> map = new HashMap<>();
        for (List<?> entry : source)
        {
            try
            {
                String structureIdString = (String) entry.get(0);
                for (Structure<?> structure : parseRegistryItems(Registry.STRUCTURE_FEATURE_REGISTRY, registryAccess, structureIdString))
                {
                    if (structure == null)
                    {   ColdSweat.LOGGER.error("Error parsing structure config: string \"{}\" contains a structure that does not exist or is not loaded yet", structureIdString);
                        continue;
                    }
                    double temp = ((Number) entry.get(1)).doubleValue();
                    Temperature.Units units = entry.size() == 3 ? Temperature.Units.valueOf(((String) entry.get(2)).toUpperCase()) : Temperature.Units.MC;
                    map.put(structure, Pair.of(Temperature.convert(temp, units, Temperature.Units.MC, absolute), units));
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
                CSMath.doIfNotNull(EntityTypeTags.getAllTags().getTag(new ResourceLocation(tagID)), tag ->
                {
                    entityList.addAll(tag.getValues());
                });
            }
            else
            {
                ResourceLocation entityId = new ResourceLocation(entity);
                if (ForgeRegistries.ENTITIES.containsKey(entityId))
                {   entityList.add(ForgeRegistries.ENTITIES.getValue(entityId));
                }
                else
                {   ColdSweat.LOGGER.error("Error parsing entity config: entity \"{}\" does not exist", entity);
                }
            }
        }
        return entityList;
    }

    public static CompoundNBT serializeNbtBool(boolean value, String key)
    {
        CompoundNBT tag = new CompoundNBT();
        tag.putBoolean(key, value);
        return tag;
    }

    public static CompoundNBT serializeNbtInt(int value, String key)
    {
        CompoundNBT tag = new CompoundNBT();
        tag.putInt(key, value);
        return tag;
    }

    public static CompoundNBT serializeNbtDouble(double value, String key)
    {
        CompoundNBT tag = new CompoundNBT();
        tag.putDouble(key, value);
        return tag;
    }

    public static CompoundNBT serializeNbtString(String value, String key)
    {
        CompoundNBT tag = new CompoundNBT();
        tag.putString(key, value);
        return tag;
    }

    public static CompoundNBT serializeBiomeTemps(Map<Biome, Triplet<Double, Double, Temperature.Units>> map, String key, DynamicRegistries registryAccess)
    {
        CompoundNBT tag = new CompoundNBT();
        CompoundNBT mapTag = new CompoundNBT();
        for (Map.Entry<Biome, Triplet<Double, Double, Temperature.Units>> entry : map.entrySet())
        {
            CompoundNBT biomeTag = new CompoundNBT();
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

    public static Map<Biome, Triplet<Double, Double, Temperature.Units>> deserializeBiomeTemps(CompoundNBT tag, String key, DynamicRegistries registryAccess)
    {
        Map<Biome, Triplet<Double, Double, Temperature.Units>> map = new HashMap<>();
        CompoundNBT mapTag = tag.getCompound(key);
        for (String biomeID : mapTag.getAllKeys())
        {
            CompoundNBT biomeTag = mapTag.getCompound(biomeID);
            Biome biome = RegistryHelper.getBiome(new ResourceLocation(biomeID), registryAccess);
            if (biome == null)
            {   ColdSweat.LOGGER.error("Error deserializing biome temperatures: biome \"{}\" does not exist", biomeID);
                continue;
            }
            map.put(biome, new Triplet<>(biomeTag.getDouble("Min"), biomeTag.getDouble("Max"), Temperature.Units.valueOf(biomeTag.getString("Units"))));
        }
        return map;
    }

    public static CompoundNBT serializeDimensionTemps(Map<DimensionType, Pair<Double, Temperature.Units>> map, String key, DynamicRegistries registryAccess)
    {
        CompoundNBT tag = new CompoundNBT();
        CompoundNBT mapTag = new CompoundNBT();
        for (Map.Entry<DimensionType, Pair<Double, Temperature.Units>> entry : map.entrySet())
        {
            CompoundNBT dimensionTag = new CompoundNBT();
            ResourceLocation dimensionId = RegistryHelper.getDimensionId(entry.getKey(), registryAccess);
            if (dimensionId == null)
            {   ColdSweat.LOGGER.error("Error serializing dimension temperatures: dimension \"{}\" does not exist", entry.getKey());
                continue;
            }
            mapTag.put(dimensionId.toString(), dimensionTag);
            dimensionTag.putDouble("Temp", entry.getValue().getFirst());
            dimensionTag.putString("Units", entry.getValue().getSecond().toString());
            mapTag.put(RegistryHelper.getDimensionId(entry.getKey(), registryAccess).toString(), dimensionTag);
        }
        tag.put(key, mapTag);
        return tag;
    }

    public static Map<DimensionType, Pair<Double, Temperature.Units>> deserializeDimensionTemps(CompoundNBT tag, String key, DynamicRegistries registryAccess)
    {
        Map<DimensionType, Pair<Double, Temperature.Units>> map = new HashMap<>();
        CompoundNBT mapTag = tag.getCompound(key);
        for (String dimensionId : mapTag.getAllKeys())
        {
            CompoundNBT biomeTag = mapTag.getCompound(dimensionId);
            DimensionType dimension = RegistryHelper.getDimension(new ResourceLocation(dimensionId), registryAccess);
            if (dimension == null)
            {   ColdSweat.LOGGER.error("Error deserializing dimension temperatures: dimension \"{}\" does not exist", dimensionId);
                continue;
            }
            map.put(dimension, Pair.of(biomeTag.getDouble("Temp"), Temperature.Units.valueOf(biomeTag.getString("Units"))));
        }
        return map;
    }

    public static CompoundNBT serializeStructureTemps(Map<Structure<?>, Pair<Double, Temperature.Units>> map, String key, DynamicRegistries registryAccess)
    {
        CompoundNBT tag = new CompoundNBT();
        CompoundNBT mapTag = new CompoundNBT();
        for (Map.Entry<Structure<?>, Pair<Double, Temperature.Units>> entry : map.entrySet())
        {
            CompoundNBT structureTag = new CompoundNBT();
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

    public static Map<Structure<?>, Pair<Double, Temperature.Units>> deserializeStructureTemps(CompoundNBT tag, String key, DynamicRegistries registryAccess)
    {
        Map<Structure<?>, Pair<Double, Temperature.Units>> map = new HashMap<>();
        CompoundNBT mapTag = tag.getCompound(key);
        for (String structureId : mapTag.getAllKeys())
        {
            CompoundNBT biomeTag = mapTag.getCompound(structureId);
            Structure<?> structure = RegistryHelper.getStructure(new ResourceLocation(structureId), registryAccess);
            if (structure == null)
            {   ColdSweat.LOGGER.error("Error deserializing structure temperatures: structure \"{}\" does not exist", structureId);
                continue;
            }
            map.put(structure, Pair.of(biomeTag.getDouble("Temp"), Temperature.Units.valueOf(biomeTag.getString("Units"))));
        }
        return map;
    }

    public static <T> CompoundNBT serializeItemMap(Map<Item, T> map, String key, Function<T, CompoundNBT> serializer)
    {
        CompoundNBT tag = new CompoundNBT();
        CompoundNBT mapTag = new CompoundNBT();
        for (Map.Entry<Item, T> entry : map.entrySet())
        {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(entry.getKey());
            if (itemId == null) continue;
            mapTag.put(itemId.toString(), serializer.apply(entry.getValue()));
        }
        tag.put(key, mapTag);

        return tag;
    }

    public static <T> CompoundNBT serializeItemMultimap(Multimap<Item, T> map, String key, Function<T, CompoundNBT> serializer)
    {
        CompoundNBT tag = new CompoundNBT();
        ListNBT mapTag = new ListNBT();
        for (Map.Entry<Item, T> entry : map.entries())
        {
            CompoundNBT entryTag = new CompoundNBT();
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(entry.getKey());
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

    public static <T> Map<Item, T> deserializeItemMap(CompoundNBT tag, String key, Function<CompoundNBT, T> deserializer)
    {
        Map<Item, T> map = new HashMap<>();
        CompoundNBT mapTag = tag.getCompound(key);
        for (String itemID : mapTag.getAllKeys())
        {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemID));
            T value = deserializer.apply(mapTag.getCompound(itemID));
            if (value != null)
            {   map.put(item, value);
            }
        }
        return map;
    }

    public static <T> Multimap<Item, T> deserializeItemMultimap(CompoundNBT tag, String key, Function<CompoundNBT, T> deserializer)
    {
        Multimap<Item, T> map = new FastMultiMap<>();
        ListNBT mapTag = tag.getList(key, 10);
        for (int i = 0; i < mapTag.size(); i++)
        {
            CompoundNBT entryTag = mapTag.getCompound(i);
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(entryTag.getString("Item")));
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
            ResourceLocation itemID = ForgeRegistries.ITEMS.getKey(item);
            if (itemID == null)
            {   ColdSweat.LOGGER.error("Error writing item map: item \"{}\" does not exist", item);
                continue;
            }
            itemData.add(itemID.toString());
            List<?> args = valueWriter.apply(value);
            if (args == null) continue;
            itemData.addAll(args);
            list.add(itemData);
        }
        saver.accept(list);
    }

    public static CompoundNBT serializeItemInsulations(Multimap<Item, Insulator> map, String key)
    {
        CompoundNBT tag = new CompoundNBT();
        ListNBT mapTag = new ListNBT();
        for (Map.Entry<Item, Insulator> entry : map.entries())
        {
            Item item = entry.getKey();
            Insulator insulator = entry.getValue();
            ResourceLocation itemID = ForgeRegistries.ITEMS.getKey(item);

            if (itemID == null)
            {   ColdSweat.LOGGER.error("Error serializing item insulations: item \"{}\" does not exist", item);
                continue;
            }
            if (insulator == null)
            {   ColdSweat.LOGGER.error("Error serializing item insulations: insulation value for item \"{}\" is null", item);
                continue;
            }

            CompoundNBT insulatorTag = new CompoundNBT();
            insulatorTag.put("Insulator", insulator.serialize());
            insulatorTag.putString("Item", itemID.toString());

            mapTag.add(insulatorTag);
        }
        tag.put(key, mapTag);

        return tag;
    }

    public static Multimap<Item, Insulator> deserializeItemInsulations(CompoundNBT tag, String key)
    {
        Multimap<Item, Insulator> map = new FastMultiMap<>();
        ListNBT mapTag = tag.getList(key, 10);
        for (int i = 0; i < mapTag.size(); i++)
        {
            CompoundNBT insulatorTag = mapTag.getCompound(i);
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(insulatorTag.getString("Item")));
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
            CompoundNBT nbt = args.size() > 3 ? NBTHelper.parseCompoundNbt(((String) args.get(3))) : new CompoundNBT();
            Insulation insulation = type.equals("static")
                                    ? new StaticInsulation(value1, value2)
                                    : new AdaptiveInsulation(value1, value2);
            ItemRequirement requirement = new ItemRequirement(Optional.of(Arrays.asList(Either.right(item))),
                                                              Optional.empty(), Optional.empty(),
                                                              Optional.empty(), Optional.empty(),
                                                              Optional.empty(), Optional.empty(),
                                                              new NbtRequirement(nbt));

            return new Insulator(insulation, slot, requirement, EntityRequirement.NONE, new AttributeModifierMap(), new HashMap<>());
        });
    }

    public static void writeItemInsulations(Multimap<Item, Insulator> items, Consumer<List<? extends List<?>>> saver)
    {
        writeItemMultimap(items, saver, insulator ->
        {
            if (insulator == null)
            {   ColdSweat.LOGGER.error("Error writing item insulations: insulator value is null");
                return new ArrayList<>();
            }
            if (!insulator.predicate.equals(EntityRequirement.NONE) || !insulator.attributes.getMap().isEmpty())
            {   return new ArrayList<>();
            }
            List<Object> itemData = new ArrayList<>();
            itemData.add(insulator.insulation instanceof StaticInsulation
                         ? insulator.insulation.getCold()
                         : ((AdaptiveInsulation) insulator.insulation).getInsulation());
            itemData.add(insulator.insulation instanceof StaticInsulation
                         ? insulator.insulation.getHeat()
                         : ((AdaptiveInsulation) insulator.insulation).getSpeed());
            itemData.add(insulator.insulation instanceof StaticInsulation
                         ? "static"
                         : "adaptive");
            itemData.add(insulator.data.nbt.tag.toString());

            return itemData;
        });
    }

    public static <T extends IForgeRegistryEntry<T>> Codec<Either<ITag<T>, T>> tagOrBuiltinCodec(RegistryKey<Registry<T>> vanillaRegistry, Registry<T> forgeRegistry)
    {
        ITagCollection<T> vanillaTags = ConfigHelper.getTagsForRegistry(vanillaRegistry);
        return Codec.either(Codec.STRING.comapFlatMap(str ->
                                                      {
                                                          if (!str.startsWith("#"))
                                                          {   return DataResult.error("Not a tag key: " + str);
                                                          }
                                                          ResourceLocation itemLocation = new ResourceLocation(str.replace("#", ""));
                                                          return DataResult.success(vanillaTags.getTag(itemLocation));
                                                      },
                                                      key -> "#" + vanillaTags.getId(key)),
                            forgeRegistry);
    }

    public static <T> Codec<T> dynamicCodec(RegistryKey<Registry<T>> vanillaRegistry)
    {
        DynamicRegistries registryAccess = WorldHelper.getServer().registryAccess();
        Registry<T> registry = registryAccess.registry(vanillaRegistry).get();
        return Codec.STRING.xmap(str -> registry.get(new ResourceLocation(str)),
                                 item -> registry.getKey(item).toString());
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

    public static <T> Optional<T> parseResource(IResourceManager resourceManager, ResourceLocation location, Codec<T> codec)
    {
        if (resourceManager == null)
        {
            return Optional.empty();
        }
        try
        {
            IResource resource = resourceManager.getResource(location);
            try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))
            {
                JsonObject json = JSONUtils.parse(reader);
                DataResult<T> result = codec.parse(JsonOps.INSTANCE, json);
                return result.result();
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to load JSON file: " + location, e);
        }
    }
}
