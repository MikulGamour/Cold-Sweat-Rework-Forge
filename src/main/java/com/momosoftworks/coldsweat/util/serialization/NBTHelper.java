package com.momosoftworks.coldsweat.util.serialization;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.registry.TempModifierRegistry;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.core.init.ModItemComponents;
import com.momosoftworks.coldsweat.core.init.ModItems;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@EventBusSubscriber
public class NBTHelper
{
    private NBTHelper() {}

    public static CompoundTag modifierToTag(TempModifier modifier)
    {
        // Write the modifier's data to a CompoundTag
        CompoundTag modifierTag = new CompoundTag();
        ResourceLocation modifierId = TempModifierRegistry.getKey(modifier);
        if (modifierId == null)
        {
            ColdSweat.LOGGER.error("Failed to get key for TempModifier: {}", modifier.getClass().getSimpleName());
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

    public static Optional<TempModifier> tagToModifier(CompoundTag modifierTag)
    {
        // Create a new modifier from the CompoundTag
        Optional<TempModifier> optional = TempModifierRegistry.getValue(ResourceLocation.parse(modifierTag.getString("Id")));
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
        CompoundTag tag;
        if (owner instanceof Entity entity)
        {   tag = entity.getPersistentData();
        }
        else if (owner instanceof ItemStack stack)
        {   tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        }
        else if (owner instanceof BlockEntity blockEntity)
        {   tag = blockEntity.getPersistentData();
        }
        else return 0;

        int value = tag.getInt(key);
        if (predicate.test(value))
        {
            tag.putInt(key, value + amount);
            if (owner instanceof ItemStack stack)
            {   stack.set(DataComponents.CUSTOM_DATA, stack.get(DataComponents.CUSTOM_DATA).update(tg -> tg.putInt(key, value + amount)));
            }
        }
        return value + amount;
    }

    /**
     * Gets an item's tag, without creating a new one if it is not present.<br>
     * An empty {@link CompoundTag} will be returned in that case, so a null check will not be necessary.<br>
     * <br>
     * Use the item's data component directly if you need to write to the tag.<br>
     * @return The item's tag, or an empty tag if it is not present
     */
    public static CompoundTag getTagOrEmpty(ItemStack stack)
    {   return stack.has(DataComponents.CUSTOM_DATA) ? stack.get(DataComponents.CUSTOM_DATA).copyTag() : new CompoundTag();
    }

    public static CustomData getOrCreateTag(ItemStack stack)
    {
        if (!stack.has(DataComponents.CUSTOM_DATA))
        {   stack.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
        }
        return stack.get(DataComponents.CUSTOM_DATA);
    }

    public static void ensureTagAndDo(ItemStack stack, Consumer<CompoundTag> action)
    {   getOrCreateTag(stack).update(action);
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
    {   updateItemTags(event.getContainer().slots.stream().map(Slot::getItem).toList());
    }

    private static void updateItemTags(Collection<ItemStack> items)
    {
        for (ItemStack stack : items)
        {
            Item item = stack.getItem();
            if (!stack.has(DataComponents.CUSTOM_DATA)) return;
            CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();

            // Convert old tags on existing items
            if (item == ModItems.SOULSPRING_LAMP.value())
            {
                IntTag oldTemp = ((IntTag) CSMath.orElse(tag.get("fuel"), tag.get("Fuel")));
                if (oldTemp != null)
                {
                    stack.set(ModItemComponents.WATER_TEMPERATURE, oldTemp.getAsDouble());
                    stack.get(DataComponents.CUSTOM_DATA).update(tg ->
                    {   tg.remove("fuel");
                        tg.remove("Fuel");
                    });
                }
            }
            else if (item == ModItems.FILLED_WATERSKIN.value())
            {
                DoubleTag oldTemp = ((DoubleTag) CSMath.orElse(tag.get("temperature"), tag.get("Temperature")));
                if (oldTemp != null)
                {
                    stack.set(ModItemComponents.WATER_TEMPERATURE, oldTemp.getAsDouble());
                    stack.get(DataComponents.CUSTOM_DATA).update(tg ->
                    {   tg.remove("temperature");
                        tg.remove("Temperature");
                    });
                }
            }
        }
    }

    public static CompoundTag parseCompoundNbt(String tag)
    {
        if (tag.isBlank()) return new CompoundTag();
        try
        {   return TagParser.parseTag(tag);
        }
        catch (Exception e)
        {
            ColdSweat.LOGGER.error("Error parsing compound tag \"{}\": {}", tag, e.getMessage());
            e.printStackTrace();
            return new CompoundTag();
        }
    }

    public static ListTag listTagOf(List<?> list)
    {
        ListTag tag = new ListTag();
        for (Object obj : list)
        {   if (obj instanceof Tag tg)
            {   tag.add(tg);
            }
            else tag.add(writeValue(obj));
        }
        return tag;
    }

    @Nullable
    public static Object getValue(Tag tag)
    {
        if (tag instanceof IntTag integer)
        {   return integer.getAsInt();
        }
        else if (tag instanceof FloatTag floating)
        {   return floating.getAsFloat();
        }
        else if (tag instanceof DoubleTag doubleTag)
        {   return doubleTag.getAsDouble();
        }
        else if (tag instanceof LongTag longTag)
        {   return longTag.getAsLong();
        }
        else if (tag instanceof ShortTag shortTag)
        {   return shortTag.getAsShort();
        }
        else if (tag instanceof ByteTag byteTag)
        {   return byteTag.getAsByte();
        }
        else if (tag instanceof ByteArrayTag byteArray)
        {   return byteArray.getAsString();
        }
        else if (tag instanceof IntArrayTag intArray)
        {   return intArray.getAsIntArray();
        }
        else if (tag instanceof LongArrayTag longArray)
        {   return longArray.getAsLongArray();
        }
        else if (tag instanceof StringTag string)
        {   return string.getAsString();
        }
        return null;
    }

    @Nullable
    public static Tag writeValue(Object obj)
    {
        if (obj instanceof Integer integer)
        {   return IntTag.valueOf(integer);
        }
        else if (obj instanceof Float floating)
        {   return FloatTag.valueOf(floating);
        }
        else if (obj instanceof Double doubleTag)
        {   return DoubleTag.valueOf(doubleTag);
        }
        else if (obj instanceof Long longTag)
        {   return LongTag.valueOf(longTag);
        }
        else if (obj instanceof Short shortTag)
        {   return ShortTag.valueOf(shortTag);
        }
        else if (obj instanceof Byte byteTag)
        {   return ByteTag.valueOf(byteTag);
        }
        else if (obj instanceof String string)
        {   return StringTag.valueOf(string);
        }
        return null;
    }

    public static class ItemMutator
    {
        private final ItemStack stack;

        public ItemMutator(ItemStack stack)
        {   this.stack = stack;
        }

        public void put(String key, Object value)
        {
            if (stack.has(DataComponents.CUSTOM_DATA))
            {   Tag tagValue = writeValue(value);
                if (tagValue == null) throw new IllegalArgumentException("Invalid value type for NBT: " + value.getClass().getName());
                stack.set(DataComponents.CUSTOM_DATA, stack.get(DataComponents.CUSTOM_DATA).update(tag -> tag.put(key, tagValue)));
            }
        }

        public <T> T get(String key)
        {
            if (stack.has(DataComponents.CUSTOM_DATA))
            {   Tag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag().get(key);
                if (tag != null) return (T) getValue(tag);
            }
            return null;
        }
    }
}
