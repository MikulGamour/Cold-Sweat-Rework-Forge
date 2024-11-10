package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;

public class BiomeTempData
{
    public final List<Biome> biomes;
    public final double min;
    public final double max;
    public final Temperature.Units units;
    public final boolean isOffset;
    public final Optional<List<String>> requiredMods;

    public BiomeTempData(List<Biome> biomes, double min, double max, Temperature.Units units, boolean isOffset, Optional<List<String>> requiredMods)
    {
        this.biomes = biomes;
        this.min = min;
        this.max = max;
        this.units = units;
        this.isOffset = isOffset;
        this.requiredMods = requiredMods;
    }

    public static final Codec<BiomeTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.dynamicCodec(Registry.BIOME_REGISTRY).listOf().fieldOf("biomes").forGetter(data -> data.biomes),
            Codec.mapEither(Codec.DOUBLE.fieldOf("temperature"), Codec.DOUBLE.fieldOf("min_temp")).xmap(
                    either ->
                    {
                        if (either.left().isPresent()) return either.left().get();
                        if (either.right().isPresent()) return either.right().get();
                        throw new IllegalArgumentException("Biome temperature min is not defined!");
                    },
                    Either::right).forGetter(data -> data.min),
            Codec.mapEither(Codec.DOUBLE.fieldOf("temperature"), Codec.DOUBLE.fieldOf("max_temp")).xmap(
                    either ->
                    {
                        if (either.left().isPresent()) return either.left().get();
                        if (either.right().isPresent()) return either.right().get();
                        throw new IllegalArgumentException("Biome temperature min is not defined!");
                    },
                    Either::right).forGetter(data -> data.max),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(data -> data.units),
            Codec.BOOL.optionalFieldOf("is_offset", false).forGetter(data -> data.isOffset),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(data -> data.requiredMods)
    ).apply(instance, BiomeTempData::new));

    @Override
    public String toString()
    {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        BiomeTempData that = (BiomeTempData) obj;
        return Double.compare(that.min, min) == 0
            && Double.compare(that.max, max) == 0
            && isOffset == that.isOffset
            && biomes.equals(that.biomes)
            && units == that.units
            && requiredMods.equals(that.requiredMods);
    }
}
