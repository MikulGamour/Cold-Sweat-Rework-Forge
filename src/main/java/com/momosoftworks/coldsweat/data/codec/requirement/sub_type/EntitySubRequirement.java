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
import java.util.function.Supplier;

public interface EntitySubRequirement
{
    BiMap<ResourceLocation, Supplier<MapCodec<? extends EntitySubRequirement>>> REQUIREMENT_MAP = HashBiMap.create(Map.of(
            new ResourceLocation("variant"), () -> EntityVariantRequirement.CODEC,
            new ResourceLocation("fishing_hook"), () -> FishingHookRequirement.CODEC,
            new ResourceLocation("lightning_bolt"), () -> LightningBoltRequirement.CODEC,
            new ResourceLocation("piglin_neutral_armor"), () -> PiglinNeutralArmorRequirement.CODEC,
            new ResourceLocation("player"), () -> PlayerDataRequirement.getCodec(EntityRequirement.getCodec()),
            new ResourceLocation("raider"), () -> RaiderRequirement.CODEC,
            new ResourceLocation("slime"), () -> SlimeRequirement.CODEC,
            new ResourceLocation("snow_boots"), () -> SnowBootsRequirement.CODEC
    ));

    Codec<EntitySubRequirement> CODEC = ResourceLocation.CODEC.dispatch(
            "type",
            requirement -> {
                Supplier<MapCodec<? extends EntitySubRequirement>> matchingSupplier = REQUIREMENT_MAP.values().stream()
                        .filter(supplier -> supplier.get().equals(requirement.getCodec()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Unknown requirement type: " + requirement.getClass()));
                return REQUIREMENT_MAP.inverse().get(matchingSupplier);
            },
            location -> {
                Supplier<MapCodec<? extends EntitySubRequirement>> supplier = REQUIREMENT_MAP.get(location);
                if (supplier == null) {
                    throw new IllegalStateException("Unknown requirement type: " + location);
                }
                return (Codec) supplier.get();
            }
    );

    default ResourceLocation getType() {
        return REQUIREMENT_MAP.inverse().get(this.getCodec());
    }

    boolean test(Entity entity, Level level, @Nullable Vec3 position);
    MapCodec<? extends EntitySubRequirement> getCodec();
}
