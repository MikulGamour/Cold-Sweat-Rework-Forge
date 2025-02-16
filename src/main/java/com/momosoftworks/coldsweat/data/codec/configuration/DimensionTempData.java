package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.DimensionType;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class DimensionTempData extends ConfigData
{
    final List<DimensionType> dimensions;
    final double temperature;
    final Temperature.Units units;
    final boolean isOffset;

    public DimensionTempData(List<DimensionType> dimensions,
                             double temperature, Temperature.Units units, boolean isOffset,
                             List<String> requiredMods)
    {
        super(requiredMods);
        this.dimensions = dimensions;
        this.temperature = temperature;
        this.units = units;
        this.isOffset = isOffset;
    }

    public DimensionTempData(List<DimensionType> dimensions,
                             double temperature, Temperature.Units units, boolean isOffset)
    {
        this(dimensions, temperature, units, isOffset, ConfigHelper.getModIDs(dimensions, Registry.DIMENSION_TYPE_REGISTRY));
    }

    public DimensionTempData(DimensionType dimension, double temperature, Temperature.Units units, boolean isOffset)
    {   this(Arrays.asList(dimension), temperature, units, isOffset);
    }

    public static final Codec<DimensionTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.dynamicCodec(Registry.DIMENSION_TYPE_REGISTRY).listOf().fieldOf("dimensions").forGetter(data -> data.dimensions),
            Codec.DOUBLE.fieldOf("temperature").forGetter(data -> data.temperature),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(data -> data.units),
            Codec.BOOL.optionalFieldOf("is_offset", false).forGetter(data -> data.isOffset),
            Codec.STRING.listOf().optionalFieldOf("required_mods", Arrays.asList()).forGetter(DimensionTempData::requiredMods)
    ).apply(instance, DimensionTempData::new));

    public List<DimensionType> dimensions()
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
    public static DimensionTempData fromToml(List<?> entry, boolean isOffset, DynamicRegistries registryAccess)
    {
        if (entry.size() < 2)
        {   ColdSweat.LOGGER.error("Error parsing dimension config: not enough arguments");
            return null;
        }
        List<DimensionType> dimensions = ConfigHelper.parseRegistryItems(Registry.DIMENSION_TYPE_REGISTRY, registryAccess, (String) entry.get(0));
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
