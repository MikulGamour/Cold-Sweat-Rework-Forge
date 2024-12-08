package com.momosoftworks.coldsweat.common.capability.shearing;

import net.minecraft.nbt.CompoundNBT;

public interface IShearableCap
{
    boolean isSheared();
    void setSheared(boolean sheared);

    int furGrowthCooldown();
    void setFurGrowthCooldown(int cooldown);

    int age();
    void setAge(int ticks);

    CompoundNBT serializeNBT();

    void deserializeNBT(CompoundNBT nbt);
}
