package com.momosoftworks.coldsweat.compat.create;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.blockentity.ThermolithBlockEntity;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import com.simibubi.create.content.redstone.displayLink.DisplayBehaviour;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Components;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

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
                double temperature = WorldHelper.getTemperatureAt(thermolith.getLevel(), thermolith.getBlockPos());

                Temperature.Units units = Temperature.Units.values()[displayLinkContext.sourceConfig().getInt("Units")];

                double convertedTemp = Temperature.convert(temperature, Temperature.Units.MC, units, true);

                String text = String.format("%.0f%s", convertedTemp, units.getFormattedName());
                return new TextComponent(text);
            }
            return new TextComponent("");
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
                         .titled(new TranslatableComponent("cold_sweat.config.units.name"));
                }, "Units");
            }
        }

        @Override
        public Component getName()
        {   return new TranslatableComponent("block.cold_sweat.thermolith");
        }
    }
}
