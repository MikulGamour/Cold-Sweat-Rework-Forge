package com.momosoftworks.coldsweat.api.temperature.modifier;

public class WarmthTempModifier extends ThermalSourceTempModifier
{
    public WarmthTempModifier()
    {   this(0);
    }

    public WarmthTempModifier(int strength)
    {   super(0, strength);
    }

    @Override
    public int getStrength()
    {   return this.getWarming();
    }
}
