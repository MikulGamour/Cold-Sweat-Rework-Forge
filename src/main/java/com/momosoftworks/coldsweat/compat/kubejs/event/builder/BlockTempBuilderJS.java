package com.momosoftworks.coldsweat.compat.kubejs.event.builder;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTemp;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import dev.latvian.mods.kubejs.level.BlockContainerJS;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
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
        this.blocks.addAll(RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.BLOCKS, ConfigHelper.getBlocks(blocks)));
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

    public BlockTempBuilderJS state(String name, Object value)
    {
        Optional<Block> block = this.blocks.stream().findFirst();
        if (block.isEmpty())
        {   ColdSweat.LOGGER.error("No blocks have been added to this KubeJS block temp yet. Cannot add state check {{} = {}}", name, value);
            return this;
        }
        // Get the property with the given name
        Property<?> property = block.get().getStateDefinition().getProperty(name);
        if (property != null)
        {
            // Parse the desired value for this property
            property.getValue(value.toString()).ifPresent(propertyValue ->
            {   // Append the new predicate to the existing one
                predicate = predicate.and(blockJS -> blockJS.getBlockState().getValue(property) == propertyValue);
            });
        }
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
