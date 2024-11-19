package com.momosoftworks.coldsweat.util.serialization;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.registry.TempModifierRegistry;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class NBTHelper
{
    private NBTHelper() {}

    public static CompoundNBT modifierToTag(TempModifier modifier)
    {
        // Write the modifier's data to a CompoundTag
        CompoundNBT modifierTag = new CompoundNBT();
        ResourceLocation modifierId = TempModifierRegistry.getKey(modifier);
        if (modifierId == null)
        {
            ColdSweat.LOGGER.error("Failed to get key for TempModifier: {}", modifier);
            return modifierTag;
        }
        modifierTag.putString("Id", modifierId.toString());

        // Add the modifier's arguments
        modifierTag.put("ModifierData", modifier.getNBT());

        // Read the modifier's expiration time
        if (modifier.getExpireTime() != -1)
            modifierTag.putInt("ExpireTicks", modifier.getExpireTime());

        // Read the modifier's tick rate
        if (modifier.getTickRate() > 1)
            modifierTag.putInt("TickRate", modifier.getTickRate());

        // Read the modifier's ticks existed
        modifierTag.putInt("TicksExisted", modifier.getTicksExisted());

        return modifierTag;
    }

    public static Optional<TempModifier> tagToModifier(CompoundNBT modifierTag)
    {
        // Create a new modifier from the CompoundTag
        Optional<TempModifier> optional = TempModifierRegistry.getValue(new ResourceLocation(modifierTag.getString("Id")));
        optional.ifPresent(modifier ->
        {
            modifier.setNBT(modifierTag.getCompound("ModifierData"));

            // Set the modifier's expiration time
            if (modifierTag.contains("ExpireTicks"))
            {   modifier.expires(modifierTag.getInt("ExpireTicks"));
            }

            // Set the modifier's tick rate
            if (modifierTag.contains("TickRate"))
            {   modifier.tickRate(modifierTag.getInt("TickRate"));
            }

            // Set the modifier's ticks existed
            modifier.setTicksExisted(modifierTag.getInt("TicksExisted"));
        });

        return optional;
    }

    public static void incrementTag(Object owner, String key, int amount)
    {   incrementTag(owner, key, amount, (tag) -> true);
    }

    public static int incrementTag(Object owner, String key, int amount, Predicate<Integer> predicate)
    {
        CompoundNBT tag;
        if (owner instanceof Entity)
        {   tag = ((Entity) owner).getPersistentData();
        }
        else if (owner instanceof ItemStack)
        {   tag = ((ItemStack) owner).getOrCreateTag();
        }
        else if (owner instanceof TileEntity)
        {   tag = ((TileEntity) owner).getTileData();
        }
        else return 0;

        int value = tag.getInt(key);
        if (predicate.test(value))
        {   tag.putInt(key, value + amount);
        }
        return value + amount;
    }

    /**
     * Gets an item's tag, without creating a new one if it is not present.<br>
     * An empty {@link CompoundNBT} will be returned in that case, so a null check will not be necessary.<br>
     * <br>
     * Use {@link ItemStack#getOrCreateTag()} if you need to write to the tag.<br>
     * @return The item's tag, or an empty tag if it is not present
     */
    public static CompoundNBT getTagOrEmpty(ItemStack stack)
    {   return CSMath.orElse(stack.getTag(), new CompoundNBT());
    }

    /**
     * Used for storing Temperature values in the player's persistent data (NBT). <br>
     * <br>
     * @param trait The type of Temperature to be stored. ({@link Temperature.Trait#WORLD} should only be stored when needed to prevent lag)
     * @return The NBT tag name for the given type
     */
    public static String getTraitTagKey(Temperature.Trait trait)
    {   return trait.getSerializedName();
    }

    @SubscribeEvent
    public static void convertTagsInContainer(PlayerContainerEvent.Open event)
    {   updateItemTags(event.getContainer().slots.stream().map(Slot::getItem).collect(Collectors.toList()));
    }

    private static void updateItemTags(Collection<ItemStack> items)
    {
        for (ItemStack stack : items)
        {
            Item item = stack.getItem();
            CompoundNBT tag = NBTHelper.getTagOrEmpty(stack);

            // Convert old tags on existing items
            if (item == ModItems.SOULSPRING_LAMP)
            {
                if (tag.contains("fuel"))
                {   tag.putDouble("Fuel", tag.getDouble("fuel"));
                    tag.remove("fuel");
                }
            }
            else if (item == ModItems.FILLED_WATERSKIN)
            {
                if (tag.contains("temperature"))
                {   tag.putDouble("Temperature", tag.getDouble("temperature"));
                    tag.remove("temperature");
                }
            }
        }
    }

    public static CompoundNBT parseCompoundNbt(String tag)
    {
        try
        {   return JsonToNBT.parseTag(tag);
        }
        catch (Exception e)
        {
            ColdSweat.LOGGER.error("Error parsing compound tag \"{}\": {}", tag, e.getMessage());
            e.printStackTrace();
            return new CompoundNBT();
        }
    }

    public static ListNBT listTagOf(List<?> list)
    {
        ListNBT tag = new ListNBT();
        for (Object obj : list)
        {   if (obj instanceof INBT)
            {   tag.add(((INBT) obj));
            }
            else tag.add(writeValue(obj));
        }
        return tag;
    }

    @Nullable
    public static Object getValue(INBT tag)
    {
        if (tag instanceof IntNBT)
        {   return ((IntNBT) tag).getAsInt();
        }
        else if (tag instanceof FloatNBT)
        {   return ((FloatNBT) tag).getAsFloat();
        }
        else if (tag instanceof DoubleNBT)
        {   return ((DoubleNBT) tag).getAsDouble();
        }
        else if (tag instanceof LongNBT)
        {   return ((LongNBT) tag).getAsLong();
        }
        else if (tag instanceof ShortNBT)
        {   return ((ShortNBT) tag).getAsShort();
        }
        else if (tag instanceof ByteNBT)
        {   return ((ByteNBT) tag).getAsByte();
        }
        else if (tag instanceof ByteArrayNBT)
        {   return ((ByteArrayNBT) tag).getAsString();
        }
        else if (tag instanceof IntArrayNBT)
        {   return ((IntArrayNBT) tag).getAsIntArray();
        }
        else if (tag instanceof LongArrayNBT)
        {   return ((LongArrayNBT) tag).getAsLongArray();
        }
        else if (tag instanceof StringNBT)
        {   return ((StringNBT) tag).getAsString();
        }
        return null;
    }

    @Nullable
    public static INBT writeValue(Object obj)
    {
        if (obj instanceof Integer)
        {   return IntNBT.valueOf((Integer) obj);
        }
        else if (obj instanceof Float)
        {   return FloatNBT.valueOf((Float) obj);
        }
        else if (obj instanceof Double)
        {   return DoubleNBT.valueOf((Double) obj);
        }
        else if (obj instanceof Long)
        {   return LongNBT.valueOf((Long) obj);
        }
        else if (obj instanceof Short)
        {   return ShortNBT.valueOf((Short) obj);
        }
        else if (obj instanceof Byte)
        {   return ByteNBT.valueOf((Byte) obj);
        }
        else if (obj instanceof String)
        {   return StringNBT.valueOf((String) obj);
        }
        return null;
    }
}
