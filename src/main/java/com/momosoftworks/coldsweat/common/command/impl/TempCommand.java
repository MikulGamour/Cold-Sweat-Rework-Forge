package com.momosoftworks.coldsweat.common.command.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.temperature.ITemperatureCap;
import com.momosoftworks.coldsweat.common.command.BaseCommand;
import com.momosoftworks.coldsweat.common.command.argument.NicerEnumArgument;
import com.momosoftworks.coldsweat.common.command.argument.TempAttributeTraitArgument;
import com.momosoftworks.coldsweat.common.command.argument.TempModifierTraitArgument;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.common.command.argument.TemperatureTraitArgument;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.Comparator;

public class TempCommand extends BaseCommand
{
    public TempCommand(String name, int permissionLevel, boolean enabled) {
        super(name, permissionLevel, enabled);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> setExecution()
    {
        return builder
                /* Set temperature for entity */
                .then(Commands.literal("set")
                        .then(Commands.argument("entities", EntityArgument.entities())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(-150, 150))
                                        .executes(source -> executeSetEntityTemp(source.getSource(),
                                                                                 EntityArgument.getEntities(source, "entities"),
                                                                                 IntegerArgumentType.getInteger(source, "amount"),
                                                                                 Temperature.Trait.BODY)
                                        )
                                )
                        )
                )
                /* Get temperature */
                .then(Commands.literal("get")
                        /* Get from entity */
                        .then(Commands.argument("entities", EntityArgument.entities())
                                .executes(source -> executeGetEntityTemp(source.getSource(),
                                                                         EntityArgument.getEntities(source, "entities"),
                                                                         Temperature.Trait.BODY)
                                )
                                .then(Commands.argument("trait", TemperatureTraitArgument.temperatureGet())
                                        .executes(source -> executeGetEntityTemp(source.getSource(),
                                                                                 EntityArgument.getEntities(source, "entities"),
                                                                                 TemperatureTraitArgument.getTemperature(source, "trait"))
                                        )
                                )
                        )
                        /* Get from world */
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(source ->
                                          {   BlockPos pos = BlockPosArgument.getLoadedBlockPos(source, "pos");
                                              return executeGetWorldTemp(source.getSource(), pos.getX(), pos.getY(), pos.getZ(), source.getSource().getLevel());
                                          }
                                )
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                              .executes(source -> executeGetWorldTemp(
                                                      source.getSource(), IntegerArgumentType.getInteger(source, "x"),
                                                      IntegerArgumentType.getInteger(source, "y"), IntegerArgumentType.getInteger(source, "z"),
                                                      DimensionArgument.getDimension(source, "dimension"))
                                              )
                                )
                        )
                )
                /* Get TempModifiers from entity */
                .then(Commands.literal("debug")
                        .then(Commands.argument("entity", EntityArgument.entity())
                                  .then(Commands.argument("type", TempModifierTraitArgument.modifier())
                                          .executes(source -> executeDebugModifiers(
                                                  source.getSource(), EntityArgument.getEntity(source, "entity"), TempModifierTraitArgument.getModifier(source, "type"))
                                          )
                                  )
                        )
                )
                /* Modify attributes */
                .then(Commands.literal("modify")
                        .then(Commands.argument("entities", EntityArgument.entities())
                                /* Clear all attributes */
                                .then(Commands.literal("clear")
                                              .then(Commands.argument("type", TempAttributeTraitArgument.attribute())
                                                            .executes(source -> executeClearModifier(
                                                                    source.getSource(), EntityArgument.getEntities(source, "entities"),
                                                                    TempAttributeTraitArgument.getAttribute(source, "type"))
                                                            )
                                              )
                                              .executes(source -> executeClearAllModifiers(
                                                      source.getSource(), EntityArgument.getEntities(source, "entities"))
                                              )
                                )
                                /* Attribute modifier */
                                .then(Commands.argument("operation", NicerEnumArgument.enumArgument(AttributeModifier.Operation.class))
                                              .then(Commands.argument("type", TempAttributeTraitArgument.attribute())
                                                            .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                                          .then(Commands.argument("permanent", BoolArgumentType.bool())
                                                                                        .executes(source -> executeModifyEntityTemp(
                                                                                                source.getSource(), EntityArgument.getEntities(source, "entities"),
                                                                                                TempAttributeTraitArgument.getAttribute(source, "type"),
                                                                                                DoubleArgumentType.getDouble(source, "amount"),
                                                                                                source.getArgument("operation", AttributeModifier.Operation.class), BoolArgumentType.getBool(source, "permanent"))
                                                                                        )
                                                                          )
                                                                          /* Default to non-permanent if not specified */
                                                                          .executes(source -> executeModifyEntityTemp(
                                                                                  source.getSource(), EntityArgument.getEntities(source, "entities"),
                                                                                  TempAttributeTraitArgument.getAttribute(source, "type"),
                                                                                  DoubleArgumentType.getDouble(source, "amount"),
                                                                                  source.getArgument("operation", AttributeModifier.Operation.class), false)
                                                                          )
                                                            )
                                              )
                                )
                                /* Set attribute to static value */
                                .then(Commands.literal("set")
                                              .then(Commands.argument("type", TempAttributeTraitArgument.attribute())
                                                            .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                                          .then(Commands.argument("permanent", BoolArgumentType.bool())
                                                                                          .executes(source -> executeModifyEntityTemp(
                                                                                                  source.getSource(), EntityArgument.getEntities(source, "entities"),
                                                                                                  TempAttributeTraitArgument.getAttribute(source, "type"),
                                                                                                  DoubleArgumentType.getDouble(source, "amount"),
                                                                                                  null, BoolArgumentType.getBool(source, "permanent"))
                                                                                          )
                                                                          )
                                                                          .executes(source -> executeModifyEntityTemp(
                                                                                  source.getSource(), EntityArgument.getEntities(source, "entities"),
                                                                                  TempAttributeTraitArgument.getAttribute(source, "type"),
                                                                                  DoubleArgumentType.getDouble(source, "amount"),
                                                                                  null, false)
                                                                          )
                                                            )
                                              )
                                )
                        )
                );
    }

    private int executeSetEntityTemp(CommandSourceStack source, Collection<? extends Entity> entities, double temp, Temperature.Trait trait)
    {
        if (entities.stream().anyMatch(entity -> !(entity instanceof Player || EntityTempManager.getEntitiesWithTemperature().contains(entity.getType()))))
        {   source.sendFailure(Component.translatable("commands.cold_sweat.temperature.invalid"));
            return 0;
        }
        // Set the temperature for all affected targets
        for (Entity entity : entities)
        {
            if (entity instanceof LivingEntity living)
            {   Temperature.set(living, trait == Temperature.Trait.BODY ? Temperature.Trait.CORE : trait, temp);
            }
        }

        Temperature.Units preferredUnits = CSMath.getIfNotNull(source.getPlayer(),
                                                               player -> EntityTempManager.getTemperatureCap(player).map(ITemperatureCap::getPreferredUnits).orElse(Temperature.Units.F),
                                                               Temperature.Units.F);
        String unitsName = trait.isForWorld() ? " " + preferredUnits.getFormattedName() : "";
        double convertedTemp = Temperature.convertIfNeeded(temp, trait, preferredUnits);

        //Compose & send message
        if (entities.size() == 1)
        {   Entity target = entities.iterator().next();
            source.sendSuccess(Component.translatable("commands.cold_sweat.temperature.set.single.result", trait.getSerializedName(), target.getName().getString(),
                                                            CSMath.truncate(convertedTemp, 1) + unitsName), true);
        }
        else
        {   source.sendSuccess(Component.translatable("commands.cold_sweat.temperature.set.many.result", trait.getSerializedName(), entities.size(),
                                                            CSMath.truncate(convertedTemp, 1) + unitsName), true);
        }
        return entities.size();
    }

    private int executeGetEntityTemp(CommandSourceStack source, Collection<? extends Entity> entities, Temperature.Trait trait)
    {
        if (entities.stream().anyMatch(entity -> !(entity instanceof Player || EntityTempManager.getEntitiesWithTemperature().contains(entity.getType()))))
        {   source.sendFailure(Component.translatable("commands.cold_sweat.temperature.invalid"));
            return 0;
        }
        for (Entity target : entities.stream().sorted(Comparator.comparing(player -> player.getName().getString())).toList())
        {   //Compose & send message
            Temperature.Units preferredUnits = EntityTempManager.getTemperatureCap(target).map(ITemperatureCap::getPreferredUnits).orElse(Temperature.Units.F);
            double temp = CSMath.truncate(Temperature.convertIfNeeded(Temperature.get((LivingEntity) target, trait), trait, preferredUnits), 1);
            String unitsName = trait.isForWorld() ? " " + preferredUnits.getFormattedName() : "";
            source.sendSuccess(Component.translatable("commands.cold_sweat.temperature.get.result", target.getName().getString(),
                                                            trait.getSerializedName(), temp + unitsName),
                               false);
        }
        return entities.size();
    }

    private int executeGetWorldTemp(CommandSourceStack source, int x , int y, int z, ServerLevel level)
    {   //Compose & send message
        EntityTempManager.getTemperatureCap(source.getPlayer()).ifPresent(cap ->
        {
            int worldTemp = (int) Temperature.convert(WorldHelper.getTemperatureAt(level != null ? level : source.getLevel(),
                                                                                   new BlockPos(x, y, z)),
                                                      Temperature.Units.MC, cap.getPreferredUnits(), true);
            source.sendSuccess(Component.translatable("commands.cold_sweat.temperature.get.world.result", x, y, z, worldTemp, cap.getPreferredUnits().getFormattedName()), true);
        });
        return Command.SINGLE_SUCCESS;
    }

    private int executeDebugModifiers(CommandSourceStack source, Entity entity, Temperature.Trait trait)
    {
        if (!(entity instanceof Player || EntityTempManager.getEntitiesWithTemperature().contains(entity.getType())))
        {   source.sendFailure(Component.translatable("commands.cold_sweat.temperature.invalid"));
            return 0;
        }

        LivingEntity living = (LivingEntity) entity;
        AttributeInstance attribute = trait.isForAttributes()
                                      ? EntityTempManager.getAttribute(trait, living)
                                      : null;
        Temperature.Units preferredUnits = EntityTempManager.getTemperatureCap(entity).map(ITemperatureCap::getPreferredUnits).orElse(Temperature.Units.F);
        double lastValue = trait == Temperature.Trait.BURNING_POINT ? ConfigSettings.MAX_TEMP.get()
                         : trait == Temperature.Trait.FREEZING_POINT ? ConfigSettings.MIN_TEMP.get()
                         : 0;

        source.sendSuccess(Component.translatable("commands.cold_sweat.temperature.debug", living.getDisplayName(), trait.getSerializedName()).withStyle(ChatFormatting.WHITE), false);

        if (attribute != null && CSMath.safeDouble(attribute.getBaseValue()).isPresent())
        {
            source.sendSuccess(Component.literal(ForgeRegistries.ATTRIBUTES.getKey(attribute.getAttribute()).toString()).withStyle(ChatFormatting.GOLD)
                       .append(Component.literal(" → ").withStyle(ChatFormatting.WHITE))
                       .append(Component.literal(attribute.getValue()+"")
                                        .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)
                                        .withHoverEvent(getConvertedUnitHover(trait, attribute.getValue(), preferredUnits)))), false);
            lastValue = attribute.getBaseValue();
        }
        else for (TempModifier modifier : Temperature.getModifiers(living, trait))
        {
            double lastInput = modifier.getLastInput();
            double lastOutput = modifier.getLastOutput();

            source.sendSuccess(Component.empty()
                       .append(Component.literal(CSMath.truncate(modifier.getLastInput(), 2)+"")
                                        .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)
                                        .withHoverEvent(getConvertedUnitHover(trait, lastInput, preferredUnits))))
                       .append(Component.literal(" → ").withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withHoverEvent(null)))
                       .append(Component.literal(modifier.toString()).withStyle(ChatFormatting.GRAY))
                       .append(Component.literal(" → ").withStyle(ChatFormatting.WHITE))
                       .append(Component.literal(CSMath.truncate(modifier.getLastOutput(), 2)+"")
                                        .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)
                                        .withHoverEvent(getConvertedUnitHover(trait, lastOutput, preferredUnits)))), false);
            lastValue = modifier.getLastOutput();
        }
        // Print attributes affecting the trait
        if (attribute != null)
        {
            double newBase = lastValue;
            for (AttributeModifier modifier : attribute.getModifiers(AttributeModifier.Operation.ADDITION))
            {
                newBase += modifier.getAmount();
                printAttributeModifierLine(source, modifier, lastValue, newBase, trait, preferredUnits);
            }
            double newValue = newBase;
            for (AttributeModifier modifier : attribute.getModifiers(AttributeModifier.Operation.MULTIPLY_BASE))
            {
                newValue += newBase * modifier.getAmount();
                printAttributeModifierLine(source, modifier, lastValue, newValue, trait, preferredUnits);
            }
            for (AttributeModifier modifier : attribute.getModifiers(AttributeModifier.Operation.MULTIPLY_TOTAL))
            {
                newValue *= 1.0D + modifier.getAmount();
                printAttributeModifierLine(source, modifier, lastValue, newValue, trait, preferredUnits);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    static void printAttributeModifierLine(CommandSourceStack source, AttributeModifier modifier, double lastValueStore, double newValueStore, Temperature.Trait trait, Temperature.Units preferredUnits)
    {
        source.sendSuccess(Component.literal(CSMath.truncate(lastValueStore, 2)+"")
                                    .withStyle(Style.EMPTY
                                    .withColor(ChatFormatting.WHITE)
                                    .withHoverEvent(getConvertedUnitHover(trait, lastValueStore, preferredUnits)))
                   .append(Component.literal(" → ").withStyle(ChatFormatting.WHITE))
                   .append(Component.literal(modifier.getName())
                                    .withStyle(Style.EMPTY
                                    .withColor(ChatFormatting.LIGHT_PURPLE)
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(modifier.getId().toString())
                                    .append(Component.literal("\n"))
                                    .append(Component.translatable("chat.copy.click").withStyle(ChatFormatting.GRAY))))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, modifier.getId().toString()))))
                   .append(Component.literal(" → ").withStyle(ChatFormatting.WHITE))
                   .append(Component.literal(CSMath.truncate(newValueStore, 2)+"")
                                    .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)
                                    .withHoverEvent(getConvertedUnitHover(trait, newValueStore, preferredUnits)))), false);
    }

    static double getFormattedTraitValue(Temperature.Trait trait, double rawValue, Temperature.Units units)
    {
        double converted = rawValue;

        if (switch (trait)
        {
            case WORLD, FREEZING_POINT, BURNING_POINT -> true;
            default -> false;
        })
        {
            converted = Temperature.convert(converted, Temperature.Units.MC, units, true);
        }
        return converted;
    }

    static HoverEvent getConvertedUnitHover(Temperature.Trait trait, double value, Temperature.Units units)
    {
        return new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                Component.literal(CSMath.truncate(getFormattedTraitValue(trait, value, units), 1) + " " + units.getFormattedName()));
    }

    private int executeModifyEntityTemp(CommandSourceStack source, Collection<? extends Entity> entities, Temperature.Trait attribute,
                                        double amount, AttributeModifier.Operation operation, boolean permanent)
    {
        for (Entity entity : entities)
        {
            if (EntityTempManager.getEntitiesWithTemperature().contains(entity.getType()) && entity instanceof LivingEntity living)
            {
                AttributeInstance instance = EntityTempManager.getAttribute(attribute, living);
                if (instance == null) continue;
                if (operation != null)
                {
                    AttributeModifier modifier = EntityTempManager.makeAttributeModifier(attribute, amount, operation);
                    instance.addPermanentModifier(modifier);
                }
                else
                {   EntityTempManager.getAttribute(attribute, living).setBaseValue(amount);
                }
                EntityTempManager.getTemperatureCap(entity).ifPresent(cap ->
                {
                    if (permanent)
                    {   cap.markPersistentAttribute(instance.getAttribute());
                    }
                    else cap.clearPersistentAttribute(instance.getAttribute());
                });
            }
            else
            {   source.sendFailure(Component.translatable("commands.cold_sweat.temperature.invalid"));
                return 0;
            }
        }
        if (entities.size() == 1)
        {
            if (operation == null)
            source.sendSuccess(Component.translatable("commands.cold_sweat.temperature.modify.set.single.result",
                                                         attribute.getSerializedName(),
                                                         entities.iterator().next().getName().getString(), amount), true);
            else source.sendSuccess(Component.translatable("commands.cold_sweat.temperature.modify.add_modifier.single.result",
                                                              attribute.getSerializedName(),
                                                              entities.iterator().next().getName().getString()), true);
        }
        else
        {
            if (operation == null)
            source.sendSuccess(Component.translatable("commands.cold_sweat.temperature.modify.set.many.result",
                                                         attribute.getSerializedName(),
                                                         entities.size(), amount), true);
            else source.sendSuccess(Component.translatable("commands.cold_sweat.temperature.modify.add_modifier.many.result",
                                                              attribute.getSerializedName(),
                                                              entities.size()), true);
        }
        return entities.size();
    }

    private int executeClearModifier(CommandSourceStack source, Collection<? extends Entity> entities, Temperature.Trait attribute)
    {
        for (Entity entity : entities)
        {
            if (EntityTempManager.getEntitiesWithTemperature().contains(entity.getType()) && entity instanceof LivingEntity living)
            {
                EntityTempManager.getTemperatureCap(entity).ifPresent(cap ->
                {   AttributeInstance instance = EntityTempManager.getAttribute(attribute, living);
                    if (instance == null) return;
                    instance.removeModifiers();
                    EntityTempManager.getAttribute(attribute, living).setBaseValue(Double.NaN);
                    cap.clearPersistentAttribute(instance.getAttribute());
                });
            }
            else
            {   source.sendFailure(Component.translatable("commands.cold_sweat.temperature.invalid"));
                return 0;
            }
        }
        if (entities.size() == 1)
        {   source.sendSuccess(Component.translatable("commands.cold_sweat.temperature.clear.single.result",
                                                         attribute.getSerializedName(),
                                                         entities.iterator().next().getName().getString()), true);
        }
        else
        {   source.sendSuccess(Component.translatable("commands.cold_sweat.temperature.clear.many.result",
                                                         attribute.getSerializedName(),
                                                         entities.size()), true);
        }
        return entities.size();
    }

    private int executeClearAllModifiers(CommandSourceStack source, Collection<? extends Entity> entities)
    {
        for (Entity entity : entities)
        {
            if (EntityTempManager.getEntitiesWithTemperature().contains(entity.getType()) && entity instanceof LivingEntity living)
            {
                EntityTempManager.getTemperatureCap(entity).ifPresent(cap ->
                {
                    for (Temperature.Trait attribute : EntityTempManager.VALID_ATTRIBUTE_TRAITS)
                    {
                        AttributeInstance instance = EntityTempManager.getAttribute(attribute, living);
                        if (instance == null) continue;
                        instance.removeModifiers();
                        instance.setBaseValue(instance.getAttribute().getDefaultValue());
                        cap.clearPersistentAttribute(instance.getAttribute());
                    }
                });
            }
            else
            {   source.sendFailure(Component.translatable("commands.cold_sweat.temperature.invalid"));
                return 0;
            }
        }
        if (entities.size() == 1)
        {   source.sendSuccess(Component.translatable("commands.cold_sweat.temperature.clear.all.single.result",
                                                            entities.iterator().next().getName().getString()), true);
        }
        else
        {   source.sendSuccess(Component.translatable("commands.cold_sweat.temperature.clear.all.many.result",
                                                            entities.size()), true);
        }
        return entities.size();
    }
}
