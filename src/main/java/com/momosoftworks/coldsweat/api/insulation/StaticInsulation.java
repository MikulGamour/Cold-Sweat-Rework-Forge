package com.momosoftworks.coldsweat.api.insulation;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public class StaticInsulation extends Insulation
{
    public static final Codec<StaticInsulation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("cold").forGetter(StaticInsulation::getCold),
            Codec.DOUBLE.fieldOf("heat").forGetter(StaticInsulation::getHeat)
    ).apply(instance, StaticInsulation::new));

    private final double cold;
    private final double heat;

    public StaticInsulation(double cold, double heat)
    {   this.cold = cold;
        this.heat = heat;
    }

    public StaticInsulation(Pair<? extends Number, ? extends Number> pair)
    {   this(pair.getFirst().doubleValue(), pair.getSecond().doubleValue());
    }

    @Override
    public double getCold()
    {   return cold;
    }

    @Override
    public double getHeat()
    {   return heat;
    }

    @Override
    public boolean isEmpty()
    {   return cold == 0 && heat == 0;
    }

    @Override
    public List<Insulation> split()
    {
        List<Insulation> insulation = new ArrayList<>();
        double cold = this.getCold();
        double heat = this.getHeat();
        double neutral = cold > 0 == heat > 0 ? CSMath.minAbs(cold, heat) : 0;
        cold -= neutral;
        heat -= neutral;

        // Cold insulation
        for (int i = 0; i < CSMath.ceil(Math.abs(cold) / 2); i++)
        {   double coldInsul = CSMath.minAbs(CSMath.shrink(cold, i * 2), 2 * CSMath.sign(cold));
            insulation.add(new StaticInsulation(coldInsul, 0d));
        }

        // Neutral insulation
        for (int i = 0; i < CSMath.ceil(Math.abs(neutral)); i++)
        {   double neutralInsul = CSMath.minAbs(CSMath.shrink(neutral, i), 1 * CSMath.sign(neutral));
            insulation.add(new StaticInsulation(neutralInsul, neutralInsul));
        }

        // Heat insulation
        for (int i = 0; i < CSMath.ceil(Math.abs(heat) / 2); i++)
        {   double heatInsul = CSMath.minAbs(CSMath.shrink(heat, i * 2),  2 * CSMath.sign(heat));
            insulation.add(new StaticInsulation(0d, heatInsul));
        }
        return insulation;
    }

    @Override
    public <T extends Insulation> T copy()
    {   return (T) new StaticInsulation(cold, heat);
    }

    @Override
    public String toString()
    {   return "Insulation{" + "cold=" + cold + ", heat=" + heat + '}';
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        return obj instanceof StaticInsulation insul
            && cold == insul.cold
            && heat == insul.heat;
    }

    @Override
    public CompoundTag serialize()
    {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("cold", cold);
        tag.putDouble("heat", heat);
        return tag;
    }

    public static StaticInsulation deserialize(CompoundTag tag)
    {   return new StaticInsulation(tag.getDouble("cold"), tag.getDouble("heat"));
    }
}
