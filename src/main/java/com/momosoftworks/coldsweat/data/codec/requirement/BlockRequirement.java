package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.util.ExtraCodecs;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.DirectionalPlaceContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.nbt.StringNBT;
import net.minecraft.state.Property;
import net.minecraft.state.StateContainer;
import net.minecraft.state.StateHolder;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BlockRequirement
{
    public final Optional<List<Either<ITag<Block>, Block>>> blocks;
    public final Optional<StateRequirement> state;
    public final Optional<NbtRequirement> nbt;
    public final Optional<Direction> sturdyFace;
    public final Optional<Boolean> withinWorldBounds;
    public final Optional<Boolean> replaceable;
    public final boolean negate;

    public BlockRequirement(Optional<List<Either<ITag<Block>, Block>>> blocks, Optional<StateRequirement> state,
                            Optional<NbtRequirement> nbt, Optional<Direction> sturdyFace,
                            Optional<Boolean> withinWorldBounds, Optional<Boolean> replaceable,
                            boolean negate)
    {
        this.blocks = blocks;
        this.state = state;
        this.nbt = nbt;
        this.sturdyFace = sturdyFace;
        this.withinWorldBounds = withinWorldBounds;
        this.replaceable = replaceable;
        this.negate = negate;
    }

    public static final BlockRequirement NONE = new BlockRequirement(Optional.empty(), Optional.empty(), Optional.empty(),
                                                                     Optional.empty(), Optional.empty(), Optional.empty(),
                                                                     false);

    public static final Codec<BlockRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrBuiltinCodec(Registry.BLOCK_REGISTRY, Registry.BLOCK).listOf().optionalFieldOf("blocks").forGetter(predicate -> predicate.blocks),
            StateRequirement.CODEC.optionalFieldOf("state").forGetter(predicate -> predicate.state),
            NbtRequirement.CODEC.optionalFieldOf("nbt").forGetter(predicate -> predicate.nbt),
            Codec.STRING.xmap(Direction::byName, Direction::getName).optionalFieldOf("has_sturdy_face").forGetter(predicate -> predicate.sturdyFace),
            Codec.BOOL.optionalFieldOf("within_world_bounds").forGetter(predicate -> predicate.withinWorldBounds),
            Codec.BOOL.optionalFieldOf("replaceable").forGetter(predicate -> predicate.replaceable),
            Codec.BOOL.optionalFieldOf("negate", false).forGetter(predicate -> predicate.negate)
    ).apply(instance, BlockRequirement::new));

    public boolean test(World pLevel, BlockPos pPos)
    {
        if (!pLevel.isLoaded(pPos))
        {   return false;
        }
        else
        {
            BlockState blockstate = pLevel.getBlockState(pPos);
            if (this.blocks.isPresent() && this.blocks.get().stream().noneMatch(either -> either.map(blockstate::is, blockstate::is)))
            {   return false ^ this.negate;
            }
            else if (this.state.isPresent() && !this.state.get().test(blockstate))
            {   return false ^ this.negate;
            }
            else if (this.nbt.isPresent())
            {
                TileEntity blockentity = pLevel.getBlockEntity(pPos);
                return (blockentity != null && this.nbt.get().test(blockentity.save(new CompoundNBT()))) ^ this.negate;
            }
            else if (this.sturdyFace.isPresent())
            {   return blockstate.isFaceSturdy(pLevel, pPos, this.sturdyFace.get()) ^ this.negate;
            }
            else if (this.withinWorldBounds.isPresent())
            {   return pLevel.getWorldBorder().isWithinBounds(pPos) ^ this.negate;
            }
            else if (this.replaceable.isPresent())
            {   return blockstate.isAir() || blockstate.canBeReplaced(new DirectionalPlaceContext(pLevel, pPos, Direction.DOWN, ItemStack.EMPTY, Direction.UP)) ^ this.negate;
            }
            else
            {   return true ^ this.negate;
            }
        }
    }

    public CompoundNBT serialize()
    {   return (CompoundNBT) CODEC.encodeStart(NBTDynamicOps.INSTANCE, this).result().orElseGet(CompoundNBT::new);
    }

    public static BlockRequirement deserialize(CompoundNBT tag)
    {   return CODEC.decode(NBTDynamicOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalArgumentException("Could not deserialize BlockRequirement")).getFirst();
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

        BlockRequirement that = (BlockRequirement) obj;

        if (!blocks.equals(that.blocks))
        {   return false;
        }
        if (!state.equals(that.state))
        {   return false;
        }
        if (!nbt.equals(that.nbt))
        {   return false;
        }
        if (!sturdyFace.equals(that.sturdyFace))
        {   return false;
        }
        if (!withinWorldBounds.equals(that.withinWorldBounds))
        {   return false;
        }
        if (!replaceable.equals(that.replaceable))
        {   return false;
        }
        return negate == that.negate;
    }

    public static class StateRequirement
    {
        public Map<String, Object> properties;

        public StateRequirement(Map<String, Object> properties)
        {   this.properties = properties;
        }

        public static final Codec<StateRequirement> CODEC = Codec.unboundedMap(Codec.STRING, ExtraCodecs.anyOf(Codec.BOOL, Codec.INT, Codec.STRING, IntegerBounds.CODEC))
                                                                 .xmap(StateRequirement::new, req -> req.properties);

        public CompoundNBT serialize()
        {   return (CompoundNBT) CODEC.encodeStart(NBTDynamicOps.INSTANCE, this).result().orElseGet(CompoundNBT::new);
        }

        public static StateRequirement deserialize(CompoundNBT tag)
        {   return CODEC.decode(NBTDynamicOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalArgumentException("Could not deserialize BlockRequirement")).getFirst();
        }

        public boolean test(BlockState state)
        {   return this.test(state.getBlock().getStateDefinition(), state);
        }

        public boolean test(FluidState state)
        {   return this.test(state.getType().getStateDefinition(), state);
        }

        public <S extends StateHolder<?, S>> boolean test(StateContainer<?, S> stateDefinition, S state)
        {
            for (Map.Entry<String, Object> entry : this.properties.entrySet())
            {
                String key = entry.getKey();
                Object value = entry.getValue();

                Property<?> property = stateDefinition.getProperty(key);

                if (property == null)
                {   return false;
                }
                if (value instanceof IntegerBounds)
                {
                    IntegerBounds bounds = (IntegerBounds) value;
                    if (!property.getPossibleValues().contains(bounds.min)
                    || !property.getPossibleValues().contains(bounds.max)
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

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {   return true;
            }
            if (obj == null || getClass() != obj.getClass())
            {   return false;
            }

            StateRequirement that = (StateRequirement) obj;

            return properties.equals(that.properties);
        }
    }

    @Override
    public String toString()
    {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
    }
}
