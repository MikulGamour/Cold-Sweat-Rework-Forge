package net.momostudios.coldsweat.core.capabilities;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.momostudios.coldsweat.core.util.PlayerTemp;

import javax.annotation.Nullable;

public class PlayerTempCapStorage implements Capability.IStorage<ITemperatureCapability> {
    @Override
    @Nullable
    public INBT writeNBT(Capability<ITemperatureCapability> capability, ITemperatureCapability instance, Direction direction)
    {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putDouble("ambient", instance.get(PlayerTemp.Types.AMBIENT));
        nbt.putDouble("body", instance.get(PlayerTemp.Types.BODY));
        nbt.putDouble("base", instance.get(PlayerTemp.Types.BASE));
        nbt.putDouble("composite", instance.get(PlayerTemp.Types.COMPOSITE));
        return nbt;
    }

    @Override
    public void readNBT(Capability<ITemperatureCapability> capability, ITemperatureCapability instance, Direction direction, INBT nbtData) {
        instance.set(PlayerTemp.Types.AMBIENT, ((CompoundNBT) nbtData).getDouble("ambient"));
        instance.set(PlayerTemp.Types.BODY, ((CompoundNBT) nbtData).getDouble("body"));
        instance.set(PlayerTemp.Types.BASE, ((CompoundNBT) nbtData).getDouble("base"));
        instance.set(PlayerTemp.Types.COMPOSITE, ((CompoundNBT) nbtData).getDouble("composite"));
    }
}
