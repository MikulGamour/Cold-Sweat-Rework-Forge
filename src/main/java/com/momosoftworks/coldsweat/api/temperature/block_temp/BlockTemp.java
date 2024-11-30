package com.momosoftworks.coldsweat.api.temperature.block_temp;

import com.google.common.collect.ImmutableSet;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Set;

public abstract class BlockTemp
{
    private final Set<Block> validBlocks;
    private final double maxEffect;
    private final double minEffect;
    private final double maxTemperature;
    private final double minTemperature;
    private final double range;
    private final boolean fade;

    public abstract double getTemperature(Level level, @Nullable LivingEntity entity, BlockState state, BlockPos pos, double distance);

    public boolean isValid(Level level, BlockPos pos, BlockState state)
    {   return true;
    }

    public BlockTemp(Block... blocks)
    {
        validBlocks = ImmutableSet.<Block>builder().add(blocks).build();
        this.minEffect = -Double.MAX_VALUE;
        this.maxEffect = Double.MAX_VALUE;
        this.minTemperature = -Double.MAX_VALUE;
        this.maxTemperature = Double.MAX_VALUE;
        this.range = Double.MAX_VALUE;
        this.fade = true;
    }

    public BlockTemp(double minEffect, double maxEffect, double minTemp, double maxTemp, double range, boolean fade, Block... blocks)
    {
        this.validBlocks = ImmutableSet.<Block>builder().add(blocks).build();
        this.minEffect = minEffect;
        this.maxEffect = maxEffect;
        this.minTemperature = minTemp;
        this.maxTemperature = maxTemp;
        this.range = range;
        this.fade = fade;
    }

    public boolean hasBlock(Block block)
    {   return validBlocks.contains(block);
    }

    public Set<Block> getAffectedBlocks()
    {   return validBlocks;
    }

    /**
     * The maximum temperature this block can emit, no matter how many there are near the player <br>
     * @return a double representing the temperature, in Minecraft units
     */
    public double maxEffect()
    {   return maxEffect;
    }

    /**
     * The minimum temperature this block can emit, no matter how many there are near the player <br>
     * (Useful for blocks with negative temperature) <br>
     * @return a double representing the temperature, in Minecraft units
     */
    public double minEffect()
    {   return minEffect;
    }

    /**
     * The maximum world temperature for this BlockTemp to be effective<br>
     * @return a double representing the temperature, in Minecraft units
     */
    public double maxTemperature()
    {   return maxTemperature;
    }

    /**
     * The minimum world temperature for this BlockTemp to be effective<br>
     * @return a double representing the temperature, in Minecraft units
     */
    public double minTemperature()
    {   return minTemperature;
    }

    public double range()
    {
        return Math.min(range, ConfigSettings.BLOCK_RANGE.get());
    }

    public boolean fade()
    {   return fade;
    }
}
