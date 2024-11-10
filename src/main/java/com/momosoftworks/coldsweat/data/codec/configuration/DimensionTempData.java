package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.DimensionType;

import java.util.List;
import java.util.Optional;

public class DimensionTempData
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

    public static final Codec<DimensionTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.dynamicCodec(Registry.DIMENSION_TYPE_REGISTRY).listOf().fieldOf("dimensions").forGetter(data -> data.dimensions),
            Codec.DOUBLE.fieldOf("temperature").forGetter(data -> data.temperature),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(data -> data.units),
            Codec.BOOL.optionalFieldOf("is_offset", false).forGetter(data -> data.isOffset),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(data -> data.requiredMods)
    ).apply(instance, DimensionTempData::new));

    @Override
    public String toString()
    {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
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
