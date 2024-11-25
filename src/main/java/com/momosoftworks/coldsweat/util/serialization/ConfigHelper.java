package com.momosoftworks.coldsweat.util.serialization;

import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
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
import com.momosoftworks.coldsweat.util.math.FastMap;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.Fluid;
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
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.apache.logging.log4j.util.TriConsumer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

public class ConfigHelper
{
    private ConfigHelper() {}

    public static <T> List<T> parseRegistryItems(RegistryKey<Registry<T>> registry, DynamicRegistries DynamicRegistries, String objects)
    {
        List<T> parsedObjects = new ArrayList<>();
        Optional<MutableRegistry<T>> optReg = DynamicRegistries.registry(registry);
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
                Optional<T> obj = reg.getOptional(RegistryKey.create(registry, id));
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

    public static <T> ITagCollection<T> getTagsForRegistry(RegistryKey<Registry<T>> registry)
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

    public static <T> ITagCollection<T> getTagsForObject(T object)
    {
        if (object instanceof Item)
        {   return ((ITagCollection<T>) ItemTags.getAllTags());
        }
        else if (object instanceof Block)
        {   return ((ITagCollection<T>) BlockTags.getAllTags());
        }
        else if (object instanceof Fluid)
        {   return ((ITagCollection<T>) FluidTags.getAllTags());
        }
        else if (object instanceof EntityType)
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

    public static <K, V> Map<K, V> getRegistryMap(List<? extends List<?>> source, DynamicRegistries DynamicRegistries, RegistryKey<Registry<K>> keyRegistry,
                                                          Function<List<?>, V> valueCreator, Function<V, List<Either<ITag<K>, K>>> taggedListGetter)
    {
        return getRegistryMapLike(source, DynamicRegistries, keyRegistry, valueCreator, taggedListGetter, FastMap::new, FastMap::put);
    }
    public static <K, V> Map<K, V> getRegistryMap(List<? extends List<?>> source, RegistryKey<Registry<K>> keyRegistry,
                                                  Function<List<?>, V> valueCreator, Function<V, List<K>> taggedListGetter)
    {
        return getRegistryMapLike(source, null, keyRegistry, valueCreator,
                                  v -> taggedListGetter.apply(v).stream().map(val -> Either.<ITag<K>, K>right(val)).collect(Collectors.toList()),
                                  FastMap::new, FastMap::put);
    }

    public static <K, V> Multimap<K, V> getRegistryMultimap(List<? extends List<?>> source, DynamicRegistries DynamicRegistries, RegistryKey<Registry<K>> keyRegistry,
                                                                    Function<List<?>, V> valueCreator, Function<V, List<Either<ITag<K>, K>>> taggedListGetter)
    {
        return getRegistryMapLike(source, DynamicRegistries, keyRegistry, valueCreator, taggedListGetter, FastMultiMap::new, FastMultiMap::put);
    }
    public static <K, V> Multimap<K, V> getRegistryMultimap(List<? extends List<?>> source, RegistryKey<Registry<K>> keyRegistry,
                                                            Function<List<?>, V> valueCreator, Function<V, List<K>> taggedListGetter)
    {
        return getRegistryMapLike(source, null, keyRegistry, valueCreator,
                                  v -> taggedListGetter.apply(v).stream().map(val -> Either.<ITag<K>, K>right(val)).collect(Collectors.toList()),
                                  FastMultiMap::new, FastMultiMap::put);
    }

    private static <K, V, M> M getRegistryMapLike(List<? extends List<?>> source, DynamicRegistries DynamicRegistries, RegistryKey<Registry<K>> keyRegistry,
                                                  Function<List<?>, V> valueCreator, Function<V, List<Either<ITag<K>, K>>> listGetter,
                                                  Supplier<M> mapSupplier, TriConsumer<M, K, V> mapAdder)
    {
        M map = mapSupplier.get();
        for (List<?> entry : source)
        {
            V data = valueCreator.apply(entry);
            if (data != null)
            {
                for (K key : RegistryHelper.mapTaggableList(listGetter.apply(data)))
                {   mapAdder.accept(map, key, data);
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

    public static <K, V extends ConfigData<?>> CompoundNBT serializeRegistry(Map<K, V> map, String key,
                                                                             RegistryKey<Registry<K>> gameRegistry, RegistryKey<Registry<V>> modRegistry,
                                                                             DynamicRegistries registryAccess)
    {
        return serializeEitherRegistry(map, key, gameRegistry, modRegistry, null, rl -> registryAccess.registryOrThrow(gameRegistry).getKey(rl));
    }

    public static <K, V extends ConfigData<?>> CompoundNBT serializeHolderRegistry(Map<K, V> map, String key,
                                                                                   RegistryKey<Registry<K>> gameRegistry, RegistryKey<Registry<V>> modRegistry,
                                                                                   DynamicRegistries dynamicRegistries)
    {
        return serializeEitherRegistry(map, key, gameRegistry, modRegistry, dynamicRegistries, k -> dynamicRegistries.registryOrThrow(gameRegistry).getKey(k));
    }

    private static <K, V extends ConfigData<?>> CompoundNBT serializeEitherRegistry(Map<K, V> map, String key,
                                                                                    RegistryKey<?> gameRegistry, RegistryKey<Registry<V>> modRegistry,
                                                                                    DynamicRegistries dynamicRegistries, Function<K, ResourceLocation> keyGetter)
    {
        Codec<V> codec = ModRegistries.getCodec(modRegistry);
        DynamicOps<INBT> encoderOps = NBTDynamicOps.INSTANCE;

        CompoundNBT tag = new CompoundNBT();
        CompoundNBT mapTag = new CompoundNBT();

        for (Map.Entry<K, V> entry : map.entrySet())
        {
            ResourceLocation elementId = keyGetter.apply(entry.getKey());
            if (elementId == null)
            {   ColdSweat.LOGGER.error("Error serializing {}: \"{}\" does not exist", gameRegistry.location(), entry.getKey());
                continue;
            }
            codec.encode(entry.getValue(), encoderOps, encoderOps.empty())
            .resultOrPartial(e -> ColdSweat.LOGGER.error("Error serializing {} \"{}\": {}", gameRegistry.location(), elementId, e))
            .ifPresent(encoded ->
            {
                ((CompoundNBT) encoded).putUUID("UUID", entry.getValue().getId());
                mapTag.put(elementId.toString(), encoded);
            });
        }
        tag.put(key, mapTag);
        return tag;
    }

    public static <K, V extends ConfigData<?>> Map<K, V> deserializeRegistry(CompoundNBT tag, String key,
                                                                            RegistryKey<Registry<K>> gameRegistry,
                                                                            RegistryKey<Registry<V>> modRegistry,
                                                                            DynamicRegistries dynamicRegistries)
    {
        Registry<K> registry = dynamicRegistries.registryOrThrow(gameRegistry);
        return deserializeEitherRegistry(tag, key, modRegistry, rl -> registry.getOptional(RegistryKey.create(gameRegistry, rl)).orElse(null), dynamicRegistries);
    }

    private static <K, V extends ConfigData<?>> Map<K, V> deserializeEitherRegistry(CompoundNBT tag, String key,
                                                                                    RegistryKey<Registry<V>> modRegistry,
                                                                                    Function<ResourceLocation, K> keyGetter,
                                                                                    DynamicRegistries dynamicRegistries)
    {
        Codec<V> codec = ModRegistries.getCodec(modRegistry);

        Map<K, V> map = new FastMap<>();
        CompoundNBT mapTag = tag.getCompound(key);
        DynamicOps<INBT> decoderOps = NBTDynamicOps.INSTANCE;

        for (String entryKey : mapTag.getAllKeys())
        {
            CompoundNBT entryData = mapTag.getCompound(entryKey);
            codec.decode(decoderOps, entryData)
            .resultOrPartial(e -> ColdSweat.LOGGER.error("Error deserializing {}: {}", modRegistry.location(), e))
            .map(Pair::getFirst)
            .ifPresent(value ->
            {
                K entry = keyGetter.apply(new ResourceLocation(entryKey));
                if (entry != null)
                {   ConfigData.IDENTIFIABLES.put(entryData.getUUID("UUID"), value);
                    map.put(entry, value);
                }
            });
        }
        return map;
    }

    public static <K, V extends ConfigData<?>> CompoundNBT serializeMultimapRegistry(Multimap<K, V> map, String key,
                                                                                    RegistryKey<Registry<V>> modRegistry,
                                                                                    Function<K, ResourceLocation> keyGetter)
    {
        return serializeEitherMultimapRegistry(map, key, modRegistry, keyGetter);
    }

    public static <K, V extends ConfigData<?>> CompoundNBT serializeHolderMultimapRegistry(Multimap<K, V> map, String key,
                                                                                           RegistryKey<Registry<K>> gameRegistry, RegistryKey<Registry<V>> modRegistry,
                                                                                           DynamicRegistries dynamicRegistries)
    {
        return serializeEitherMultimapRegistry(map, key, modRegistry, k -> dynamicRegistries.registryOrThrow(gameRegistry).getKey(k));
    }

    private static <K, V extends ConfigData<?>> CompoundNBT serializeEitherMultimapRegistry(Multimap<K, V> map, String key,
                                                                                           RegistryKey<Registry<V>> modRegistry,
                                                                                           Function<K, ResourceLocation> keyGetter)
    {
        Codec<V> codec = ModRegistries.getCodec(modRegistry);
        CompoundNBT tag = new CompoundNBT();
        CompoundNBT mapTag = new CompoundNBT();

        for (Map.Entry<K, Collection<V>> entry : map.asMap().entrySet())
        {
            ResourceLocation elementId = keyGetter.apply(entry.getKey());
            if (elementId == null)
            {   ColdSweat.LOGGER.error("Error serializing: \"{}\" does not exist in registry", entry.getKey());
                continue;
            }
            ListNBT valuesTag = new ListNBT();
            for (V value : entry.getValue())
            {
                codec.encodeStart(NBTDynamicOps.INSTANCE, value)
                .resultOrPartial(e -> ColdSweat.LOGGER.error("Error serializing {} \"{}\": {}", modRegistry.location(), elementId, e))
                .ifPresent(encoded ->
                {
                    ((CompoundNBT) encoded).putUUID("UUID", value.getId());
                    valuesTag.add(encoded);
                });
            }
            mapTag.put(elementId.toString(), valuesTag);
        }
        tag.put(key, mapTag);
        return tag;
    }

    public static <K, V extends ConfigData<?>> Multimap<K, V> deserializeMultimapRegistry(CompoundNBT tag, String key,
                                                                                          RegistryKey<Registry<V>> modRegistry,
                                                                                          Function<ResourceLocation, K> keyGetter)
    {
        return deserializeEitherMultimapRegistry(tag, key, modRegistry, keyGetter);
    }

    public static <K, V extends ConfigData<?>> Multimap<K, V> deserializeHolderMultimapRegistry(CompoundNBT tag, String key,
                                                                                                        RegistryKey<Registry<K>> gameRegistry, RegistryKey<Registry<V>> modRegistry,
                                                                                                        DynamicRegistries DynamicRegistries)
    {
        Registry<K> registry = DynamicRegistries.registryOrThrow(gameRegistry);
        return deserializeEitherMultimapRegistry(tag, key, modRegistry, k -> registry.getOptional(RegistryKey.create(gameRegistry, k)).orElse(null));
    }

    private static <K, V extends ConfigData<?>> Multimap<K, V> deserializeEitherMultimapRegistry(CompoundNBT tag, String key,
                                                                                                RegistryKey<Registry<V>> modRegistry,
                                                                                                Function<ResourceLocation, K> keyGetter)
    {
        Codec<V> codec = ModRegistries.getCodec(modRegistry);

        Multimap<K, V> map = new FastMultiMap<>();
        CompoundNBT mapTag = tag.getCompound(key);

        for (String entryKey : mapTag.getAllKeys())
        {
            ListNBT entryData = mapTag.getList(entryKey, 10);
            K object = keyGetter.apply(new ResourceLocation(entryKey));
            if (object == null)
            {   ColdSweat.LOGGER.error("Error deserializing: \"{}\" does not exist in registry", entryKey);
                continue;
            }
            for (INBT valueTag : entryData)
            {
                CompoundNBT valueData = (CompoundNBT) valueTag;
                codec.decode(NBTDynamicOps.INSTANCE, valueData).result().map(Pair::getFirst)
                .ifPresent(value ->
                {
                    ConfigData.IDENTIFIABLES.put(valueData.getUUID("UUID"), value);
                    map.put(object, value);
                });
            }
        }
        return map;
    }

    public static <T> void writeRegistryMap(Map<Item, T> map, Function<T, List<String>> keyWriter,
                                            Function<T, List<?>> valueWriter, Consumer<List<? extends List<?>>> saver)
    {   writeRegistryMapLike(Either.left(map), keyWriter, valueWriter, saver);
    }

    public static <K, V> void writeRegistryMultimap(Multimap<K, V> map, Function<V, List<String>> keyWriter,
                                                    Function<V, List<?>> valueWriter, Consumer<List<? extends List<?>>> saver)
    {   writeRegistryMapLike(Either.right(map), keyWriter, valueWriter, saver);
    }

    private static <K, V> void writeRegistryMapLike(Either<Map<K, V>, Multimap<K, V>> map, Function<V, List<String>> keyWriter,
                                                    Function<V, List<?>> valueWriter, Consumer<List<? extends List<?>>> saver)
    {
        List<List<?>> list = new ArrayList<>();
        for (Map.Entry<K, V> entry : map.map(Map::entrySet, Multimap::entries))
        {
            V value = entry.getValue();

            List<Object> itemData = new ArrayList<>();
            List<String> keySet = keyWriter.apply(value);

            itemData.add(concatStringList(keySet));

            List<?> args = valueWriter.apply(value);
            if (args == null) continue;

            itemData.addAll(args);
            list.add(itemData);
        }
        saver.accept(list);
    }

    public static void writeItemInsulations(Multimap<Item, InsulatorData> items, Consumer<List<? extends List<?>>> saver)
    {
        writeRegistryMultimap(items, insulator -> getTaggableListStrings(insulator.data.items.orElse(Arrays.asList()), Registry.ITEM_REGISTRY), insulator ->
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
        }, saver);
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
        DynamicRegistries DynamicRegistries = WorldHelper.getServer().registryAccess();
        Registry<T> registry = DynamicRegistries.registry(vanillaRegistry).get();
        return Codec.STRING.xmap(str -> registry.get(new ResourceLocation(str)),
                                 item -> registry.getKey(item).toString());
    }

    public static <T> String serializeTagOrRegistryKey(Either<ITag<T>, RegistryKey<T>> obj)
    {
        return obj.map(tag -> "#" + ConfigHelper.getTagsForObject(obj).getId((ITag) tag),
                       key -> key.location().toString());
    }

    public static <T extends IForgeRegistryEntry<T>> String serializeTagOrBuiltinObject(IForgeRegistry<T> forgeRegistry, Either<ITag<T>, T> obj)
    {
        return obj.map(tag -> "#" + ConfigHelper.getTagsForObject(obj).getId((ITag) tag),
                       regObj -> Optional.ofNullable(forgeRegistry.getKey(regObj)).map(ResourceLocation::toString).orElse(""));
    }

    public static <T> String serializeTagOrRegistryObject(RegistryKey<Registry<T>> registry, Either<ITag<T>, T> obj, DynamicRegistries DynamicRegistries)
    {
        Registry<T> reg = DynamicRegistries.registryOrThrow(registry);
        return obj.map(tag -> "#" + ConfigHelper.getTagsForObject(obj).getId((ITag) tag),
                       regObj -> Optional.ofNullable(reg.getKey(regObj)).map(ResourceLocation::toString).orElse(""));
    }

    public static <T> Either<ITag<T>, RegistryKey<T>> deserializeTagOrRegistryKey(RegistryKey<Registry<T>> registry, String key)
    {
        if (key.startsWith("#"))
        {
            ResourceLocation tagID = new ResourceLocation(key.replace("#", ""));
            return Either.left(ConfigHelper.getTagsForRegistry(registry).getTag(tagID));
        }
        else
        {
            RegistryKey<T> biomeKey = RegistryKey.create(registry, new ResourceLocation(key));
            return Either.right(biomeKey);
        }
    }

    public static <T extends IForgeRegistryEntry<T>> Either<ITag<T>, T> deserializeTagOrRegistryObject(String tagOrRegistryObject, RegistryKey<Registry<T>> vanillaRegistry, IForgeRegistry<T> forgeRegistry)
    {
        if (tagOrRegistryObject.startsWith("#"))
        {
            ResourceLocation tagID = new ResourceLocation(tagOrRegistryObject.replace("#", ""));
            return Either.left(ConfigHelper.getTagsForRegistry(vanillaRegistry).getTag(tagID));
        }
        else
        {
            ResourceLocation id = new ResourceLocation(tagOrRegistryObject);
            T obj = forgeRegistry.getValue(id);
            if (obj == null)
            {   ColdSweat.LOGGER.error("Error deserializing config: object \"{}\" does not exist", tagOrRegistryObject);
                return null;
            }
            return Either.right(obj);
        }
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

    public static String concatStringList(List<String> list)
    {
        StringBuilder builder = new StringBuilder();
        Iterator<String> iter = list.iterator();
        while (iter.hasNext())
        {
            builder.append(iter.next());
            if (iter.hasNext())
            {   builder.append(",");
            }
        }
        return builder.toString();
    }

    public static <T> List<String> getTaggableListStrings(List<Either<ITag<T>, T>> list, RegistryKey<Registry<T>> registry)
    {
        DynamicRegistries DynamicRegistries = RegistryHelper.getDynamicRegistries();
        if (DynamicRegistries == null) return Arrays.asList();
        List<String> strings = new ArrayList<>();

        for (Either<ITag<T>, T> entry : list)
        {   strings.add(serializeTagOrRegistryObject(registry, entry, DynamicRegistries));
        }
        return strings;
    }
}
