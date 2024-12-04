package com.momosoftworks.coldsweat.core.init;

import com.mojang.brigadier.CommandDispatcher;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.common.command.BaseCommand;
import com.momosoftworks.coldsweat.common.command.argument.NicerEnumArgument;
import com.momosoftworks.coldsweat.common.command.argument.TempAttributeTraitArgument;
import com.momosoftworks.coldsweat.common.command.argument.TempModifierTraitArgument;
import com.momosoftworks.coldsweat.common.command.argument.TemperatureTraitArgument;
import com.momosoftworks.coldsweat.common.command.impl.TempCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;

@Mod.EventBusSubscriber
public class CommandInit
{
    private static final ArrayList<BaseCommand> COMMANDS = new ArrayList<>();

    @SubscribeEvent
    public static void registerCommands(final RegisterCommandsEvent event)
    {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        COMMANDS.add(new TempCommand("temperature", 2, true));
        COMMANDS.add(new TempCommand("temp", 2, true));

        COMMANDS.forEach(command ->
        {
            if (command.isEnabled() && command.setExecution() != null)
            {   dispatcher.register(command.getBuilder());
            }
        });
    }

    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENTS = DeferredRegister.create(ForgeRegistries.COMMAND_ARGUMENT_TYPES, ColdSweat.MOD_ID);

    public static final RegistryObject<ArgumentTypeInfo<?, ?>> TEMP_MODIFIER_TYPE = ARGUMENTS.register("temp_modifier_type",
                                                               () -> ArgumentTypeInfos.registerByClass(TemperatureTraitArgument.class, new TemperatureTraitArgument.Info()));
    public static final RegistryObject<ArgumentTypeInfo<?, ?>> TEMPERATURE_TYPE = ARGUMENTS.register("temperature_type",
                                                               () -> ArgumentTypeInfos.registerByClass(TempAttributeTraitArgument.class, new TempAttributeTraitArgument.Info()));
    public static final RegistryObject<ArgumentTypeInfo<?, ?>> ATTRIBUTE_TRAIT_TYPE = ARGUMENTS.register("attribute_trait_type",
                                                               () -> ArgumentTypeInfos.registerByClass(TempModifierTraitArgument.class, new TempModifierTraitArgument.Info()));
    public static final RegistryObject<ArgumentTypeInfo<?, ?>> ENUM_TYPE = ARGUMENTS.register("enum_type",
                                                               () -> ArgumentTypeInfos.registerByClass(NicerEnumArgument.class, new NicerEnumArgument.Info()));
}
