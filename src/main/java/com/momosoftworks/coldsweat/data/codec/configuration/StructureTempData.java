package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;

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

    public static final Codec<StructureTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Registry.STRUCTURE_FEATURE.listOf().fieldOf("structures").forGetter(data -> data.structures),
            Codec.DOUBLE.fieldOf("temperature").forGetter(data -> data.temperature),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(data -> data.units),
            Codec.BOOL.optionalFieldOf("offset", false).forGetter(data -> data.isOffset),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(data -> data.requiredMods)
    ).apply(instance, StructureTempData::new));

    @Override
    public String toString()
    {
        return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("");
    }
}