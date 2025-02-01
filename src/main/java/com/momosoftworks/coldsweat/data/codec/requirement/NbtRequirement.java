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
    private final CompoundNBT tag;

    public NbtRequirement(CompoundNBT tag)
    {   this.tag = tag;
    }

    public static final Codec<NbtRequirement> CODEC = CompoundNBT.CODEC.xmap(NbtRequirement::new, req -> req.tag);

    public NbtRequirement()
    {   this(new CompoundNBT());
    }

    public CompoundNBT tag()
    {   return tag;
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
        if (tag.equals(other)) return true;

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

        // Handle numeric comparison
        if (tag instanceof NumberNBT && other instanceof NumberNBT)
        {   return compareNumbers((NumberNBT) tag, (NumberNBT) other);
        }

        return false;
    }

    private static boolean handleCompoundNBTComparison(CompoundNBT compoundTag, INBT other, boolean compareListNBT)
    {
        // Case 1: Compare with another CompoundNBT
        if (other instanceof CompoundNBT)
        {
            CompoundNBT otherCompound = (CompoundNBT) other;
            for (String key : compoundTag.getAllKeys())
            {
                if (!compareNbt(compoundTag.get(key), otherCompound.get(key), compareListNBT))
                {   return false;
                }
            }
            return true;
        }

        // Case 2: Special comparison with cs:contains or cs:any_of
        if (compoundTag.getAllKeys().size() != 1)
            return false;

        ListNBT anyOfValues = (ListNBT) compoundTag.get("cs:any_of");
        if (anyOfValues != null && !anyOfValues.isEmpty())
        {
            for (int i = 0; i < anyOfValues.size(); i++)
            {
                INBT value = anyOfValues.get(i);
                if (compareNbt(value, other, compareListNBT))
                {   return true;
                }
            }
            return false;
        }

        ListNBT containsAnyValues = (ListNBT) compoundTag.get("cs:contains_any");
        if (containsAnyValues != null && !containsAnyValues.isEmpty() && other instanceof ListNBT)
        {
            ListNBT otherList = (ListNBT) other;
            for (int i = 0; i < containsAnyValues.size(); i++)
            {
                INBT value = containsAnyValues.get(i);
                for (int i1 = 0; i1 < otherList.size(); i1++)
                {
                    INBT otherValue = otherList.get(i1);
                    if (compareNbt(value, otherValue, compareListNBT))
                    {   return true;
                    }
                }
            }
        }

        ListNBT containsAllValues = (ListNBT) compoundTag.get("cs:contains_all");
        if (containsAllValues != null && !containsAllValues.isEmpty() && other instanceof ListNBT)
        {
            ListNBT otherList = (ListNBT) other;
            for (int i = 0; i < containsAllValues.size(); i++)
            {
                INBT value = containsAllValues.get(i);
                find:
                {
                    for (int i1 = 0; i1 < otherList.size(); i1++)
                    {
                        INBT otherValue = otherList.get(i1);
                        if (compareNbt(value, otherValue, compareListNBT))
                        {   break find;
                        }
                    }
                    return false;
                }
            }
            return true;
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

    private static boolean compareNumbers(NumberNBT tag, NumberNBT other)
    {   return tag.getAsDouble() == other.getAsDouble();
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
