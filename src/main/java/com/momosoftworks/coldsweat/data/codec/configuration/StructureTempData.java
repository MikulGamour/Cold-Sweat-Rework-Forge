package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.tags.ITag;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StructureTempData
{
    public final List<Structure<?>> structures;
    public final double temperature;
    public final Temperature.Units units;
    public final boolean isOffset;
    public final Optional<List<String>> requiredMods;

    public StructureTempData(List<Structure<?>> structures, double temperature,
                             Temperature.Units units, boolean isOffset, Optional<List<String>> requiredMods)
    {
        this.structures = structures;
        this.temperature = temperature;
        this.units = units;
        this.isOffset = isOffset;
        this.requiredMods = requiredMods;
    }

    public StructureTempData(Structure<?> structure, double temperature, boolean isOffset, Temperature.Units units)
    {   this(Arrays.asList(structure), temperature, units, !isOffset, Optional.empty());
    }

    public StructureTempData(List<Structure<?>> structures, double temperature, boolean isOffset, Temperature.Units units)
    {   this(structures, temperature, units, isOffset, Optional.empty());
    }

    public static final Codec<StructureTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Registry.STRUCTURE_FEATURE.listOf().fieldOf("structures").forGetter(data -> data.structures),
            Codec.DOUBLE.fieldOf("temperature").forGetter(data -> data.temperature),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(data -> data.units),
            Codec.BOOL.optionalFieldOf("offset", false).forGetter(data -> data.isOffset),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(data -> data.requiredMods)
    ).apply(instance, (structures, temperature, units, isOffset, requiredMods) ->
    {
        double cTemp = Temperature.convert(temperature, units, Temperature.Units.MC, !isOffset);
        return new StructureTempData(structures, cTemp, units, isOffset, requiredMods);
    }));

    @Nullable
    public static StructureTempData fromToml(List<?> entry, boolean absolute, DynamicRegistries registryAccess)
    {
        String structureIdString = (String) entry.get(0);
        List<Structure<?>> structures = ConfigHelper.parseRegistryItems(Registry.STRUCTURE_FEATURE_REGISTRY, registryAccess, structureIdString);
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