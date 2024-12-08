package com.momosoftworks.coldsweat.common.capability.shearing;

import net.minecraft.nbt.CompoundNBT;

public class ShearableFurCap implements IShearableCap
{
    boolean sheared = false;
    int furGrowthCooldown = 0;
    int age = 0;

    @Override
    public boolean isSheared()
    {   return sheared;
    }

    @Override
    public void setSheared(boolean sheared)
    {   this.sheared = sheared;
    }

    @Override
    public int furGrowthCooldown()
    {   return furGrowthCooldown;
    }

    @Override
    public void setFurGrowthCooldown(int cooldown)
    {   furGrowthCooldown = cooldown;
    }

    @Override
    public int age()
    {   return age;
    }

    @Override
    public void setAge(int age)
    {   this.age = age;
    }

    @Override
    public CompoundNBT serializeNBT()
    {   CompoundNBT nbt = new CompoundNBT();
        nbt.putBoolean("Sheared", sheared);
        nbt.putInt("FurGrowthCooldown", furGrowthCooldown);
        nbt.putInt("Age", age);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt)
    {   sheared = nbt.getBoolean("Sheared");
        furGrowthCooldown = nbt.getInt("FurGrowthCooldown");
        age = nbt.getInt("Age");
    }
}
