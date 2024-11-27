package com.momosoftworks.coldsweat.client.gui.config;

import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.client.gui.widget.Slider;

public class ConfigSliderButton extends Slider
{
    public ConfigSliderButton(int xPos, int yPos, ITextComponent displayStr, double minVal, double maxVal, double currentVal, IPressable handler, ISlider par)
    {   super(xPos, yPos, displayStr, minVal, maxVal, currentVal, handler, par);
    }

    public void setValue(double value)
    {   this.sliderValue = CSMath.clamp(value, 0, 1);
    }

    public double getValue()
    {   return this.sliderValue;
    }
}
