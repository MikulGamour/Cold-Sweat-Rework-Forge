package com.momosoftworks.coldsweat.common.command.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.util.serialization.ObjectBuilder;
import com.momosoftworks.coldsweat.util.serialization.StringRepresentable;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.IArgumentSerializer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TemperatureTraitArgument implements ArgumentType<Temperature.Trait>
{
    private static final Dynamic2CommandExceptionType INVALID_ENUM = new Dynamic2CommandExceptionType(
            (found, constants) -> new TranslationTextComponent("commands.forge.arguments.enum.invalid", constants, found));

    private static final Temperature.Trait[] VALID_GETTER_TRAITS = ObjectBuilder.build(() -> {
        Temperature.Trait[] traits = Arrays.copyOf(EntityTempManager.VALID_TEMPERATURE_TRAITS, EntityTempManager.VALID_TEMPERATURE_TRAITS.length + 1);
        traits[traits.length - 1] = com.momosoftworks.coldsweat.api.util.Temperature.Trait.BODY;
        return traits;
    });

    private final boolean includeBody;
    private final Temperature.Trait[] traits;

    private TemperatureTraitArgument(boolean includeBody)
    {
        this.traits = includeBody ? VALID_GETTER_TRAITS : EntityTempManager.VALID_TEMPERATURE_TRAITS;
        this.includeBody = includeBody;
    }

    public static TemperatureTraitArgument temperatureGet()
    {   return new TemperatureTraitArgument(true);
    }

    public static TemperatureTraitArgument temperatureSet()
    {   return new TemperatureTraitArgument(false);
    }

    public static Temperature.Trait getTemperature(CommandContext<CommandSource> context, String argument)
    {   return context.getArgument(argument, Temperature.Trait.class);
    }

    @Override
    public Temperature.Trait parse(final StringReader reader) throws CommandSyntaxException
    {
        String name = reader.readUnquotedString();
        try
        {   return Temperature.Trait.fromID(name);
        }
        catch (IllegalArgumentException e)
        {   throw INVALID_ENUM.createWithContext(reader, name, Arrays.toString(this.getExamples().toArray()));
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder)
    {
        return ISuggestionProvider.suggest(Stream.of(traits).map(StringRepresentable::getSerializedName), builder);
    }

    @Override
    public Collection<String> getExamples()
    {
        return Stream.of(traits).map(StringRepresentable::getSerializedName).collect(Collectors.toList());
    }

    public static class Serializer implements IArgumentSerializer<TemperatureTraitArgument>
    {
        @Override
        public void serializeToNetwork(TemperatureTraitArgument argument, PacketBuffer buffer)
        {   buffer.writeBoolean(argument.includeBody);
        }

        @SuppressWarnings({"unchecked"})
        @Override
        public TemperatureTraitArgument deserializeFromNetwork(PacketBuffer buffer)
        {   return new TemperatureTraitArgument(buffer.readBoolean());
        }

        @Override
        public void serializeToJson(TemperatureTraitArgument argument, JsonObject json)
        {   json.addProperty("includeBody", argument.includeBody);
        }
    }
}
