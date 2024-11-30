package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.dimension.DimensionType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class DimensionTempData extends ConfigData
{
    final List<Either<TagKey<DimensionType>, Holder<DimensionType>>> dimensions;
    final double temperature;
    final Temperature.Units units;
    final boolean isOffset;
    final Optional<List<String>> requiredMods;

    public DimensionTempData(List<Either<TagKey<DimensionType>, Holder<DimensionType>>> dimensions,
                             double temperature, Temperature.Units units, boolean isOffset,
                             Optional<List<String>> requiredMods)
    {
        this.dimensions = dimensions;
        this.temperature = temperature;
        this.units = units;
        this.isOffset = isOffset;
        this.requiredMods = requiredMods;
    }

    public DimensionTempData(Holder<DimensionType> dimension, double temperature, Temperature.Units units)
    {   this(List.of(Either.right(dimension)), temperature, units, false, Optional.empty());
    }

    public DimensionTempData(List<Holder<DimensionType>> dimensions, double temperature, Temperature.Units units)
    {   this(dimensions.stream().map(Either::<TagKey<DimensionType>, Holder<DimensionType>>right).toList(), temperature, units, false, Optional.empty());
    }

    public static final Codec<DimensionTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrHolderCodec(Registries.DIMENSION_TYPE, DimensionType.CODEC).listOf().fieldOf("dimensions").forGetter(DimensionTempData::dimensions),
            Codec.DOUBLE.fieldOf("temperature").forGetter(DimensionTempData::temperature),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(DimensionTempData::units),
            Codec.BOOL.optionalFieldOf("is_offset", false).forGetter(DimensionTempData::isOffset),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(DimensionTempData::requiredMods)
    ).apply(instance, DimensionTempData::new));

    public List<Either<TagKey<DimensionType>, Holder<DimensionType>>> dimensions()
    {   return dimensions;
    }
    public double temperature()
    {   return temperature;
    }
    public Temperature.Units units()
    {   return units;
    }
    public boolean isOffset()
    {   return isOffset;
    }
    public Optional<List<String>> requiredMods()
    {   return requiredMods;
    }

    public double getTemperature()
    {   return Temperature.convert(temperature, units, Temperature.Units.MC, !isOffset);
    }

    @Nullable
    public static DimensionTempData fromToml(List<?> entry, boolean absolute, RegistryAccess registryAccess)
    {
        String dimensionIdString = (String) entry.get(0);
        List<Holder<DimensionType>> dimensions = ConfigHelper.parseRegistryItems(Registries.DIMENSION_TYPE, registryAccess, dimensionIdString);
        if (dimensions.isEmpty())
        {   ColdSweat.LOGGER.error("Error parsing dimension config: string \"{}\" does not contain valid dimensions", dimensionIdString);
            return null;
        }
        if (entry.size() < 2)
        {
            ColdSweat.LOGGER.error("Error parsing dimension config: not enough arguments");
            return null;
        }
        double temp = ((Number) entry.get(1)).doubleValue();
        Temperature.Units units = entry.size() == 3 ? Temperature.Units.valueOf(((String) entry.get(2)).toUpperCase()) : Temperature.Units.MC;
        return new DimensionTempData(dimensions, Temperature.convert(temp, units, Temperature.Units.MC, absolute), units);
    }

    @Override
    public Codec<DimensionTempData> getCodec()
    {   return CODEC;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        DimensionTempData that = (DimensionTempData) obj;
        return Double.compare(that.temperature, temperature) == 0
            && isOffset == that.isOffset
            && dimensions.equals(that.dimensions)
            && units == that.units
            && requiredMods.equals(that.requiredMods);
    }
}
