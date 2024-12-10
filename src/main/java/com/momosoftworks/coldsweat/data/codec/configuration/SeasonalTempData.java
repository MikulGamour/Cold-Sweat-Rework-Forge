package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.api.util.Temperature;

import java.util.List;

public class SeasonalTempData
{
    private final double start;
    private final double middle;
    private final double end;
    private final Temperature.Units units;

    public SeasonalTempData(double start, double middle, double end, Temperature.Units units)
    {
        this.start = start;
        this.middle = middle;
        this.end = end;
        this.units = units;
    }

    public static final Codec<SeasonalTempData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.DOUBLE.fieldOf("start").forGetter(SeasonalTempData::start),
            Codec.DOUBLE.fieldOf("middle").forGetter(SeasonalTempData::middle),
            Codec.DOUBLE.fieldOf("end").forGetter(SeasonalTempData::end),
            Temperature.Units.CODEC.fieldOf("units").forGetter(SeasonalTempData::units)
    ).apply(builder, SeasonalTempData::new));

    public double start()
    {   return start;
    }
    public double middle()
    {   return middle;
    }
    public double end()
    {   return end;
    }
    public Temperature.Units units()
    {   return units;
    }

    public SeasonalTempData()
    {   this(0, 0, 0, Temperature.Units.MC);
    }

    public double getStartTemp()
    {   return Temperature.convert(start, units, Temperature.Units.MC, false);
    }

    public double getMiddleTemp()
    {   return Temperature.convert(middle, units, Temperature.Units.MC, false);
    }

    public double getEndTemp()
    {   return Temperature.convert(end, units, Temperature.Units.MC, false);
    }

    public static SeasonalTempData fromToml(List<?> toml)
    {
        double start = (Double) toml.get(0);
        double middle = (Double) toml.get(1);
        double end = (Double) toml.get(2);
        Temperature.Units units = toml.size() < 4 ? Temperature.Units.MC
                                                  : (Temperature.Units)  toml.get(3);

        return new SeasonalTempData(start, middle, end, units);
    }
}
