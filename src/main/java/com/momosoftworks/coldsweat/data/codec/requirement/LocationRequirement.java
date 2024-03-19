package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public record LocationRequirement(Optional<Integer> x, Optional<Integer> y, Optional<Integer> z,
                                  Optional<ResourceKey<Biome>> biome, Optional<ResourceKey<Structure>> structure,
                                  Optional<ResourceKey<Level>> dimension,
                                  Optional<IntegerBounds> light, Optional<BlockRequirement> block,
                                  Optional<FluidRequirement> fluid)
{
    public static final Codec<LocationRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("x").forGetter(location -> location.x),
            Codec.INT.optionalFieldOf("y").forGetter(location -> location.y),
            Codec.INT.optionalFieldOf("z").forGetter(location -> location.z),
            ResourceKey.codec(Registries.BIOME).optionalFieldOf("biome").forGetter(location -> location.biome),
            ResourceKey.codec(Registries.STRUCTURE).optionalFieldOf("structure").forGetter(location -> location.structure),
            ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension").forGetter(location -> location.dimension),
            IntegerBounds.CODEC.optionalFieldOf("light").forGetter(location -> location.light),
            BlockRequirement.CODEC.optionalFieldOf("block").forGetter(location -> location.block),
            FluidRequirement.CODEC.optionalFieldOf("fluid").forGetter(location -> location.fluid)
    ).apply(instance, LocationRequirement::new));

    public boolean test(Level level, Vec3 pos)
    {   return this.test(level, (int) pos.x(), (int) pos.y(), (int) pos.z());
    }

    public boolean test(Level level, int x, int y, int z)
    {
        if (this.dimension.isPresent()
        && !level.dimension().equals(this.dimension.get()))
        {   return false;
        }

        if (this.biome.isPresent()
        && !level.getBiome(new BlockPos(x, y, z)).unwrapKey().get().equals(this.biome.get()))
        {   return false;
        }

        if (this.structure.isPresent()
        && WorldHelper.getServerLevel(level).structureManager().getStructureWithPieceAt(BlockPos.containing(x, y, z), this.structure.get()) == StructureStart.INVALID_START)
        {   return false;
        }

        if (this.light.isPresent())
        {
            int light = level.getMaxLocalRawBrightness(new BlockPos(x, y, z));
            if (light < this.light.get().min() || light > this.light.get().max())
            {   return false;
            }
        }
        if (this.block.isPresent() && !this.block.get().test(level, BlockPos.containing(x, y, z)))
        {   return false;
        }
        if (this.fluid.isPresent() && !this.fluid.get().test(level, BlockPos.containing(x, y, z)))
        {
            return false;
        }
        return true;
    }

    public CompoundTag serialize()
    {
        CompoundTag tag = new CompoundTag();
        x.ifPresent(value -> tag.putInt("x", value));
        y.ifPresent(value -> tag.putInt("y", value));
        z.ifPresent(value -> tag.putInt("z", value));
        biome.ifPresent(value -> tag.putString("biome", value.location().toString()));
        structure.ifPresent(value -> tag.putString("structure", value.location().toString()));
        dimension.ifPresent(value -> tag.putString("dimension", value.location().toString()));
        light.ifPresent(bounds -> tag.put("light", bounds.serialize()));
        block.ifPresent(predicate -> tag.put("block", predicate.serialize()));
        fluid.ifPresent(predicate -> tag.put("fluid", predicate.serialize()));
        return tag;
    }

    public static LocationRequirement deserialize(CompoundTag tag)
    {
        Optional<Integer> x = tag.contains("x") ? Optional.of(tag.getInt("x")) : Optional.empty();
        Optional<Integer> y = tag.contains("y") ? Optional.of(tag.getInt("y")) : Optional.empty();
        Optional<Integer> z = tag.contains("z") ? Optional.of(tag.getInt("z")) : Optional.empty();
        Optional<ResourceKey<Biome>> biome = tag.contains("biome") ? Optional.of(ResourceKey.create(Registries.BIOME, new ResourceLocation(tag.getString("biome"))) ) : Optional.empty();
        Optional<ResourceKey<Structure>> structure = tag.contains("structure") ? Optional.of(ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(tag.getString("structure"))) ) : Optional.empty();
        Optional<ResourceKey<Level>> dimension = tag.contains("dimension") ? Optional.of(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(tag.getString("dimension"))) ) : Optional.empty();
        Optional<IntegerBounds> light = tag.contains("light") ? Optional.of(IntegerBounds.deserialize(tag.getCompound("light"))) : Optional.empty();
        Optional<BlockRequirement> block = tag.contains("block") ? Optional.of(BlockRequirement.deserialize(tag.getCompound("block"))) : Optional.empty();
        Optional<FluidRequirement> fluid = tag.contains("fluid") ? Optional.of(FluidRequirement.deserialize(tag.getCompound("fluid"))) : Optional.empty();
        return new LocationRequirement(x, y, z, biome, structure, dimension, light, block, fluid);
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
    {
        return "Location{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", biome=" + biome +
                ", structure=" + structure +
                ", dimension=" + dimension +
                ", light=" + light +
                ", block=" + block +
                ", fluid=" + fluid +
                '}';
    }
}
