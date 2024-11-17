package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public record StructureTempData(List<Either<TagKey<Structure>, Structure>> structures, double temperature,
                                Temperature.Units units, boolean isOffset, Optional<List<String>> requiredMods)
{
    public StructureTempData(Structure structure, double temperature, boolean isOffset, Temperature.Units units)
    {   this(List.of(Either.right(structure)), temperature, units, !isOffset, Optional.empty());
    }

    public StructureTempData(List<Structure> structures, double temperature, boolean isOffset, Temperature.Units units)
    {   this(structures.stream().map(Either::<TagKey<Structure>, Structure>right).toList(), temperature, units, isOffset, Optional.empty());
    }

    public static final Codec<StructureTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrVanillaRegistryCodec(Registries.STRUCTURE, Structure.CODEC).listOf().fieldOf("structures").forGetter(StructureTempData::structures),
            Codec.DOUBLE.fieldOf("temperature").forGetter(StructureTempData::temperature),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(StructureTempData::units),
            Codec.BOOL.optionalFieldOf("offset", false).forGetter(StructureTempData::isOffset),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(StructureTempData::requiredMods)
    ).apply(instance, (structures, temperature, units, isOffset, requiredMods) ->
    {
        double cTemp = Temperature.convert(temperature, units, Temperature.Units.MC, !isOffset);
        return new StructureTempData(structures, cTemp, units, isOffset, requiredMods);
    }));

    @Nullable
    public static StructureTempData fromToml(List<?> entry, boolean absolute, RegistryAccess registryAccess)
    {
        String structureIdString = (String) entry.get(0);
        List<Structure> structures = ConfigHelper.parseRegistryItems(Registries.STRUCTURE, registryAccess, structureIdString);
        if (structures.isEmpty())
        {
            ColdSweat.LOGGER.error("Error parsing structure config: string \"{}\" does not contain valid structures", structureIdString);
            return null;
        }
        double temp = ((Number) entry.get(1)).doubleValue();
        Temperature.Units units = entry.size() == 3 ? Temperature.Units.valueOf(((String) entry.get(2)).toUpperCase()) : Temperature.Units.MC;
        return new StructureTempData(structures, Temperature.convert(temp, units, Temperature.Units.MC, absolute), !absolute, units);
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

        StructureTempData that = (StructureTempData) obj;
        return Double.compare(that.temperature, temperature) == 0
            && isOffset == that.isOffset
            && structures.equals(that.structures)
            && units == that.units
            && requiredMods.equals(that.requiredMods);
    }
}