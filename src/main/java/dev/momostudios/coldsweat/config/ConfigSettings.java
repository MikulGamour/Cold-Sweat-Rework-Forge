package dev.momostudios.coldsweat.config;

import com.mojang.datafixers.util.Pair;
import dev.momostudios.coldsweat.api.util.Temperature;
import dev.momostudios.coldsweat.util.compat.CompatManager;
import dev.momostudios.coldsweat.config.util.ConfigHelper;
import dev.momostudios.coldsweat.config.util.ValueSupplier;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import oshi.util.tuples.Triplet;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Holds almost all configs for Cold Sweat in memory for easy access.
 * Handles syncing configs between the client/server.
 */
public class ConfigSettings
{
    public static final Map<String, ValueSupplier<?>> SYNCED_SETTINGS = new HashMap<>();

    // Settings visible in the config screen
    public static final ValueSupplier<Integer> DIFFICULTY;
    public static final ValueSupplier<Double> MAX_TEMP;
    public static final ValueSupplier<Double> MIN_TEMP;
    public static final ValueSupplier<Double> TEMP_RATE;
    public static final ValueSupplier<Boolean> FIRE_RESISTANCE_ENABLED;
    public static final ValueSupplier<Boolean> ICE_RESISTANCE_ENABLED;
    public static final ValueSupplier<Boolean> DAMAGE_SCALING;
    public static final ValueSupplier<Boolean> REQUIRE_THERMOMETER;
    public static final ValueSupplier<Integer> GRACE_LENGTH;
    public static final ValueSupplier<Boolean> GRACE_ENABLED;

    // World Settings
    public static final ValueSupplier<Map<ResourceLocation, Triplet<Double, Double, Temperature.Units>>> BIOME_TEMPS;
    public static final ValueSupplier<Map<ResourceLocation, Triplet<Double, Double, Temperature.Units>>> BIOME_OFFSETS;
    public static final ValueSupplier<Map<ResourceLocation, Double>> DIMENSION_TEMPS;
    public static final ValueSupplier<Map<ResourceLocation, Double>> DIMENSION_OFFSETS;
    public static final ValueSupplier<Double[]> SUMMER_TEMPS;
    public static final ValueSupplier<Double[]> AUTUMN_TEMPS;
    public static final ValueSupplier<Double[]> WINTER_TEMPS;
    public static final ValueSupplier<Double[]> SPRING_TEMPS;

    // Item settings
    public static final ValueSupplier<Map<Item, Pair<Double, Double>>> INSULATION_ITEMS;
    public static final ValueSupplier<Map<Item, Pair<Double, Double>>> ADAPTIVE_INSULATION_ITEMS;
    public static final ValueSupplier<Map<Item, Pair<Double, Double>>> INSULATING_ARMORS;
    public static final ValueSupplier<Integer[]> INSULATION_SLOTS;

    public static final ValueSupplier<Map<Item, Double>> TEMPERATURE_FOODS;

    public static final ValueSupplier<Integer> WATERSKIN_STRENGTH;

    public static final ValueSupplier<Map<Item, Integer>> LAMP_FUEL_ITEMS;

    public static final ValueSupplier<List<ResourceLocation>> LAMP_DIMENSIONS;

    public static final ValueSupplier<Map<Item, Double>> BOILER_FUEL;
    public static final ValueSupplier<Map<Item, Double>> ICEBOX_FUEL;
    public static final ValueSupplier<Map<Item, Double>> HEARTH_FUEL;
    public static final ValueSupplier<Boolean> HEARTH_POTIONS_ENABLED;
    public static final ValueSupplier<List<ResourceLocation>> BLACKLISTED_POTIONS;

    public static final ValueSupplier<Triplet<Integer, Integer, Double>> GOAT_FUR_TIMINGS;

    // Entity Settings
    public static final ValueSupplier<Map<ResourceLocation, Integer>> CHAMELEON_BIOMES;
    public static final ValueSupplier<Map<ResourceLocation, Integer>> GOAT_BIOMES;


