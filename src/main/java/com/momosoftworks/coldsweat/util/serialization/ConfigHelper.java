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
import com.momosoftworks.coldsweat.api.insulation.StaticInsulation;
import com.momosoftworks.coldsweat.data.ModRegistries;
import com.momosoftworks.coldsweat.data.codec.configuration.FuelData;
import com.momosoftworks.coldsweat.data.codec.configuration.InsulatorData;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTDynamicOps;
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

    public static <K, V> Map<K, V> getRegistryMap(List<? extends List<?>> source, DynamicRegistries registryAccess, RegistryKey<Registry<K>> keyRegistry,
                                                  Function<List<?>, V> valueCreator, Function<V, List<Either<ITag<K>, K>>> taggedListGetter)
    {
        Map<K, V> map = new HashMap<>();
        for (List<?> entry : source)
        {
            V data = valueCreator.apply(entry);
            if (data != null)
            {
                for (K key : RegistryHelper.mapTaggableList(taggedListGetter.apply(data)))
                {   map.put(key, data);
                }
            }
            else ColdSweat.LOGGER.error("Error parsing {} config \"{}\"", keyRegistry.location(), entry.toString());
        }
        return map;
    }

    public static <K, V> Map<K, V> getRegistryMap(List<? extends List<?>> source, RegistryKey<Registry<K>> keyRegistry,
                                                  Function<List<?>, V> valueCreator, Function<V, List<K>> listGetter)
    {
        Map<K, V> map = new HashMap<>();
        for (List<?> entry : source)
        {
            V data = valueCreator.apply(entry);
            if (data != null)
            {
                for (K key : listGetter.apply(data))
                {   map.put(key, data);
                }
            }
            else ColdSweat.LOGGER.error("Error parsing {} config \"{}\"", keyRegistry.location(), entry.toString());
        }
        return map;
    }

    public static Map<String, Object> getBlockStatePredicates(Block block, String predicates)
    {
        Map<String, Object> blockPredicates = new HashMap<>();
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
                    blockPredicates.put(key, propertyValue);
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

    public static <K, V extends ConfigData<?>> CompoundNBT serializeRegistry(Map<K, V> map, String key, RegistryKey<Registry<K>> keyRegistry, RegistryKey<Registry<V>> modRegistry, DynamicRegistries registryAccess)
    {
        Codec<V> codec = ModRegistries.getCodec(modRegistry);
        CompoundNBT tag = new CompoundNBT();
        CompoundNBT mapTag = new CompoundNBT();

        for (Map.Entry<K, V> entry : map.entrySet())
        {
            ResourceLocation elementId = registryAccess.registryOrThrow(keyRegistry).getKey(entry.getKey());
            if (elementId == null)
            {   ColdSweat.LOGGER.error("Error serializing {}: \"{}\" does not exist", keyRegistry.location(), entry.getKey());
                continue;
            }
            Optional<INBT> encoded = codec.encodeStart(NBTDynamicOps.INSTANCE, entry.getValue())
            .resultOrPartial(e ->
            {   ColdSweat.LOGGER.error("Error serializing {} \"{}\": {}", keyRegistry.location(), elementId, e);
            });
            if (!encoded.isPresent())
            {   continue;
            }
            ((CompoundNBT) encoded.get()).putUUID("UUID", entry.getValue().getId());
            mapTag.put(elementId.toString(), encoded.get());
        }
        tag.put(key, mapTag);
        return tag;
    }

    public static <K, V extends ConfigData<?>> Map<K, V> deserializeRegistry(CompoundNBT tag, String key, RegistryKey<Registry<K>> keyRegistry, RegistryKey<Registry<V>> modRegistry, DynamicRegistries registryAccess)
    {
        Codec<V> codec = ModRegistries.getCodec(modRegistry);

        Map<K, V> map = new HashMap<>();
        CompoundNBT mapTag = tag.getCompound(key);

        for (String entryKey : mapTag.getAllKeys())
        {
            CompoundNBT entryData = mapTag.getCompound(entryKey);
            K dimension = registryAccess.registryOrThrow(keyRegistry).get(new ResourceLocation(entryKey));
            if (dimension == null)
            {   ColdSweat.LOGGER.error("Error deserializing {}: \"{}\" does not exist", keyRegistry.location(), entryKey);
                continue;
            }
            codec.decode(NBTDynamicOps.INSTANCE, entryData).result().ifPresent(
                    result ->
                    {   ConfigData.IDENTIFIABLES.put(entryData.getUUID("UUID"), result.getFirst());
                        map.put(dimension, result.getFirst());
                    });
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
            for (Item item : getItems(itemId.split(",")))
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

            itemData.add(itemID.toString());

            List<?> args = valueWriter.apply(value);
            if (args == null) continue;

            itemData.addAll(args);
            list.add(itemData);
        }
        saver.accept(list);
    }

    public static void writeItemInsulations(Multimap<Item, InsulatorData> items, Consumer<List<? extends List<?>>> saver)
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

    public static Optional<FuelData> findFirstFuelMatching(DynamicHolder<Multimap<Item, FuelData>> predicates, ItemStack stack)
    {
        for (FuelData predicate : predicates.get().get(stack.getItem()))
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
