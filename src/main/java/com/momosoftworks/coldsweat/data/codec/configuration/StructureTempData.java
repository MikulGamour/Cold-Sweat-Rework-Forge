package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.structure.Structure;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class StructureTempData extends ConfigData
{
    List<StructureFeature<?, ?>> structures;
    double temperature;
    Temperature.Units units;
    boolean isOffset;

    public StructureTempData(List<StructureFeature<?, ?>> structures, double temperature,
                             Temperature.Units units, boolean isOffset, List<String> requiredMods)
    {
        super(requiredMods);
        this.structures = structures;
        this.temperature = temperature;
        this.units = units;
        this.isOffset = isOffset;
    }

    public StructureTempData(List<StructureFeature<?, ?>> structures, double temperature,
                             Temperature.Units units, boolean isOffset)
    {
        this(structures, temperature, units, isOffset, ConfigHelper.getModIDs(structures, Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY));
    }

    public StructureTempData(StructureFeature<?, ?> structure, double temperature,
                             Temperature.Units units, boolean isOffset)
    {
        this(Arrays.asList(structure), temperature, units, isOffset);
    }

    public static final Codec<StructureTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.dynamicCodec(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).listOf().fieldOf("structures").forGetter(data -> data.structures),
            Codec.DOUBLE.fieldOf("temperature").forGetter(data -> data.temperature),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(data -> data.units),
            Codec.BOOL.optionalFieldOf("offset", false).forGetter(data -> data.isOffset),
            Codec.STRING.listOf().optionalFieldOf("required_mods", Arrays.asList()).forGetter(StructureTempData::requiredMods)
    ).apply(instance, StructureTempData::new));

    public List<StructureFeature<?, ?>> structures()
    {   return structures;
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
    {   return Temperature.convert(temperature, units, Temperature.Units.MC, isOffset);
    }

    @Nullable
    public static StructureTempData fromToml(List<?> entry, boolean isOffset, DynamicRegistries registryAccess)
    {
        if (entry.size() < 2)
        {   ColdSweat.LOGGER.error("Error parsing structure config: {} does not have enough arguments", entry);
            return null;
        }
        List<StructureFeature<?, ?>> structures = ConfigHelper.parseRegistryItems(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, registryAccess, (String) entry.get(0));
        if (structures.isEmpty()) return null;
        double temp = ((Number) entry.get(1)).doubleValue();
        Temperature.Units units = entry.size() == 3 ? Temperature.Units.valueOf(((String) entry.get(2)).toUpperCase()) : Temperature.Units.MC;

        return new StructureTempData(structures, temp, units, isOffset);
    }

    @Override
    public Codec<StructureTempData> getCodec()
    {   return CODEC;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        StructureTempData that = (StructureTempData) obj;
        return super.equals(obj)
            && Double.compare(that.temperature, temperature) == 0
            && isOffset == that.isOffset
            && structures.equals(that.structures)
            && units == that.units;
    }
}