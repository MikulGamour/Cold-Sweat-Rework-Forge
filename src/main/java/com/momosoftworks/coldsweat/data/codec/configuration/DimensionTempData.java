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
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.dimension.DimensionType;

import javax.annotation.Nullable;
import java.util.List;

public class DimensionTempData extends ConfigData
{
    final List<Either<TagKey<DimensionType>, Holder<DimensionType>>> dimensions;
    final double temperature;
    final Temperature.Units units;
    final boolean isOffset;

    public DimensionTempData(List<Either<TagKey<DimensionType>, Holder<DimensionType>>> dimensions,
                             double temperature, Temperature.Units units, boolean isOffset,
                             List<String> requiredMods)
    {
        super(requiredMods);
        this.dimensions = dimensions;
        this.temperature = temperature;
        this.units = units;
        this.isOffset = isOffset;
    }

    public DimensionTempData(List<Either<TagKey<DimensionType>, Holder<DimensionType>>> dimensions,
                             double temperature, Temperature.Units units, boolean isOffset)
    {
        this(dimensions, temperature, units, isOffset, ConfigHelper.getModIDs(dimensions));
    }

    public DimensionTempData(Holder<DimensionType> dimension, double temperature, Temperature.Units units, boolean isOffset)
    {   this(List.of(Either.right(dimension)), temperature, units, isOffset);
    }

    public static final Codec<DimensionTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrHolderCodec(Registry.DIMENSION_TYPE_REGISTRY, DimensionType.CODEC).listOf().fieldOf("dimensions").forGetter(DimensionTempData::dimensions),
            Codec.DOUBLE.fieldOf("temperature").forGetter(DimensionTempData::temperature),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(DimensionTempData::units),
            Codec.BOOL.optionalFieldOf("is_offset", false).forGetter(DimensionTempData::isOffset),
            Codec.STRING.listOf().optionalFieldOf("required_mods", List.of()).forGetter(DimensionTempData::requiredMods)
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

    public double getTemperature()
    {   return Temperature.convert(temperature, units, Temperature.Units.MC, !isOffset);
    }

    @Nullable
    public static DimensionTempData fromToml(List<?> entry, boolean isOffset, RegistryAccess registryAccess)
    {
        if (entry.size() < 2)
        {   ColdSweat.LOGGER.error("Error parsing dimension config: not enough arguments");
            return null;
        }
        List<Either<TagKey<DimensionType>, Holder<DimensionType>>> dimensions = ConfigHelper.parseRegistryItems(Registry.DIMENSION_TYPE_REGISTRY, registryAccess, (String) entry.get(0));
        if (dimensions.isEmpty()) return null;
        double temp = ((Number) entry.get(1)).doubleValue();
        Temperature.Units units = entry.size() == 3 ? Temperature.Units.valueOf(((String) entry.get(2)).toUpperCase()) : Temperature.Units.MC;
        return new DimensionTempData(dimensions, temp, units, isOffset);
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
        return super.equals(obj)
            && Double.compare(that.temperature, temperature) == 0
            && isOffset == that.isOffset
            && dimensions.equals(that.dimensions)
            && units == that.units;
    }
}
