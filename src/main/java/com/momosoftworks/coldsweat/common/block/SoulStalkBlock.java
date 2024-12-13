package com.momosoftworks.coldsweat.common.block;

import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.init.ModBlocks;
import com.momosoftworks.coldsweat.data.tag.ModBlockTags;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import org.jetbrains.annotations.Nullable;

public class SoulStalkBlock extends Block
{
    public static final IntegerProperty AGE = BlockStateProperties.AGE_15;
    public static final IntegerProperty SECTION = IntegerProperty.create("section", 0, 3);
    protected static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D);
    
    public SoulStalkBlock(Properties p_49795_)
    {
        super(p_49795_);
        this.registerDefaultState(this.defaultBlockState().setValue(AGE, 0).setValue(SECTION, 0));
    }

    public static Properties getProperties()
    {
        return Properties
                .of()
                .sound(SoundType.BIG_DRIPLEAF)
                .strength(0f, 0.5f)
                .randomTicks()
                .lightLevel(state -> state.getValue(SECTION) == 3 ? 6 : 0)
                .noOcclusion()
                .noCollission();
    }

    public static Item.Properties getItemProperties()
    {
        return new Item.Properties();
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand)
    {
        if (level.isEmptyBlock(pos.above()))
        {
            // Get the height of the plant
            int i;
            for(i = 1; level.getBlockState(pos.below(i)).getBlock() == this; ++i)
            {}

            if (i < 6 && rand.nextDouble() < 0.05 + CSMath.blend(ConfigSettings.MIN_TEMP.get(), ConfigSettings.MAX_TEMP.get(), WorldHelper.getRoughTemperatureAt(level, pos.below(i - 1)), 0, 0.95))
            {
                int j = state.getValue(AGE);
                if (CommonHooks.canCropGrow(level, pos, state, true))
                {   if (j >= 8)
                    {   level.setBlockAndUpdate(pos.above(), this.defaultBlockState().setValue(SECTION, 3));
                        int section = rand.nextDouble() < 0.3 ? 2 : 1;
                        level.setBlock(pos, state.setValue(AGE, 0).setValue(SECTION, section), 4);
                    }
                    else level.setBlock(pos, state.setValue(AGE, j + 1), 4);
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {   BlockPos pos = context.getClickedPos();
        return context.getLevel().getBlockState(pos.below()).getBlock() == this ? this.defaultBlockState().setValue(SECTION, 3)
             : context.getLevel().getBlockState(pos.above()).isAir() && this.canSurvive(this.defaultBlockState(), context.getLevel(), pos) ? this.defaultBlockState()
             : null;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState lastState, boolean p_60570_)
    {
        if (level.getBlockState(pos.below()).is(ModBlockTags.SOUL_STALK_PLACEABLE_ON))
        {
            if (level.getBlockState(pos.above()).isAir())
            {   level.setBlock(pos.above(), ModBlocks.SOUL_STALK.value().defaultBlockState().setValue(SECTION, 3), 3);
            }
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState otherState, LevelAccessor ilevel, BlockPos pos, BlockPos otherPos)
    {
        if (!this.canSurvive(state, ilevel, pos))
        {   return Blocks.AIR.defaultBlockState();
        }

        if (direction == Direction.UP)
        {
            if (otherState.getBlock() != this)
            {   return Blocks.AIR.defaultBlockState();
            }
            else if (state.getValue(SECTION) == 3)
            {   return state.setValue(SECTION, Math.random() < 0.33 ? 2 : 1);
            }
        }
        return state;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context)
    {   return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {   builder.add(AGE, SECTION);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {   BlockState below = level.getBlockState(pos.below());
        return below.is(ModBlockTags.SOUL_STALK_PLACEABLE_ON) || below.getBlock() == this;
    }
}
