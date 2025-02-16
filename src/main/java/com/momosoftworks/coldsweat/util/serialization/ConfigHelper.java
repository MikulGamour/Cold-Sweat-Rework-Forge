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
import com.momosoftworks.coldsweat.data.codec.configuration.*;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import net.minecraft.core.RegistryAccess;
import com.momosoftworks.coldsweat.util.math.FastMap;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.logging.log4j.util.TriConsumer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.*;

public class ConfigHelper
{
    private ConfigHelper() {}

    public static <T> List<Either<TagKey<T>, Holder<T>>> parseRegistryItems(ResourceKey<Registry<T>> registry, RegistryAccess registryAccess, String objects)
    {   return parseRegistryItems(registry, registryAccess, objects.split(","));
    }

    public static <T> List<Either<TagKey<T>, Holder<T>>> parseRegistryItems(ResourceKey<Registry<T>> registry, RegistryAccess registryAccess, String[] objects)
    {
        List<Either<TagKey<T>, Holder<T>>> registryList = new ArrayList<>();
        Registry<T> reg = registryAccess.registryOrThrow(registry);

        for (String objString : objects)
        {
            if (objString.startsWith("#"))
            {
                final String tagID = objString.replace("#", "");
                registryList.add(Either.left(TagKey.create(registry, new ResourceLocation(tagID))));
            }
            else
            {
                ResourceLocation id = new ResourceLocation(objString);
                Optional<Holder<T>> obj = reg.getHolder(ResourceKey.create(registry, id));
                if (!reg.containsKey(id) || obj.isEmpty())
                {
                    ColdSweat.LOGGER.error("Error parsing config: {} \"{}\" does not exist", registry.location().getPath(), objString);
                    continue;
                }
                registryList.add(Either.right(obj.get()));
            }
        }
        return registryList;
    }

    public static <T> List<Either<TagKey<T>, T>> parseBuiltinItems(ResourceKey<Registry<T>> registryKey, IForgeRegistry<T> registry, String objects)
    {   return parseBuiltinItems(registryKey, registry, objects.split(","));
    }

    public static <T> List<Either<TagKey<T>, T>> parseBuiltinItems(ResourceKey<Registry<T>> registryKey, IForgeRegistry<T> registry, String[] objects)
    {
        List<Either<TagKey<T>, T>> registryList = new ArrayList<>();

        for (String objString : objects)
        {
            if (objString.startsWith("#"))
            {
                final String tagID = objString.replace("#", "");
                registryList.add(Either.left(TagKey.create(registryKey, new ResourceLocation(tagID))));
            }
            else
            {
                ResourceLocation id = new ResourceLocation(objString);
                if (!registry.containsKey(id))
                {
                    ColdSweat.LOGGER.error("Error parsing config: {} \"{}\" does not exist", registryKey.location().getPath(), objString);
                    continue;
                }
                T obj = registry.getValue(id);
                registryList.add(Either.right(obj));
            }
        }
        return registryList;
    }

    public static List<Either<TagKey<Block>, Block>> getBlocks(String blocks)
    {   return getBlocks(blocks.split(","));
    }
    public static List<Either<TagKey<Block>, Block>> getBlocks(String[] blocks)
    {   return parseBuiltinItems(Registry.BLOCK_REGISTRY, ForgeRegistries.BLOCKS, blocks);
    }

    public static List<Either<TagKey<Item>, Item>> getItems(String items)
    {   return getItems(items.split(","));
    }
    public static List<Either<TagKey<Item>, Item>> getItems(String[] items)
    {   return parseBuiltinItems(Registry.ITEM_REGISTRY, ForgeRegistries.ITEMS, items);
    }

    public static List<Either<TagKey<EntityType<?>>, EntityType<?>>> getEntityTypes(String entities)
    {   return getEntityTypes(entities.split(","));
    }
    public static List<Either<TagKey<EntityType<?>>, EntityType<?>>> getEntityTypes(String[] entities)
    {   return parseBuiltinItems(Registry.ENTITY_TYPE_REGISTRY, ForgeRegistries.ENTITY_TYPES, entities);
    }

