package com.momosoftworks.coldsweat.api.temperature.modifier;

public class FrigidnessTempModifier extends ThermalSourceTempModifier
{
    public FrigidnessTempModifier()
    {   this(0);
    }

    public FrigidnessTempModifier(int strength)
    {   super(strength, 0);
    }

    @Override
    public int getStrength()
    {   return this.getCooling();
    }
}
