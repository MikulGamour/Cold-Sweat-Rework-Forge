package com.momosoftworks.coldsweat.api.temperature.modifier;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.EntityTempData;
import com.momosoftworks.coldsweat.util.entity.EntityHelper;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class EntitiesTempModifier extends TempModifier
{
    @Override
    protected Function<Double, Double> calculate(LivingEntity affectedEnt, Temperature.Trait trait)
    {
        World level = affectedEnt.level;
        // Search for entities in an 8-block radius
        AxisAlignedBB aabb = new AxisAlignedBB(affectedEnt.blockPosition()).move(0, affectedEnt.getBbHeight() / 2 - 0.5, 0).inflate(8);
        List<Entity> entities = affectedEnt.level.getEntities(affectedEnt, aabb, e -> e != affectedEnt);

        double totalTemp = 0;
        for (Entity nearbyEnt : entities)
        {
            // Get temperatures associated with this entity
            Collection<EntityTempData> entityTemps = ConfigSettings.ENTITY_TEMPERATURES.get().get(nearbyEnt.getType());
            for (EntityTempData tempData : entityTemps)
            {
                // Test if the entity and player meet the requirements
                if (tempData.test(nearbyEnt, affectedEnt))
                {
                    // Get the temperature of the entity (considering distance)
                    double entityTemp = tempData.getTemperatureEffect(nearbyEnt, affectedEnt);
                    // Dampen the effect by the number of solid blocks between the entities
                    AtomicInteger blocksBetween = new AtomicInteger();
                    WorldHelper.forBlocksInRay(EntityHelper.getCenterOf(affectedEnt),
                                               EntityHelper.getCenterOf(nearbyEnt),
                                               level,
                                               (state, pos) ->
                                               {
                                                   if (state.isSolidRender(level, pos))
                                                   {    blocksBetween.getAndIncrement();
                                                   }
                                               }, 3);
                    entityTemp /= blocksBetween.get() + 1;
                    // Add the temperature to the total
                    totalTemp += entityTemp;
                }
            }
        }
        final double finalTemp = totalTemp;
        return temp -> temp + finalTemp;
    }
}
