package com.momosoftworks.coldsweat.config.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class InsulatingMount implements NbtSerializable
{
    public EntityType entityType;
    public double coldInsulation;
    public double heatInsulation;
    public EntityRequirement requirement;

    public InsulatingMount(EntityType entityType, double coldInsulation, double heatInsulation, EntityRequirement requirement)
    {
        this.entityType = entityType;
        this.coldInsulation = coldInsulation;
        this.heatInsulation = heatInsulation;
        this.requirement = requirement;
    }
    public static final Codec<InsulatingMount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.xmap(ForgeRegistries.ENTITIES::getValue, ForgeRegistries.ENTITIES::getKey).fieldOf("entity").forGetter(data -> data.entityType),
            Codec.DOUBLE.fieldOf("cold_insulation").forGetter(data -> data.coldInsulation),
            Codec.DOUBLE.fieldOf("heat_insulation").forGetter(data -> data.heatInsulation),
            EntityRequirement.getCodec().fieldOf("requirement").forGetter(data -> data.requirement))
    .apply(instance, InsulatingMount::new));

    public boolean test(Entity entity)
    {   return requirement.test(entity);
    }

    @Override
    public CompoundNBT serialize()
    {
        return (CompoundNBT) CODEC.encodeStart(NBTDynamicOps.INSTANCE, this).result().orElseGet(CompoundNBT::new);
    }

    public static InsulatingMount deserialize(CompoundNBT tag)
    {
        return CODEC.decode(NBTDynamicOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalStateException("Failed to deserialize InsulatingMount")).getFirst();
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
