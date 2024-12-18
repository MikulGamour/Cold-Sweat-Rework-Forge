package com.momosoftworks.coldsweat.api.temperature.modifier.compat;

import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import org.jwaresoftware.mcmods.lib.Armory;

import java.util.function.Function;

/**
 * Special TempModifier class for Armor Underwear
 */
public class ArmorUnderTempModifier extends TempModifier
{
    public ArmorUnderTempModifier() {}

    @Override
    protected Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait)
    {
        CompoundNBT nbt = this.getNBT();
        double bodyTemp = Temperature.get(entity, Temperature.Trait.BODY);
        double totalOffset = 0;

        // If this modifier is applied to MIN, get the cold lining; same for hot
        for (ItemStack stack : entity.getArmorSlots())
        {
            switch (trait)
            {
                case FREEZING_POINT :
                {   totalOffset += Math.min(0,
                            Armory.getTLining(stack).getModifier() * 3
                            + nbt.getFloat("OzzyTemp"));
                    break;
                }
                case BURNING_POINT :
                {   totalOffset += Math.max(0,
                            Armory.getTLining(stack).getModifier() * 3
                            + nbt.getFloat("OzzyTemp"));
                    break;
                }
            }

            // Special functionality for certain linings
            if (CompatManager.ArmorUnderwear.hasOzzyLiner(stack))
            {   nbt.putFloat("OzzyTemp", (float) (CSMath.blend(-4, 4, bodyTemp, -100, 100)));
            }
            else nbt.remove("OzzyTemp");
        }

        double returnTemp = Temperature.convert(totalOffset, Temperature.Units.F, Temperature.Units.MC, false);
        return temp -> temp + returnTemp;
    }
}
