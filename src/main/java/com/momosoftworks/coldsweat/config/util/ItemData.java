package com.momosoftworks.coldsweat.config.util;

import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Represents an item and its NBT data, as well as an optional entity predicate.
 */
public class ItemData
{
    @Nonnull
    private final Item item;
    @Nonnull
    private final CompoundNBT nbt;
    @Nullable
    private final EntityRequirement predicate;

    public ItemData(Item item, CompoundNBT nbt, EntityRequirement predicate)
    {   this.item = item;
        this.nbt = nbt;
        this.predicate = predicate;
    }

    public ItemData(Item item, CompoundNBT nbt)
    {   this.item = item;
        this.nbt = nbt;
        this.predicate = null;
    }

    public Item getItem()
    {   return item;
    }

    public CompoundNBT getTag()
    {   return nbt;
    }

    @Nullable
    public EntityRequirement getPredicate()
    {   return predicate;
    }

    public boolean testEntity(Entity entity)
    {   return predicate == null || predicate.test(entity);
    }

    public boolean isEmpty()
    {   return item == Items.AIR;
    }

    public CompoundNBT save(CompoundNBT tag)
    {   tag.putString("Id", ForgeRegistries.ITEMS.getKey(item).toString());
        if (!nbt.isEmpty())
        {   tag.put("Tag", nbt);
        }
        if (predicate != null)
        {   tag.put("Predicate", NBTHelper.writeEntityRequirement(predicate));
        }
        return tag;
    }

    public static ItemData load(CompoundNBT tag)
    {   Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(tag.getString("Id")));
        CompoundNBT nbt = tag.contains("Tag", 10)
                          ? tag.getCompound("Tag")
                          : new CompoundNBT();
        EntityRequirement predicate = tag.contains("Predicate", 10)
                                      ? NBTHelper.readEntityPredicate(tag.getCompound("Predicate"))
                                      : null;
        return new ItemData(item, nbt, predicate);
    }

    public static ItemData of(ItemStack stack)
    {   return new ItemData(stack.getItem(), CSMath.orElse(stack.getTag(), new CompoundNBT()).copy(), null);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o instanceof ItemData)
        {
            ItemData other = ((ItemData) o);
            return item == other.item
                && (other.nbt.isEmpty() || other.getTag().getAllKeys().stream().allMatch(key -> Objects.equals(other.nbt.get(key), nbt.get(key))))
                && (other.predicate == null || predicate == null
                || EntityRequirement.ANY.equals(other.predicate)
                || predicate.equals(other.predicate));
        }
        return false;
    }

    @Override
    public int hashCode()
    {   return 31 * item.hashCode();
    }

    @Override
    public String toString()
    {   StringBuilder builder = new StringBuilder("ItemData{item=").append(item);
        if (!nbt.isEmpty())
        {   builder.append(", nbt=").append(nbt);
        }
        if (predicate != null)
        {   builder.append(", predicate=").append(predicate.serialize());
        }
        return builder.append('}').toString();
    }
}
