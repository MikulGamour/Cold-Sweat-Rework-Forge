package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.util.ExtraCodecs;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public record BlockRequirement(Optional<List<Either<TagKey<Block>, Block>>> blocks, Optional<StateRequirement> state,
                               Optional<NbtRequirement> nbt, Optional<List<Direction>> sturdyFaces,
                               Optional<Boolean> withinWorldBounds, Optional<Boolean> replaceable,
                               boolean negate)
{
    public static final BlockRequirement NONE = new BlockRequirement(Optional.empty(), Optional.empty(), Optional.empty(),
                                                                     Optional.empty(), Optional.empty(), Optional.empty(),
                                                                     false);

    public static final Codec<BlockRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrBuiltinCodec(Registries.BLOCK, ForgeRegistries.BLOCKS).listOf().optionalFieldOf("blocks").forGetter(predicate -> predicate.blocks),
            StateRequirement.CODEC.optionalFieldOf("state").forGetter(predicate -> predicate.state),
            NbtRequirement.CODEC.optionalFieldOf("nbt").forGetter(predicate -> predicate.nbt),
            Direction.CODEC.listOf().optionalFieldOf("sturdy_faces").forGetter(predicate -> predicate.sturdyFaces),
            Codec.BOOL.optionalFieldOf("within_world_bounds").forGetter(predicate -> predicate.withinWorldBounds),
            Codec.BOOL.optionalFieldOf("replaceable").forGetter(predicate -> predicate.replaceable),
            Codec.BOOL.optionalFieldOf("negate", false).forGetter(predicate -> predicate.negate)
    ).apply(instance, BlockRequirement::new));

    public boolean test(Level level, BlockPos pos, BlockState state)
    {
        if (!level.isLoaded(pos)) return false;

        if (this.blocks.isPresent() && this.blocks.get().stream().noneMatch(either -> either.map(state::is, state::is)))
        {   return false ^ this.negate;
        }
        if (this.state.isPresent() && !this.state.get().test(state))
        {   return false ^ this.negate;
        }
        if (this.nbt.isPresent())
        {
            BlockEntity blockentity = level.getBlockEntity(pos);
            return (blockentity != null && this.nbt.get().test(blockentity.saveWithFullMetadata())) ^ this.negate;
        }
        if (this.sturdyFaces.isPresent())
        {
            for (Direction face : this.sturdyFaces.get())
            {
                if (!state.isFaceSturdy(level, pos, face))
                {   return false ^ this.negate;
                }
            }
        }
        if (this.withinWorldBounds.isPresent())
        {   return level.getWorldBorder().isWithinBounds(pos) ^ this.negate;
        }
        if (this.replaceable.isPresent())
        {   return state.isAir() || state.canBeReplaced() ^ this.negate;
        }
        return true ^ this.negate;
    }

    public boolean test(Level level, BlockPos pos)
    {
        if (!level.isLoaded(pos))
        {   return false;
        }
        return this.test(level, pos, level.getBlockState(pos));
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

        BlockRequirement that = (BlockRequirement) obj;
        return negate == that.negate
            && blocks.equals(that.blocks)
            && state.equals(that.state)
            && nbt.equals(that.nbt)
            && sturdyFaces.equals(that.sturdyFaces)
            && withinWorldBounds.equals(that.withinWorldBounds)
            && replaceable.equals(that.replaceable);
    }

    public record StateRequirement(Map<String, Object> properties)
    {
        public static final Codec<StateRequirement> CODEC = Codec.unboundedMap(Codec.STRING, ExtraCodecs.anyOf(Codec.BOOL, Codec.INT, Codec.STRING, IntegerBounds.CODEC))
                                                                 .xmap(StateRequirement::new, StateRequirement::properties);

        public static final StateRequirement NONE = new StateRequirement(new HashMap<>());

        public boolean test(BlockState state)
        {   return this.test(state.getBlock().getStateDefinition(), state);
        }

        public boolean test(FluidState state)
        {   return this.test(state.getType().getStateDefinition(), state);
        }

        public <S extends StateHolder<?, S>> boolean test(StateDefinition<?, S> stateDefinition, S state)
        {
            for (Map.Entry<String, Object> entry : this.properties.entrySet())
            {
                String key = entry.getKey();
                Object value = entry.getValue();

                Property<?> property = stateDefinition.getProperty(key);

                if (property == null)
                {   return false;
                }
                if (value instanceof IntegerBounds bounds)
                {
                    if (!property.getPossibleValues().contains(bounds.min())
                    || !property.getPossibleValues().contains(bounds.max())
                    || !bounds.test((Integer) state.getValue(property)))
                    {   return false;
                    }
                }
                else
                {
                    if (!property.getPossibleValues().contains(value)
                    || !state.getValue(property).toString().equals(value.toString()))
                    {   return false;
                    }
                }
            }
            return true;
        }

        public static StateRequirement fromToml(String[] entries, Block block)
        {   return fromToml(Arrays.asList(entries), block);
        }

        public static StateRequirement fromToml(List<String> entries, Block block)
        {
            Map<String, Object> blockPredicates = new HashMap<>();

            // Iterate predicates
            for (String predicate : entries)
            {
                // Split predicate into key-value pairs separated by "="
                String[] pair = predicate.split("=");
                String key = pair[0];
                String value = pair[1];

                // Get the property with the given name
                Property<?> property = block.getStateDefinition().getProperty(key);
                if (property != null)
                {
                    // Parse the desired value for this property
                    property.getValue(value).ifPresent(propertyValue ->
                    {   // Add a new predicate to the list
                        blockPredicates.put(key, propertyValue);
                    });
                }
            }
            return new StateRequirement(blockPredicates);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            StateRequirement that = (StateRequirement) obj;
            return properties.equals(that.properties);
        }
    }
}