    // Makes the settings instantiation collapsible & easier to read
    static
    {
        DIFFICULTY = addSyncedSetting("difficulty", () -> ColdSweatConfig.getInstance().getDifficulty(),
        encoder -> ConfigHelper.writeNBTInt(encoder, "Difficulty"),
        decoder -> decoder.getInt("Difficulty"),
        saver -> ColdSweatConfig.getInstance().setDifficulty(saver));

        MAX_TEMP = addSyncedSetting("max_temp", () -> ColdSweatConfig.getInstance().getMaxTempHabitable(),
        encoder -> ConfigHelper.writeNBTDouble(encoder, "MaxTemp"),
        decoder -> decoder.getDouble("MaxTemp"),
        saver -> ColdSweatConfig.getInstance().setMaxHabitable(saver));

        MIN_TEMP = addSyncedSetting("min_temp", () -> ColdSweatConfig.getInstance().getMinTempHabitable(),
        encoder -> ConfigHelper.writeNBTDouble(encoder, "MinTemp"),
        decoder -> decoder.getDouble("MinTemp"),
        saver -> ColdSweatConfig.getInstance().setMinHabitable(saver));

        TEMP_RATE = addSyncedSetting("temp_rate", () -> ColdSweatConfig.getInstance().getRateMultiplier(),
        encoder -> ConfigHelper.writeNBTDouble(encoder, "TempRate"),
        decoder -> decoder.getDouble("TempRate"),
        saver -> ColdSweatConfig.getInstance().setRateMultiplier(saver));

        FIRE_RESISTANCE_ENABLED = addSyncedSetting("fire_resistance_enabled", () -> ColdSweatConfig.getInstance().isFireResistanceEnabled(),
        encoder -> ConfigHelper.writeNBTBoolean(encoder, "FireResistanceEnabled"),
        decoder -> decoder.getBoolean("FireResistanceEnabled"),
        saver -> ColdSweatConfig.getInstance().setFireResistanceEnabled(saver));

        ICE_RESISTANCE_ENABLED = addSyncedSetting("ice_resistance_enabled", () -> ColdSweatConfig.getInstance().isIceResistanceEnabled(),
        encoder -> ConfigHelper.writeNBTBoolean(encoder, "IceResistanceEnabled"),
        decoder -> decoder.getBoolean("IceResistanceEnabled"),
        saver -> ColdSweatConfig.getInstance().setIceResistanceEnabled(saver));

        DAMAGE_SCALING = addSyncedSetting("damage_scaling", () -> ColdSweatConfig.getInstance().doDamageScaling(),
        encoder -> ConfigHelper.writeNBTBoolean( encoder, "DamageScaling"),
        decoder -> decoder.getBoolean("DamageScaling"),
        saver -> ColdSweatConfig.getInstance().setDamageScaling(saver));

        REQUIRE_THERMOMETER = addSyncedSetting("require_thermometer", () -> ColdSweatConfig.getInstance().thermometerRequired(),
        encoder -> ConfigHelper.writeNBTBoolean(encoder, "RequireThermometer"),
        decoder -> decoder.getBoolean("RequireThermometer"),
        saver -> ColdSweatConfig.getInstance().setRequireThermometer(saver));

        GRACE_LENGTH = addSyncedSetting("grace_length", () -> ColdSweatConfig.getInstance().getGracePeriodLength(),
        encoder -> ConfigHelper.writeNBTInt(encoder, "GraceLength"),
        decoder -> decoder.getInt("GraceLength"),
        saver -> ColdSweatConfig.getInstance().setGracePeriodLength(saver));

        GRACE_ENABLED = addSyncedSetting("grace_enabled", () -> ColdSweatConfig.getInstance().isGracePeriodEnabled(),
        encoder -> ConfigHelper.writeNBTBoolean(encoder, "GraceEnabled"),
        decoder -> decoder.getBoolean("GraceEnabled"),
        saver -> ColdSweatConfig.getInstance().setGracePeriodEnabled(saver));

        BIOME_TEMPS = ValueSupplier.of(() -> ConfigHelper.getBiomesWithValues(WorldSettingsConfig.getInstance().getBiomeTemperatures(), true));

        BIOME_OFFSETS = ValueSupplier.of(() -> ConfigHelper.getBiomesWithValues(WorldSettingsConfig.getInstance().getBiomeTempOffsets(), false));

        DIMENSION_TEMPS = ValueSupplier.of(() ->
        {   Map<ResourceLocation, Double> map = new HashMap<>();

            for (List<?> entry : WorldSettingsConfig.getInstance().getDimensionTemperatures())
            {   map.put(new ResourceLocation((String) entry.get(0)), ((Number) entry.get(1)).doubleValue());
            }
            return map;
        });

        DIMENSION_OFFSETS = ValueSupplier.of(() ->
        {   Map<ResourceLocation, Double> map = new HashMap<>();

            for (List<?> entry : WorldSettingsConfig.getInstance().getDimensionTempOffsets())
            {   map.put(new ResourceLocation((String) entry.get(0)), ((Number) entry.get(1)).doubleValue());
            }
            return map;
        });

        BOILER_FUEL = ValueSupplier.of(() -> ConfigHelper.getItemsWithValues(ItemSettingsConfig.getInstance().getBoilerFuelItems()));
        HEARTH_FUEL = ValueSupplier.of(() -> ConfigHelper.getItemsWithValues(ItemSettingsConfig.getInstance().getHearthFuelItems()));
        ICEBOX_FUEL = ValueSupplier.of(() -> ConfigHelper.getItemsWithValues(ItemSettingsConfig.getInstance().getIceboxFuelItems()));

        HEARTH_POTIONS_ENABLED = ValueSupplier.of(() -> ItemSettingsConfig.getInstance().arePotionsEnabled());
        BLACKLISTED_POTIONS = ValueSupplier.of(() -> ItemSettingsConfig.getInstance().getPotionBlacklist().stream().map(ResourceLocation::new).toList());

        INSULATION_ITEMS = addSyncedSetting("insulation_items", () ->
        {
            Map<Item, Pair<Double, Double>> map = new HashMap<>();
            for (List<?> entry : ItemSettingsConfig.getInstance().getInsulationItems())
            {
                String itemID = (String) entry.get(0);
                for (Item item : ConfigHelper.getItems(itemID))
                {   map.put(item, Pair.of(((Number) entry.get(1)).doubleValue(), ((Number) entry.get(2)).doubleValue()));
                }
            }
            return map;
        },
        encoder -> ConfigHelper.writeNBTItemMap(encoder, "InsulationItems"),
        decoder -> ConfigHelper.readNBTItemMap(decoder, "InsulationItems"),
        saver ->
        {
            List<List<?>> list = new ArrayList<>();
            for (Map.Entry<Item, Pair<Double, Double>> entry : saver.entrySet())
            {   ResourceLocation itemID = ForgeRegistries.ITEMS.getKey(entry.getKey());
                if (itemID != null)
                {   list.add(Arrays.asList(itemID.toString(), entry.getValue().getFirst(), entry.getValue().getSecond()));
                }
            }
            ItemSettingsConfig.getInstance().setInsulationItems(list);
        });

        ADAPTIVE_INSULATION_ITEMS = addSyncedSetting("adaptive_insulation_items", () ->
        {
            Map<Item, Pair<Double, Double>> map = new HashMap<>();
            for (List<?> entry : ItemSettingsConfig.getInstance().getAdaptiveInsulationItems())
            {
                String itemID = (String) entry.get(0);
                for (Item item : ConfigHelper.getItems(itemID))
                {   map.put(item, Pair.of(((Number) entry.get(1)).doubleValue(), ((Number) entry.get(2)).doubleValue()));
                }
            }
            return map;
        },
        encoder -> ConfigHelper.writeNBTItemMap(encoder, "AdaptiveInsulationItems"),
        decoder -> ConfigHelper.readNBTItemMap(decoder, "AdaptiveInsulationItems"),
        saver ->
        {
            List<List<?>> list = new ArrayList<>();
            for (Map.Entry<Item, Pair<Double, Double>> entry : saver.entrySet())
            {
                ResourceLocation itemID = ForgeRegistries.ITEMS.getKey(entry.getKey());
                if (itemID != null)
                {   list.add(Arrays.asList(itemID.toString(), entry.getValue().getFirst(), entry.getValue().getSecond()));
                }
            }
            ItemSettingsConfig.getInstance().setAdaptiveInsulationItems(list);
        });

        INSULATING_ARMORS = addSyncedSetting("insulating_armors", () ->
        {
            Map<Item, Pair<Double, Double>> map = new HashMap<>();
            for (List<?> entry : ItemSettingsConfig.getInstance().getInsulatingArmorItems())
            {
                String itemID = (String) entry.get(0);
                for (Item item : ConfigHelper.getItems(itemID))
                {   map.put(item, Pair.of(((Number) entry.get(1)).doubleValue(), ((Number) entry.get(2)).doubleValue()));
                }
            }
            return map;
        },
        encoder -> ConfigHelper.writeNBTItemMap(encoder, "InsulatingArmors"),
        decoder -> ConfigHelper.readNBTItemMap(decoder, "InsulatingArmors"),
        saver ->
        {
            List<List<?>> list = new ArrayList<>();
            for (Map.Entry<Item, Pair<Double, Double>> entry : saver.entrySet())
            {   ResourceLocation itemID = ForgeRegistries.ITEMS.getKey(entry.getKey());
                if (itemID != null)
                {   list.add(Arrays.asList(itemID.toString(), entry.getValue().getFirst(), entry.getValue().getSecond()));
                }
            }
            ItemSettingsConfig.getInstance().setInsulatingArmorItems(list);
        });
        INSULATION_SLOTS = addSyncedSetting("insulation_slots", () ->
        {
            List<? extends Number> list = ItemSettingsConfig.getInstance().getArmorInsulationSlots();
            return new Integer[] { list.get(0).intValue(), list.get(1).intValue(), list.get(2).intValue(), list.get(3).intValue() };
        },
        encoder ->
        {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Head", encoder[0]);
            tag.putInt("Chest", encoder[1]);
            tag.putInt("Legs", encoder[2]);
            tag.putInt("Feet", encoder[3]);
            return tag;
        },
        decoder ->
        {
            return new Integer[] { decoder.getInt("Head"), decoder.getInt("Chest"), decoder.getInt("Legs"), decoder.getInt("Feet") };
        },
        saver ->
        {
            ItemSettingsConfig.getInstance().setArmorInsulationSlots(Arrays.asList(saver[0], saver[1], saver[2], saver[3]));
        });

        TEMPERATURE_FOODS = ValueSupplier.of(() -> ConfigHelper.getItemsWithValues(ItemSettingsConfig.getInstance().getFoodTemperatures()));

        WATERSKIN_STRENGTH = ValueSupplier.of(() -> ItemSettingsConfig.getInstance().getWaterskinStrength());

        LAMP_FUEL_ITEMS = addSyncedSetting("lamp_fuel_items", () ->
        {
            Map<Item, Integer> list = new HashMap<>();
            for (List<?> item : ItemSettingsConfig.getInstance().getSoulLampFuelItems())
            {
                ConfigHelper.getItems((String) item.get(0)).forEach(i -> list.put(i, (Integer) item.get(1)));
            }
            return list;
        },
        encoder ->
        {
            CompoundTag tag = new CompoundTag();
            for (Map.Entry<Item, Integer> entry : encoder.entrySet())
            {
                ResourceLocation itemID = ForgeRegistries.ITEMS.getKey(entry.getKey());
                if (itemID != null)
                {   tag.putInt(itemID.toString(), entry.getValue());
                }
            }
            return tag;
        },
        decoder ->
        {
            Map<Item, Integer> map = new HashMap<>();
            for (String key : decoder.getAllKeys())
            {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(key));
                if (item != null)
                {   map.put(item, decoder.getInt(key));
                }
            }
            return map;
        },
        saver ->
        {
            List<List<?>> list = new ArrayList<>();
            for (Map.Entry<Item, Integer> entry : saver.entrySet())
            {   ResourceLocation itemID = ForgeRegistries.ITEMS.getKey(entry.getKey());
                if (itemID != null)
                {   list.add(Arrays.asList(itemID.toString(), entry.getValue()));
                }
            }
            ItemSettingsConfig.getInstance().setSoulLampFuelItems(list);
        });

