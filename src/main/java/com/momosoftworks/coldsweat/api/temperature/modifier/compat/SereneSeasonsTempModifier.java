package com.momosoftworks.coldsweat.api.temperature.modifier.compat;

import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.SeasonalTempData;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.world.entity.LivingEntity;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.config.ServerConfig;

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
        if (ServerConfig.whitelistedDimensions.get().contains(entity.level.dimension().location().toString()))
        {
            ISeasonState season = SeasonHelper.getSeasonState(entity.level);

            SeasonalTempData springTemps = ConfigSettings.SPRING_TEMPS.get();
            SeasonalTempData summerTemps = ConfigSettings.SUMMER_TEMPS.get();
            SeasonalTempData autumnTemps = ConfigSettings.AUTUMN_TEMPS.get();
            SeasonalTempData winterTemps = ConfigSettings.WINTER_TEMPS.get();

            Pair<Double, Double> startEndTemps = switch (season.getSubSeason())
            {
                case EARLY_AUTUMN -> Pair.of(autumnTemps.getStartTemp(),  autumnTemps.getMiddleTemp());
                case MID_AUTUMN   -> Pair.of(autumnTemps.getMiddleTemp(), autumnTemps.getEndTemp());
                case LATE_AUTUMN  -> Pair.of(autumnTemps.getEndTemp(),    winterTemps.getStartTemp());

                case EARLY_WINTER -> Pair.of(winterTemps.getStartTemp(),  winterTemps.getMiddleTemp());
                case MID_WINTER   -> Pair.of(winterTemps.getMiddleTemp(), winterTemps.getEndTemp());
                case LATE_WINTER  -> Pair.of(winterTemps.getEndTemp(),    springTemps.getStartTemp());

                case EARLY_SPRING -> Pair.of(springTemps.getStartTemp(),  springTemps.getMiddleTemp());
                case MID_SPRING   -> Pair.of(springTemps.getMiddleTemp(), springTemps.getEndTemp());
                case LATE_SPRING  -> Pair.of(springTemps.getEndTemp(),    summerTemps.getStartTemp());

                case EARLY_SUMMER -> Pair.of(summerTemps.getStartTemp(),  summerTemps.getMiddleTemp());
                case MID_SUMMER   -> Pair.of(summerTemps.getMiddleTemp(), summerTemps.getEndTemp());
                case LATE_SUMMER  -> Pair.of(summerTemps.getEndTemp(),    autumnTemps.getStartTemp());
            };
            double startValue = startEndTemps.getFirst();
            double endValue = startEndTemps.getSecond();

            return temp -> temp + (float) CSMath.blend(startValue, endValue, season.getDay() % (season.getSubSeasonDuration() / season.getDayDuration()), 0, 8);
        }

        return temp -> temp;
    }
}
