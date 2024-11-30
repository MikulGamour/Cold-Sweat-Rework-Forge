package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public class FluidRequirement
{
    private final Optional<List<Either<ITag<Fluid>, Fluid>>> fluids;
    private final Optional<ITag<Fluid>> tag;
    private final Optional<BlockRequirement.StateRequirement> state;
    private final Optional<NbtRequirement> nbt;

    public FluidRequirement(Optional<List<Either<ITag<Fluid>, Fluid>>> fluids, Optional<ITag<Fluid>> tag, Optional<BlockRequirement.StateRequirement> state, Optional<NbtRequirement> nbt)
    {
        this.fluids = fluids;
        this.tag = tag;
        this.state = state;
        this.nbt = nbt;
    }

    public static final Codec<FluidRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrBuiltinCodec(Registry.FLUID_REGISTRY, Registry.FLUID).listOf().optionalFieldOf("fluids").forGetter(predicate -> predicate.fluids),
            ITag.codec(FluidTags::getAllTags).optionalFieldOf("tag").forGetter(predicate -> predicate.tag),
            BlockRequirement.StateRequirement.CODEC.optionalFieldOf("state").forGetter(predicate -> predicate.state),
            NbtRequirement.CODEC.optionalFieldOf("nbt").forGetter(predicate -> predicate.nbt)
    ).apply(instance, FluidRequirement::new));

    public Optional<List<Either<ITag<Fluid>, Fluid>>> fluids()
    {   return fluids;
    }
    public Optional<ITag<Fluid>> tag()
    {   return tag;
    }
    public Optional<BlockRequirement.StateRequirement> state()
    {   return state;
    }
    public Optional<NbtRequirement> nbt()
    {   return nbt;
    }

    public boolean test(World pLevel, BlockPos pPos)
    {
        if (!pLevel.isLoaded(pPos))
        {   return false;
        }
        else
        {   FluidState lState = pLevel.getFluidState(pPos);
            return this.test(lState);
        }
    }

    public boolean test(FluidState fluidState)
    {
        if (this.tag.isPresent() && !fluidState.is(this.tag.get()))
        {   return false;
        }
        else if (this.fluids.isPresent() && fluids.get().stream().noneMatch(either -> either.map(tag -> tag.contains(fluidState.getType()), fluid -> fluid == fluidState.getType())))
        {   return false;
        }
        else
        {   return !this.state.isPresent() || state.get().test(fluidState);
        }
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

        FluidRequirement that = (FluidRequirement) obj;
        return fluids.equals(that.fluids)
            && tag.equals(that.tag)
            && state.equals(that.state)
            && nbt.equals(that.nbt);
    }
}
