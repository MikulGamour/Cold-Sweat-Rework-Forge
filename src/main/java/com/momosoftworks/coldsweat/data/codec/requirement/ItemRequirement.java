package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.util.CommonStreamCodecs;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public record ItemRequirement(Optional<List<Either<TagKey<Item>, Item>>> items, Optional<TagKey<Item>> tag,
                              Optional<IntegerBounds> count, Optional<IntegerBounds> durability,
                              Optional<List<EnchantmentRequirement>> enchantments, Optional<List<EnchantmentRequirement>> storedEnchantments,
                              Optional<Potion> potion, ItemComponentsRequirement components, Optional<Predicate<ItemStack>> predicate)
{
    public static final Codec<ItemRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrBuiltinCodec(Registries.ITEM, BuiltInRegistries.ITEM).listOf().optionalFieldOf("items").forGetter(predicate -> predicate.items),
            TagKey.codec(Registries.ITEM).optionalFieldOf("tag").forGetter(predicate -> predicate.tag),
            IntegerBounds.CODEC.optionalFieldOf("count").forGetter(predicate -> predicate.count),
            IntegerBounds.CODEC.optionalFieldOf("durability").forGetter(predicate -> predicate.durability),
            EnchantmentRequirement.CODEC.listOf().optionalFieldOf("enchantments").forGetter(predicate -> predicate.enchantments),
            EnchantmentRequirement.CODEC.listOf().optionalFieldOf("stored_enchantments").forGetter(predicate -> predicate.storedEnchantments),
            BuiltInRegistries.POTION.byNameCodec().optionalFieldOf("potion").forGetter(predicate -> predicate.potion),
            ItemComponentsRequirement.CODEC.optionalFieldOf("components", new ItemComponentsRequirement()).forGetter(predicate -> predicate.components)
    ).apply(instance, ItemRequirement::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemRequirement> STREAM_CODEC = StreamCodec.of(
    (buf, predicate) ->
    {
        CommonStreamCodecs.writeOptionalList(buf, predicate.items, CommonStreamCodecs.tagOrRegistryCodec(Registries.ITEM));
        buf.writeOptional(predicate.tag, CommonStreamCodecs.tagKeyCodec(Registries.ITEM));
        buf.writeOptional(predicate.count, IntegerBounds.STREAM_CODEC);
        buf.writeOptional(predicate.durability, IntegerBounds.STREAM_CODEC);
        CommonStreamCodecs.writeOptionalList(buf, predicate.enchantments, EnchantmentRequirement.STREAM_CODEC);
        CommonStreamCodecs.writeOptionalList(buf, predicate.storedEnchantments, EnchantmentRequirement.STREAM_CODEC);
        CommonStreamCodecs.writeOptional(buf, predicate.potion, ByteBufCodecs.registry(Registries.POTION));
        ItemComponentsRequirement.STREAM_CODEC.encode(buf, predicate.components);
    },
    (buf) ->
    {
        Optional<List<Either<TagKey<Item>, Item>>> items = CommonStreamCodecs.readOptionalList(buf, CommonStreamCodecs.tagOrRegistryCodec(Registries.ITEM));
        Optional<TagKey<Item>> tag = buf.readOptional(CommonStreamCodecs.tagKeyCodec(Registries.ITEM));
        Optional<IntegerBounds> count = buf.readOptional(IntegerBounds.STREAM_CODEC);
        Optional<IntegerBounds> durability = buf.readOptional(IntegerBounds.STREAM_CODEC);
        Optional<List<EnchantmentRequirement>> enchantments = CommonStreamCodecs.readOptionalList(buf, EnchantmentRequirement.STREAM_CODEC);
        Optional<List<EnchantmentRequirement>> storedEnchantments = CommonStreamCodecs.readOptionalList(buf, EnchantmentRequirement.STREAM_CODEC);
        Optional<Potion> potion = CommonStreamCodecs.readOptional(buf, ByteBufCodecs.registry(Registries.POTION));
        ItemComponentsRequirement components = ItemComponentsRequirement.STREAM_CODEC.decode(buf);
        return new ItemRequirement(items, tag, count, durability, enchantments, storedEnchantments, potion, components);
    });

    public static final ItemRequirement NONE = new ItemRequirement(Optional.empty(), Optional.empty(), Optional.empty(),
                                                                   Optional.empty(), Optional.empty(), Optional.empty(),
                                                                   Optional.empty(), new ItemComponentsRequirement());

    public ItemRequirement(Optional<List<Either<TagKey<Item>, Item>>> items, Optional<TagKey<Item>> tag,
                           Optional<IntegerBounds> count, Optional<IntegerBounds> durability,
                           Optional<List<EnchantmentRequirement>> enchantments,
                           Optional<List<EnchantmentRequirement>> storedEnchantments,
                           Optional<Potion> potion, ItemComponentsRequirement components)
    {
        this(items, tag, count, durability, enchantments, storedEnchantments, potion, components, Optional.empty());
    }

    public ItemRequirement(List<Either<TagKey<Item>, Item>> items, ItemComponentsRequirement components)
    {
        this(Optional.of(items), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), components);
    }

    public ItemRequirement(Collection<Item> items, @Nullable Predicate<ItemStack> predicate)
    {
        this(Optional.of(items.stream().map(Either::<TagKey<Item>, Item>right).toList()), Optional.empty(), Optional.empty(), Optional.empty(),
             Optional.empty(), Optional.empty(), Optional.empty(), new ItemComponentsRequirement(), Optional.ofNullable(predicate));
    }

    public boolean test(ItemStack stack, boolean ignoreCount)
    {
        if (stack.isEmpty() && items.isPresent() && !items.get().isEmpty())
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
        if (this.predicate.isPresent())
        {   return this.predicate.get().test(stack);
        }
        if (!this.components.test(stack.getComponents()))
        {   return false;
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
        else if (potion.isPresent() && !potion.get().getEffects().equals(stack.getOrDefault(DataComponents.POTION_CONTENTS, new PotionContents(Potions.AWKWARD)).potion().get().value().getEffects()))
        {   return false;
        }
        else if (!components.test(stack.getComponents()))
        {   return false;
        }
        else if (enchantments.isPresent())
        {
            ItemEnchantments stackEnchantments = stack.get(DataComponents.ENCHANTMENTS);
            if (stackEnchantments == null)
            {   return false;
            }
            for (EnchantmentRequirement enchantment : enchantments.get())
            {
                if (!enchantment.test(stackEnchantments))
                {   return false;
                }
            }
        }
        else if (storedEnchantments.isPresent())
        {
            ItemEnchantments stackEnchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
            if (stackEnchantments == null)
            {   return false;
            }
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
            && components.equals(that.components) && predicate.equals(that.predicate);
    }
}