    public static <K, V extends ConfigData> Map<Holder<K>, V> getRegistryMap(List<? extends List<?>> source, RegistryAccess registryAccess, ResourceKey<Registry<K>> keyRegistry,
                                                                             Function<List<?>, V> valueCreator, Function<V, List<Either<TagKey<K>, Holder<K>>>> taggedListGetter)
    {
        return getRegistryMapLike(source, registryAccess, keyRegistry, valueCreator, taggedListGetter, FastMap::new, FastMap::put);
    }

    public static <K, V extends ConfigData> Multimap<Holder<K>, V> getRegistryMultimap(List<? extends List<?>> source, RegistryAccess registryAccess, ResourceKey<Registry<K>> keyRegistry,
                                                                                       Function<List<?>, V> valueCreator, Function<V, List<Either<TagKey<K>, Holder<K>>>> taggedListGetter)
    {
        return getRegistryMapLike(source, registryAccess, keyRegistry, valueCreator, taggedListGetter, FastMultiMap::new, FastMultiMap::put);
    }

    private static <K, V extends ConfigData, M> M getRegistryMapLike(List<? extends List<?>> source, RegistryAccess registryAccess, ResourceKey<Registry<K>> keyRegistry,
                                                                     Function<List<?>, V> valueCreator, Function<V, List<Either<TagKey<K>, Holder<K>>>> taggedListGetter,
                                                                     Supplier<M> mapSupplier, TriConsumer<M, Holder<K>, V> mapAdder)
    {
        M map = mapSupplier.get();
        for (List<?> entry : source)
        {
            V data = valueCreator.apply(entry);
            if (data != null)
            {
                data.setType(ConfigData.Type.TOML);
                for (Holder<K> key : RegistryHelper.mapVanillaRegistryTagList(keyRegistry, taggedListGetter.apply(data), registryAccess))
                {   mapAdder.accept(map, key, data);
                }
            }
            else ColdSweat.LOGGER.error("Error parsing {} config \"{}\"", keyRegistry.location(), entry.toString());
        }
        return map;
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

    public static <K, V extends ConfigData> CompoundTag serializeRegistry(Map<K, V> map, String key,
                                                                          ResourceKey<Registry<K>> gameRegistry, ResourceKey<Registry<V>> modRegistry,
                                                                          Function<K, ResourceLocation> keyGetter)
    {
        return serializeEitherRegistry(map, key, gameRegistry, modRegistry, null, keyGetter);
    }

    public static <K, V extends ConfigData> CompoundTag serializeHolderRegistry(Map<Holder<K>, V> map, String key,
                                                                                ResourceKey<Registry<K>> gameRegistry, ResourceKey<Registry<V>> modRegistry,
                                                                                RegistryAccess registryAccess)
    {
        return serializeEitherRegistry(map, key, gameRegistry, modRegistry, registryAccess, RegistryHelper::getKey);
    }

    private static <K, V extends ConfigData> CompoundTag serializeEitherRegistry(Map<K, V> map, String key,
                                                                                 ResourceKey<?> gameRegistry, ResourceKey<Registry<V>> modRegistry,
                                                                                 RegistryAccess registryAccess, Function<K, ResourceLocation> keyGetter)
    {
        Codec<V> codec = ModRegistries.getCodec(modRegistry);
        DynamicOps<Tag> encoderOps = registryAccess != null
                                     ? RegistryOps.create(NbtOps.INSTANCE, registryAccess)
                                     : NbtOps.INSTANCE;

        CompoundTag tag = new CompoundTag();
        CompoundTag mapTag = new CompoundTag();

        for (Map.Entry<K, V> entry : map.entrySet())
        {
            ResourceLocation elementId = keyGetter.apply(entry.getKey());
            if (elementId == null)
            {   ColdSweat.LOGGER.error("Error serializing {}: \"{}\" does not exist", gameRegistry.location(), entry.getKey());
                continue;
            }
            codec.encode(entry.getValue(), encoderOps, encoderOps.empty())
            .resultOrPartial(e -> ColdSweat.LOGGER.error("Error serializing {} {}: {}", modRegistry.location(), entry.getValue(), e))
            .ifPresent(encoded ->
            {
                ((CompoundTag) encoded).putUUID("UUID", entry.getValue().getId());
                mapTag.put(elementId.toString(), encoded);
            });
        }
        tag.put(key, mapTag);
        return tag;
    }

    public static <K, V extends ConfigData> Map<K, V> deserializeRegistry(CompoundTag tag, String key,
                                                                          ResourceKey<Registry<V>> modRegistry,
                                                                          Function<ResourceLocation, K> keyGetter)
    {
        return deserializeEitherRegistry(tag, key, modRegistry, keyGetter, null);
    }

    public static <K, V extends ConfigData> Map<Holder<K>, V> deserializeHolderRegistry(CompoundTag tag, String key,
                                                                                        ResourceKey<Registry<K>> gameRegistry, ResourceKey<Registry<V>> modRegistry,
                                                                                        RegistryAccess registryAccess)
    {
        Registry<K> registry = registryAccess.registryOrThrow(gameRegistry);
        return deserializeEitherRegistry(tag, key, modRegistry, k -> registry.getHolder(ResourceKey.create(gameRegistry, k)).orElse(null), registryAccess);
    }

    private static <K, V extends ConfigData> Map<K, V> deserializeEitherRegistry(CompoundTag tag, String key,
                                                                                 ResourceKey<Registry<V>> modRegistry,
                                                                                 Function<ResourceLocation, K> keyGetter,
                                                                                 RegistryAccess registryAccess)
    {
        Codec<V> codec = ModRegistries.getCodec(modRegistry);

        Map<K, V> map = new FastMap<>();
        CompoundTag mapTag = tag.getCompound(key);
        DynamicOps<Tag> decoderOps = registryAccess != null
                                     ? RegistryOps.create(NbtOps.INSTANCE, registryAccess)
                                     : NbtOps.INSTANCE;

        for (String entryKey : mapTag.getAllKeys())
        {
            CompoundTag entryData = mapTag.getCompound(entryKey);
            codec.decode(decoderOps, entryData)
            .resultOrPartial(e -> ColdSweat.LOGGER.error("Error deserializing {}: {}", modRegistry.location(), e))
            .map(Pair::getFirst)
            .ifPresent(value ->
            {
                K entry = keyGetter.apply(new ResourceLocation(entryKey));
                if (entry != null)
                {   value.setId(entryData.getUUID("UUID"));
                    map.put(entry, value);
                }
            });
        }
        return map;
    }

    public static <K, V extends ConfigData> CompoundTag serializeMultimapRegistry(Multimap<K, V> map, String key,
                                                                                  ResourceKey<Registry<K>> gameRegistry,
                                                                                  ResourceKey<Registry<V>> modRegistry,
                                                                                  Function<K, ResourceLocation> keyGetter)
    {
        return serializeEitherMultimapRegistry(map, key, gameRegistry, modRegistry, null, keyGetter);
    }

    public static <K, V extends ConfigData> CompoundTag serializeHolderMultimapRegistry(Multimap<Holder<K>, V> map, String key,
                                                                                        ResourceKey<Registry<K>> gameRegistry,
                                                                                        ResourceKey<Registry<V>> modRegistry,
                                                                                        RegistryAccess registryAccess)
    {
        return serializeEitherMultimapRegistry(map, key, gameRegistry, modRegistry, registryAccess, RegistryHelper::getKey);
    }

    private static <K, V extends ConfigData> CompoundTag serializeEitherMultimapRegistry(Multimap<K, V> map, String key,
                                                                                         ResourceKey<?> gameRegistry, ResourceKey<Registry<V>> modRegistry,
                                                                                         RegistryAccess registryAccess, Function<K, ResourceLocation> keyGetter)
    {
        Codec<V> codec = ModRegistries.getCodec(modRegistry);
        DynamicOps<Tag> encoderOps = registryAccess != null
                                     ? RegistryOps.create(NbtOps.INSTANCE, registryAccess)
                                     : NbtOps.INSTANCE;

        CompoundTag tag = new CompoundTag();
        CompoundTag mapTag = new CompoundTag();

        for (Map.Entry<K, Collection<V>> entry : map.asMap().entrySet())
        {
            ResourceLocation elementId = keyGetter.apply(entry.getKey());
            if (elementId == null)
            {   ColdSweat.LOGGER.error("Error serializing {}: \"{}\" does not exist", gameRegistry.location(), entry.getKey());
                continue;
            }
            ListTag valuesTag = new ListTag();
            for (V value : entry.getValue())
            {
                codec.encode(value, encoderOps, encoderOps.empty())
                .resultOrPartial(e -> ColdSweat.LOGGER.error("Error serializing {} {}: {}", modRegistry.location(), entry.getValue(), e))
                .ifPresent(encoded ->
                {
                    ((CompoundTag) encoded).putUUID("UUID", value.getId());
                    valuesTag.add(encoded);
                });
            }
            mapTag.put(elementId.toString(), valuesTag);
        }
        tag.put(key, mapTag);
        return tag;
    }

    public static <K, V extends ConfigData> Multimap<K, V> deserializeMultimapRegistry(CompoundTag tag, String key,
                                                                                       ResourceKey<Registry<V>> modRegistry,
                                                                                       Function<ResourceLocation, K> keyGetter)
    {
        return deserializeEitherMultimapRegistry(tag, key, modRegistry, keyGetter, null);
    }

    public static <K, V extends ConfigData> Multimap<Holder<K>, V> deserializeHolderMultimapRegistry(CompoundTag tag, String key,
                                                                                                     ResourceKey<Registry<K>> gameRegistry,
                                                                                                     ResourceKey<Registry<V>> modRegistry,
                                                                                                     RegistryAccess registryAccess)
    {
        Registry<K> registry = registryAccess.registryOrThrow(gameRegistry);
        return deserializeEitherMultimapRegistry(tag, key, modRegistry, k -> registry.getHolder(ResourceKey.create(gameRegistry, k)).orElse(null), registryAccess);
    }

    private static <K, V extends ConfigData> Multimap<K, V> deserializeEitherMultimapRegistry(CompoundTag tag, String key,
                                                                                              ResourceKey<Registry<V>> modRegistry,
                                                                                              Function<ResourceLocation, K> keyGetter,
                                                                                              RegistryAccess registryAccess)
    {
        Codec<V> codec = ModRegistries.getCodec(modRegistry);
        DynamicOps<Tag> decoderOps = registryAccess != null
                                     ? RegistryOps.create(NbtOps.INSTANCE, registryAccess)
                                     : NbtOps.INSTANCE;

        Multimap<K, V> map = new FastMultiMap<>();
        CompoundTag mapTag = tag.getCompound(key);

        for (String entryKey : mapTag.getAllKeys())
        {
            ListTag entryData = mapTag.getList(entryKey, 10);
            K object = keyGetter.apply(new ResourceLocation(entryKey));
            if (object == null)
            {   ColdSweat.LOGGER.error("Error deserializing: \"{}\" does not exist in registry", entryKey);
                continue;
            }
            for (Tag valueTag : entryData)
            {
                CompoundTag valueData = (CompoundTag) valueTag;
                codec.decode(decoderOps, valueData).result().map(Pair::getFirst)
                .ifPresent(value ->
                {
                    value.setId(valueData.getUUID("UUID"));
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
        writeRegistryMultimap(items, insulator -> getTaggableListStrings(insulator.data().items().orElse(List.of()), Registry.ITEM_REGISTRY), insulator ->
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
            itemData.add(insulator.data().nbt().tag().toString());

            return itemData;
        }, saver);
    }

    public static <T> Codec<Either<TagKey<T>, T>> tagOrBuiltinCodec(ResourceKey<Registry<T>> vanillaRegistry, IForgeRegistry<T> forgeRegistry)
    {
        return Codec.either(Codec.STRING.comapFlatMap(str ->
                                                      {
                                                          if (!str.startsWith("#"))
                                                          {   return DataResult.error("Not a tag key: " + str);
                                                          }
                                                          ResourceLocation itemLocation = new ResourceLocation(str.replace("#", ""));
                                                          return DataResult.success(TagKey.create(vanillaRegistry, itemLocation));
                                                      },
                                                      key -> "#" + key.location()),
                            forgeRegistry.getCodec());
    }

    public static <T> Codec<Either<TagKey<T>, Holder<T>>> tagOrHolderCodec(ResourceKey<Registry<T>> vanillaRegistry, Codec<Holder<T>> codec)
    {
        return Codec.either(Codec.STRING.comapFlatMap(str ->
                                                      {
                                                          if (!str.startsWith("#"))
                                                          {   return DataResult.error("Not a tag key: " + str);
                                                          }
                                                          ResourceLocation itemLocation = new ResourceLocation(str.replace("#", ""));
                                                          return DataResult.success(TagKey.create(vanillaRegistry, itemLocation));
                                                      },
                                                      key -> "#" + key.location()),
                            codec);
    }

    public static <T> Codec<Either<TagKey<T>, ResourceKey<T>>> tagOrResourceKeyCodec(ResourceKey<Registry<T>> vanillaRegistry)
    {
        return Codec.either(Codec.STRING.comapFlatMap(str ->
                                                      {
                                                          if (!str.startsWith("#"))
                                                          {   return DataResult.error("Not a tag key: " + str);
                                                          }
                                                          ResourceLocation itemLocation = new ResourceLocation(str.replace("#", ""));
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

    public static <T> String serializeTagOrBuiltinObject(IForgeRegistry<T> forgeRegistry, Either<TagKey<T>, T> obj)
    {
        return obj.map(tag -> "#" + tag.location(),
                       regObj -> Optional.ofNullable(forgeRegistry.getKey(regObj)).map(ResourceLocation::toString).orElse(""));
    }

    public static <T> String serializeTagOrRegistryObject(ResourceKey<Registry<T>> registry, Either<TagKey<T>, T> obj, RegistryAccess registryAccess)
    {
        Registry<T> reg = registryAccess.registryOrThrow(registry);
        return obj.map(tag -> "#" + tag.location(),
                       regObj -> Optional.ofNullable(reg.getKey(regObj)).map(ResourceLocation::toString).orElse(""));
    }

    public static <T> Either<TagKey<T>, ResourceKey<T>> deserializeTagOrResourceKey(ResourceKey<Registry<T>> registry, String key)
    {
        if (key.startsWith("#"))
        {
            ResourceLocation tagID = new ResourceLocation(key.replace("#", ""));
            return Either.left(TagKey.create(registry, tagID));
        }
        else
        {
            ResourceKey<T> biomeKey = ResourceKey.create(registry, new ResourceLocation(key));
            return Either.right(biomeKey);
        }
    }

    public static <T> Either<TagKey<T>, T> deserializeTagOrRegistryObject(String tagOrRegistryObject, ResourceKey<Registry<T>> vanillaRegistry, IForgeRegistry<T> forgeRegistry)
    {
        if (tagOrRegistryObject.startsWith("#"))
        {
            ResourceLocation tagID = new ResourceLocation(tagOrRegistryObject.replace("#", ""));
            return Either.left(TagKey.create(vanillaRegistry, tagID));
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

    public static <T> List<String> getTaggableListStrings(List<Either<TagKey<T>, T>> list, ResourceKey<Registry<T>> registry)
    {
        RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();
        if (registryAccess == null) return List.of();
        List<String> strings = new ArrayList<>();

        for (Either<TagKey<T>, T> entry : list)
        {   strings.add(serializeTagOrRegistryObject(registry, entry, registryAccess));
        }
        return strings;
    }

    public static List<String> getModIDs(String[] ids)
    {
        List<String> modIDs = new ArrayList<>();
        for (String id : ids)
        {
            String[] split = id.split(":");
            if (split.length > 1)
            {   modIDs.add(split[0].replace("#", ""));
            }
        }
        return modIDs;
    }

    public static String getModID(String id)
    {
        String[] split = id.split(":");
        if (split.length > 1)
        {   return split[0].replace("#", "");
        }
        return "";
    }

    public static <T> List<String> getModIDs(List<Either<TagKey<T>, T>> list, IForgeRegistry<T> registry)
    {
        return list.stream().map(either -> either.map(tag -> tag.location().getNamespace(),
                                                      obj -> Optional.ofNullable(registry.getKey(obj)).map(ResourceLocation::getNamespace).orElse("")))
                   .distinct()
                   .filter(s -> !s.isEmpty())
                   .toList();
    }

    public static <T> List<String> getModIDs(List<Either<TagKey<T>, Holder<T>>> list)
    {
        List<String> mods = new ArrayList<>();
        for (Either<TagKey<T>, Holder<T>> either : list)
        {
            mods.add(either.map(tag -> tag.location().getNamespace(),
                                obj -> obj.unwrapKey().map(key -> key.location().getNamespace()).orElse("")));
        }
        return mods;
    }
}
