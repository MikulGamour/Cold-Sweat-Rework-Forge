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
import javax.xml.ws.Holder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class DimensionTempData implements ConfigData<DimensionTempData>
{
    public final List<DimensionType> dimensions;
    public final double temperature;
    public final Temperature.Units units;
    public final boolean isOffset;
    public final Optional<List<String>> requiredMods;

    public DimensionTempData(List<DimensionType> dimensions, double temperature,
                             Temperature.Units units, boolean isOffset, Optional<List<String>> requiredMods)
    {
        this.dimensions = dimensions;
        this.temperature = temperature;
        this.units = units;
        this.isOffset = isOffset;
        this.requiredMods = requiredMods;
    }

    public DimensionTempData(List<DimensionType> dimensions, double temperature, Temperature.Units units, boolean isOffset)
    {   this(dimensions, temperature, units, isOffset, Optional.empty());
    }

    public DimensionTempData(DimensionType dimension, double temperature, Temperature.Units units, boolean isOffset)
    {   this(Arrays.asList(dimension), temperature, units, isOffset);
    }

    public static final Codec<DimensionTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.dynamicCodec(Registry.DIMENSION_TYPE_REGISTRY).listOf().fieldOf("dimensions").forGetter(data -> data.dimensions),
            Codec.DOUBLE.fieldOf("temperature").forGetter(data -> data.temperature),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(data -> data.units),
            Codec.BOOL.optionalFieldOf("is_offset", false).forGetter(data -> data.isOffset),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(data -> data.requiredMods)
    ).apply(instance, (dimensions, temperature, units, isOffset, requiredMods) ->
    {
        double cTemp = Temperature.convert(temperature, units, Temperature.Units.MC, !isOffset);
        return new DimensionTempData(dimensions, cTemp, units, isOffset, requiredMods);
    }));

    @Nullable
    public static DimensionTempData fromToml(List<?> entry, boolean isOffset, DynamicRegistries registryAccess)
    {
        String dimensionIdString = (String) entry.get(0);
        List<DimensionType> dimensions = ConfigHelper.parseRegistryItems(Registry.DIMENSION_TYPE_REGISTRY, registryAccess, dimensionIdString);
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
        return new DimensionTempData(dimensions, temp, units, isOffset);
    }

    @Override
    public Codec<DimensionTempData> getCodec()
    {   return CODEC;
    }

    @Override
    public String toString()
    {   return this.asString();
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
