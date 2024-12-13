package com.momosoftworks.coldsweat.compat.create;

import com.google.common.collect.ImmutableList;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.blockentity.ThermolithBlockEntity;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Optional;

// TODO: Reimplement when Create is updated
public class ColdSweatDisplayBehaviors
{
    /*public static DisplayBehaviour THERMOLITH;

    public static class Thermolith extends SingleLineDisplaySource
    {
        @Override
        protected MutableComponent provideLine(DisplayLinkContext displayLinkContext, DisplayTargetStats displayTargetStats)
        {
            if (displayLinkContext.getSourceBlockEntity() instanceof ThermolithBlockEntity thermolith)
            {
                double temperature = WorldHelper.getTemperatureAt(thermolith.getLevel(), thermolith.getBlockPos());

                String unitsString = displayLinkContext.sourceConfig().getString("Units");
                Temperature.Units units = Optional.ofNullable(Temperature.Units.fromID(unitsString)).orElse(Temperature.Units.MC);

                double convertedTemp = Temperature.convert(temperature, Temperature.Units.MC, units, true);

                String text = String.format("%.0f%s", convertedTemp, units.getFormattedName());
                return Component.literal(text);
            }
            return Component.empty();
        }

        @Override
        protected boolean allowsLabeling(DisplayLinkContext displayLinkContext)
        {   return true;
        }

        @Override
        public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine)
        {
            super.initConfigurationWidgets(context, builder, isFirstLine);
            if (!isFirstLine)
            {
                builder.addTextInput(0, 137, (e, t) -> {
                    e.setValue("mc");
                    t.withTooltip(ImmutableList.of(Component.translatable("cold_sweat.config.units.name").append(" (f, c, mc)")
                                                           .withStyle((s) -> s.withColor(5476833)),
                                                   Lang.translateDirect("gui.schedule.lmb_edit")
                                                           .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
                }, "Units");
            }
        }

        @Override
        public Component getName()
        {   return Component.translatable("block.cold_sweat.thermolith");
        }
    }*/
}
