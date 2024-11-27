package com.momosoftworks.coldsweat.client.gui.config;

import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class ConfigSliderButton extends AbstractSliderButton
{
    public ConfigSliderButton(int x, int y, int width, int height, Component message, double value)
    {   super(x, y, width, height, message, value);
    }

    @Override
    protected void updateMessage()
    {
    }

    @Override
    protected void applyValue()
    {
    }

    public void setValue(double value)
    {   this.value = CSMath.clamp(value, 0, 1);
    }

    public double getValue()
    {   return this.value;
    }
}
