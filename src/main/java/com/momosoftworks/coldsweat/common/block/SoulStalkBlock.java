package com.momosoftworks.coldsweat.common.block;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.itemgroup.ColdSweatGroup;
import com.momosoftworks.coldsweat.data.tag.ModBlockTags;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModBlocks;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootContext;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.IPlantable;

import java.util.Random;

public class SoulStalkBlock extends Block implements IPlantable
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
                .of(Material.PLANT)
                .sound(SoundType.CROP)
                .strength(0f, 0.5f)
                .randomTicks()
                .lightLevel(state -> state.getValue(SECTION) == 3 ? 6 : 0)
                .noOcclusion()
                .noCollission();
    }

    public static Item.Properties getItemProperties()
    {
        return new Item.Properties().tab(ColdSweatGroup.COLD_SWEAT);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld level, BlockPos pos, Random rand)
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
                if (ForgeHooks.onCropsGrowPre(level, pos, state, true))
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

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context)
    {   BlockPos pos = context.getClickedPos();
        return context.getLevel().getBlockState(pos.below()).getBlock() == this ? this.defaultBlockState().setValue(SECTION, 3)
             : context.getLevel().getBlockState(pos.above()).isAir() && this.canSurvive(this.defaultBlockState(), context.getLevel(), pos) ? this.defaultBlockState()
             : null;
    }

    @Override
    public void onPlace(BlockState state, World world, BlockPos pos, BlockState lastState, boolean p_60570_)
    {
        if (world.getBlockState(pos.below()).is(ModBlockTags.SOUL_STALK_PLACEABLE_ON))
        {
            if (world.getBlockState(pos.above()).isAir())
            {   world.setBlock(pos.above(), ModBlocks.SOUL_STALK.defaultBlockState().setValue(SECTION, 3), 3);
            }
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState otherState, IWorld ilevel, BlockPos pos, BlockPos otherPos)
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
    public VoxelShape getShape(BlockState state, IBlockReader getter, BlockPos pos, ISelectionContext context)
    {   return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
    {   builder.add(AGE, SECTION);
    }

    @Override
    public BlockState getPlant(IBlockReader level, BlockPos pos)
    {   return this.defaultBlockState();
    }

    @Override
    public boolean canSurvive(BlockState state, IWorldReader level, BlockPos pos)
    {   BlockState below = level.getBlockState(pos.below());
        return below.is(ModBlockTags.SOUL_STALK_PLACEABLE_ON) || below.getBlock() == this;
    }
}
