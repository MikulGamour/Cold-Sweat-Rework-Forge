package com.momosoftworks.coldsweat.util.serialization;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.IForgeRegistry;

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

    public static <T> List<T> mapForgeRegistryTagList(IForgeRegistry<T> registry, List<Either<TagKey<T>, T>> eitherList)
    {
        List<T> list = new ArrayList<>();
        for (Either<TagKey<T>, T> either : eitherList)
        {
            either.ifLeft(tagKey -> list.addAll(registry.tags().getTag(tagKey).stream().toList()));
            either.ifRight(object -> list.add(object));
        }
        return list;
    }

    public static <T> List<Holder<T>> mapVanillaRegistryTagList(ResourceKey<Registry<T>> registry, List<Either<TagKey<T>, Holder<T>>> eitherList, @Nullable RegistryAccess registryAccess)
    {
        Registry<T> reg = registryAccess != null ? registryAccess.registryOrThrow(registry) : getRegistry(registry);
        List<Holder<T>> list = new ArrayList<>();
        if (reg == null) return list;

        for (Either<TagKey<T>, Holder<T>> either : eitherList)
        {
            either.ifLeft(tagKey ->
            {
                Optional<HolderSet.Named<T>> tag = reg.getTag(tagKey);
                tag.ifPresent(tag1 -> list.addAll(tag1.stream().toList()));
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
    public static ResourceLocation getKey(Holder<?> holder)
    {   return holder.unwrapKey().map(ResourceKey::location).orElse(null);
    }

    @Nullable
    public static Holder<Biome> getBiome(ResourceLocation biomeId, RegistryAccess registryAccess)
    {   return registryAccess.registryOrThrow(Registries.BIOME).getHolder(ResourceKey.create(Registries.BIOME, biomeId)).orElse(null);
    }

    @Nullable
    public static ResourceLocation getBiomeId(Biome biome, RegistryAccess registryAccess)
    {   return registryAccess.registryOrThrow(Registries.BIOME).getKey(biome);
    }

    @Nullable
    public static Holder<DimensionType> getDimension(ResourceLocation dimensionId, RegistryAccess registryAccess)
    {   return registryAccess.registryOrThrow(Registries.DIMENSION_TYPE).getHolder(ResourceKey.create(Registries.DIMENSION_TYPE, dimensionId)).orElse(null);
    }

    @Nullable
    public static ResourceLocation getDimensionId(DimensionType dimension, RegistryAccess registryAccess)
    {   return registryAccess.registryOrThrow(Registries.DIMENSION_TYPE).getKey(dimension);
    }

    @Nullable
    public static Holder<Structure> getStructure(ResourceLocation structureId, RegistryAccess registryAccess)
    {   return registryAccess.registryOrThrow(Registries.STRUCTURE).getHolder(ResourceKey.create(Registries.STRUCTURE, structureId)).orElse(null);
    }

    @Nullable
    public static ResourceLocation getStructureId(Structure structure, RegistryAccess registryAccess)
    {   return registryAccess.registryOrThrow(Registries.STRUCTURE).getKey(structure);
    }
}
