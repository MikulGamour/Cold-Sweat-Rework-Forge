package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.tags.ITag;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class BiomeTempData
{
    public final List<Biome> biomes;
    public final double min;
    public final double max;
    public final Temperature.Units units;
    public final boolean isOffset;
    public final Optional<List<String>> requiredMods;

    public BiomeTempData(List<Biome> biomes, double min, double max, Temperature.Units units, boolean isOffset, Optional<List<String>> requiredMods)
    {
        this.biomes = biomes;
        this.min = min;
        this.max = max;
        this.units = units;
        this.isOffset = isOffset;
        this.requiredMods = requiredMods;
    }

    public BiomeTempData(Biome biome, double min, double max, Temperature.Units units)
    {   this(Arrays.asList(biome), min, max, units, false, Optional.empty());
    }

    public BiomeTempData(List<Biome> biomes, double min, double max, Temperature.Units units)
    {   this(biomes, min, max, units, false, Optional.empty());
    }

    public static final Codec<BiomeTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.dynamicCodec(Registry.BIOME_REGISTRY).listOf().fieldOf("biomes").forGetter(data -> data.biomes),
            Codec.mapEither(Codec.DOUBLE.fieldOf("temperature"), Codec.DOUBLE.fieldOf("min_temp")).xmap(
                    either ->
                    {
                        if (either.left().isPresent()) return either.left().get();
                        if (either.right().isPresent()) return either.right().get();
                        throw new IllegalArgumentException("Biome temperature min is not defined!");
                    },
                    Either::right).forGetter(data -> data.min),
            Codec.mapEither(Codec.DOUBLE.fieldOf("temperature"), Codec.DOUBLE.fieldOf("max_temp")).xmap(
                    either ->
                    {
                        if (either.left().isPresent()) return either.left().get();
                        if (either.right().isPresent()) return either.right().get();
                        throw new IllegalArgumentException("Biome temperature min is not defined!");
                    },
                    Either::right).forGetter(data -> data.max),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(data -> data.units),
            Codec.BOOL.optionalFieldOf("is_offset", false).forGetter(data -> data.isOffset),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(data -> data.requiredMods)
    ).apply(instance, (biomes, min, max, units, isOffset, requiredMods) ->
    {
        double cMin = Temperature.convert(min, units, Temperature.Units.MC, !isOffset);
        double cMax = Temperature.convert(max, units, Temperature.Units.MC, !isOffset);
        return new BiomeTempData(biomes, cMin, cMax, units, isOffset, requiredMods);
    }));

    @Nullable
    public static BiomeTempData fromToml(List<?> data, boolean absolute, DynamicRegistries registryAccess)
    {
        String biomeIdString = (String) data.get(0);
        List<Biome> biomes = ConfigHelper.parseRegistryItems(Registry.BIOME_REGISTRY, registryAccess, biomeIdString);

        if (biomes.isEmpty())
        {   ColdSweat.LOGGER.error("Error parsing biome config: string \"{}\" does not contain any valid biomes", biomeIdString);
            return null;
        }
        if (data.size() < 3)
        {   ColdSweat.LOGGER.error("Error parsing biome config: not enough arguments");
            return null;
        }

        // The config defines a min and max value, with optional unit conversion
        Temperature.Units units = data.size() == 4 ? Temperature.Units.valueOf(((String) data.get(3)).toUpperCase()) : Temperature.Units.MC;
        double min = Temperature.convert(((Number) data.get(1)).doubleValue(), units, Temperature.Units.MC, absolute);
        double max = Temperature.convert(((Number) data.get(2)).doubleValue(), units, Temperature.Units.MC, absolute);

        // Maps the biome ID to the temperature (and variance if present)
        return new BiomeTempData(biomes, min, max, units);
    }

    @Override
    public String toString()
    {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        BiomeTempData that = (BiomeTempData) obj;
        return Double.compare(that.min, min) == 0
            && Double.compare(that.max, max) == 0
            && isOffset == that.isOffset
            && biomes.equals(that.biomes)
            && units == that.units
            && requiredMods.equals(that.requiredMods);
    }
}
