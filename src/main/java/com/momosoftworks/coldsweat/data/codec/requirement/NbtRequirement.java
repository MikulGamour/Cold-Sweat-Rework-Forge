package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.serialization.Codec;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.stream.IntStream;

import static net.minecraft.advancements.critereon.NbtPredicate.getEntityTagToCompare;

public record NbtRequirement(CompoundTag tag)
{
    public static final Codec<NbtRequirement> CODEC = CompoundTag.CODEC.xmap(NbtRequirement::new, NbtRequirement::tag);

    public NbtRequirement()
    {   this(new CompoundTag());
    }

    public boolean test(ItemStack pStack)
    {   return this.tag().isEmpty() || this.test(pStack.getTag());
    }

    public boolean test(Entity pEntity)
    {   return this.tag().isEmpty() || this.test(getEntityTagToCompare(pEntity));
    }

    public boolean test(@Nullable Tag pTag)
    {
        if (pTag == null)
        {   return this.tag().isEmpty();
        }
        else
        {   return compareNbt(this.tag, pTag, true);
        }
    }

    /**
     * It is assumed that the first tag is a predicate, and the second tag is the tag to compare.
     */
    public static boolean compareNbt(@Nullable Tag tag, @Nullable Tag other, boolean compareListTag)
    {
        if (tag == other) return true;
        if (tag == null) return true;
        if (other == null) return false;
        if (tag.equals(other)) return true;

        // Handle CompoundTag comparison
        if (tag instanceof CompoundTag compoundTag)
        {   return handleCompoundTagComparison(compoundTag, other, compareListTag);
        }

        // Handle ListTag comparison
        if (tag instanceof ListTag && other instanceof ListTag && compareListTag)
        {   return compareListTags((ListTag) tag, (ListTag) other, compareListTag);
        }

        // Handle numeric range comparison
        if (tag instanceof StringTag string && other instanceof NumericTag numericTag)
        {   return compareNumericRange(string, numericTag);
        }

        // Handle numeric comparison
        if (tag instanceof NumericTag numericTag && other instanceof NumericTag otherNumeric)
        {   return compareNumbers(numericTag, otherNumeric);
        }

        return false;
    }

    private static boolean handleCompoundTagComparison(CompoundTag compoundTag, Tag other, boolean compareListTag)
    {
        // Case 1: Compare with another CompoundTag
        if (other instanceof CompoundTag otherCompound)
        {
            for (String key : compoundTag.getAllKeys())
            {
                if (!compareNbt(compoundTag.get(key), otherCompound.get(key), compareListTag))
                {   return false;
                }
            }
            return true;
        }

        // Case 2: Special comparison with cs:contains or cs:any_of
        if (compoundTag.getAllKeys().size() != 1)
            return false;

        ListTag anyOfValues = (ListTag) compoundTag.get("cs:any_of");
        if (anyOfValues != null && !anyOfValues.isEmpty())
        {
            for (int i = 0; i < anyOfValues.size(); i++)
            {
                Tag value = anyOfValues.get(i);
                if (compareNbt(value, other, compareListTag))
                {   return true;
                }
            }
            return false;
        }

        ListTag containsAnyValues = (ListTag) compoundTag.get("cs:contains_any");
        if (containsAnyValues != null && !containsAnyValues.isEmpty() && other instanceof ListTag otherList)
        {
            for (int i = 0; i < containsAnyValues.size(); i++)
            {
                Tag value = containsAnyValues.get(i);
                for (int i1 = 0; i1 < otherList.size(); i1++)
                {
                    Tag otherValue = otherList.get(i1);
                    if (compareNbt(value, otherValue, compareListTag))
                    {   return true;
                    }
                }
            }
        }

        ListTag containsAllValues = (ListTag) compoundTag.get("cs:contains_all");
        if (containsAllValues != null && !containsAllValues.isEmpty() && other instanceof ListTag otherList)
        {
            for (int i = 0; i < containsAllValues.size(); i++)
            {
                Tag value = containsAllValues.get(i);
                find:
                {
                    for (int i1 = 0; i1 < otherList.size(); i1++)
                    {
                        Tag otherValue = otherList.get(i1);
                        if (compareNbt(value, otherValue, compareListTag))
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

    private static boolean compareListTags(ListTag list1, ListTag list2, boolean compareListTag)
    {
        if (list1.isEmpty()) return list2.isEmpty();

        return list1.stream()
                .allMatch(element ->
                                  IntStream.range(0, list2.size())
                                          .anyMatch(j -> compareNbt(element, list2.get(j), compareListTag))
                );
    }

    private static boolean compareNumericRange(StringTag rangeTag, NumericTag numberTag)
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

    private static boolean compareNumbers(NumericTag tag, NumericTag other)
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
