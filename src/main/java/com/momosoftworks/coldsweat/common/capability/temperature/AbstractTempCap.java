package com.momosoftworks.coldsweat.common.capability.temperature;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.event.common.temperautre.TemperatureChangedEvent;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.Temperature.Trait;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.advancement.trigger.ModAdvancementTriggers;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModDamageSources;
import com.momosoftworks.coldsweat.util.registries.ModEffects;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager.*;

/**
 * Holds all the information regarding the entity's temperature. This should very rarely be used directly.
 */
public class AbstractTempCap implements ITemperatureCap
{
    boolean changed = true;
    int syncTimer = 0;
    Temperature.Units preferredUnits = Temperature.Units.F;

    private final Set<Attribute> persistentAttributes = new HashSet<>();

    // Map valid temperature types to a new EnumMap
    private final EnumMap<Trait, Double> traits = Arrays.stream(VALID_TEMPERATURE_TRAITS).collect(
            () -> new EnumMap<>(Trait.class),
            (map, type) -> map.put(type, 0.0),
            EnumMap::putAll);

    // Map valid modifier types to a new EnumMap
    private final EnumMap<Trait, List<TempModifier>> modifiers = Arrays.stream(VALID_MODIFIER_TRAITS).collect(
            () -> new EnumMap<>(Trait.class),
            (map, type) -> map.put(type, new ArrayList<>()),
            EnumMap::putAll);

    public boolean showBodyTemp;
    public boolean showWorldTemp;

    @Override
    public double getTrait(Trait trait)
    {   // Special case for BODY
        if (trait == Trait.BODY) return getTrait(Trait.CORE) + getTrait(Trait.BASE);
        // Throw exception if this temperature trait is not supported
        if (!traits.containsKey(trait))
        {   throw ColdSweat.LOGGER.throwing(new IllegalArgumentException("Invalid temperature trait: " + trait));
        }

        return traits.get(trait);
    }

    @Override
    public EnumMap<Trait, Double> getTraits()
    {   return new EnumMap<>(traits);
    }

    @Override
    public void setTrait(Trait trait, double value)
    {
        switch (trait)
        {
            case CORE  : changed |= ((int) value) != ((int) getTrait(Trait.CORE)); break;
            case BASE  : changed |= ((int) value) != ((int) getTrait(Temperature.Trait.BASE)); break;
            case WORLD : changed |= Math.abs(value - getTrait(Trait.WORLD)) >= 0.02; break;
            default : changed |= true;
        };
        // Throw exception if this temperature trait is not supported
        if (traits.replace(trait, value) == null)
        {   throw ColdSweat.LOGGER.throwing(new IllegalArgumentException("Invalid temperature trait: " + trait));
        }
    }

    public void setTrait(Trait trait, double value, LivingEntity entity)
    {
        double oldTemp = this.getTrait(trait);
        this.setTrait(trait, value);
        if (entity.tickCount > 5 && oldTemp != value && entity instanceof ServerPlayerEntity)
        {   ModAdvancementTriggers.TEMPERATURE_CHANGED.trigger(((ServerPlayerEntity) entity), this.getTraits());
        }
    }

    @Override
    public void addModifier(TempModifier modifier, Trait trait)
    {   modifiers.get(trait).add(modifier);
    }

    @Override
    public List<TempModifier> getModifiers(Trait trait)
    {   // Throw exception if this modifier type is not supported
        return modifiers.computeIfAbsent(trait, t ->
        {   throw ColdSweat.LOGGER.throwing(new IllegalArgumentException("Invalid modifier trait: " + t));
        });
    }

    @Override
    public boolean hasModifier(Trait trait, Class<? extends TempModifier> mod)
    {   return getModifiers(trait).stream().anyMatch(mod::isInstance);
    }

    @Override
    public void markPersistentAttribute(Attribute attribute)
    {   persistentAttributes.add(attribute);
    }

    @Override
    public void clearPersistentAttribute(Attribute attribute)
    {   persistentAttributes.remove(attribute);
    }

    @Override
    public Collection<Attribute> getPersistentAttributes()
    {   return persistentAttributes;
    }

    @Override
    public void clearModifiers(Trait trait)
    {   getModifiers(trait).clear();
    }

    @Override
    public Temperature.Units getPreferredUnits()
    {   return preferredUnits;
    }

    @Override
    public void setPreferredUnits(Temperature.Units units)
    {   preferredUnits = units;
    }

