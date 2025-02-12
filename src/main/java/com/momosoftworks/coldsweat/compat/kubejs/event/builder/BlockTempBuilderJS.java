package com.momosoftworks.coldsweat.compat.kubejs.event.builder;

import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTemp;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import dev.latvian.kubejs.world.BlockContainerJS;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BlockTempBuilderJS
{
    public final Set<Block> blocks = new HashSet<>();
    public double maxEffect = Double.MAX_VALUE;
    public double maxTemperature = Double.MAX_VALUE;
    public double minTemperature = -Double.MAX_VALUE;
    public double range = ConfigSettings.BLOCK_RANGE.get();
    public boolean fade = true;
    public Predicate<BlockContainerJS> predicate = blockInstance -> true;

    public BlockTempBuilderJS()
    {}

    public BlockTempBuilderJS blocks(String... items)
    {
        this.blocks.addAll(Arrays.stream(items).map(key -> ForgeRegistries.BLOCKS.getValue(new ResourceLocation(key))).collect(Collectors.toList()));
        return this;
    }

    public BlockTempBuilderJS blockTag(String tag)
    {
        blocks.addAll(BlockTags.getAllTags().getTag(new ResourceLocation(tag)).getValues());
        return this;
    }

    public BlockTempBuilderJS maxEffect(double maxEffect)
    {
        this.maxEffect = maxEffect;
        return this;
    }

    public BlockTempBuilderJS maxTemperature(double maxTemperature)
    {
        this.maxTemperature = maxTemperature;
        return this;
    }

    public BlockTempBuilderJS minTemperature(double minTemperature)
    {
        this.minTemperature = minTemperature;
        return this;
    }

    public BlockTempBuilderJS range(double range)
    {
        this.range = range;
        return this;
    }

    public BlockTempBuilderJS fades(boolean fade)
    {
        this.fade = fade;
        return this;
    }

    public BlockTempBuilderJS blockPredicate(Predicate<BlockContainerJS> predicate)
    {
        this.predicate = predicate;
        return this;
    }

    public interface Function
    {
        double getTemperature(World level, LivingEntity entity, BlockState state, BlockPos pos, double distance);
    }

    public BlockTemp build(Function function)
    {
        return new BlockTemp(-maxEffect, maxEffect, minTemperature, maxTemperature, range, fade,
                             blocks.toArray(new Block[0]))
        {
            @Override
            public double getTemperature(World level, LivingEntity entity, BlockState state, BlockPos pos, double distance)
            {
                if (predicate.test(new BlockContainerJS(level, pos)))
                {   return function.getTemperature(level, entity, state, pos, distance);
                }
                return 0;
            }
        };
    }
}
