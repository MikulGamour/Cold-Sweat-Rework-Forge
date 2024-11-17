package com.momosoftworks.coldsweat.util.serialization;

import com.mojang.datafixers.util.Either;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RegistryHelper
{
    @Nullable
    public static <T> Registry<T> getRegistry(ResourceKey<Registry<T>> registry)
    {   return CSMath.getIfNotNull(getRegistryAccess(), access -> access.registryOrThrow(registry), null);
    }

    @Nullable
    public static RegistryAccess getRegistryAccess()
    {
        RegistryAccess access = null;

        MinecraftServer server = WorldHelper.getServer();

        if (server != null)
        {
            Level level = server.getLevel(Level.OVERWORLD);
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
                ClientPacketListener connection = Minecraft.getInstance().getConnection();
                if (connection != null)
                {   access = connection.registryAccess();
                }
            }
        }
        return access;
    }

    public static <T extends IForgeRegistryEntry<T>> List<T> mapForgeRegistryTagList(IForgeRegistry<T> registry, List<Either<TagKey<T>, T>> eitherList)
    {
        List<T> list = new ArrayList<>();
        for (Either<TagKey<T>, T> either : eitherList)
        {
            either.ifLeft(tagKey -> list.addAll(registry.tags().getTag(tagKey).stream().toList()));
            either.ifRight(object -> list.add(object));
        }
        return list;
    }

    public static <T> List<T> mapVanillaRegistryTagList(ResourceKey<Registry<T>> registry, List<Either<TagKey<T>, T>> eitherList, @Nullable RegistryAccess registryAccess)
    {
        Registry<T> reg = registryAccess != null ? registryAccess.registryOrThrow(registry) : getRegistry(registry);
        List<T> list = new ArrayList<>();
        if (reg == null) return list;

        for (Either<TagKey<T>, T> either : eitherList)
        {
            either.ifLeft(tagKey ->
            {
                Optional<HolderSet.Named<T>> tag = reg.getTag(tagKey);
                tag.ifPresent(tag1 -> list.addAll(tag1.stream().map(Holder::value).toList()));
            });
            either.ifRight(list::add);
        }
        return list;
    }

    public static <T> Optional<T> getVanillaRegistryValue(ResourceKey<Registry<T>> registry, ResourceLocation id)
    {
        try
        {   return Optional.ofNullable(getRegistry(registry)).map(reg -> reg.get(id));
        }
        catch (Exception e)
        {   return Optional.empty();
        }
    }

    @Nullable
    public static Biome getBiome(ResourceLocation biomeId, RegistryAccess registryAccess)
    {   return registryAccess.registryOrThrow(Registry.BIOME_REGISTRY).get(biomeId);
    }

    @Nullable
    public static ResourceLocation getBiomeId(Biome biome, RegistryAccess registryAccess)
    {   return registryAccess.registryOrThrow(Registry.BIOME_REGISTRY).getKey(biome);
    }

    @Nullable
    public static DimensionType getDimension(ResourceLocation dimensionId, RegistryAccess registryAccess)
    {   return registryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).get(dimensionId);
    }

    @Nullable
    public static ResourceLocation getDimensionId(DimensionType dimension, RegistryAccess registryAccess)
    {   return registryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).getKey(dimension);
    }

    @Nullable
    public static ConfiguredStructureFeature<?,?> getStructure(ResourceLocation structureId, RegistryAccess registryAccess)
    {   return registryAccess.registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).get(structureId);
    }

    @Nullable
    public static ResourceLocation getStructureId(ConfiguredStructureFeature<?,?> structure, RegistryAccess registryAccess)
    {   return registryAccess.registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).getKey(structure);
    }
}
