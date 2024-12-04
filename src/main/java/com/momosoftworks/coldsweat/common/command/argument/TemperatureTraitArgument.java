package com.momosoftworks.coldsweat.common.command.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TemperatureTraitArgument implements ArgumentType<Temperature.Trait>
{
    private final boolean includeBody;

    private TemperatureTraitArgument(boolean includeBody)
    {
        this.includeBody = includeBody;
    }

    public static TemperatureTraitArgument temperatureGet()
    {   return new TemperatureTraitArgument(true);
    }

    public static TemperatureTraitArgument temperatureSet()
    {   return new TemperatureTraitArgument(false);
    }

    public static Temperature.Trait getTemperature(CommandContext<CommandSourceStack> context, String argument)
    {   return context.getArgument(argument, Temperature.Trait.class);
    }

    @Override
    public Temperature.Trait parse(StringReader reader)
    {
        String s = reader.readUnquotedString();
        return Temperature.Trait.fromID(s);
    }

    private List<Temperature.Trait> getTraits()
    {
        List<Temperature.Trait> traits = new ArrayList<>(Arrays.asList(EntityTempManager.VALID_TEMPERATURE_TRAITS));
        if (includeBody)
        {
            int coreIndex = traits.indexOf(Temperature.Trait.CORE);
            if (coreIndex != -1)
            {   traits.add(coreIndex + 1, Temperature.Trait.BODY);
            }
        }
        return traits;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder)
    {
        return SharedSuggestionProvider.suggest(this.getTraits().stream()
                    .map(Temperature.Trait::getSerializedName)
                    .collect(Collectors.toList()), pBuilder);
    }

    @Override
    public Collection<String> getExamples()
    {
        return this.getTraits().stream().map(Temperature.Trait::getSerializedName).limit(2L).collect(Collectors.toList());
    }

    public static class Info implements ArgumentTypeInfo<TemperatureTraitArgument, Info.Template>
    {
        @Override
        public void serializeToNetwork(Template template, FriendlyByteBuf buffer)
        {   buffer.writeByte(template.includeBody ? 1 : 0);
        }

        @Override
        public Template deserializeFromNetwork(FriendlyByteBuf buffer)
        {   boolean includeBody = buffer.readByte() == 1;
            return new Template(includeBody);
        }

        @Override
        public void serializeToJson(Template template, JsonObject json)
        {   json.addProperty("include_body", template.includeBody);
        }

        @Override
        public Template unpack(TemperatureTraitArgument argument)
        {   return new Template(argument.includeBody);
        }

        public final class Template implements ArgumentTypeInfo.Template<TemperatureTraitArgument>
        {
            private final boolean includeBody;

            public Template(boolean includeBody)
            {   this.includeBody = includeBody;
            }

            @Override
            public TemperatureTraitArgument instantiate(CommandBuildContext context)
            {   return new TemperatureTraitArgument(includeBody);
            }

            @Override
            public ArgumentTypeInfo<TemperatureTraitArgument, ?> type()
            {   return Info.this;
            }
        }
    }
}
