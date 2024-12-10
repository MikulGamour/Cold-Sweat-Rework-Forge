package com.momosoftworks.coldsweat.api.temperature.modifier.compat;

import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.data.codec.configuration.SeasonalTempData;
import com.momosoftworks.coldsweat.util.math.CSMath;
import corgitaco.betterweather.api.season.Season;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraft.entity.LivingEntity;

import java.util.function.Function;

/**
 * Special TempModifier class for Serene Seasons
 */
public class BetterWeatherTempModifier extends TempModifier
{
    public BetterWeatherTempModifier() {}

    @Override
    public Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait)
    {
        Season season;
        if (entity.level.dimensionType().natural() && (season = Season.getSeason(entity.level)) != null)
        {
            SeasonalTempData springTemps = ConfigSettings.SPRING_TEMPS.get();
            SeasonalTempData summerTemps = ConfigSettings.SUMMER_TEMPS.get();
            SeasonalTempData autumnTemps = ConfigSettings.AUTUMN_TEMPS.get();
            SeasonalTempData winterTemps = ConfigSettings.WINTER_TEMPS.get();

            Pair<Double, Double> startEndTemps;
            switch (season.getKey())
            {
                case AUTUMN:
                {
                    switch (season.getPhase())
                    {
                        case START : startEndTemps = Pair.of(autumnTemps.getStartTemp(),  autumnTemps.getMiddleTemp()); break;
                        case MID   : startEndTemps = Pair.of(autumnTemps.getMiddleTemp(), autumnTemps.getEndTemp()); break;
                        case END   : startEndTemps = Pair.of(autumnTemps.getEndTemp(),    winterTemps.getStartTemp()); break;
                        default : return temp -> temp;
                    }
                    break;
                }

                case WINTER:
                {
                    switch (season.getPhase())
                    {
                        case START : startEndTemps = Pair.of(winterTemps.getStartTemp(),  winterTemps.getMiddleTemp()); break;
                        case MID   : startEndTemps = Pair.of(winterTemps.getMiddleTemp(), winterTemps.getEndTemp()); break;
                        case END   : startEndTemps = Pair.of(winterTemps.getEndTemp(),    springTemps.getStartTemp()); break;
                        default : return temp -> temp;
                    }
                    break;
                }

                case SPRING:
                {
                    switch (season.getPhase())
                    {
                        case START : startEndTemps = Pair.of(springTemps.getStartTemp(),  springTemps.getMiddleTemp()); break;
                        case MID   : startEndTemps = Pair.of(springTemps.getMiddleTemp(), springTemps.getEndTemp()); break;
                        case END   : startEndTemps = Pair.of(springTemps.getEndTemp(),    summerTemps.getStartTemp()); break;
                        default : return temp -> temp;
                    }
                    break;
                }

                case SUMMER:
                {
                    switch (season.getPhase())
                    {
                        case START : startEndTemps = Pair.of(summerTemps.getStartTemp(),  summerTemps.getMiddleTemp()); break;
                        case MID   : startEndTemps = Pair.of(summerTemps.getMiddleTemp(), summerTemps.getEndTemp()); break;
                        case END   : startEndTemps = Pair.of(summerTemps.getEndTemp(),    autumnTemps.getStartTemp()); break;
                        default : return temp -> temp;
                    }
                    break;
                }

                default : return temp -> temp;
            }

            double startValue = startEndTemps.getFirst();
            double endValue = startEndTemps.getSecond();

            int yearLength = season.getYearLength();
            int phaseLength = Season.getPhaseLength(yearLength);
            return temp -> temp + (float) CSMath.blend(startValue, endValue, season.getCurrentYearTime() % phaseLength, 0, phaseLength);
        }
        else
        {   return temp -> temp;
        }
    }
}
