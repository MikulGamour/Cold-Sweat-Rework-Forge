package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.requirement.BlockRequirement;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.block.Block;
import net.minecraft.tags.ITag;
import net.minecraft.util.registry.Registry;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class BlockTempData
{
    public final List<Either<ITag<Block>, Block>> blocks;
    public final double temperature;
    public final double range;
    public final double maxEffect;
    public final boolean fade;
    public final double minTemp;
    public final double maxTemp;
    public final List<BlockRequirement> conditions;
    public final Optional<List<String>> requiredMods;

    public BlockTempData(List<Either<ITag<Block>, Block>> blocks, double temperature, double range,
                         double maxEffect, boolean fade, double maxTemp, double minTemp,
                         List<BlockRequirement> conditions, Optional<List<String>> requiredMods)
    {
        this.blocks = blocks;
        this.temperature = temperature;
        this.range = range;
        this.maxEffect = maxEffect;
        this.fade = fade;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.conditions = conditions;
        this.requiredMods = requiredMods;
    }
    public static final Codec<BlockTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrBuiltinCodec(Registry.BLOCK_REGISTRY, Registry.BLOCK).listOf().fieldOf("blocks").forGetter(data -> data.blocks),
            Codec.DOUBLE.fieldOf("temperature").forGetter(data -> data.temperature),
            Codec.DOUBLE.optionalFieldOf("range", Double.MAX_VALUE).forGetter(data -> data.range),
            Codec.DOUBLE.optionalFieldOf("max_effect", Double.MAX_VALUE).forGetter(data -> data.maxEffect),
            Codec.BOOL.optionalFieldOf("fade", true).forGetter(data -> data.fade),
            Codec.DOUBLE.optionalFieldOf("max_temp", Double.MAX_VALUE).forGetter(data -> data.maxTemp),
            Codec.DOUBLE.optionalFieldOf("min_temp", -Double.MAX_VALUE).forGetter(data -> data.minTemp),
            BlockRequirement.CODEC.listOf().optionalFieldOf("conditions", Arrays.asList()).forGetter(data -> data.conditions),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(data -> data.requiredMods)
    ).apply(instance, BlockTempData::new));

    @Override
    public String toString()
    {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
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
