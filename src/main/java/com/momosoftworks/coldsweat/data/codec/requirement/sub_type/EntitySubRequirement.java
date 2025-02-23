package com.momosoftworks.coldsweat.data.codec.requirement.sub_type;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.PlayerDataRequirement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Map;

public interface EntitySubRequirement
{
    BiMap<ResourceLocation, MapCodec<? extends EntitySubRequirement>> REQUIREMENT_MAP = HashBiMap.create(Map.of(
            new ResourceLocation("variant"), EntityVariantRequirement.CODEC,
            new ResourceLocation("fishing_hook"), FishingHookRequirement.CODEC,
            new ResourceLocation("lightning_bolt"), LightningBoltRequirement.CODEC,
            new ResourceLocation("piglin_neutral_armor"), PiglinNeutralArmorRequirement.CODEC,
            new ResourceLocation("player"), PlayerDataRequirement.getCodec(EntityRequirement.getCodec()),
            new ResourceLocation("raider"), RaiderRequirement.CODEC,
            new ResourceLocation("slime"), SlimeRequirement.CODEC,
            new ResourceLocation("snow_boots"), SnowBootsRequirement.CODEC
    ));

    Codec<EntitySubRequirement> CODEC = ResourceLocation.CODEC.dispatch("type",
    requirement -> REQUIREMENT_MAP.inverse().get(requirement.getCodec()),
    REQUIREMENT_MAP::get);

    boolean test(Entity entity, Level level, @Nullable Vec3 position);
    MapCodec<? extends EntitySubRequirement> getCodec();
}