    public boolean shouldShowBodyTemp()
    {   return showBodyTemp;
    }

    public boolean showAdvancedWorldTemp()
    {   return showWorldTemp;
    }

    /* See Temperature.class for more temperature-related methods */

    /**
     * Used for clientside ticking of TempModifiers. The result is not used.
     */
    @Override
    public void tickDummy(LivingEntity entity)
    {
        if (!(entity instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) entity;

        Temperature.apply(0, player, Trait.WORLD, getModifiers(Trait.WORLD));
        Temperature.apply(getTrait(Trait.CORE), player, Temperature.Trait.CORE, getModifiers(Trait.CORE));
        Temperature.apply(0, player, Trait.BASE, getModifiers(Trait.BASE));
    }

    @Override
    public void tick(LivingEntity entity)
    {
        // Tick TempModifiers and pre-attribute-bases
        double newWorldTemp = this.modifyFromAttribute(entity, Trait.WORLD, 0);
        double newBaseTemp  = this.modifyFromAttribute(entity, Trait.BASE, 0);
        double newCoreTemp  = Temperature.apply(getTrait(Trait.CORE), entity, Trait.CORE, getModifiers(Trait.CORE));

        // Get abilities
        double maxTemp = this.modifyFromAttribute(entity, Trait.BURNING_POINT, ConfigSettings.MAX_TEMP.get());
        double minTemp = this.modifyFromAttribute(entity, Trait.FREEZING_POINT, ConfigSettings.MIN_TEMP.get());
        double coldDampening   = this.modifyFromAttribute(entity, Trait.COLD_DAMPENING, 0);
        double heatDampening   = this.modifyFromAttribute(entity, Trait.HEAT_DAMPENING, 0);
        double coldResistance  = this.modifyFromAttribute(entity, Trait.COLD_RESISTANCE, 0);
        double heatResistance  = this.modifyFromAttribute(entity, Trait.HEAT_RESISTANCE, 0);

        // 1 if newWorldTemp is above max, -1 if below min, 0 if between the values (safe)
        int worldTempSign = CSMath.signForRange(newWorldTemp, minTemp, maxTemp);

        boolean immuneToTemp = EntityTempManager.isPeacefulMode(entity);
        boolean isFullyColdDampened = worldTempSign < 0 && (coldDampening >= 1 || immuneToTemp);
        boolean isFullyHeatDampened = worldTempSign > 0 && (heatDampening >= 1 || immuneToTemp);

        // Don't change player temperature if they're in creative/spectator mode
        if (worldTempSign != 0 && (!(entity instanceof PlayerEntity) || !((PlayerEntity) entity).isCreative()) && !entity.isSpectator()
        && !EntityTempManager.isPeacefulMode(entity))
        {
            // How much hotter/colder the player's temp is compared to max/min
            double difference = Math.abs(newWorldTemp - CSMath.clamp(newWorldTemp, minTemp, maxTemp));

            // How much the player's temperature should change
            double changeBy = (Math.max(
                    // Change proportionally to the extremity of the world temperature
                    (difference / 7d) * ConfigSettings.TEMP_RATE.get().floatValue(),
                    // Ensure a minimum speed for temperature change
                    Math.abs(ConfigSettings.TEMP_RATE.get().floatValue() / 50d)
            // If it's hot or cold
            ) * worldTempSign);

            // Temp is decreasing; apply cold dampening
            if (changeBy < 0)
            {   changeBy = (coldDampening < 0
                            // Cold dampening is negative; increase the change by the dampening
                            ? changeBy * (1 + Math.abs(coldDampening))
                            // Cold dampening is positive; apply the change as a percentage of the dampening
                            : CSMath.blend(changeBy, 0, coldDampening, 0, 1));
            }
            // Temp is increasing; apply heat dampening
            else if (changeBy > 0)
            {   changeBy = (heatDampening < 0
                            // Heat dampening is negative; increase the change by the dampening
                            ? changeBy * (1 + Math.abs(heatDampening))
                            // Heat dampening is positive; apply the change as a percentage of the dampening
                            : CSMath.blend(changeBy, 0, heatDampening, 0, 1));
            }
            newCoreTemp += Temperature.apply(changeBy, entity, Trait.RATE, this.getModifiers(Temperature.Trait.RATE));
        }

        // Get the sign of the player's core temperature (-1, 0, or 1)
        int coreTempSign = CSMath.sign(newCoreTemp);
        // If needed, blend the player's temperature back to 0
        List<TempModifier> coreModifiers = this.getModifiers(Trait.CORE);
        boolean hasCoreModifiers = !coreModifiers.isEmpty() && (coreModifiers.get(0).getLastInput() == coreModifiers.get(coreModifiers.size()-1).getLastOutput());
        double amount = 0;
        // Player is fully cold dampened & body is cold
        if (isFullyColdDampened && coreTempSign < 0)
        {   amount = ConfigSettings.TEMP_RATE.get() / 10d;
        }
        // Player is fully heat dampened & body is hot
        else if (isFullyHeatDampened && coreTempSign > 0)
        {   amount = ConfigSettings.TEMP_RATE.get() / -10d;
        }
        // Else if the player's core temp is not the same as the world temp
        else if (coreTempSign != 0 && coreTempSign != worldTempSign)
        {   amount = (coreTempSign == 1 ? newWorldTemp - maxTemp : newWorldTemp - minTemp) / 3;
        }
        // Blend back to 0
        if (amount != 0)
        {
            double changeBy = CSMath.maxAbs(amount * ConfigSettings.TEMP_RATE.get(), ConfigSettings.TEMP_RATE.get() / 10d * -coreTempSign);
            if (hasCoreModifiers)
            {   changeBy /= 2;
            }
            newCoreTemp += CSMath.minAbs(changeBy, -getTrait(Trait.CORE));
        }

        // Write the new temperature values
        this.setTrait(Trait.CORE, CSMath.clamp(newCoreTemp, -150, 150), entity);
        this.setTrait(Trait.BASE, CSMath.clamp(newBaseTemp, -150, 150), entity);
        this.setTrait(Trait.WORLD, newWorldTemp, entity);
        // Write the new ability values
        this.setTrait(Trait.BURNING_POINT, maxTemp);
        this.setTrait(Trait.FREEZING_POINT, minTemp);
        this.setTrait(Trait.COLD_RESISTANCE, coldResistance);
        this.setTrait(Trait.HEAT_RESISTANCE, heatResistance);
        this.setTrait(Trait.COLD_DAMPENING, coldDampening);
        this.setTrait(Trait.HEAT_DAMPENING, heatDampening);

        if (syncTimer > 0)
        {   syncTimer--;
        }

        // Sync the temperature values to the client
        if (changed && syncTimer <= 0)
        {   this.syncValues(entity);
        }

        // Deal damage to the player at a set interval if temperature is critical
        this.tickHurting(entity, heatResistance, coldResistance);
    }

    private double modifyFromAttribute(LivingEntity entity, Temperature.Trait type, double baseValue)
    {
        Supplier<Double> defaultSupplier = () -> Temperature.apply(baseValue, entity, type, this.getModifiers(type));
        ModifiableAttributeInstance attribute = EntityTempManager.getAttribute(type, entity);
        double newValue;
        // If the attribute is null, return the default value
        if (attribute == null)
        {   newValue = defaultSupplier.get();
        }
        // If base attribute is unset
        else
        {
            double base = CSMath.safeDouble(attribute.getBaseValue()).orElse(defaultSupplier.get());

            for (AttributeModifier mod : attribute.getModifiers(AttributeModifier.Operation.ADDITION))
            {   base += mod.getAmount();
            }
            double value = base;
            for (AttributeModifier mod : attribute.getModifiers(AttributeModifier.Operation.MULTIPLY_BASE))
            {   value += base * mod.getAmount();
            }
            for (AttributeModifier mod : attribute.getModifiers(AttributeModifier.Operation.MULTIPLY_TOTAL))
            {   value *= 1.0D + mod.getAmount();
            }
            newValue = value;
        }
        if (newValue != baseValue)
        {   MinecraftForge.EVENT_BUS.post(new TemperatureChangedEvent(entity, type, getTrait(type), newValue));
        }
        return newValue;
    }

    @Override
    public void syncValues(LivingEntity entity)
    {
        Temperature.updateTemperature(entity, this, false);
        changed = false;
        syncTimer = 5;
    }

    public void tickHurting(LivingEntity entity, double heatResistance, double coldResistance)
    {
        if (EntityTempManager.isPeacefulMode(entity)) return;

        double bodyTemp = getTrait(Temperature.Trait.BODY);

        boolean hasGrace = entity.hasEffect(ModEffects.GRACE);
        boolean hasFireResist = entity.hasEffect(Effects.FIRE_RESISTANCE);
        boolean hasIceResist = entity.hasEffect(ModEffects.ICE_RESISTANCE);

        if (!hasGrace && entity.tickCount % 40 == 0)
        {
            if (bodyTemp >= 100 && !(hasFireResist && ConfigSettings.FIRE_RESISTANCE_ENABLED.get()))
            {
                DamageSource hot = ModDamageSources.HOT;
                entity.hurt(hot, (float) CSMath.blend(ConfigSettings.TEMP_DAMAGE.get(), 0, heatResistance, 0, 1));
            }
            else if (bodyTemp <= -100 && !(hasIceResist && ConfigSettings.ICE_RESISTANCE_ENABLED.get()))
            {
                DamageSource cold = ModDamageSources.COLD;
                entity.hurt(cold, (float) CSMath.blend(ConfigSettings.TEMP_DAMAGE.get(), 0, coldResistance, 0, 1));
            }
        }
    }

    @Override
    public void copy(ITemperatureCap cap)
    {
        // Copy temperature values
        for (Trait trait : VALID_TEMPERATURE_TRAITS)
        {   this.setTrait(trait, cap.getTrait(trait));
        }

        // Copy the modifiers
        for (Trait trait : VALID_MODIFIER_TRAITS)
        {   this.getModifiers(trait).clear();
            this.getModifiers(trait).addAll(cap.getModifiers(trait));
        }

        // Copy persistent attributes
        this.persistentAttributes.clear();
        this.persistentAttributes.addAll(cap.getPersistentAttributes());

        // Copy preferred units
        this.setPreferredUnits(cap.getPreferredUnits());
    }

    @Override
    public CompoundNBT serializeNBT()
    {
        CompoundNBT nbt = new CompoundNBT();
        // Save the player's temperatures
        nbt.put("Traits", this.serializeTraits());
        // Save the player's modifiers
        nbt.put("TempModifiers", this.serializeModifiers());
        // Save the player's persistent attributes
        ListNBT attributes = new ListNBT();
        for (Attribute attribute : this.getPersistentAttributes())
        {   attributes.add(StringNBT.valueOf(ForgeRegistries.ATTRIBUTES.getKey(attribute).toString()));
        }
        nbt.put("PersistentAttributes", attributes);
        return nbt;
    }

    @Override
    public CompoundNBT serializeTraits()
    {
        CompoundNBT nbt = new CompoundNBT();

        // Save the player's temperature data
        for (Map.Entry<Trait, Double> trait : traits.entrySet())
        {   nbt.putDouble(NBTHelper.getTraitTagKey(trait.getKey()), trait.getValue());
        }
        return nbt;
    }

    @Override
    public CompoundNBT serializeModifiers()
    {
        CompoundNBT nbt = new CompoundNBT();

        // Save the player's modifiers
        for (Trait trait : VALID_MODIFIER_TRAITS)
        {
            ListNBT modifiers = new ListNBT();
            for (TempModifier modifier : this.getModifiers(trait))
            {   modifiers.add(NBTHelper.modifierToTag(modifier));
            }
            // Write the list of modifiers to the player's persistent data
            nbt.put(NBTHelper.getTraitTagKey(trait), modifiers);
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt)
    {   // Load the player's temperatures
        deserializeTraits(nbt.getCompound("Traits"));
        // Load the player's modifiers
        deserializeModifiers(nbt.getCompound("TempModifiers"));
        // Load the player's persistent attributes
        ListNBT attributes = nbt.getList("PersistentAttributes", 8);
        for (int i = 0; i < attributes.size(); i++)
        {   this.markPersistentAttribute(ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attributes.getString(i))));
        }
    }

    @Override
    public void deserializeTraits(CompoundNBT nbt)
    {
        for (Trait trait : VALID_TEMPERATURE_TRAITS)
        {   setTrait(trait, nbt.getDouble(NBTHelper.getTraitTagKey(trait)));
        }
    }

    @Override
    public void deserializeModifiers(CompoundNBT nbt)
    {
        for (Trait trait : VALID_MODIFIER_TRAITS)
        {
            getModifiers(trait).clear();

            // Get the list of modifiers from the player's persistent data
            ListNBT modifiers = nbt.getList(NBTHelper.getTraitTagKey(trait), 10);

            // For each modifier in the list
            modifiers.forEach(modNBT ->
            {
                NBTHelper.tagToModifier((CompoundNBT) modNBT).ifPresent(modifier ->
                {   getModifiers(trait).add(modifier);
                });
            });
        }
    }
}
