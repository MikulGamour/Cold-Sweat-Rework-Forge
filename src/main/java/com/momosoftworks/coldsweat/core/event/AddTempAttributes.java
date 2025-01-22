package com.momosoftworks.coldsweat.core.event;

import com.google.common.collect.Maps;
import com.momosoftworks.coldsweat.api.event.core.init.EnableTemperatureEvent;
import com.momosoftworks.coldsweat.api.event.vanilla.ServerConfigsLoadedEvent;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModAttributes;
import com.momosoftworks.coldsweat.util.registries.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
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
    private static final Field ATTRIBUTE_MAP_INSTANCES;
    static
    {
        try
        {   FORGE_ATTRIBUTES = ForgeHooks.class.getDeclaredField("FORGE_ATTRIBUTES");
            ATTRIBUTE_MAP_INSTANCES = ObfuscationReflectionHelper.findField(AttributeModifierMap.class, "field_233802_a_");
            FORGE_ATTRIBUTES.setAccessible(true);
            ATTRIBUTE_MAP_INSTANCES.setAccessible(true);
        }
        catch (NoSuchFieldException e)
        {   throw new RuntimeException(e);
        }
    }

    private static Map<EntityType<? extends LivingEntity>, AttributeModifierMap> getForgeAttributes()
    {
        try
        {   return (Map<EntityType<? extends LivingEntity>, AttributeModifierMap>) FORGE_ATTRIBUTES.get(null);
        }
        catch (IllegalAccessException e)
        {   throw new RuntimeException(e);
        }
    }
    private static Map<Attribute, ModifiableAttributeInstance> getAttributeInstances(AttributeModifierMap map)
    {
        try
        {   return (Map<Attribute, ModifiableAttributeInstance>) ATTRIBUTE_MAP_INSTANCES.get(map);
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

            AttributeModifierMap attributes = CSMath.orElse(getForgeAttributes().get(type), GlobalEntityTypeAttributes.getSupplier(type));
            if (attributes == null) continue;
            Builder builder = new Builder(attributes);

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

    public static class Builder
    {
        private final Map<Attribute, ModifiableAttributeInstance> builder = Maps.newHashMap();
        private boolean instanceFrozen;
        private final List<Builder> others = new ArrayList();

        public Builder()
        {}

        public Builder(AttributeModifierMap attributeMap)
        {   this.builder.putAll(getAttributeInstances(attributeMap));
        }

        public void combine(Builder other)
        {   this.builder.putAll(other.builder);
            this.others.add(other);
        }

        public boolean hasAttribute(Attribute attribute)
        {   return this.builder.containsKey(attribute);
        }

        private ModifiableAttributeInstance create(Attribute pAttribute)
        {
            ModifiableAttributeInstance attributeinstance = new ModifiableAttributeInstance(pAttribute, (p_22273_) -> {
                if (this.instanceFrozen)
                {   throw new UnsupportedOperationException("Tried to change value for default attribute instance: " + Registry.ATTRIBUTE.getKey(pAttribute));
                }
            });
            this.builder.put(pAttribute, attributeinstance);
            return attributeinstance;
        }

        public Builder add(Attribute pAttribute)
        {   this.create(pAttribute);
            return this;
        }

        public Builder add(Attribute pAttribute, double pValue)
        {   ModifiableAttributeInstance attributeinstance = this.create(pAttribute);
            attributeinstance.setBaseValue(pValue);
            return this;
        }

        public AttributeModifierMap build()
        {   this.instanceFrozen = true;
            this.others.forEach((p_70141_) -> p_70141_.instanceFrozen = true);
            return new AttributeModifierMap(this.builder);
        }
    }
}