        LAMP_DIMENSIONS = ValueSupplier.of(() -> ItemSettingsConfig.getInstance().getValidSoulLampDimensions().stream().map(ResourceLocation::new).toList());

        GOAT_FUR_TIMINGS = addSyncedSetting("goat_fur_timings", () ->
        {
            List<?> entry = EntitySettingsConfig.getInstance().getGoatFurStats();
            return new Triplet<>(((Number) entry.get(0)).intValue(), ((Number) entry.get(1)).intValue(), ((Number) entry.get(2)).doubleValue());
        },
        triplet ->
        {
            CompoundTag tag = new CompoundTag();
            tag.put("Interval", IntTag.valueOf(triplet.getA()));
            tag.put("Cooldown", IntTag.valueOf(triplet.getB()));
            tag.put("Chance", DoubleTag.valueOf(triplet.getC()));
            return tag;
        },
        tag ->
        {
            int interval = tag.getInt("Interval");
            int cooldown = tag.getInt("Cooldown");
            double chance = tag.getDouble("Chance");
            return new Triplet<>(interval, cooldown, chance);
        },
        triplet ->
        {
            List<Number> list = new ArrayList<>();
            list.add(triplet.getA());
            list.add(triplet.getB());
            list.add(triplet.getC());
            EntitySettingsConfig.getInstance().setGoatFurStats(list);
        });

