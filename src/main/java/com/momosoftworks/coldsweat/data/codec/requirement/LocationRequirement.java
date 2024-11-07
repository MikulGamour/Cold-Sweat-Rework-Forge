package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public record LocationRequirement(Optional<Integer> x, Optional<Integer> y, Optional<Integer> z,
                                  Optional<Either<TagKey<Biome>, ResourceKey<Biome>>> biome,
                                  Optional<Either<TagKey<ConfiguredStructureFeature<?, ?>>, ResourceKey<ConfiguredStructureFeature<?, ?>>>> structure,
                                  Optional<Either<TagKey<Level>, ResourceKey<Level>>> dimension,
                                  Optional<IntegerBounds> light, Optional<BlockRequirement> block,
                                  Optional<FluidRequirement> fluid)
{
    public static final Codec<LocationRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("x").forGetter(location -> location.x),
            Codec.INT.optionalFieldOf("y").forGetter(location -> location.y),
            Codec.INT.optionalFieldOf("z").forGetter(location -> location.z),
            ConfigHelper.tagOrResourceKeyCodec(Registry.BIOME_REGISTRY).optionalFieldOf("biome").forGetter(location -> location.biome),
            ConfigHelper.tagOrResourceKeyCodec(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).optionalFieldOf("structure").forGetter(location -> location.structure),
            ConfigHelper.tagOrResourceKeyCodec(Registry.DIMENSION_REGISTRY).optionalFieldOf("dimension").forGetter(location -> location.dimension),
            IntegerBounds.CODEC.optionalFieldOf("light").forGetter(location -> location.light),
            BlockRequirement.CODEC.optionalFieldOf("block").forGetter(location -> location.block),
            FluidRequirement.CODEC.optionalFieldOf("fluid").forGetter(location -> location.fluid)
    ).apply(instance, LocationRequirement::new));

    public boolean test(Level level, Vec3 pos)
    {   return this.test(level, new BlockPos(pos));
    }

    public boolean test(Level level, BlockPos origin)
    {
        BlockPos.MutableBlockPos pos = origin.mutable();
        this.x.ifPresent(x -> pos.move(x, 0, 0));
        this.y.ifPresent(y -> pos.move(0, y, 0));
        this.z.ifPresent(z -> pos.move(0, 0, z));

        if (this.dimension.isPresent()
        && !this.dimension.get().map(tag -> level.dimensionTypeRegistration().is(tag.location()),
                                     key -> level.dimension().equals(key)))
        {   return false;
        }

        if (this.biome.isPresent()
        && !this.biome.get().map(tag -> level.getBiomeManager().getNoiseBiomeAtPosition(pos).is(tag),
                                 key -> level.getBiomeManager().getNoiseBiomeAtPosition(pos).is(key)))
        {   return false;
        }

        if (this.structure.isPresent())
        {
            StructureFeatureManager structureManager = WorldHelper.getServerLevel(level).structureFeatureManager();
            StructureStart structureStart = this.structure.get().map(tag -> WorldHelper.getStructureWithPieceAt(structureManager, pos, tag),
                                                                     key -> structureManager.getStructureWithPieceAt(pos, key));
            if (structureStart == null || structureStart == StructureStart.INVALID_START)
            {   return false;
            }
        }

        if (this.light.isPresent())
        {
            int light = level.getMaxLocalRawBrightness(pos);
            if (light < this.light.get().min() || light > this.light.get().max())
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
    public boolean equals(Object obj)
    {
        if (this == obj)
        {   return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {   return false;
        }

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

    @Override
    public String toString()
    {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
    }
}
