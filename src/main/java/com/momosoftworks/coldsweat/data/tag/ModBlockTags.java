package com.momosoftworks.coldsweat.data.tag;

import com.momosoftworks.coldsweat.ColdSweat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModBlockTags
{
    public static final TagKey<Block> SOUL_STALK_PLACEABLE_ON = createTag("may_place_on/soul_stalk");
    public static final TagKey<Block> SOUL_SAND_REPLACEABLE = createTag("soul_sand_replaceable");

    public static final TagKey<Block> HEARTH_SPREAD_WHITELIST = createTag("hearth/spread_whitelist");
    public static final TagKey<Block> HEARTH_SPREAD_BLACKLIST = createTag("hearth/spread_blacklist");

    public static final TagKey<Block> IGNORE_SLEEP_CHECK = createTag("ignore_sleep_check");
    public static final TagKey<Block> EXTENDS_SMOKESTACK = createTag("extends_smokestack");

    private static TagKey<Block> createTag(String name)
    {
        return BlockTags.create(new ResourceLocation(ColdSweat.MOD_ID, name));
    }
}