        CHAMELEON_BIOMES = ValueSupplier.of(() ->
        {
            Map<ResourceLocation, Integer> map = new HashMap<>();
            for (List<?> entry : EntitySettingsConfig.getInstance().getChameleonSpawnBiomes())
            {
                map.put(new ResourceLocation((String) entry.get(0)), ((Number) entry.get(1)).intValue());
            }
            return map;
        });

        GOAT_BIOMES = ValueSupplier.of(() ->
        {
            Map<ResourceLocation, Integer> map = new HashMap<>();
            for (List<?> entry : EntitySettingsConfig.getInstance().getGoatSpawnBiomes())
            {
                map.put(new ResourceLocation((String) entry.get(0)), ((Number) entry.get(1)).intValue());
            }
            return map;
        });

        if (CompatManager.isSereneSeasonsLoaded())
        {   SUMMER_TEMPS = ValueSupplier.of(() -> WorldSettingsConfig.getInstance().getSummerTemps());
            AUTUMN_TEMPS = ValueSupplier.of(() -> WorldSettingsConfig.getInstance().getAutumnTemps());
            WINTER_TEMPS = ValueSupplier.of(() -> WorldSettingsConfig.getInstance().getWinterTemps());
            SPRING_TEMPS = ValueSupplier.of(() -> WorldSettingsConfig.getInstance().getSpringTemps());
        }
        else
        {   SUMMER_TEMPS = ValueSupplier.of(() -> new Double[3]);
            AUTUMN_TEMPS = ValueSupplier.of(() -> new Double[3]);
            WINTER_TEMPS = ValueSupplier.of(() -> new Double[3]);
            SPRING_TEMPS = ValueSupplier.of(() -> new Double[3]);
        }
    }

    public static <T> ValueSupplier<T> addSyncedSetting(String id, Supplier<T> supplier, Function<T, CompoundTag> writer, Function<CompoundTag, T> reader, Consumer<T> saver)
    {
        ValueSupplier<T> loader = ValueSupplier.synced(supplier, writer, reader, saver);
        SYNCED_SETTINGS.put(id, loader);
        return loader;
    }

    public static Map<String, CompoundTag> encode()
    {
        Map<String, CompoundTag> map = new HashMap<>();
        SYNCED_SETTINGS.forEach((key, value) ->
        {
            if (value.isSynced())
                map.put(key, value.encode());
        });
        return map;
    }

    public static void decode(String key, CompoundTag tag)
    {
        SYNCED_SETTINGS.computeIfPresent(key, (k, value) ->
        {
            value.decode(tag);
            return value;
        });
    }

    public static void saveValues()
    {
        SYNCED_SETTINGS.values().forEach(value ->
        {
            if (value.isSynced())
                value.save();
        });
    }

    public static void load()
    {
        SYNCED_SETTINGS.values().forEach(ValueSupplier::load);
    }
}
