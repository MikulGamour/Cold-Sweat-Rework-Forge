package com.momosoftworks.coldsweat.data.codec.requirement.sub_type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record EntityVariantRequirement(String variant) implements EntitySubRequirement
{
    public static final MapCodec<EntityVariantRequirement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("variant").forGetter(requirement -> requirement.variant)
    ).apply(instance, EntityVariantRequirement::new));

    @Override
    public MapCodec<? extends EntitySubRequirement> getCodec()
    {   return CODEC;
    }

    @Override
    public boolean test(Entity entity, Level level, @Nullable Vec3 position)
    {
        return entity instanceof VariantHolder<?> variantHolder
            && variantHolder.getVariant() instanceof Enum<?> enm
            && enm instanceof StringRepresentable variantType
            && variantType.getSerializedName().equals(this.variant);
    }
}
