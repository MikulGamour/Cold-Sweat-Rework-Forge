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
import com.momosoftworks.coldsweat.data.codec.configuration.*;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.core.RegistryAccess;
import com.momosoftworks.coldsweat.util.math.FastMultiMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.nbt.*;
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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.tags.ITag;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.*;

public class ConfigHelper
{
    private ConfigHelper() {}

    public static <T> List<T> parseRegistryItems(ResourceKey<Registry<T>> registry, RegistryAccess registryAccess, String objects)
    {
        List<T> registryList = new ArrayList<>();
        Registry<T> reg = registryAccess.registryOrThrow(registry);

        for (String objString : objects.split(","))
        {
            if (objString.startsWith("#"))
            {
                final String tagID = objString.replace("#", "");
                Optional<HolderSet.Named<T>> tag = reg.getTag(TagKey.create(registry, new ResourceLocation(tagID)));
                tag.ifPresent(tg -> registryList.addAll(tg.stream().map(Holder::value).toList()));
            }
            else
            {
                ResourceLocation id = new ResourceLocation(objString);
                Optional<T> obj = Optional.ofNullable(reg.get(id));
                if (obj.isEmpty())
                {
                    ColdSweat.LOGGER.error("Error parsing config: \"{}\" does not exist", objString);
                    continue;
                }
                registryList.add(obj.get());
            }
        }
        return registryList;
    }

