package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.impl.RequirementHolder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.NbtRequirement;
import com.momosoftworks.coldsweat.data.codec.util.ExtraCodecs;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemCarryTempData extends ConfigData implements RequirementHolder, IForgeRegistryEntry<ItemCarryTempData>
{
    final ItemRequirement data;
    final List<Either<IntegerBounds, EquipmentSlot>> slots;
    final double temperature;
    final Temperature.Trait trait;
    final Double maxEffect;
    final EntityRequirement entityRequirement;

    public ItemCarryTempData(ItemRequirement data, List<Either<IntegerBounds, EquipmentSlot>> slots, double temperature,
                             Temperature.Trait trait, Double maxEffect, EntityRequirement entityRequirement,
                             List<String> requiredMods)
    {
        super(requiredMods);
        this.data = data;
        this.slots = slots;
        this.temperature = temperature;
        this.trait = trait;
        this.maxEffect = maxEffect;
        this.entityRequirement = entityRequirement;
    }

    public ItemCarryTempData(ItemRequirement data, List<Either<IntegerBounds, EquipmentSlot>> slots, double temperature,
                             Temperature.Trait trait, Double maxEffect, EntityRequirement entityRequirement)
    {
        this(data, slots, temperature, trait, maxEffect, entityRequirement, ConfigHelper.getModIDs(CSMath.listOrEmpty(data.items()), ForgeRegistries.ITEMS));
    }

    public static final Codec<ItemCarryTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemRequirement.CODEC.fieldOf("data").forGetter(ItemCarryTempData::data),
            Codec.either(IntegerBounds.CODEC, ExtraCodecs.EQUIPMENT_SLOT).listOf().fieldOf("slots").forGetter(ItemCarryTempData::slots),
            Codec.DOUBLE.fieldOf("temperature").forGetter(ItemCarryTempData::temperature),
            Temperature.Trait.CODEC.optionalFieldOf("trait", Temperature.Trait.WORLD).forGetter(ItemCarryTempData::trait),
            Codec.DOUBLE.optionalFieldOf("max_effect", java.lang.Double.MAX_VALUE).forGetter(ItemCarryTempData::maxEffect),
            EntityRequirement.getCodec().optionalFieldOf("entity", EntityRequirement.NONE).forGetter(ItemCarryTempData::entityRequirement),
            Codec.STRING.listOf().optionalFieldOf("required_mods", List.of()).forGetter(ItemCarryTempData::requiredMods)
    ).apply(instance, ItemCarryTempData::new));

    public ItemRequirement data()
    {   return data;
    }
    public List<Either<IntegerBounds, EquipmentSlot>> slots()
    {   return slots;
    }
    public double temperature()
    {   return temperature;
    }
    public Temperature.Trait trait()
    {   return trait;
    }
    public Double maxEffect()
    {   return maxEffect;
    }
    public EntityRequirement entityRequirement()
    {   return entityRequirement;
    }

    @Override
    public boolean test(Entity entity)
    {   return entityRequirement.test(entity);
    }

    public boolean test(Entity entity, ItemStack stack, @Nullable Integer slot, @Nullable EquipmentSlot equipmentSlot)
    {   return test(stack, slot, equipmentSlot) && test(entity);
    }

    public boolean test(ItemStack stack, @Nullable Integer slot, @Nullable EquipmentSlot equipmentSlot)
    {
        if (!data.test(stack, true))
        {   return false;
        }
        if (slot == null && equipmentSlot == null)
        {   return false;
        }
        for (Either<IntegerBounds, EquipmentSlot> either : slots)
        {
            if (slot != null && either.left().isPresent() && either.left().get().test(slot))
            {   return true;
            }
            else if (equipmentSlot != null && either.right().isPresent() && either.right().get().equals(equipmentSlot))
            {   return true;
            }
        }
        return false;
    }

    @Nullable
    public static ItemCarryTempData fromToml(List<?> entry)
    {
        if (entry.size() < 4)
        {   return null;
        }
        List<Either<TagKey<Item>, Item>> items = ConfigHelper.getItems((String) entry.get(0));

        if (items.isEmpty())
        {   ColdSweat.LOGGER.error("Error parsing entity config: {} does not contain any valid entities", entry);
            return null;
        }
        //temp
        double temp = ((Number) entry.get(1)).doubleValue();
        // slots
        List<Either<IntegerBounds, EquipmentSlot>> slots = switch ((String) entry.get(2))
        {
            case "inventory" -> java.util.List.of(Either.left(IntegerBounds.NONE));
            case "hotbar"    -> java.util.List.of(Either.left(new IntegerBounds(36, 44)));
            case "hand" -> java.util.List.of(Either.right(EquipmentSlot.MAINHAND), Either.right(EquipmentSlot.OFFHAND));
            default -> java.util.List.of(Either.left(new IntegerBounds(-1, -1)));
        };
        // trait
        Temperature.Trait trait = Temperature.Trait.fromID((String) entry.get(3));
        // nbt
        NbtRequirement nbtRequirement = entry.size() > 4
                                        ? new NbtRequirement(NBTHelper.parseCompoundNbt((String) entry.get(4)))
                                        : new NbtRequirement(new CompoundTag());
        // max effect
        double maxEffect = entry.size() > 5 ? ((Number) entry.get(5)).doubleValue() : Double.MAX_VALUE;
        // compile item requirement
        ItemRequirement itemRequirement = new ItemRequirement(items, nbtRequirement);

        return new ItemCarryTempData(itemRequirement, slots, temp, trait, maxEffect, EntityRequirement.NONE);
    }

    public String getSlotRangeName()
    {
        String[] strictType = {""};
        if (this.slots().size() == 1) this.slots().get(0).ifLeft(left ->
        {
            if (left.equals(IntegerBounds.NONE))
            {  strictType[0] = "inventory";
            }
            if (left.min() == 36 && left.max() == 44)
            {  strictType[0] = "hotbar";
            }
        });
        else if (this.slots().size() == 2
        && this.slots().get(0).right().map(right -> right == EquipmentSlot.MAINHAND).orElse(false)
        && this.slots().get(1).right().map(right -> right == EquipmentSlot.OFFHAND).orElse(false))
        {  strictType[0] = "hand";
        }

        return strictType[0];
    }

    @Override
    public Codec<ItemCarryTempData> getCodec()
    {   return CODEC;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ItemCarryTempData that = (ItemCarryTempData) obj;
        return super.equals(obj)
            && temperature == that.temperature
            && data.equals(that.data)
            && slots.equals(that.slots)
            && trait.equals(that.trait)
            && maxEffect.equals(that.maxEffect)
            && entityRequirement.equals(that.entityRequirement);
    }

    @Override
    public ItemCarryTempData setRegistryName(ResourceLocation name)
    {   return this;
    }

    @Nullable
    @Override
    public ResourceLocation getRegistryName()
    {   return null;
    }

    @Override
    public Class<ItemCarryTempData> getRegistryType()
    {   return ItemCarryTempData.class;
    }
}
