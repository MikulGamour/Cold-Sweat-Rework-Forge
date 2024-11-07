package com.momosoftworks.coldsweat.config.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

public record InsulatingMount(EntityType entityType, double coldInsulation, double heatInsulation, EntityRequirement requirement) implements NbtSerializable
{
    public static final Codec<InsulatingMount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.xmap(ForgeRegistries.ENTITIES::getValue, ForgeRegistries.ENTITIES::getKey).fieldOf("entity").forGetter(InsulatingMount::entityType),
            Codec.DOUBLE.fieldOf("cold_insulation").forGetter(InsulatingMount::coldInsulation),
            Codec.DOUBLE.fieldOf("heat_insulation").forGetter(InsulatingMount::heatInsulation),
            EntityRequirement.getCodec().fieldOf("requirement").forGetter(InsulatingMount::requirement))
    .apply(instance, InsulatingMount::new));

    public boolean test(Entity entity)
    {   return requirement.test(entity);
    }

    @Override
    public CompoundTag serialize()
    {
        return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
    }

    public static InsulatingMount deserialize(CompoundTag tag)
    {
        return CODEC.decode(NbtOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalStateException("Failed to deserialize InsulatingMount")).getFirst();
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
        InsulatingMount that = (InsulatingMount) obj;
        return this.entityType.equals(that.entityType)
            && this.coldInsulation == that.coldInsulation
            && this.heatInsulation == that.heatInsulation
            && this.requirement.equals(that.requirement);
    }
}
