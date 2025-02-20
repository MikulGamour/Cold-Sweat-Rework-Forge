package com.momosoftworks.coldsweat.util.serialization;

import com.mojang.datafixers.util.Either;
import com.momosoftworks.coldsweat.data.ModRegistries;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ITag;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RegistryHelper
{
    @Nullable
    public static <T> Registry<T> getRegistry(RegistryKey<Registry<T>> registry)
    {   return CSMath.getIfNotNull(getDynamicRegistries(), access -> access.registryOrThrow(registry), null);
    }

    @Nullable
    public static DynamicRegistries getDynamicRegistries()
    {
        DynamicRegistries access = null;

        MinecraftServer server = WorldHelper.getServer();

        if (server != null)
        {
            World level = server.getLevel(World.OVERWORLD);
            if (level != null)
            {   access = level.registryAccess();
            }
            else access = server.registryAccess();
        }

        if (access == null && FMLEnvironment.dist == Dist.CLIENT)
        {
            if (Minecraft.getInstance().level != null)
            {   access = Minecraft.getInstance().level.registryAccess();
            }
            else
            {
                ClientPlayNetHandler connection = Minecraft.getInstance().getConnection();
                if (connection != null)
                {   access = connection.registryAccess();
                }
            }
        }
        return access;
    }

    public static <T> List<T> mapTaggableList(List<Either<ITag<T>, T>> eitherList)
    {
        List<T> list = new ArrayList<>();
        for (Either<ITag<T>, T> either : eitherList)
        {
            either.ifLeft(tag ->
            {   if (tag != null) list.addAll(tag.getValues());
            });
            either.ifRight(list::add);
        }
        return list;
    }

    public static <T> Optional<T> getVanillaRegistryValue(RegistryKey<Registry<T>> registry, ResourceLocation id)
    {
        try
        {   return Optional.ofNullable(getRegistry(registry)).map(reg -> reg.get(id));
        }
        catch (Exception e)
        {   return Optional.empty();
        }
    }

    public static ResourceLocation getKey(ConfigData object)
    {
        for (ModRegistries.ConfigRegistry<?> registry : ModRegistries.getRegistries().values())
        {
            if (registry.type().isInstance(object))
            {
                for (Map.Entry<ResourceLocation, ?> entry : registry.data().entrySet())
                {
                    if (entry.getValue() == object)
                    {   return entry.getKey();
                    }
                }
            }
        }
        return new ResourceLocation("unknown");
    }

    @Nullable
    public static Biome getBiome(ResourceLocation biomeId, DynamicRegistries registryAccess)
    {   return registryAccess.registryOrThrow(Registry.BIOME_REGISTRY).get(biomeId);
    }

    @Nullable
    public static ResourceLocation getBiomeId(Biome biome, DynamicRegistries registryAccess)
    {   return registryAccess.registryOrThrow(Registry.BIOME_REGISTRY).getKey(biome);
    }

    @Nullable
    public static DimensionType getDimension(ResourceLocation dimensionId, DynamicRegistries registryAccess)
    {   return registryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).get(dimensionId);
    }

    @Nullable
    public static ResourceLocation getDimensionId(DimensionType dimension, DynamicRegistries registryAccess)
    {   return registryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).getKey(dimension);
    }

    @Nullable
    public static StructureFeature<?, ?> getStructure(ResourceLocation structureId, DynamicRegistries registryAccess)
    {   return registryAccess.registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).get(structureId);
    }

    @Nullable
    public static ResourceLocation getStructureId(StructureFeature<?, ?> structure, DynamicRegistries registryAccess)
    {   return registryAccess.registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).getKey(structure);
    }
}