    public static List<Block> getBlocks(String... ids)
    {
        List<Block> blocks = new ArrayList<>();
        for (String id : ids)
        {
            if (id.startsWith("#"))
            {
                final String tagID = id.replace("#", "");
                CSMath.doIfNotNull(ForgeRegistries.BLOCKS.tags(), tags ->
                {   Optional<ITag<Block>> optionalTag = tags.stream().filter(tag -> tag.getKey() != null && tag.getKey().location().toString().equals(tagID)).findFirst();
                    optionalTag.ifPresent(blockITag -> blocks.addAll(blockITag.stream().toList()));
                });
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
                CSMath.doIfNotNull(ForgeRegistries.ITEMS.tags(), tags ->
                {   Optional<ITag<Item>> optionalTag = tags.stream().filter(tag -> tag.getKey() != null && tag.getKey().location().toString().equals(tagID)).findFirst();
                    optionalTag.ifPresent(itemITag -> items.addAll(itemITag.stream().toList()));
                });
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

    public static <K, V> Map<K, V> getRegistryMap(List<? extends List<?>> source, RegistryAccess registryAccess, ResourceKey<Registry<K>> keyRegistry,
                                                  Function<List<?>, V> valueCreator, Function<V, List<Either<TagKey<K>, K>>> taggedListGetter)
    {
        Map<K, V> map = new HashMap<>();
        for (List<?> entry : source)
        {
            V data = valueCreator.apply(entry);
            if (data != null)
            {
                for (K key : RegistryHelper.mapVanillaRegistryTagList(keyRegistry, taggedListGetter.apply(data), registryAccess))
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
                CSMath.doIfNotNull(ForgeRegistries.ENTITIES.tags(), tags ->
                {
                    Optional<ITag<EntityType<?>>> optionalTag = tags.stream().filter(tag -> tag.getKey() != null && tag.getKey().location().toString().equals(tagID)).findFirst();
                    optionalTag.ifPresent(entityITag -> entityList.addAll(entityITag.stream().toList()));
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

    public static <K, V extends ConfigData<?>> CompoundTag serializeRegistry(Map<K, V> map, String key,
                                                                             ResourceKey<Registry<K>> keyRegistry, ResourceKey<Registry<V>> modRegistry,
                                                                             Function<K, ResourceLocation> registryGetter)
    {
        Codec<V> codec = ModRegistries.getCodec(modRegistry);
        CompoundTag tag = new CompoundTag();
        CompoundTag mapTag = new CompoundTag();

        for (Map.Entry<K, V> entry : map.entrySet())
        {
            ResourceLocation elementId = registryGetter.apply(entry.getKey());
            if (elementId == null)
            {   ColdSweat.LOGGER.error("Error serializing {}: \"{}\" does not exist", keyRegistry.location(), entry.getKey());
                continue;
            }
            codec.encodeStart(NbtOps.INSTANCE, entry.getValue())
            .resultOrPartial(e -> ColdSweat.LOGGER.error("Error serializing {} \"{}\": {}", keyRegistry.location(), elementId, e))
            .ifPresent(encoded ->
            {
                ((CompoundTag) encoded).putUUID("UUID", entry.getValue().getId());
                mapTag.put(elementId.toString(), encoded);
            });
        }
        tag.put(key, mapTag);
        return tag;
    }

    public static <K, V extends ConfigData<?>> Map<K, V> deserializeRegistry(CompoundTag tag, String key,
                                                                             ResourceKey<Registry<K>> keyRegistry, ResourceKey<Registry<V>> modRegistry,
                                                                             Function<ResourceLocation, K> registryGetter)
    {
        Codec<V> codec = ModRegistries.getCodec(modRegistry);

        Map<K, V> map = new HashMap<>();
        CompoundTag mapTag = tag.getCompound(key);

        for (String entryKey : mapTag.getAllKeys())
        {
            CompoundTag entryData = mapTag.getCompound(entryKey);
            K object = registryGetter.apply(new ResourceLocation(entryKey));
            if (object == null)
            {   ColdSweat.LOGGER.error("Error deserializing {}: \"{}\" does not exist", keyRegistry.location(), entryKey);
                continue;
            }
            codec.decode(NbtOps.INSTANCE, entryData).result().map(Pair::getFirst)
            .ifPresent(value ->
            {
                ConfigData.IDENTIFIABLES.put(entryData.getUUID("UUID"), value);
                map.put(object, value);
            });
        }
        return map;
    }

    public static <K, V extends ConfigData<?>> CompoundTag serializeMultimapRegistry(Multimap<K, V> map, String key,
                                                                                     ResourceKey<Registry<V>> modRegistry,
                                                                                     Function<K, ResourceLocation> keyGetter)
    {
        Codec<V> codec = ModRegistries.getCodec(modRegistry);
        CompoundTag tag = new CompoundTag();
        CompoundTag mapTag = new CompoundTag();

        for (Map.Entry<K, Collection<V>> entry : map.asMap().entrySet())
        {
            ResourceLocation elementId = keyGetter.apply(entry.getKey());
            if (elementId == null)
            {   ColdSweat.LOGGER.error("Error serializing: \"{}\" does not exist in registry", entry.getKey());
                continue;
            }
            ListTag valuesTag = new ListTag();
            for (V value : entry.getValue())
            {
                codec.encodeStart(NbtOps.INSTANCE, value)
                .resultOrPartial(e -> ColdSweat.LOGGER.error("Error serializing {} \"{}\": {}", modRegistry.location(), elementId, e))
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

    public static <K, V extends ConfigData<?>> Multimap<K, V> deserializeMultimapRegistry(CompoundTag tag, String key,
                                                                                          ResourceKey<Registry<V>> modRegistry,
                                                                                          Function<ResourceLocation, K> keyGetter)
    {
        Codec<V> codec = ModRegistries.getCodec(modRegistry);

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
                codec.decode(NbtOps.INSTANCE, valueData).result().map(Pair::getFirst)
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

    public static <T extends IForgeRegistryEntry<T>> Codec<Either<TagKey<T>, T>> tagOrForgeRegistryCodec(ResourceKey<Registry<T>> vanillaRegistry, IForgeRegistry<T> forgeRegistry)
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

    public static <T> Codec<Either<TagKey<T>, T>> tagOrVanillaRegistryCodec(ResourceKey<Registry<T>> vanillaRegistry, Codec<Holder<T>> codec)
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
                            codec.xmap(Holder::value, Holder::direct));
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

    public static <T extends IForgeRegistryEntry<T>> String serializeTagOrBuiltinObject(IForgeRegistry<T> forgeRegistry, Either<TagKey<T>, T> obj)
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

    public static <T extends IForgeRegistryEntry<T>> Either<TagKey<T>, T> deserializeTagOrRegistryObject(String tagOrRegistryObject, ResourceKey<Registry<T>> vanillaRegistry, IForgeRegistry<T> forgeRegistry)
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
            Resource resource = resourceManager.getResource(location);
            try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))
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
}
