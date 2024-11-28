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
import net.minecraft.block.Block;
import net.minecraft.tags.ITag;
import net.minecraft.util.registry.Registry;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class BlockTempData implements ConfigData<BlockTempData>
{
    public final List<Either<ITag<Block>, Block>> blocks;
    public final double temperature;
    public final double range;
    public final double maxEffect;
    public final boolean fade;
    public final double minTemp;
    public final double maxTemp;
    public final Temperature.Units units;
    public final List<BlockRequirement> conditions;
    public final Optional<List<String>> requiredMods;

    public BlockTempData(List<Either<ITag<Block>, Block>> blocks, double temperature, double range,
                         double maxEffect, boolean fade, double maxTemp, double minTemp,
                         Temperature.Units units, List<BlockRequirement> conditions,
                         Optional<List<String>> requiredMods)
    {
        this.blocks = blocks;
        this.temperature = temperature;
        this.range = range;
        this.maxEffect = maxEffect;
        this.fade = fade;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.units = units;
        this.conditions = conditions;
        this.requiredMods = requiredMods;
    }

    public BlockTempData(Collection<Block> blocks, double temperature, double range, double maxEffect, boolean fade, double maxTemp,
                         double minTemp, Temperature.Units units, List<BlockRequirement> conditions)
    {
        this(blocks.stream().map(Either::<ITag<Block>, Block>right).collect(Collectors.toList()),
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
             Arrays.asList());
    }

    public static final Codec<BlockTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrBuiltinCodec(Registry.BLOCK_REGISTRY, Registry.BLOCK).listOf().fieldOf("blocks").forGetter(data -> data.blocks),
            Codec.DOUBLE.fieldOf("temperature").forGetter(data -> data.temperature),
            Codec.DOUBLE.optionalFieldOf("range", Double.MAX_VALUE).forGetter(data -> data.range),
            Codec.DOUBLE.optionalFieldOf("max_effect", Double.MAX_VALUE).forGetter(data -> data.maxEffect),
            Codec.BOOL.optionalFieldOf("fade", true).forGetter(data -> data.fade),
            Codec.DOUBLE.optionalFieldOf("max_temp", Double.MAX_VALUE).forGetter(data -> data.maxTemp),
            Codec.DOUBLE.optionalFieldOf("min_temp", -Double.MAX_VALUE).forGetter(data -> data.minTemp),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(data -> data.units),
            BlockRequirement.CODEC.listOf().optionalFieldOf("conditions", Arrays.asList()).forGetter(data -> data.conditions),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(data -> data.requiredMods)
    ).apply(instance, (blocks, temperature, range, maxEffect, fade, maxTemp, minTemp, units, conditions, requiredMods) ->
    {
        double cTemp = Temperature.convert(temperature, units, Temperature.Units.MC, false);
        double cMaxEffect = Temperature.convert(maxEffect, units, Temperature.Units.MC, false);
        double cMaxTemp = Temperature.convert(maxTemp, units, Temperature.Units.MC, false);
        double cMinTemp = Temperature.convert(minTemp, units, Temperature.Units.MC, false);
        return new BlockTempData(blocks, cTemp, range, cMaxEffect, fade, cMaxTemp, cMinTemp, units, conditions, requiredMods);
    }));

    @Nullable
    public static BlockTempData fromToml(List<?> entry)
    {
        if (entry.size() < 3)
        {   return null;
        }
        // Get IDs associated with this config entry
        String[] blockIDs = ((String) entry.get(0)).split(",");

        // Parse block IDs into blocks
        Block[] effectBlocks = Arrays.stream(blockIDs).map(ConfigHelper::getBlocks).flatMap(List::stream).toArray(Block[]::new);
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
        BlockRequirement.StateRequirement blockPredicates = entry.size() > 4 && entry.get(4) instanceof String && !((String) entry.get(4)).isEmpty()
                                                            ? BlockRequirement.StateRequirement.fromToml(((String) entry.get(4)).split(","), effectBlocks[0])
                                                            : BlockRequirement.StateRequirement.NONE;

        NbtRequirement tag = entry.size() > 5 && entry.get(5) instanceof String && !((String) entry.get(5)).isEmpty()
                             ? new NbtRequirement(NBTHelper.parseCompoundNbt(((String) entry.get(5))))
                             : new NbtRequirement();

        double tempLimit = entry.size() > 6
                           ? ((Number) entry.get(6)).doubleValue()
                           : Double.MAX_VALUE;

        double maxEffect = blockTemp > 0 ?  maxChange :  Double.MAX_VALUE;

        double maxTemperature = blockTemp > 0 ? tempLimit : Double.MAX_VALUE;
        double minTemperature = blockTemp < 0 ? tempLimit : -Double.MAX_VALUE;

        BlockRequirement blockRequirement = new BlockRequirement(Optional.empty(), Optional.of(blockPredicates), Optional.of(tag),
                                                                 Optional.empty(), Optional.empty(), Optional.empty(), false);

        return new BlockTempData(Arrays.asList(effectBlocks), blockTemp, blockRange, maxEffect, true, maxTemperature, minTemperature, Temperature.Units.MC, Arrays.asList(blockRequirement));
    }

    @Override
    public Codec<BlockTempData> getCodec()
    {   return CODEC;
    }

    @Override
    public String toString()
    {   return this.asString();
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
