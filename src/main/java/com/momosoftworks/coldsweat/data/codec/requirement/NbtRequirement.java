package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.serialization.Codec;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.stream.IntStream;

import static net.minecraft.advancements.criterion.NBTPredicate.getEntityTagToCompare;

public class NbtRequirement
{
    public final CompoundNBT tag;

    public NbtRequirement(CompoundNBT tag)
    {   this.tag = tag;
    }

    public static final Codec<NbtRequirement> CODEC = CompoundNBT.CODEC.xmap(NbtRequirement::new, req -> req.tag);

    public NbtRequirement()
    {   this(new CompoundNBT());
    }

    public boolean test(ItemStack pStack)
    {   return this.tag.isEmpty() || this.test(pStack.getTag());
    }

    public boolean test(Entity pEntity)
    {   return this.tag.isEmpty() || this.test(getEntityTagToCompare(pEntity));
    }

    public boolean test(@Nullable INBT pTag)
    {
        if (pTag == null)
        {   return this.tag.isEmpty();
        }
        else
        {   return compareNbt(this.tag, pTag, true);
        }
    }

    /**
     * It is assumed that the first tag is a predicate, and the second tag is the tag to compare.
     */
    public static boolean compareNbt(@Nullable INBT tag, @Nullable INBT other, boolean compareListNBT)
    {
        if (tag == other) return true;
        if (tag == null) return true;
        if (other == null) return false;

        // Handle CompoundNBT comparison
        if (tag instanceof CompoundNBT)
        {   return handleCompoundNBTComparison((CompoundNBT) tag, other, compareListNBT);
        }

        // Handle ListNBT comparison
        if (tag instanceof ListNBT && other instanceof ListNBT && compareListNBT)
        {   return compareListNBTs((ListNBT) tag, (ListNBT) other, compareListNBT);
        }

        // Handle numeric range comparison
        if (tag instanceof StringNBT && other instanceof NumberNBT)
        {   return compareNumericRange((StringNBT) tag, (NumberNBT) other);
        }

        return tag.equals(other);
    }

    private static boolean handleCompoundNBTComparison(CompoundNBT CompoundNBT, INBT other, boolean compareListNBT)
    {
        // Case 1: Compare with another CompoundNBT
        if (other instanceof CompoundNBT)
        {
            return CompoundNBT.getAllKeys().stream()
                    .allMatch(key -> compareNbt(CompoundNBT.get(key), ((CompoundNBT) other).get(key), compareListNBT));
        }

        // Case 2: Special comparison with cs:contains or cs:any_of
        if (CompoundNBT.getAllKeys().size() != 1) return false;

        ListNBT anyOfValues = (ListNBT) CompoundNBT.get("cs:any_of");
        if (anyOfValues != null && !anyOfValues.isEmpty())
        {
            return anyOfValues.stream()
                    .anyMatch(value -> compareNbt(value, other, compareListNBT));
        }

        ListNBT containsValues = (ListNBT) CompoundNBT.get("cs:contains");
        if (containsValues != null && !containsValues.isEmpty() && other instanceof ListNBT)
        {
            return containsValues.stream()
                    .anyMatch(((ListNBT) other)::contains);
        }

        return false;
    }

    private static boolean compareListNBTs(ListNBT list1, ListNBT list2, boolean compareListNBT)
    {
        if (list1.isEmpty()) return list2.isEmpty();

        return list1.stream()
                .allMatch(element ->
                                  IntStream.range(0, list2.size())
                                          .anyMatch(j -> compareNbt(element, list2.get(j), compareListNBT))
                );
    }

    private static boolean compareNumericRange(StringNBT rangeTag, NumberNBT numberTag)
    {
        try
        {
            String[] parts = rangeTag.getAsString().split("-");
            if (parts.length != 2) return false;

            double min = Double.parseDouble(parts[0]);
            double max = Double.parseDouble(parts[1]);
            double value = numberTag.getAsDouble();

            return CSMath.betweenInclusive(value, min, max);
        }
        catch (Exception e)
        {   return false;
        }
    }

    @Override
    public String toString()
    {
        return "NbtRequirement{" +
                "tag=" + tag +
                '}';
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {   return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {   return false;
        }

        NbtRequirement that = (NbtRequirement) obj;
        return tag.equals(that.tag);
    }
}
