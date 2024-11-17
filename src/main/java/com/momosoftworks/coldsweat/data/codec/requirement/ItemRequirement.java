package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public record ItemRequirement(Optional<List<Either<TagKey<Item>, Item>>> items, Optional<TagKey<Item>> tag,
                              Optional<IntegerBounds> count, Optional<IntegerBounds> durability,
                              Optional<List<EnchantmentRequirement>> enchantments, Optional<List<EnchantmentRequirement>> storedEnchantments,
                              Optional<Potion> potion, NbtRequirement nbt, Optional<Predicate<ItemStack>> predicate)
{
    public static final Codec<ItemRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrForgeRegistryCodec(Registry.ITEM_REGISTRY, ForgeRegistries.ITEMS).listOf().optionalFieldOf("items").forGetter(predicate -> predicate.items),
            TagKey.codec(Registry.ITEM_REGISTRY).optionalFieldOf("tag").forGetter(predicate -> predicate.tag),
            IntegerBounds.CODEC.optionalFieldOf("count").forGetter(predicate -> predicate.count),
            IntegerBounds.CODEC.optionalFieldOf("durability").forGetter(predicate -> predicate.durability),
            EnchantmentRequirement.CODEC.listOf().optionalFieldOf("enchantments").forGetter(predicate -> predicate.enchantments),
            EnchantmentRequirement.CODEC.listOf().optionalFieldOf("stored_enchantments").forGetter(predicate -> predicate.storedEnchantments),
            ForgeRegistries.POTIONS.getCodec().optionalFieldOf("potion").forGetter(predicate -> predicate.potion),
            NbtRequirement.CODEC.optionalFieldOf("nbt", new NbtRequirement(new CompoundTag())).forGetter(predicate -> predicate.nbt)
    ).apply(instance, ItemRequirement::new));

    public static final ItemRequirement NONE = new ItemRequirement(Optional.empty(), Optional.empty(), Optional.empty(),
                                                                   Optional.empty(), Optional.empty(), Optional.empty(),
                                                                   Optional.empty(), new NbtRequirement());

    public ItemRequirement(Optional<List<Either<TagKey<Item>, Item>>> items, Optional<TagKey<Item>> tag,
                           Optional<IntegerBounds> count, Optional<IntegerBounds> durability,
                           Optional<List<EnchantmentRequirement>> enchantments,
                           Optional<List<EnchantmentRequirement>> storedEnchantments,
                           Optional<Potion> potion, NbtRequirement nbt)
    {
        this(items, tag, count, durability, enchantments, storedEnchantments, potion, nbt, Optional.empty());
    }

    public ItemRequirement(List<Item> items, NbtRequirement nbt)
    {
        this(Optional.of(items.stream().map(Either::<TagKey<Item>, Item>right).toList()), Optional.empty(),
             Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), nbt);
    }

    public ItemRequirement(Predicate<ItemStack> predicate)
    {
        this(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
             Optional.empty(), Optional.empty(), Optional.empty(), new NbtRequirement(),
             Optional.of(predicate));
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
            checkItem:
            {
                for (int i = 0; i < items.get().size(); i++)
                {
                    Either<TagKey<Item>, Item> either = items.get().get(i);
                    if (either.map(stack::is, stack::is))
                    {   break checkItem;
                    }
                }
                return false;
            }
        }
        if (tag.isPresent() && !stack.is(tag.get()))
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
    public String toString()
    {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ItemRequirement that = (ItemRequirement) obj;
        return items.equals(that.items) && tag.equals(that.tag) && count.equals(that.count)
            && durability.equals(that.durability) && enchantments.equals(that.enchantments)
            && storedEnchantments.equals(that.storedEnchantments) && potion.equals(that.potion)
            && nbt.equals(that.nbt) && predicate.equals(that.predicate);
    }
}
