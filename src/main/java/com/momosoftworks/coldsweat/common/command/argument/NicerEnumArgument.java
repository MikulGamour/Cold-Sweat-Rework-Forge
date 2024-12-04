package com.momosoftworks.coldsweat.common.command.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NicerEnumArgument<T extends Enum<T>> implements ArgumentType<T>
{
    private static final Dynamic2CommandExceptionType INVALID_ENUM = new Dynamic2CommandExceptionType(
            (found, constants) -> Component.translatable("commands.forge.arguments.enum.invalid", constants, found));
    private final Class<T> enumClass;

    public static <R extends Enum<R>> NicerEnumArgument<R> enumArgument(Class<R> enumClass)
    {   return new NicerEnumArgument<>(enumClass);
    }

    public NicerEnumArgument(final Class<T> enumClass)
    {   this.enumClass = enumClass;
    }

    @Override
    public T parse(final StringReader reader) throws CommandSyntaxException
    {
        String name = reader.readUnquotedString();
        try
        {   return Enum.valueOf(enumClass, name.toUpperCase());
        }
        catch (IllegalArgumentException e)
        {   throw INVALID_ENUM.createWithContext(reader, name, Arrays.toString(Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).toArray()));
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder)
    {   return SharedSuggestionProvider.suggest(Stream.of(enumClass.getEnumConstants()).map(en -> en.name().toLowerCase()), builder);
    }

    @Override
    public Collection<String> getExamples()
    {   return Stream.of(enumClass.getEnumConstants()).map(en -> en.name().toLowerCase()).collect(Collectors.toList());
    }

    public static class Info<T extends Enum<T>> implements ArgumentTypeInfo<NicerEnumArgument<T>, NicerEnumArgument.Info<T>.Template>
    {
        @Override
        public void serializeToNetwork(NicerEnumArgument.Info.Template template, FriendlyByteBuf buffer)
        {
            buffer.writeUtf(template.enumClass.getName());
        }

        @SuppressWarnings("unchecked")
        @Override
        public NicerEnumArgument.Info.Template deserializeFromNetwork(FriendlyByteBuf buffer)
        {
            try
            {   String name = buffer.readUtf();
                return new NicerEnumArgument.Info.Template((Class<T>) Class.forName(name));
            }
            catch (ClassNotFoundException e)
            {   return null;
            }
        }

        @Override
        public void serializeToJson(NicerEnumArgument.Info.Template template, JsonObject json)
        {
            json.addProperty("enum", template.enumClass.getName());
        }

        @Override
        public NicerEnumArgument.Info.Template unpack(NicerEnumArgument<T> argument)
        {
            return new NicerEnumArgument.Info.Template(argument.enumClass);
        }

        public class Template implements ArgumentTypeInfo.Template<NicerEnumArgument<T>>
        {
            final Class<T> enumClass;

            Template(Class<T> enumClass)
            {
                this.enumClass = enumClass;
            }

            @Override
            public NicerEnumArgument<T> instantiate(CommandBuildContext pStructure)
            {
                return new NicerEnumArgument<>(this.enumClass);
            }

            @Override
            public ArgumentTypeInfo<NicerEnumArgument<T>, ?> type()
            {
                return NicerEnumArgument.Info.this;
            }
        }
    }
}
