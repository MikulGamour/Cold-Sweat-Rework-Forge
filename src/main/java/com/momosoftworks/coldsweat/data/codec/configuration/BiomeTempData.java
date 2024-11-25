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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public record BiomeTempData(List<Either<TagKey<Biome>, Holder<Biome>>> biomes, double min, double max,
                            Temperature.Units units, boolean isOffset, Optional<List<String>> requiredMods) implements ConfigData<BiomeTempData>, IForgeRegistryEntry<BiomeTempData>
{
    public BiomeTempData(Holder<Biome> biome, double min, double max, Temperature.Units units, boolean absolute)
    {   this(List.of(Either.right(biome)), min, max, units, !absolute, Optional.empty());
    }

    public BiomeTempData(Collection<Holder<Biome>> biomes, double min, double max, Temperature.Units units, boolean absolute)
    {   this(biomes.stream().map(Either::<TagKey<Biome>, Holder<Biome>>right).toList(), min, max, units, !absolute, Optional.empty());
    }

    public static final Codec<BiomeTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrHolderCodec(Registry.BIOME_REGISTRY, Biome.CODEC).listOf().fieldOf("biomes").forGetter(BiomeTempData::biomes),
            Codec.mapEither(Codec.DOUBLE.fieldOf("temperature"), Codec.DOUBLE.fieldOf("min_temp")).xmap(
                either -> either.map(left -> left, right -> right), Either::right).forGetter(BiomeTempData::min),
            Codec.mapEither(Codec.DOUBLE.fieldOf("temperature"), Codec.DOUBLE.fieldOf("max_temp")).xmap(
                either -> either.map(left -> left, right -> right), Either::right).forGetter(BiomeTempData::max),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(BiomeTempData::units),
            Codec.BOOL.optionalFieldOf("is_offset", false).forGetter(BiomeTempData::isOffset),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(BiomeTempData::requiredMods)
    ).apply(instance, BiomeTempData::new));

    public double minTemp()
    {   return Temperature.convert(min, units, Temperature.Units.MC, !this.isOffset);
    }

    public double maxTemp()
    {   return Temperature.convert(max, units, Temperature.Units.MC, !this.isOffset);
    }

    @Nullable
    public static BiomeTempData fromToml(List<?> data, boolean absolute, RegistryAccess registryAccess)
    {
        String biomeIdString = (String) data.get(0);
        List<Holder<Biome>> biomes = ConfigHelper.parseRegistryItems(Registry.BIOME_REGISTRY, registryAccess, biomeIdString);

        if (biomes.isEmpty())
        {   ColdSweat.LOGGER.error("Error parsing biome config: string \"{}\" does not contain any valid biomes", biomeIdString);
            return null;
        }
        if (data.size() < 3)
        {   ColdSweat.LOGGER.error("Error parsing biome config: not enough arguments");
            return null;
        }

        // The config defines a min and max value, with optional unit conversion
        Temperature.Units units = data.size() == 4 ? Temperature.Units.valueOf(((String) data.get(3)).toUpperCase()) : Temperature.Units.MC;
        double min = ((Number) data.get(1)).doubleValue();
        double max = ((Number) data.get(2)).doubleValue();

        // Maps the biome ID to the temperature (and variance if present)
        return new BiomeTempData(biomes, min, max, units, absolute);
    }

    @Override
    public Codec<BiomeTempData> getCodec()
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

        BiomeTempData that = (BiomeTempData) obj;
        return Double.compare(that.min, min) == 0
            && Double.compare(that.max, max) == 0
            && isOffset == that.isOffset
            && biomes.equals(that.biomes)
            && units == that.units
            && requiredMods.equals(that.requiredMods);
    }

    @Override
    public BiomeTempData setRegistryName(ResourceLocation name)
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
    public Class<BiomeTempData> getRegistryType()
    {
        return null;
    }
}
