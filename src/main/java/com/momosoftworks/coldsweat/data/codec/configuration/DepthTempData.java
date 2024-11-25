package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.StringRepresentable;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public class DepthTempData implements ConfigData<DepthTempData>
{
    public final List<TempRegion> temperatures;
    public final List<DimensionType> dimensions;
    public final Optional<List<String>> requiredMods;

    public DepthTempData(List<TempRegion> temperatures, List<DimensionType> dimensions, Optional<List<String>> requiredMods)
    {   this.temperatures = temperatures;
        this.dimensions = dimensions;
        this.requiredMods = requiredMods;
    }

    public static final Codec<DepthTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TempRegion.CODEC.listOf().fieldOf("regions").forGetter(data -> data.temperatures),
            ConfigHelper.dynamicCodec(Registry.DIMENSION_TYPE_REGISTRY).listOf().fieldOf("dimensions").forGetter(data -> data.dimensions),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(data -> data.requiredMods)
    ).apply(instance, DepthTempData::new));

    public boolean withinBounds(World level, BlockPos pos)
    {
        DimensionType dim = level.dimensionType();
        if (!CSMath.anyMatch(this.dimensions, dimension -> dimension.equals(dim)))
        {   return false;
        }
        for (TempRegion region : temperatures)
        {
            if (region.withinBounds(level, pos))
            {   return true;
            }
        }
        return false;
    }

    public Double getTemperature(double temperature, BlockPos pos, World level)
    {
        for (TempRegion region : temperatures)
        {
            if (region.withinBounds(level, pos))
            {   return region.getTemperature(temperature, pos, level);
            }
        }
        return null;
    }

    @Override
    public Codec<DepthTempData> getCodec()
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

        DepthTempData that = (DepthTempData) obj;
        return temperatures.equals(that.temperatures)
            && dimensions.equals(that.dimensions)
            && requiredMods.equals(that.requiredMods);
    }

    public static class TempRegion
    {
        public static final TempRegion NONE = new TempRegion(RampType.CONSTANT, VerticalBound.NONE, VerticalBound.NONE);

        public static final Codec<TempRegion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RampType.CODEC.optionalFieldOf("type", RampType.CONSTANT).forGetter(region -> region.rampType),
                VerticalBound.CODEC.optionalFieldOf("top", VerticalBound.NONE).forGetter(region -> region.top),
                VerticalBound.CODEC.optionalFieldOf("bottom", VerticalBound.NONE).forGetter(region -> region.bottom)
        ).apply(instance, (type, top, bottom) ->
        {
            // Checks to ensure the region is valid
            // Must have at least one bound
            if (top == VerticalBound.NONE && bottom == VerticalBound.NONE) throw new IllegalArgumentException("Temperature region must have at least one bound");
            // Boundless upward
            if (top == VerticalBound.NONE)
            {   // Boundless region must be constant
                if (type != RampType.CONSTANT) throw new IllegalArgumentException("\"top\" region undefined. Boundless temperature region must have a constant temperature");
                top = new VerticalBound(VerticalAnchor.CONSTANT, Integer.MAX_VALUE, bottom.units, bottom.temperature);
            }
            // Boundless downward
            if (bottom == VerticalBound.NONE)
            {   // Boundless region must be constant
                if (type != RampType.CONSTANT) throw new IllegalArgumentException("\"bottom\" region undefined. Boundless temperature region must have a constant temperature");
                bottom = new VerticalBound(VerticalAnchor.CONSTANT, Integer.MIN_VALUE, top.units, top.temperature);
            }
            // Constant temperature ramp type must have a constant temperature
            if (type == RampType.CONSTANT && !top.temperature.equals(bottom.temperature))
            {   throw new IllegalArgumentException("Constant temperature ramp type must have a constant temperature; got " + top.temperature + " and " + bottom.temperature);
            }

            // Create the region
            return new TempRegion(type, top, bottom);
        }));

        RampType rampType;
        VerticalBound top;
        VerticalBound bottom;

        public TempRegion(RampType rampType, VerticalBound top, VerticalBound bottom)
        {
            this.rampType = rampType;
            this.top = top;
            this.bottom = bottom;
        }

        public boolean withinBounds(World level, BlockPos pos)
        {
            return pos.getY() <= top.getHeight(pos, level)
                && pos.getY() >= bottom.getHeight(pos, level);
        }

        public double getTemperature(double temperature, BlockPos pos, World level)
        {
            double topTemp = Temperature.convert(top.getTemperature(temperature), top.units, Temperature.Units.MC, true);
            double bottomTemp = Temperature.convert(bottom.getTemperature(temperature), bottom.units, Temperature.Units.MC, true);
            switch (rampType)
            {
                case CONSTANT : return pos.getY() <= bottom.getHeight(pos, level) ? bottomTemp : topTemp;
                case LINEAR : return CSMath.blend(bottomTemp,
                                                  topTemp,
                                                  pos.getY(),
                                                  bottom.getHeight(pos, level),
                                                  top.getHeight(pos, level));
                case EXPONENTIAL : return CSMath.blendExp(bottomTemp,
                                                          topTemp,
                                                          pos.getY(),
                                                          bottom.getHeight(pos, level),
                                                          top.getHeight(pos, level));
                case LOGARITHMIC : return CSMath.blendLog(bottomTemp,
                                                          topTemp,
                                                          pos.getY(),
                                                          bottom.getHeight(pos, level),
                                                          top.getHeight(pos, level));
            }
            return topTemp;
        }

        @Override
        public String toString()
        {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            TempRegion that = (TempRegion) obj;
            return rampType == that.rampType
                && top.equals(that.top)
                && bottom.equals(that.bottom);
        }
    }

    public static class VerticalBound
    {
        public static final VerticalBound NONE = new VerticalBound(VerticalAnchor.CONSTANT, 0, Temperature.Units.MC, TempContainer.NONE);

        public static final Codec<VerticalBound> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                VerticalAnchor.CODEC.optionalFieldOf("anchor", VerticalAnchor.CONSTANT).forGetter(bound -> bound.anchor),
                Codec.INT.fieldOf("depth").forGetter(bound -> bound.depth),
                Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(bound -> bound.units),
                TempContainer.CODEC.fieldOf("temperature").forGetter(bound -> bound.temperature)
        ).apply(instance, VerticalBound::new));

        public final VerticalAnchor anchor;
        public final Integer depth;
        public final Temperature.Units units;
        public final TempContainer temperature;

        public VerticalBound(VerticalAnchor anchor, Integer depth, Temperature.Units units, TempContainer temperature)
        {
            this.anchor = anchor;
            this.depth = depth;
            this.units = units;
            this.temperature = temperature;
        }

        public static class TempContainer
        {
            public static final TempContainer NONE = new TempContainer(0, ContainerType.STATIC, 1.0);

            public static final Codec<TempContainer> CODEC = Codec.either(
                RecordCodecBuilder.<TempContainer>create(instance -> instance.group(
                    Codec.DOUBLE.optionalFieldOf("value", Double.NaN).forGetter(container -> container.temperature),
                    ContainerType.CODEC.optionalFieldOf("type", ContainerType.STATIC).forGetter(container -> container.type),
                    Codec.doubleRange(0, 1).optionalFieldOf("strength", 1.0).forGetter(container -> container.strength)
                ).apply(instance, (temp, type, strength) ->
                {
                    if (type == ContainerType.STATIC && temp.equals(Double.NaN) && strength > 0)
                    {   throw new IllegalArgumentException("Static temperature container must have a temperature");
                    }
                    return new TempContainer(temp, type, strength);
                })),
                Codec.DOUBLE.xmap(d -> new TempContainer(d, ContainerType.STATIC, 1.0), container -> container.temperature)
            )
            .xmap(either -> either.map(c -> c, c -> c), container -> container.type == ContainerType.STATIC
                                                                     ? Either.right(container) : Either.left(container));

            public final double temperature;
            public final ContainerType type;
            public final double strength;

            public TempContainer(double temperature, ContainerType type, double strength)
            {
                this.temperature = temperature;
                this.type = type;
                this.strength = strength;
            }

            @Override
            public boolean equals(Object obj)
            {
                if (obj instanceof TempContainer)
                {
                    TempContainer container = ((TempContainer) obj);
                    return Double.valueOf(container.temperature).equals(temperature)
                            && container.type == type
                            && container.strength == strength;
                }
                return false;
            }
        }

        public int getHeight(BlockPos checkPos, World level)
        {
            switch (anchor)
            {
                case CONSTANT : return depth;
                case WORLD_TOP : return level.getMaxBuildHeight() + depth;
                case WORLD_BOTTOM : return 0 + depth;
                case GROUND_LEVEL : return WorldHelper.getHeight(checkPos, level) + depth;
            }
            return 0;
        }

        public double getTemperature(double temperature)
        {
            switch (this.temperature.type)
            {
                case STATIC :
                {
                    if (this.temperature.strength == 0) return temperature;
                    return CSMath.blend(temperature, this.temperature.temperature, this.temperature.strength, 0, 1);
                }
                case MIDPOINT :
                {
                    if (this.temperature.strength == 0) return temperature;
                    return CSMath.blend(temperature, (ConfigSettings.MIN_TEMP.get() + ConfigSettings.MAX_TEMP.get()) / 2, this.temperature.strength, 0, 1);
                }
            }
            return 0;
        }

        public enum ContainerType implements StringRepresentable
        {
            STATIC("static"),
            MIDPOINT("midpoint");

            private final String name;

            ContainerType(String name)
            {   this.name = name;
            }

            public static final Codec<ContainerType> CODEC = Codec.STRING.xmap(ContainerType::byName, ContainerType::getSerializedName);

            @Override
            public String getSerializedName()
            {   return name;
            }

            public static ContainerType byName(String name)
            {   for (ContainerType type : values())
                {   if (type.name.equals(name))
                    {   return type;
                    }
                }
                throw new IllegalArgumentException("Unknown special temperature value: " + name);
            }
        }

        @Override
        public String toString()
        {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            VerticalBound that = (VerticalBound) obj;
            return anchor == that.anchor
                && depth.equals(that.depth)
                && units == that.units
                && temperature.equals(that.temperature);
        }
    }

    public enum RampType implements StringRepresentable
    {
        CONSTANT("constant"),
        LINEAR("linear"),
        EXPONENTIAL("exponential"),
        LOGARITHMIC("logarithmic");

        private final String name;

        RampType(String name)
        {   this.name = name;
        }

        public static final Codec<RampType> CODEC = Codec.STRING.xmap(RampType::byName, RampType::getSerializedName);

        @Override
        public String getSerializedName()
        {   return name;
        }

        public static RampType byName(String name)
        {   for (RampType type : values())
            {   if (type.name.equals(name))
                {   return type;
                }
            }
            throw new IllegalArgumentException("Unknown ramp type: " + name);
        }
    }

    public enum VerticalAnchor implements StringRepresentable
    {
        CONSTANT("constant"),
        WORLD_TOP("world_top"),
        WORLD_BOTTOM("world_bottom"),
        GROUND_LEVEL("ground_level");

        private final String name;

        VerticalAnchor(String name)
        {   this.name = name;
        }

        public static final Codec<VerticalAnchor> CODEC = Codec.STRING.xmap(VerticalAnchor::byName, VerticalAnchor::getSerializedName);

        @Override
        public String getSerializedName()
        {   return name;
        }

        public static VerticalAnchor byName(String name)
        {   for (VerticalAnchor type : values())
            {   if (type.name.equals(name))
                {   return type;
                }
            }
            throw new IllegalArgumentException("Unknown vertical anchor: " + name);
        }
    }
}
