package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.impl.RequirementHolder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.NbtRequirement;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.ListBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DryingItemData extends ConfigData implements RequirementHolder
{
    public static final Codec<DryingItemData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            ItemRequirement.CODEC.fieldOf("item").forGetter(data -> data.data),
            ItemStack.CODEC.optionalFieldOf("result", ItemStack.EMPTY).forGetter(data -> data.result),
            EntityRequirement.getCodec().optionalFieldOf("entity", EntityRequirement.NONE).forGetter(data -> data.entity),
            SoundEvent.CODEC.optionalFieldOf("sound", SoundEvents.WET_GRASS_STEP).forGetter(data -> data.sound)
    ).apply(builder, DryingItemData::new));

    private final ItemRequirement data;
    private final ItemStack result;
    private final EntityRequirement entity;
    private final SoundEvent sound;

    public DryingItemData(ItemRequirement data, ItemStack result, EntityRequirement entity, SoundEvent sound,
                          List<String> requiredMods)
    {
        super(requiredMods);
        this.data = data;
        this.result = result;
        this.entity = entity;
        this.sound = sound;
    }

    public DryingItemData(ItemRequirement data, ItemStack result, EntityRequirement entity, SoundEvent sound)
    {
        this(data, result, entity, sound,
             // Required mods
             ListBuilder.begin(ConfigHelper.getModIDs(CSMath.listOrEmpty(data.items()), ForgeRegistries.ITEMS))
             .add(ForgeRegistries.ITEMS.getKey(result.getItem()).getNamespace())
             .add(sound.getLocation().getNamespace())
             .build());
    }

    public ItemRequirement data()
    {   return data;
    }
    public ItemStack result()
    {   return result;
    }
    public EntityRequirement entity()
    {   return entity;
    }
    public SoundEvent sound()
    {   return sound;
    }

    @Nullable
    public static DryingItemData fromToml(List<?> entry)
    {
        if (entry.size() < 2)
        {   ColdSweat.LOGGER.error("Error parsing drying item config: not enough arguments");
            return null;
        }
        List<Either<TagKey<Item>, Item>> items = ConfigHelper.getItems((String) entry.get(0));
        if (items.isEmpty()) return null;
        Item result = ForgeRegistries.ITEMS.getValue(new ResourceLocation((String) entry.get(1)));
        if (result == null) return null;

        ResourceLocation sound = entry.size() > 2
                                 ? new ResourceLocation((String) entry.get(2))
                                 : new ResourceLocation("minecraft:block.wet_grass.step");

        if (result != null)
        {   ItemRequirement input = new ItemRequirement(items, new NbtRequirement());
            return new DryingItemData(input, new ItemStack(result), EntityRequirement.NONE, ForgeRegistries.SOUND_EVENTS.getValue(sound));
        }
        else return null;
    }

    @Override
    public boolean test(Entity entity)
    {   return this.entity.test(entity);
    }

    @Override
    public boolean test(ItemStack stack)
    {   return this.data.test(stack, true);
    }

    @Override
    public Codec<? extends ConfigData> getCodec()
    {   return CODEC;
    }
}
