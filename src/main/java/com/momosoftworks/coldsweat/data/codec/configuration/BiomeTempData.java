package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class BiomeTempData extends ConfigData
{
    final List<Biome> biomes;
    final double min;
    final double max;
    final Temperature.Units units;
    final boolean isOffset;

    public BiomeTempData(List<Biome> biomes, double min, double max,
                         Temperature.Units units, boolean isOffset, List<String> requiredMods)
    {
        super(requiredMods);
        this.biomes = biomes;
        this.min = min;
        this.max = max;
        this.units = units;
        this.isOffset = isOffset;
    }

    public BiomeTempData(List<Biome> biomes, double min, double max,
                         Temperature.Units units, boolean isOffset)
    {
        this(biomes, min, max, units, isOffset, ConfigHelper.getModIDs(biomes, Registry.BIOME_REGISTRY));
    }

    public BiomeTempData(Biome biome, double min, double max, Temperature.Units units, boolean absolute)
    {   this(Arrays.asList(biome), min, max, units, !absolute);
    }

    public static final Codec<BiomeTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.dynamicCodec(Registry.BIOME_REGISTRY).listOf().fieldOf("biomes").forGetter(data -> data.biomes),
            Codec.mapEither(Codec.DOUBLE.fieldOf("temperature"), Codec.DOUBLE.fieldOf("min_temp")).xmap(
                    either ->
                    either.map(left -> left, right -> right),
                    Either::right).forGetter(data -> data.min),
            Codec.mapEither(Codec.DOUBLE.fieldOf("temperature"), Codec.DOUBLE.fieldOf("max_temp")).xmap(
                    either ->
                    either.map(left -> left, right -> right),
                    Either::right).forGetter(data -> data.max),
            com.momosoftworks.coldsweat.api.util.Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(data -> data.units),
            Codec.BOOL.optionalFieldOf("is_offset", false).forGetter(data -> data.isOffset),
            Codec.STRING.listOf().optionalFieldOf("required_mods", Arrays.asList()).forGetter(BiomeTempData::requiredMods)
    ).apply(instance, BiomeTempData::new));

    public List<Biome> biomes()
    {   return biomes;
    }
    public double min()
    {   return min;
    }
    public double max()
    {   return max;
    }
    public Temperature.Units units()
    {   return units;
    }
    public boolean isOffset()
    {   return isOffset;
    }

    public double minTemp()
    {   return Temperature.convert(min, units, Temperature.Units.MC, !this.isOffset);
    }
    public double maxTemp()
    {   return Temperature.convert(max, units, Temperature.Units.MC, !this.isOffset);
    }

    @Nullable
    public static BiomeTempData fromToml(List<?> data, boolean isOffset, DynamicRegistries registryAccess)
    {
        if (data.size() < 3)
        {   ColdSweat.LOGGER.error("Error parsing biome config: not enough arguments");
            return null;
        }

        List<Biome> biomes = ConfigHelper.parseRegistryItems(Registry.BIOME_REGISTRY, registryAccess, (String) data.get(0));

        if (biomes.isEmpty())
        {   ColdSweat.LOGGER.error("Error parsing biome config: {} does not contain any valid biomes", data);
            return null;
        }

        // The config defines a min and max value, with optional unit conversion
        Temperature.Units units = data.size() == 4 ? Temperature.Units.valueOf(((String) data.get(3)).toUpperCase()) : Temperature.Units.MC;
        double min = ((Number) data.get(1)).doubleValue();
        double max = ((Number) data.get(2)).doubleValue();

        // Maps the biome ID to the temperature (and variance if present)
        return new BiomeTempData(biomes, min, max, units, isOffset);
    }

    @Override
    public Codec<BiomeTempData> getCodec()
    {   return CODEC;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        BiomeTempData that = (BiomeTempData) obj;
        return super.equals(obj)
            && Double.compare(that.min, min) == 0
            && Double.compare(that.max, max) == 0
            && isOffset == that.isOffset
            && biomes.equals(that.biomes)
            && units == that.units;
    }
}
