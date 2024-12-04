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
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class DepthTempData extends ConfigData
{
    final List<TempRegion> temperatures;
    final List<Either<TagKey<DimensionType>, Holder<DimensionType>>> dimensions;

    public DepthTempData(List<TempRegion> temperatures,
                         List<Either<TagKey<DimensionType>, Holder<DimensionType>>> dimensions,
                         List<String> requiredMods)
    {
        super(requiredMods);
        this.temperatures = temperatures;
        this.dimensions = dimensions;
    }

    public DepthTempData(List<TempRegion> temperatures,
                         List<Either<TagKey<DimensionType>, Holder<DimensionType>>> dimensions)
    {
        this(temperatures, dimensions, ConfigHelper.getModIDs(dimensions));
    }

    public static final Codec<DepthTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TempRegion.CODEC.listOf().fieldOf("regions").forGetter(DepthTempData::temperatures),
            ConfigHelper.tagOrHolderCodec(Registries.DIMENSION_TYPE, DimensionType.CODEC).listOf().fieldOf("dimensions").forGetter(DepthTempData::dimensions),
            Codec.STRING.listOf().optionalFieldOf("required_mods", List.of()).forGetter(DepthTempData::requiredMods)
    ).apply(instance, DepthTempData::new));

    public List<TempRegion> temperatures()
    {   return temperatures;
    }
    public List<Either<TagKey<DimensionType>, Holder<DimensionType>>> dimensions()
    {   return dimensions;
    }

    public boolean withinBounds(Level level, BlockPos pos)
    {
        Holder<DimensionType> dim = level.dimensionTypeRegistration();
        if (!CSMath.anyMatch(this.dimensions, dimension -> dimension.map(dim::is, type -> type.equals(dim))))
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

    @Nullable
    public Double getTemperature(double temperature, BlockPos pos, Level level)
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
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        DepthTempData that = (DepthTempData) obj;
        return super.equals(obj)
            && temperatures.equals(that.temperatures)
            && dimensions.equals(that.dimensions);
    }

    public record TempRegion(RampType rampType, VerticalBound top, VerticalBound bottom)
    {
        public static final TempRegion NONE = new TempRegion(RampType.CONSTANT, VerticalBound.NONE, VerticalBound.NONE);

        public static final Codec<TempRegion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RampType.CODEC.optionalFieldOf("type", RampType.CONSTANT).forGetter(TempRegion::rampType),
                VerticalBound.CODEC.optionalFieldOf("top", VerticalBound.NONE).forGetter(TempRegion::top),
                VerticalBound.CODEC.optionalFieldOf("bottom", VerticalBound.NONE).forGetter(TempRegion::bottom)
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

        public boolean withinBounds(Level level, BlockPos pos)
        {
            return pos.getY() <= top.getHeight(pos, level)
                && pos.getY() >= bottom.getHeight(pos, level);
        }

        public double getTemperature(double temperature, BlockPos pos, Level level)
        {
            double topTemp = Temperature.convert(top.getTemperature(temperature), top.units, Temperature.Units.MC, true);
            double bottomTemp = Temperature.convert(bottom.getTemperature(temperature), bottom.units, Temperature.Units.MC, true);
            return switch (rampType)
            {
                case CONSTANT ->
                {   yield pos.getY() <= bottom.getHeight(pos, level) ? bottomTemp : topTemp;
                }
                case LINEAR ->
                {
                    yield CSMath.blend(bottomTemp,
                                       topTemp,
                                       pos.getY(),
                                       bottom.getHeight(pos, level),
                                       top.getHeight(pos, level));
                }
                case EXPONENTIAL ->
                {
                    yield CSMath.blendExp(bottomTemp,
                                          topTemp,
                                          pos.getY(),
                                          bottom.getHeight(pos, level),
                                          top.getHeight(pos, level));
                }
                case LOGARITHMIC ->
                {
                    yield CSMath.blendLog(bottomTemp,
                                          topTemp,
                                          pos.getY(),
                                          bottom.getHeight(pos, level),
                                          top.getHeight(pos, level));
                }
            };
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

    public record VerticalBound(VerticalAnchor anchor, Integer depth, Temperature.Units units, TempContainer temperature)
    {
        public static final VerticalBound NONE = new VerticalBound(VerticalAnchor.CONSTANT, 0, Temperature.Units.MC, TempContainer.NONE);

        public static final Codec<VerticalBound> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                VerticalAnchor.CODEC.optionalFieldOf("anchor", VerticalAnchor.CONSTANT).forGetter(VerticalBound::anchor),
                Codec.INT.fieldOf("depth").forGetter(VerticalBound::depth),
                Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(VerticalBound::units),
                TempContainer.CODEC.fieldOf("temperature").forGetter(VerticalBound::temperature)
        ).apply(instance, VerticalBound::new));

        public record TempContainer(double temperature, ContainerType type, double strength)
        {
            public static final TempContainer NONE = new TempContainer(0, ContainerType.STATIC, 1.0);

            public static final Codec<TempContainer> CODEC = Codec.either(
                RecordCodecBuilder.<TempContainer>create(instance -> instance.group(
                    Codec.DOUBLE.optionalFieldOf("value", Double.NaN).forGetter(TempContainer::temperature),
                    ContainerType.CODEC.optionalFieldOf("type", ContainerType.STATIC).forGetter(TempContainer::type),
                    Codec.doubleRange(0, 1).optionalFieldOf("strength", 1.0).forGetter(TempContainer::strength)
                ).apply(instance, (temp, type, strength) ->
                {
                    if (type == ContainerType.STATIC && temp.equals(Double.NaN) && strength > 0)
                    {   throw new IllegalArgumentException("Static temperature container must have a temperature");
                    }
                    return new TempContainer(temp, type, strength);
                })),
                Codec.DOUBLE.xmap(d -> new TempContainer(d, ContainerType.STATIC, 1.0), TempContainer::temperature)
            )
            .xmap(either -> either.map(c -> c, c -> c), container -> container.type == ContainerType.STATIC
                                                                     ? Either.right(container) : Either.left(container));

            @Override
            public boolean equals(Object obj)
            {
                return obj instanceof TempContainer container
                    && Double.valueOf(container.temperature).equals(temperature)
                    && container.type == type
                    && container.strength == strength;
            }
        }

        public int getHeight(BlockPos checkPos, Level level)
        {
            return switch (anchor)
            {
                case CONSTANT -> depth;
                case WORLD_TOP -> level.getMaxBuildHeight() + depth;
                case WORLD_BOTTOM -> level.getMinBuildHeight() + depth;
                case GROUND_LEVEL -> WorldHelper.getHeight(checkPos, level) + depth;
            };
        }

        public double getTemperature(double temperature)
        {
            return switch (this.temperature.type)
            {
                case STATIC ->
                {
                    if (this.temperature.strength == 0) yield temperature;
                    yield CSMath.blend(temperature, this.temperature.temperature, this.temperature.strength, 0, 1);
                }
                case MIDPOINT ->
                {
                    if (this.temperature.strength == 0) yield temperature;
                    yield CSMath.blend(temperature, (ConfigSettings.MIN_TEMP.get() + ConfigSettings.MAX_TEMP.get()) / 2, this.temperature.strength, 0, 1);
                }
            };
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
