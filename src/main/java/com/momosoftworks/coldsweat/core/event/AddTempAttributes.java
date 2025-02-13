package com.momosoftworks.coldsweat.core.event;

import com.momosoftworks.coldsweat.api.event.core.init.EnableTemperatureEvent;
import com.momosoftworks.coldsweat.api.event.vanilla.ServerConfigsLoadedEvent;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModAttributes;
import com.momosoftworks.coldsweat.util.registries.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.Map;

import static com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager.TEMPERATURE_ENABLED_ENTITIES;

@Mod.EventBusSubscriber
public class AddTempAttributes
{
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ForPlayer
    {
        @SubscribeEvent
        public static void addPlayerAttributes(EntityAttributeModificationEvent event)
        {
            event.add(EntityType.PLAYER, ModAttributes.COLD_DAMPENING, Double.NaN);
            event.add(EntityType.PLAYER, ModAttributes.HEAT_DAMPENING, Double.NaN);
            event.add(EntityType.PLAYER, ModAttributes.COLD_RESISTANCE, Double.NaN);
            event.add(EntityType.PLAYER, ModAttributes.HEAT_RESISTANCE, Double.NaN);
            event.add(EntityType.PLAYER, ModAttributes.BURNING_POINT, Double.NaN);
            event.add(EntityType.PLAYER, ModAttributes.FREEZING_POINT, Double.NaN);
            event.add(EntityType.PLAYER, ModAttributes.BASE_BODY_TEMPERATURE, Double.NaN);
            event.add(EntityType.PLAYER, ModAttributes.WORLD_TEMPERATURE, Double.NaN);
        }
    }

    private static final Field FORGE_ATTRIBUTES;
    static
    {
        try
        {   FORGE_ATTRIBUTES = ForgeHooks.class.getDeclaredField("FORGE_ATTRIBUTES");
            FORGE_ATTRIBUTES.setAccessible(true);
        }
        catch (NoSuchFieldException e)
        {   throw new RuntimeException(e);
        }
    }

    private static Map<EntityType<? extends LivingEntity>, AttributeSupplier> getForgeAttributes()
    {
        try
        {   return (Map<EntityType<? extends LivingEntity>, AttributeSupplier>) FORGE_ATTRIBUTES.get(null);
        }
        catch (IllegalAccessException e)
        {   throw new RuntimeException(e);
        }
    }

    @SubscribeEvent
    public static void onEntitiesCreated(ServerConfigsLoadedEvent event)
    {
        for (EntityType<?> entityType : ForgeRegistries.ENTITIES.getValues())
        {
            EntityType<? extends LivingEntity> type;
            try
            {   type = (EntityType<? extends LivingEntity>) entityType;
            }
            catch (ClassCastException e)
            {   continue;
            }

            if (type == EntityType.PLAYER) continue;

            EnableTemperatureEvent enableEvent = new EnableTemperatureEvent(type);
            MinecraftForge.EVENT_BUS.post(enableEvent);
            if (!enableEvent.isEnabled() || enableEvent.isCanceled()) continue;

            TEMPERATURE_ENABLED_ENTITIES.add(type);

            AttributeSupplier attributes = CSMath.orElse(getForgeAttributes().get(type), DefaultAttributes.getSupplier(type));
            if (attributes == null) continue;
            AttributeSupplier.Builder builder = new AttributeSupplier.Builder(attributes);

            builder.add(ModAttributes.COLD_DAMPENING, Double.NaN);
            builder.add(ModAttributes.HEAT_DAMPENING, Double.NaN);
            builder.add(ModAttributes.COLD_RESISTANCE, Double.NaN);
            builder.add(ModAttributes.HEAT_RESISTANCE, Double.NaN);
            builder.add(ModAttributes.BURNING_POINT, Double.NaN);
            builder.add(ModAttributes.FREEZING_POINT, Double.NaN);
            builder.add(ModAttributes.BASE_BODY_TEMPERATURE, Double.NaN);
            builder.add(ModAttributes.WORLD_TEMPERATURE, Double.NaN);

            getForgeAttributes().put(type, builder.build());
        }
    }

    /**
     * Enable temperature handling for chameleons
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEnableTemperatureEvent(EnableTemperatureEvent event)
    {
        if (event.getEntityType() == ModEntities.CHAMELEON)
        {   event.setEnabled(true);
        }
    }
}
