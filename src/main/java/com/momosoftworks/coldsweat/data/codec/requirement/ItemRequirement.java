package com.momosoftworks.coldsweat.data.codec.requirement;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.util.serialization.SerializablePredicate;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtils;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.registry.Registry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class ItemRequirement
{
    public final Optional<List<Either<ITag<Item>, Item>>> items;
    public final Optional<ITag<Item>> tag;
    public final Optional<IntegerBounds> count;
    public final Optional<IntegerBounds> durability;
    public final Optional<List<EnchantmentRequirement>> enchantments;
    public final Optional<List<EnchantmentRequirement>> storedEnchantments;
    public final Optional<Potion> potion;
    public final NbtRequirement nbt;
    public final Optional<SerializablePredicate<ItemStack>> predicate;

    public ItemRequirement(Optional<List<Either<ITag<Item>, Item>>> items, Optional<ITag<Item>> tag,
                           Optional<IntegerBounds> count, Optional<IntegerBounds> durability,
                           Optional<List<EnchantmentRequirement>> enchantments, Optional<List<EnchantmentRequirement>> storedEnchantments,
                           Optional<Potion> potion, NbtRequirement nbt, Optional<SerializablePredicate<ItemStack>> predicate)
    {
        this.items = items;
        this.tag = tag;
        this.count = count;
        this.durability = durability;
        this.enchantments = enchantments;
        this.storedEnchantments = storedEnchantments;
        this.potion = potion;
        this.nbt = nbt;
        this.predicate = predicate;
    }
    public static final Codec<ItemRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrBuiltinCodec(Registry.ITEM_REGISTRY, Registry.ITEM).listOf().optionalFieldOf("items").forGetter(predicate -> predicate.items),
            ITag.codec(ItemTags::getAllTags).optionalFieldOf("tag").forGetter(predicate -> predicate.tag),
            IntegerBounds.CODEC.optionalFieldOf("count").forGetter(predicate -> predicate.count),
            IntegerBounds.CODEC.optionalFieldOf("durability").forGetter(predicate -> predicate.durability),
            EnchantmentRequirement.CODEC.listOf().optionalFieldOf("enchantments").forGetter(predicate -> predicate.enchantments),
            EnchantmentRequirement.CODEC.listOf().optionalFieldOf("stored_enchantments").forGetter(predicate -> predicate.storedEnchantments),
            Registry.POTION.optionalFieldOf("potion").forGetter(predicate -> predicate.potion),
            NbtRequirement.CODEC.optionalFieldOf("nbt", new NbtRequirement(new CompoundNBT())).forGetter(predicate -> predicate.nbt),
            Codec.STRING.xmap(SerializablePredicate::<ItemStack>deserialize, SerializablePredicate::serialize)
                        .optionalFieldOf("predicate").forGetter(predicate -> predicate.predicate)
    ).apply(instance, ItemRequirement::new));

    public static final ItemRequirement NONE = new ItemRequirement(Optional.empty(), Optional.empty(), Optional.empty(),
                                                                   Optional.empty(), Optional.empty(), Optional.empty(),
                                                                   Optional.empty(), new NbtRequirement());

    public ItemRequirement(Optional<List<Either<ITag<Item>, Item>>> items, Optional<ITag<Item>> tag,
                           Optional<IntegerBounds> count, Optional<IntegerBounds> durability,
                           Optional<List<EnchantmentRequirement>> enchantments,
                           Optional<List<EnchantmentRequirement>> storedEnchantments,
                           Optional<Potion> potion, NbtRequirement nbt)
    {
        this(items, tag, count, durability, enchantments, storedEnchantments, potion, nbt, Optional.empty());
    }

    public ItemRequirement(Predicate<ItemStack> predicate)
    {
        this(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
             Optional.empty(), Optional.empty(), Optional.empty(), new NbtRequirement(),
             Optional.of(new SerializablePredicate<>(predicate)));
    }

    public boolean test(ItemStack stack, boolean ignoreCount)
    {
        if (this.predicate.isPresent())
        {   return this.predicate.get().test(stack);
        }
        if (stack.isEmpty() && items.isPresent() && !items.get().isEmpty())
        {   return false;
        }

        if (!this.nbt.test(stack.getTag()))
        {   return false;
        }
        if (items.isPresent())
        {
            for (int i = 0; i < items.get().size(); i++)
            {
                Either<ITag<Item>, Item> either = items.get().get(i);
                if (either.map(tag -> tag.contains(stack.getItem()), item -> stack.getItem() == item))
                {   break;
                }
                if (i == items.get().size() - 1)
                {   return false;
                }
            }
        }
        if (tag.isPresent() && !tag.get().contains(stack.getItem()))
        {   return false;
        }
        if (!ignoreCount && count.isPresent() && !count.get().test(stack.getCount()))
        {   return false;
        }
        else if (durability.isPresent() && !durability.get().test(stack.getMaxDamage() - stack.getDamageValue()))
        {   return false;
        }
        else if (potion.isPresent() && !potion.get().getEffects().equals(PotionUtils.getPotion(stack).getEffects()))
        {   return false;
        }
        else if (!nbt.test(stack.getTag()))
        {   return false;
        }
        else if (enchantments.isPresent())
        {
            Map<Enchantment, Integer> stackEnchantments = EnchantmentHelper.deserializeEnchantments(stack.getEnchantmentTags());
            for (EnchantmentRequirement enchantment : enchantments.get())
            {
                if (!enchantment.test(stackEnchantments))
                {   return false;
                }
            }
        }
        else if (storedEnchantments.isPresent())
        {
            Map<Enchantment, Integer> stackEnchantments = EnchantmentHelper.deserializeEnchantments(EnchantedBookItem.getEnchantments(stack));
            for (EnchantmentRequirement enchantment : storedEnchantments.get())
            {   if (!enchantment.test(stackEnchantments))
                {   return false;
                }
            }
        }
        return true;
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

        ItemRequirement that = (ItemRequirement) obj;

        if (!items.equals(that.items))
        {   return false;
        }
        if (!count.equals(that.count))
        {   return false;
        }
        if (!durability.equals(that.durability))
        {   return false;
        }
        if (!enchantments.equals(that.enchantments))
        {   return false;
        }
        if (!storedEnchantments.equals(that.storedEnchantments))
        {   return false;
        }
        if (!potion.equals(that.potion))
        {   return false;
        }
        return nbt.equals(that.nbt);
    }

    @Override
    public String toString()
    {
        return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(JsonElement::toString).orElse("");
    }
}
