package com.momosoftworks.coldsweat.client.gui.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public class InsulationAttributeTooltip implements TooltipComponent
{
    Component original;
    Font font;
    boolean strikethrough;

    public InsulationAttributeTooltip(Component original, Font font, boolean strikethrough)
    {   this.original = original;
        this.font = font;
        this.strikethrough = strikethrough;
    }

    public Component getOriginal()
    {   return this.original;
    }

    public Font getFont()
    {   return this.font;
    }

    public boolean isStrikethrough()
    {   return this.strikethrough;
    }
}
