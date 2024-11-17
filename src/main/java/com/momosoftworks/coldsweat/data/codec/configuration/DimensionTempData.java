package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record DimensionTempData(List<Either<TagKey<DimensionType>, DimensionType>> dimensions, double temperature,
                                Temperature.Units units, boolean isOffset, Optional<List<String>> requiredMods) implements IForgeRegistryEntry<DimensionTempData>
{
    public DimensionTempData(DimensionType dimension, double temperature, Temperature.Units units)
    {   this(List.of(Either.right(dimension)), temperature, units, false, Optional.empty());
    }

    public DimensionTempData(List<DimensionType> dimensions, double temperature, Temperature.Units units)
    {   this(dimensions.stream().map(Either::<TagKey<DimensionType>, DimensionType>right).toList(), temperature, units, false, Optional.empty());
    }

    public static final Codec<DimensionTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrVanillaRegistryCodec(Registry.DIMENSION_TYPE_REGISTRY, DimensionType.CODEC).listOf().fieldOf("dimensions").forGetter(DimensionTempData::dimensions),
            Codec.DOUBLE.fieldOf("temperature").forGetter(DimensionTempData::temperature),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(DimensionTempData::units),
            Codec.BOOL.optionalFieldOf("is_offset", false).forGetter(DimensionTempData::isOffset),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(DimensionTempData::requiredMods)
    ).apply(instance, (dimensions, temperature, units, isOffset, requiredMods) ->
    {
        double cTemp = Temperature.convert(temperature, units, Temperature.Units.MC, !isOffset);
        return new DimensionTempData(dimensions, cTemp, units, isOffset, requiredMods);
    }));

    @Nullable
    public static DimensionTempData fromToml(List<?> entry, boolean absolute, RegistryAccess registryAccess)
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
        return new DimensionTempData(dimensions, Temperature.convert(temp, units, Temperature.Units.MC, absolute), units);
    }

    @Override
    public DimensionTempData setRegistryName(ResourceLocation name)
    {
        return null;
    }

    @Nullable
    @Override
    public ResourceLocation getRegistryName()
    {
        return null;
    }

    @Override
    public Class<DimensionTempData> getRegistryType()
    {
        return null;
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

        DimensionTempData that = (DimensionTempData) obj;
        return Double.compare(that.temperature, temperature) == 0
            && isOffset == that.isOffset
            && dimensions.equals(that.dimensions)
            && units == that.units
            && requiredMods.equals(that.requiredMods);
    }
}
