package com.momosoftworks.coldsweat.compat.create;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.blockentity.ThermolithBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayBehaviour;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Components;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

public class ColdSweatDisplayBehaviors
{
    public static DisplayBehaviour THERMOLITH;

    public static class Thermolith extends SingleLineDisplaySource
    {
        @Override
        protected MutableComponent provideLine(DisplayLinkContext displayLinkContext, DisplayTargetStats displayTargetStats)
        {
            if (displayLinkContext.getSourceBlockEntity() instanceof ThermolithBlockEntity thermolith)
            {
                double temperature = Temperature.getTemperatureAt(thermolith.getBlockPos(), thermolith.getLevel());

                Temperature.Units units = Temperature.Units.values()[displayLinkContext.sourceConfig().getInt("Units")];

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
                builder.addSelectionScrollInput(0, 137, (input, label) -> {
                    input.forOptions(List.of(Components.literal(Temperature.Units.F.getFormattedName()),
                                             Components.literal(Temperature.Units.C.getFormattedName()),
                                             Components.literal(Temperature.Units.MC.getFormattedName())))
                         .titled(Component.translatable("cold_sweat.config.units.name"));
                }, "Units");
            }
        }

        @Override
        public Component getName()
        {   return Component.translatable("block.cold_sweat.thermolith");
        }
    }
}
