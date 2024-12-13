package com.momosoftworks.coldsweat.compat.kubejs.event.builder;

import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTemp;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import dev.latvian.mods.kubejs.level.BlockContainerJS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class BlockTempBuilderJS
{
    public final Set<Block> blocks = new HashSet<>();
    public double maxEffect = Double.MAX_VALUE;
    public double maxTemperature = Double.MAX_VALUE;
    public double minTemperature = -Double.MAX_VALUE;
    public double range = ConfigSettings.BLOCK_RANGE.get();
    public boolean fade = true;
    public Temperature.Units units = Temperature.Units.MC;
    public Predicate<BlockContainerJS> predicate = blockInstance -> true;

    public BlockTempBuilderJS()
    {}

    public BlockTempBuilderJS blocks(String... blocks)
    {
        this.blocks.addAll(Arrays.stream(blocks).map(key -> ForgeRegistries.BLOCKS.getValue(new ResourceLocation(key))).toList());
        return this;
    }

    public BlockTempBuilderJS blockTag(String tag)
    {
        blocks.addAll(ForgeRegistries.BLOCKS.tags().getTag(TagKey.create(Registries.BLOCK, new ResourceLocation(tag))).stream().toList());
        return this;
    }

    public BlockTempBuilderJS maxEffect(double maxEffect)
    {
        this.maxEffect = Temperature.convert(maxEffect, units, Temperature.Units.MC, false);
        return this;
    }

    public BlockTempBuilderJS maxTemperature(double maxTemperature)
    {
        this.maxTemperature = Temperature.convert(maxTemperature, units, Temperature.Units.MC, false);
        return this;
    }

    public BlockTempBuilderJS minTemperature(double minTemperature)
    {
        this.minTemperature = Temperature.convert(minTemperature, units, Temperature.Units.MC, false);
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

    public BlockTempBuilderJS units(Temperature.Units units)
    {
        this.units = units;
        return this;
    }

    @FunctionalInterface
    public interface Function
    {
        double getTemperature(Level level, LivingEntity entity, BlockState state, BlockPos pos, double distance);
    }

    public BlockTemp build(Function function)
    {
        return new BlockTemp(-maxEffect, maxEffect, minTemperature, maxTemperature, range, fade,
                             blocks.toArray(new Block[0]))
        {
            @Override
            public double getTemperature(Level level, LivingEntity entity, BlockState state, BlockPos pos, double distance)
            {
                if (predicate.test(new BlockContainerJS(level, pos)))
                {   return Temperature.convert(function.getTemperature(level, entity, state, pos, distance), units, Temperature.Units.MC, false);
                }
                return 0;
            }
        };
    }
}
