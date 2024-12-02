package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTemp;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.requirement.BlockRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.NbtRequirement;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

public class BlockTempData extends ConfigData
{
    final List<Either<TagKey<Block>, Block>> blocks;
    final double temperature;
    final double range;
    final double maxEffect;
    final boolean fade;
    final double maxTemp;
    final double minTemp;
    final Temperature.Units units;
    final List<BlockRequirement> conditions;
    final Optional<List<String>> requiredMods;

    public BlockTempData(List<Either<TagKey<Block>, Block>> blocks, double temperature, double range,
                         double maxEffect, boolean fade, double maxTemp, double minTemp,
                         Temperature.Units units, List<BlockRequirement> conditions,
                         Optional<List<String>> requiredMods)
    {
        this.blocks = blocks;
        this.temperature = temperature;
        this.range = range;
        this.maxEffect = maxEffect;
        this.fade = fade;
        this.maxTemp = maxTemp;
        this.minTemp = minTemp;
        this.units = units;
        this.conditions = conditions;
        this.requiredMods = requiredMods;
    }

    public BlockTempData(Collection<Block> blocks, double temperature, double range, double maxEffect, boolean fade, double maxTemp,
                         double minTemp, Temperature.Units units, List<BlockRequirement> conditions)
    {
        this(blocks.stream().map(Either::<TagKey<Block>, Block>right).toList(),
             temperature, range, maxEffect, fade, maxTemp, minTemp, units, conditions, Optional.empty());
    }

    /**
     * Creates a BlockTempData from a Java BlockTemp.<br>
     * <br>
     * !! The resulting BlockTempData will not have a temperature, as it is defined solely in the BlockTemp's getTemperature() method.
     */
    public BlockTempData(BlockTemp blockTemp)
    {
        this(blockTemp.getAffectedBlocks(), 0, blockTemp.range(), blockTemp.maxEffect(),
             true, blockTemp.maxTemperature(), blockTemp.minTemperature(), Temperature.Units.MC,
             List.of());
    }

    public static final Codec<BlockTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrBuiltinCodec(Registries.BLOCK, ForgeRegistries.BLOCKS).listOf().fieldOf("blocks").forGetter(BlockTempData::blocks),
            Codec.DOUBLE.fieldOf("temperature").forGetter(BlockTempData::temperature),
            Codec.DOUBLE.optionalFieldOf("range", Double.MAX_VALUE).forGetter(BlockTempData::range),
            Codec.DOUBLE.optionalFieldOf("max_effect", Double.MAX_VALUE).forGetter(BlockTempData::maxEffect),
            Codec.BOOL.optionalFieldOf("fade", true).forGetter(BlockTempData::fade),
            Codec.DOUBLE.optionalFieldOf("max_temp", Double.MAX_VALUE).forGetter(BlockTempData::maxTemp),
            Codec.DOUBLE.optionalFieldOf("min_temp", -Double.MAX_VALUE).forGetter(BlockTempData::minTemp),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(BlockTempData::units),
            BlockRequirement.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(BlockTempData::conditions),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(BlockTempData::requiredMods)
    ).apply(instance, BlockTempData::new));

    public List<Either<TagKey<Block>, Block>> blocks()
    {   return blocks;
    }
    public double temperature()
    {   return temperature;
    }
    public double range()
    {   return range;
    }
    public double maxEffect()
    {   return maxEffect;
    }
    public boolean fade()
    {   return fade;
    }
    public double maxTemp()
    {   return maxTemp;
    }
    public double minTemp()
    {   return minTemp;
    }
    public Temperature.Units units()
    {   return units;
    }
    public List<BlockRequirement> conditions()
    {   return conditions;
    }
    public Optional<List<String>> requiredMods()
    {   return requiredMods;
    }

    public double getTemperature()
    {   return Temperature.convert(temperature, units, Temperature.Units.MC, false);
    }
    public double getMaxEffect()
    {   return Temperature.convert(maxEffect, units, Temperature.Units.MC, false);
    }
    public double getMaxTemp()
    {   return Temperature.convert(maxTemp, units, Temperature.Units.MC, false);
    }
    public double getMinTemp()
    {   return Temperature.convert(minTemp, units, Temperature.Units.MC, false);
    }

    @Nullable
    public static BlockTempData fromToml(List<?> entry)
    {
        if (entry.size() < 3)
        {   return null;
        }
        // Get IDs associated with this config entry
        String[] blockIDs = ((String) entry.get(0)).split(",");

        // Parse block IDs into blocks
        Block[] effectBlocks = Arrays.stream(blockIDs)
                               .map(ConfigHelper::getBlocks)
                               .map(blocks -> RegistryHelper.mapForgeRegistryTagList(ForgeRegistries.BLOCKS, blocks))
                               .flatMap(List::stream)
                               .toArray(Block[]::new);
        if (effectBlocks.length == 0)
        {   return null;
        }

        // Temp of block
        final double blockTemp = ((Number) entry.get(1)).doubleValue();
        // Range of effect
        final double blockRange = ((Number) entry.get(2)).doubleValue();

        // Get min/max effect
        final double maxChange = entry.size() > 3 && entry.get(3) instanceof Number
                                 ? ((Number) entry.get(3)).doubleValue()
                                 : Double.MAX_VALUE;

        // Get block predicate
        Optional<BlockRequirement.StateRequirement> blockPredicates = entry.size() > 4 && entry.get(4) instanceof String str && !str.isEmpty()
                                                                      ? Optional.of(BlockRequirement.StateRequirement.fromToml(str.split(","), effectBlocks[0]))
                                                                      : Optional.empty();

        Optional<NbtRequirement> nbtRequirement = entry.size() > 5 && entry.get(5) instanceof String str && !str.isEmpty()
                                                  ? Optional.of(new NbtRequirement(NBTHelper.parseCompoundNbt(str)))
                                                  : Optional.empty();

        double tempLimit = entry.size() > 6
                           ? ((Number) entry.get(6)).doubleValue()
                           : Double.MAX_VALUE;

        double maxEffect = blockTemp > 0 ?  maxChange :  Double.MAX_VALUE;

        double maxTemperature = blockTemp > 0 ? tempLimit : Double.MAX_VALUE;
        double minTemperature = blockTemp < 0 ? tempLimit : -Double.MAX_VALUE;

        BlockRequirement blockRequirement = new BlockRequirement(Optional.empty(), blockPredicates, nbtRequirement,
                                                                 Optional.empty(), Optional.empty(), Optional.empty(), false);

        return new BlockTempData(Arrays.asList(effectBlocks), blockTemp, blockRange, maxEffect, true, maxTemperature, minTemperature, Temperature.Units.MC, List.of(blockRequirement));
    }

    @Override
    public Codec<BlockTempData> getCodec()
    {   return CODEC;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        BlockTempData that = (BlockTempData) obj;
        return Double.compare(that.temperature, temperature) == 0
            && Double.compare(that.range, range) == 0
            && Double.compare(that.maxEffect, maxEffect) == 0
            && Double.compare(that.maxTemp, maxTemp) == 0
            && Double.compare(that.minTemp, minTemp) == 0
            && fade == that.fade
            && blocks.equals(that.blocks)
            && conditions.equals(that.conditions)
            && requiredMods.equals(that.requiredMods);
    }
}
