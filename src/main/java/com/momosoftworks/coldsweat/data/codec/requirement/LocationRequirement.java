package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.data.codec.util.ExtraCodecs;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;

import java.util.Optional;

public class LocationRequirement
{
    public final Optional<Integer> x;
    public final Optional<Integer> y;
    public final Optional<Integer> z;
    public final Optional<RegistryKey<Biome>> biome;
    public final Optional<RegistryKey<Structure<?>>> structure;
    public final Optional<RegistryKey<World>> dimension;
    public final Optional<IntegerBounds> light;
    public final Optional<BlockRequirement> block;
    public final Optional<FluidRequirement> fluid;

    public LocationRequirement(Optional<Integer> x, Optional<Integer> y, Optional<Integer> z, Optional<RegistryKey<Biome>> biome,
                               Optional<RegistryKey<Structure<?>>> structure, Optional<RegistryKey<World>> dimension,
                               Optional<IntegerBounds> light, Optional<BlockRequirement> block, Optional<FluidRequirement> fluid)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.biome = biome;
        this.structure = structure;
        this.dimension = dimension;
        this.light = light;
        this.block = block;
        this.fluid = fluid;
    }

    public static final Codec<LocationRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("x").forGetter(location -> location.x),
            Codec.INT.optionalFieldOf("y").forGetter(location -> location.y),
            Codec.INT.optionalFieldOf("z").forGetter(location -> location.z),
            ExtraCodecs.codec(Registry.BIOME_REGISTRY).optionalFieldOf("biome").forGetter(location -> location.biome),
            ExtraCodecs.codec(Registry.STRUCTURE_FEATURE_REGISTRY).optionalFieldOf("structure").forGetter(location -> location.structure),
            ExtraCodecs.codec(Registry.DIMENSION_REGISTRY).optionalFieldOf("dimension").forGetter(location -> location.dimension),
            IntegerBounds.CODEC.optionalFieldOf("light").forGetter(location -> location.light),
            BlockRequirement.CODEC.optionalFieldOf("block").forGetter(location -> location.block),
            FluidRequirement.CODEC.optionalFieldOf("fluid").forGetter(location -> location.fluid)
    ).apply(instance, LocationRequirement::new));

    public boolean test(World level, Vector3d pos)
    {   return this.test(level, new BlockPos(pos));
    }

    public boolean test(World level, BlockPos origin)
    {
        BlockPos.Mutable pos = origin.mutable();
        this.x.ifPresent(x -> pos.move(x, 0, 0));
        this.y.ifPresent(y -> pos.move(0, y, 0));
        this.z.ifPresent(z -> pos.move(0, 0, z));

        if (this.dimension.isPresent()
        && !level.dimension().equals(this.dimension.get()))
        {   return false;
        }

        if (this.biome.isPresent()
        && !this.biome.get().location().equals(level.getBiome(pos).getRegistryName()))
        {   return false;
        }

        if (this.structure.isPresent()
        && WorldHelper.getServerLevel(level).structureFeatureManager().getStructureAt(pos, false,
                                                                                      level.registryAccess().registryOrThrow(Registry.STRUCTURE_FEATURE_REGISTRY).get(this.structure.get())) == StructureStart.INVALID_START)
        {   return false;
        }

        if (this.light.isPresent())
        {
            int light = level.getMaxLocalRawBrightness(pos);
            if (light < this.light.get().min || light > this.light.get().max)
            {   return false;
            }
        }
        if (this.block.isPresent() && !this.block.get().test(level, pos))
        {   return false;
        }
        if (this.fluid.isPresent() && !this.fluid.get().test(level, pos))
        {
            return false;
        }
        return true;
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

        LocationRequirement that = (LocationRequirement) obj;
        return x.equals(that.x)
            && y.equals(that.y)
            && z.equals(that.z)
            && biome.equals(that.biome)
            && structure.equals(that.structure)
            && dimension.equals(that.dimension)
            && light.equals(that.light)
            && block.equals(that.block)
            && fluid.equals(that.fluid);
    }
}
