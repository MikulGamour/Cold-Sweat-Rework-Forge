package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.serialization.Codec;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

import java.util.Arrays;

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
        if (tag == other)
        {   return true;
        }
        else if (tag == null)
        {   return true;
        }
        else if (other == null)
        {   return false;
        }
        else if (tag instanceof CompoundTag)
        {
            CompoundTag compoundNbt1 = (CompoundTag) tag;
            CompoundTag compoundNbt2 = (CompoundTag) other;

            for (String s : compoundNbt1.getAllKeys())
            {
                Tag tag1 = compoundNbt1.get(s);
                Tag tag2 = compoundNbt2.get(s);
                if (!compareNbt(tag1, tag2, compareListTag))
                {   return false;
                }
            }

            return true;
        }
        else if (tag instanceof ListTag && compareListTag)
        {
            ListTag listtag = (ListTag) tag;
            ListTag listtag1 = (ListTag) other;
            if (listtag.isEmpty())
            {   return listtag1.isEmpty();
            }
            else
            {
                for (int i = 0; i < listtag.size(); ++i)
                {
                    Tag element = listtag.get(i);
                    boolean flag = false;

                    for (int j = 0; j < listtag1.size(); ++j)
                    {
                        if (compareNbt(element, listtag1.get(j), compareListTag))
                        {   flag = true;
                            break;
                        }
                    }

                    if (!flag)
                    {   return false;
                    }
                }

                return true;
            }
        }
        // Compare a number range (represented as a string "min-max") in the predicate tag to a number in the compared tag
        else if (tag instanceof StringTag string && other instanceof NumericTag otherNumber)
        {
            try
            {
                // Parse value ranges in to min and max values
                Double[] range = Arrays.stream(string.getAsString().split("-")).map(Double::parseDouble).toArray(Double[]::new);
                if (range.length == 2)
                {   return CSMath.betweenInclusive(otherNumber.getAsDouble(), range[0], range[1]);
                }
            }
            catch (Exception e)
            {   return false;
            }
        }
        return tag.equals(other);
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
