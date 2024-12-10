package com.momosoftworks.coldsweat.api.temperature.modifier.compat;

import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.SeasonalTempData;
import com.momosoftworks.coldsweat.util.math.CSMath;
import corgitaco.betterweather.api.season.Season;
import net.minecraft.entity.LivingEntity;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.config.SeasonsConfig;

import java.util.function.Function;

/**
 * Special TempModifier class for Serene Seasons
 */
public class SereneSeasonsTempModifier extends TempModifier
{
    public SereneSeasonsTempModifier() {}

    @Override
    public Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait)
    {
        if (SeasonsConfig.whitelistedDimensions.get().contains(entity.level.dimension().location().toString()))
        {
            ISeasonState season = SeasonHelper.getSeasonState(entity.level);

            SeasonalTempData springTemps = ConfigSettings.SPRING_TEMPS.get();
            SeasonalTempData summerTemps = ConfigSettings.SUMMER_TEMPS.get();
            SeasonalTempData autumnTemps = ConfigSettings.AUTUMN_TEMPS.get();
            SeasonalTempData winterTemps = ConfigSettings.WINTER_TEMPS.get();

            Pair<Double, Double> startEndTemps;
            switch (season.getSubSeason())
            {
                case EARLY_AUTUMN : startEndTemps = Pair.of(autumnTemps.getStartTemp(),  autumnTemps.getMiddleTemp()); break;
                case MID_AUTUMN   : startEndTemps = Pair.of(autumnTemps.getMiddleTemp(), autumnTemps.getEndTemp()); break;
                case LATE_AUTUMN  : startEndTemps = Pair.of(autumnTemps.getEndTemp(),    winterTemps.getStartTemp()); break;

                case EARLY_WINTER : startEndTemps = Pair.of(winterTemps.getStartTemp(),  winterTemps.getMiddleTemp()); break;
                case MID_WINTER   : startEndTemps = Pair.of(winterTemps.getMiddleTemp(), winterTemps.getEndTemp()); break;
                case LATE_WINTER  : startEndTemps = Pair.of(winterTemps.getEndTemp(),    springTemps.getStartTemp()); break;

                case EARLY_SPRING : startEndTemps = Pair.of(springTemps.getStartTemp(),  springTemps.getMiddleTemp()); break;
                case MID_SPRING   : startEndTemps = Pair.of(springTemps.getMiddleTemp(), springTemps.getEndTemp()); break;
                case LATE_SPRING  : startEndTemps = Pair.of(springTemps.getEndTemp(),    summerTemps.getStartTemp()); break;

                case EARLY_SUMMER : startEndTemps = Pair.of(summerTemps.getStartTemp(),  summerTemps.getMiddleTemp()); break;
                case MID_SUMMER   : startEndTemps = Pair.of(summerTemps.getMiddleTemp(), summerTemps.getEndTemp()); break;
                case LATE_SUMMER  : startEndTemps = Pair.of(summerTemps.getEndTemp(),    autumnTemps.getStartTemp()); break;

                default : return temp -> temp;
            }
            double startValue = startEndTemps.getFirst();
            double endValue = startEndTemps.getSecond();

            return temp -> temp + (float) CSMath.blend(startValue, endValue, season.getDay() % (season.getSubSeasonDuration() / season.getDayDuration()), 0, 8);
        }

        return temp -> temp;
    }
}
