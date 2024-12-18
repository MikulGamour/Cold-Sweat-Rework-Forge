package com.momosoftworks.coldsweat.util.world;

import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.api.event.core.init.GatherDefaultTempModifiersEvent;
import com.momosoftworks.coldsweat.api.registry.BlockTempRegistry;
import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTemp;
import com.momosoftworks.coldsweat.api.temperature.modifier.BiomeTempModifier;
import com.momosoftworks.coldsweat.api.temperature.modifier.BlockTempModifier;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.temperature.modifier.UndergroundTempModifier;
import com.momosoftworks.coldsweat.api.util.Placement;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.block.SmokestackBlock;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.core.network.message.BlockDataUpdateMessage;
import com.momosoftworks.coldsweat.core.network.message.ParticleBatchMessage;
import com.momosoftworks.coldsweat.core.network.message.PlayEntityAttachedSoundMessage;
import com.momosoftworks.coldsweat.core.network.message.SyncForgeDataMessage;
import com.momosoftworks.coldsweat.data.codec.configuration.BiomeTempData;
import com.momosoftworks.coldsweat.util.ClientOnlyHelper;
import com.momosoftworks.coldsweat.util.entity.DummyPlayer;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.FastMap;
import com.momosoftworks.coldsweat.util.serialization.DynamicHolder;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.IParticleData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.GameRules;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public abstract class WorldHelper
{
    static Map<ResourceLocation, DummyPlayer> DUMMIES = new HashMap<>();
    static Map<RegistryKey<World>, List<TempSnapshot>> TEMPERATURE_CHECKS = new FastMap<>();

    public static int getHeight(BlockPos pos, World level)
    {
        int seaLevel = level.getSeaLevel();
        // If chunk isn't loaded, return sea level
        if (!level.isLoaded(pos)) return seaLevel;

        IChunk chunk = getChunk(level, pos);
        if (chunk == null) return seaLevel;

        return chunk.getHeight(Heightmap.Type.OCEAN_FLOOR, pos.getX() & 15, pos.getZ() & 15);
    }

    /**
     * Returns all block positions in a grid of the specified size<br>
     * Search area scales with the number of samples
     * @param pos The center of the search area
     * @param samples The total number of checks performed.
     * @param interval How far apart each check is. Higher values = less dense and larger search area
     */
    public static List<BlockPos> getPositionGrid(BlockPos pos, int samples, int interval)
    {
        List<BlockPos> posList = new ArrayList<>();
        int sampleRoot = (int) Math.sqrt(samples);
        int radius = (sampleRoot * interval) / 2;

        for (int x = -radius; x < radius; x += interval)
        {
            for (int z = -radius; z < radius; z += interval)
            {   posList.add(pos.offset(x + interval / 2, 0, z + interval / 2));
            }
        }

        return posList;
    }

    /**
     * Returns a cube of BlockPos of the specified size and density
     * @param pos The center of the cube
     * @param size The side length of the cube, in intervals
     * @param interval The length of one interval (distance between each BlockPos)
     * @return A list of BlockPos
     */
    public static List<BlockPos> getPositionCube(BlockPos pos, int size, int interval)
    {
        List<BlockPos> posList = new ArrayList<>();
        int radius = (size * interval) / 2;

        for (int x = -radius; x < radius; x += interval)
        {
            for (int y = -radius; y < radius; y += interval)
            {
                for (int z = -radius; z < radius; z += interval)
                {   posList.add(pos.offset(x + interval / 2, y + interval / 2, z + interval / 2));
                }
            }
        }

        return posList;
    }

    /**
     * More accurate method for detecting skylight access. Relies on block hitbox shape instead of light level.
     * @param pos The position to check
     * @param maxDistance The maximum distance to check
     * @return True if the specified position can see the sky (if no full y-axis block faces are within the detection range)
     */
    public static boolean canSeeSky(IWorld level, BlockPos pos, int maxDistance)
    {
        BlockPos.Mutable pos2 = pos.mutable();
        int iterations = Math.min(maxDistance, level.getMaxBuildHeight() - pos.getY());
        IChunk chunk = getChunk(level, pos);
        if (chunk == null) return true;

        for (int i = 0; i < iterations; i++)
        {
            try
            {
                BlockState state = chunk.getBlockState(pos2);
                if (state.isAir() || state.getMaterial().isLiquid())
                {   continue;
                }
                VoxelShape shape = state.getShape(level, pos, ISelectionContext.empty());
                if (shape.equals(VoxelShapes.block())) return false;

                if (isFullSide(CSMath.flattenShape(Direction.Axis.Y, shape), Direction.UP))
                {   return false;
                }
            }
            finally
            {   pos2.move(0, 1, 0);
            }
        }
        return true;
    }

    public static boolean isSpreadBlocked(IWorld world, BlockState state, BlockPos pos, Direction toDir, Direction fromDir)
    {
        Block block = state.getBlock();

        if (block instanceof SmokestackBlock && toDir.getAxis() == Direction.Axis.Y)
        {   return true;
        }
        if (state.isAir() || ConfigSettings.THERMAL_SOURCE_SPREAD_WHITELIST.get().contains(block))
        {   return false;
        }
        if (ConfigSettings.THERMAL_SOURCE_SPREAD_BLACKLIST.get().contains(block)) return true;

        VoxelShape shape = state.getShape(world, pos, ISelectionContext.empty());
        if (shape.equals(VoxelShapes.block())) return true;

               // Should it have spread here in the first place?
        return isFullSide(shape.getFaceShape(fromDir.getOpposite()), fromDir)
               // Can it spread out?
            || isFullSide(CSMath.flattenShape(toDir.getAxis(), shape), toDir);
    }

    public static boolean isFullSide(VoxelShape shape, Direction dir)
    {
        if (shape.isEmpty()) return false;

        // Return true if the 2D x/y area of the shape is >= 1
        double[] area = new double[1];
        switch (dir.getAxis())
        {
            case X : shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> area[0] += Math.abs(y2 - y1) * Math.abs(z2 - z1));
            case Y : shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> area[0] += Math.abs(x2 - x1) * Math.abs(z2 - z1));
            case Z : shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> area[0] += Math.abs(x2 - x1) * Math.abs(y2 - y1));
        }

        return area[0] >= 1;
    }

    @Nullable
    public static IChunk getChunk(IWorld level, BlockPos pos)
    {   return getChunk(level, pos.getX() >> 4, pos.getZ() >> 4);
    }

    @Nullable
    public static IChunk getChunk(IWorld level, ChunkPos pos)
    {   return getChunk(level, pos.x, pos.z);
    }

    @Nullable
    public static IChunk getChunk(IWorld level, int chunkX, int chunkZ)
    {   return level.getChunkSource().getChunkNow(chunkX, chunkZ);
    }

    public static ChunkSection getChunkSection(IChunk chunk, int y)
    {   ChunkSection[] sections = chunk.getSections();

        return sections[CSMath.clamp(y >> 4, 0, sections.length - 1)];
    }

    @Nullable
    public static Structure<?> getStructureAt(World level, BlockPos pos)
    {
        if (!(level instanceof ServerWorld)) return null;

        ServerWorld serverLevel = ((ServerWorld) level);
        StructureManager structureManager = serverLevel.structureFeatureManager();

        // Iterate over all structures at the position (ignores Y level)
        for (Map.Entry<Structure<?>, LongSet> entry : level.getChunk(pos).getAllReferences().entrySet())
        {
            Structure<?> structure = entry.getKey();
            LongSet strucCoordinates = entry.getValue();

            // Iterate over all chunk coordinates within the structures
            for (long coordinate : strucCoordinates)
            {
                SectionPos sectionpos = SectionPos.of(new ChunkPos(coordinate), SectionPos.blockToSectionCoord(0));
                // Get the structure start
                StructureStart<?> structurestart = structureManager.getStartForFeature(sectionpos, structure, level.getChunk(sectionpos.x(), sectionpos.z(), ChunkStatus.STRUCTURE_STARTS));

                if (structurestart != null && structurestart.isValid() && structurestart.getBoundingBox().isInside(pos))
                {
                    // If the structure has a piece at the position, get the temperature
                    if (structurestart.getPieces().stream().anyMatch(piece -> piece.getBoundingBox().isInside(pos)))
                    {
                        // If the structure has a piece at the position, get the temperature
                        if (structurestart.getPieces().stream().anyMatch(piece -> piece.getBoundingBox().isInside(pos)))
                        {   return structure;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Plays a sound for all tracking clients that follows the source entity around.<br>
     * Why this isn't in Vanilla Minecraft is beyond me
     * @param sound The SoundEvent to play
     * @param entity The entity to attach the sound to (all tracking entities will hear the sound)
     * @param volume The volume of the sound
     * @param pitch The pitch of the sound
     */
    public static void playEntitySound(SoundEvent sound, Entity entity, SoundCategory source, float volume, float pitch)
    {
        if (!entity.isSilent())
        {
            if (entity.level.isClientSide)
            {   ClientOnlyHelper.playEntitySound(sound, source, volume, pitch, entity);
            }
            else
            {   ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                        new PlayEntityAttachedSoundMessage(sound, source, volume, pitch, entity.getId()));
            }
        }
    }

    public static boolean isInWater(Entity entity)
    {   BlockPos pos = entity.blockPosition();
        IChunk chunk = WorldHelper.getChunk(entity.level, pos);
        if (chunk == null) return false;

        return entity.isInWater() || chunk.getBlockState(pos).getBlock() == Blocks.BUBBLE_COLUMN;
    }

    public static boolean isRainingAt(World level, BlockPos pos)
    {   DynamicHolder<Biome> biome = DynamicHolder.create(() -> null, h -> h.set(level.getBiomeManager().getBiome(pos)));

        return (level.isRaining() && biome.get().getPrecipitation() == Biome.RainType.RAIN)
            && canSeeSky(level, pos.above(), level.getMaxBuildHeight())
            && biome.get().getTemperature(pos) >= 0.15f
            && !CompatManager.SereneSeasons.isColdEnoughToSnow(level, pos);
    }

    /**
     * Iterates through every block along the given vector
     * @param from The starting position
     * @param to The ending position
     * @param rayTracer function to run on each found block
     * @param maxHits the maximum number of blocks to act upon before the ray expires
     */
    public static void forBlocksInRay(Vector3d from, Vector3d to, World level, IChunk chunk, Map<BlockPos, BlockState> stateCache,
                                      BiConsumer<BlockState, BlockPos> rayTracer, int maxHits)
    {
        // Don't bother if the ray has no length
        if (!from.equals(to))
        {
            Vector3d ray = to.subtract(from);
            Vector3d normalRay = ray.normalize();
            BlockPos.Mutable pos = new BlockPos(from).mutable();
            IChunk workingChunk = chunk;

            // Iterate over every block-long segment of the ray
            for (int i = 0; i < ray.length(); i++)
            {
                // Get the position of the current segment
                Vector3d vec = from.add(normalRay.scale(i));

                // Skip if the position is the same as the last one
                if (new BlockPos(vec).equals(pos)) continue;
                pos.set(vec.x, vec.y, vec.z);

                // Get the blockstate at the current position
                BlockState state = stateCache.get(pos);

                if (state == null)
                {   // Set new workingChunk if the ray travels outside the current one
                    if (workingChunk == null || !workingChunk.getPos().equals(new ChunkPos(pos)))
                    {   workingChunk = getChunk(level, pos);
                    }
                    if (workingChunk == null) continue;

                    state = workingChunk.getBlockState(pos);
                    stateCache.put(pos.immutable(), state);
                }


                // If the block isn't air, then we hit something
                if (!state.isAir() && --maxHits <= 0)
                {   break;
                }

                rayTracer.accept(state, pos);
            }
        }
    }

    /**
     * Overload for {@link #forBlocksInRay(Vector3d, Vector3d, World, IChunk, Map, BiConsumer, int)} with less bloated params
     */
    public static void forBlocksInRay(Vector3d from, Vector3d to, World level, BiConsumer<BlockState, BlockPos> rayTracer, int maxHits)
    {   forBlocksInRay(from, to, level, getChunk(level, new BlockPos(from)), new HashMap<>(), rayTracer, maxHits);
    }

    public static Entity raycastEntity(Vector3d from, Vector3d to, World level, Predicate<Entity> filter)
    {
        // Don't bother if the ray has no length
        if (!from.equals(to))
        {
            Vector3d ray = to.subtract(from);
            Vector3d normalRay = ray.normalize();
            BlockPos.Mutable pos = new BlockPos(from).mutable();

            // Iterate over every block-long segment of the ray
            for (int i = 0; i < ray.length(); i++)
            {
                // Get the position of the current segment
                Vector3d vec = from.add(normalRay.scale(i));

                // Skip if the position is the same as the last one
                if (new BlockPos(vec).equals(pos)) continue;
                pos.set(vec.x, vec.y, vec.z);

                // Return the first entity in the current block, or continue if there is none
                List<Entity> entities = level.getEntitiesOfClass(Entity.class, new AxisAlignedBB(pos), filter);
                if (!entities.isEmpty()) return entities.get(0);
            }
        }
        return null;
    }

    public static void spawnParticle(World world, IParticleData particle, double x, double y, double z,
                                     double xSpeed, double ySpeed, double zSpeed)
    {
        if (!world.isClientSide)
        {
            ParticleBatchMessage particles = new ParticleBatchMessage();
            particles.addParticle(particle, new ParticleBatchMessage.ParticlePlacement(x, y, z, xSpeed, ySpeed, zSpeed));
            ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> (Chunk) getChunk(world, (int) x >> 4, (int) z >> 4)), particles);
        }
        else
        {   world.addParticle(particle, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }

    public static void spawnParticleBatch(World level, IParticleData particle, double x, double y, double z,
                                          double xSpread, double ySpread, double zSpread, double count, double speed)
    {
        Random rand = new Random();

        if (!level.isClientSide)
        {
            ParticleBatchMessage particles = new ParticleBatchMessage();
            for (int i = 0; i < count; i++)
            {
                Vector3d vec = new Vector3d(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).normalize().scale(speed);
                particles.addParticle(particle, new ParticleBatchMessage.ParticlePlacement(
                        x + xSpread - rand.nextDouble() * (xSpread * 2),
                        y + ySpread - rand.nextDouble() * (ySpread * 2),
                        z + zSpread - rand.nextDouble() * (zSpread * 2), vec.x, vec.y, vec.z));
            }
            ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension), particles);
        }
        else
        {
            for (int i = 0; i < count; i++)
            {
                Vector3d vec = new Vector3d(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).normalize().scale(speed);
                level.addParticle(particle,
                        x + xSpread - rand.nextDouble() * (xSpread * 2),
                        y + ySpread - rand.nextDouble() * (ySpread * 2),
                        z + zSpread - rand.nextDouble() * (zSpread * 2), vec.x, vec.y, vec.z);
            }
        }
    }

    public static ItemEntity dropItem(World level, BlockPos pos, ItemStack stack)
    {   return dropItem(level, pos, stack, 6000);
    }

    public static ItemEntity dropItem(World level, BlockPos pos, ItemStack stack, int lifeTime)
    {
        Random rand = new Random();
        ItemEntity item = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), stack);

        item.setDeltaMovement(item.getDeltaMovement().add(((rand.nextFloat() - rand.nextFloat()) * 0.1F), (rand.nextFloat() * 0.05F), ((rand.nextFloat() - rand.nextFloat()) * 0.1F)));
        Field age = ObfuscationReflectionHelper.findField(ItemEntity.class, "f_31985_");
        age.setAccessible(true);
        try
        {   age.set(item, 6000 - lifeTime);
        }
        catch (Exception e)
        {   e.printStackTrace();
        }
        return item;
    }

    /**
     * Drops an item with random velocity from the entity's position
     * @return The dropped item entity
     */
    public static ItemEntity entityDropItem(Entity entity, ItemStack stack)
    {   return entityDropItem(entity, stack, 6000);
    }

    /**
     * Drops an item with random velocity from the entity's position
     * @param lifeTime The despawn time of the item, in ticks
     * @return The dropped item entity
     */
    public static ItemEntity entityDropItem(Entity entity, ItemStack stack, int lifeTime)
    {
        Random rand = new Random();
        ItemEntity item = entity.spawnAtLocation(stack, entity.getBbHeight());
        if (item != null)
        {   item.setDeltaMovement(item.getDeltaMovement().add(((rand.nextFloat() - rand.nextFloat()) * 0.1F), (rand.nextFloat() * 0.05F), ((rand.nextFloat() - rand.nextFloat()) * 0.1F)));
            Field age = ObfuscationReflectionHelper.findField(ItemEntity.class, "field_70292_b");
            age.setAccessible(true);
            try
            {   age.set(item, 6000 - lifeTime);
            }
            catch (Exception e)
            {   e.printStackTrace();
            }
        }
        return item;
    }

    /**
     * @return The closest Vec3, contained in the entity's bounding box, to the given pos.
     */
    public static Vector3d getClosestPointOnEntity(LivingEntity entity, Vector3d pos)
    {
        double playerRadius = entity.getBbWidth() / 2;
        return new Vector3d(CSMath.clamp(pos.x, entity.getX() - playerRadius, entity.getX() + playerRadius),
                            CSMath.clamp(pos.y, entity.getY(), entity.getY() + entity.getBbHeight()),
                            CSMath.clamp(pos.z, entity.getZ() - playerRadius, entity.getZ() + playerRadius));
    }

    /**
     * Merges the entity's server-side persistent data into the client-side persistent data
     * @param destination The player to send the data to. If null, sends to all tracking entities
     */
    public static void syncEntityForgeData(Entity entity, ServerPlayerEntity destination)
    {
        ColdSweatPacketHandler.INSTANCE.send(destination != null ? PacketDistributor.PLAYER.with(() -> destination)
                                                                 : PacketDistributor.TRACKING_ENTITY.with(() -> entity),
                                             new SyncForgeDataMessage(entity));
    }

    /**
     * Manually sends a block entity's update tag to tracking clients
     */
    public static void syncBlockEntityData(TileEntity te)
    {
        if (te.getLevel() == null || te.getLevel().isClientSide) return;

        IChunk ichunk = getChunk(te.getLevel(), te.getBlockPos());
        if (ichunk instanceof Chunk)
        {   ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> ((Chunk) ichunk)), new BlockDataUpdateMessage(te));
        }
    }

    /**
     * Allows the server world to be accessed if you only have a generic "Level" object<br>
     * ONLY USE ON THE SERVER THREAD
     * @return The server world
     */
    public static ServerWorld getServerLevel(World level)
    {   return ServerLifecycleHooks.getCurrentServer().getLevel(level.dimension());
    }

    /**
     * Gets the server instance<br>
     * ONLY USE ON THE SERVER THREAD
     * @return The server instance
     */
    public static MinecraftServer getServer()
    {   return ServerLifecycleHooks.getCurrentServer();
    }

    /**
     * Gets the min and max temperature of the biome.
     * @return A pair of the min and max temperature of the biome
     */
    public static Pair<Double, Double> getBiomeTemperatureRange(IWorld level, Biome biome)
    {   return getBiomeTemperatureRange(level.registryAccess(), biome);
    }

    /**
     * Gets the min and max temperature of the biome.
     * @return A pair of the min and max temperature of the biome
     */
    public static Pair<Double, Double> getBiomeTemperatureRange(DynamicRegistries registryAccess, Biome biome)
    {
        double variance = 1 / Math.max(1, 2 + biome.getDownfall() * 2);
        double baseTemp = biome.getBaseTemperature();

        BiomeTempData biomeTemp = ConfigSettings.BIOME_TEMPS.get(registryAccess)
                                  .getOrDefault(biome, new BiomeTempData(biome, baseTemp - variance, baseTemp + variance, Temperature.Units.MC, true));
        BiomeTempData configOffset = ConfigSettings.BIOME_OFFSETS.get(registryAccess)
                                     .getOrDefault(biome, new BiomeTempData(biome, 0d, 0d, Temperature.Units.MC, false));
        return CSMath.addPairs(Pair.of(biomeTemp.minTemp(), biomeTemp.maxTemp()),
                               Pair.of(configOffset.minTemp(), configOffset.maxTemp()));
    }

    /**
     * Gets the temperature of the biome at the specified position, including time of day.
     * @return The temperature of the biome at the specified position
     */
    public static double getBiomeTemperature(IWorld level, Biome biome)
    {
        Pair<Double, Double> temps = getBiomeTemperatureRange(level, biome);
        return CSMath.blend(temps.getFirst(), temps.getSecond(), Math.sin(level.dayTime() / (12000 / Math.PI)), -1, 1);
    }


    /**
     * Returns a cached temperature value
     */
    public static double getRoughTemperatureAt(World level, BlockPos pos)
    {
        List<TempSnapshot> snapshots = TEMPERATURE_CHECKS.computeIfAbsent(level.dimension(), dim -> new ArrayList<>());
        int tickSpeedMultiplier = Math.max(1, level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING) / 20);

        for (int i = 0; i < snapshots.size(); i++)
        {
            TempSnapshot snapshot = snapshots.get(i);
            if (level == snapshot.level() && CSMath.withinCubeDistance(pos, snapshot.pos(), 10))
            {
                if (level.getGameTime() - snapshot.timestamp < 200 / tickSpeedMultiplier)
                {   return snapshot.temperature();
                }
                else
                {   snapshots.remove(i);
                    i--;
                }
            }
        }
        DummyPlayer dummy = getDummyPlayer(level);
        // Move the dummy to the position being tested
        Vector3d newPos = CSMath.getCenterPos(pos);
        dummy.setPos(newPos.x, newPos.y, newPos.z);

        List<TempModifier> modifiers = new ArrayList<>(Temperature.getModifiers(dummy, Temperature.Trait.WORLD));
        for (int i = 0; i < modifiers.size(); i++)
        {
            TempModifier modifier = modifiers.get(i);
            if (modifier instanceof BlockTempModifier) modifiers.set(i, new BlockTempModifier(3));
            else if (modifier instanceof BiomeTempModifier) modifiers.set(i, new BiomeTempModifier(9));
            else if (modifier instanceof UndergroundTempModifier) modifiers.set(i, new UndergroundTempModifier(9));
        }

        double tempAt = Temperature.apply(0, dummy, Temperature.Trait.WORLD, modifiers);

        snapshots.add(new TempSnapshot(level, pos, level.getGameTime(), tempAt));

        return tempAt;
    }

    /**
     * Gets the "raw" temperature for a block, ignoring distance and occlusion
     */
    public static double getBlockTemperature(World level, BlockState block)
    {
        Collection<BlockTemp> blockTemps = BlockTempRegistry.getBlockTempsFor(block);
        double temp = 0;
        for (BlockTemp blockTemp : blockTemps)
        {   temp += blockTemp.getTemperature(level, null, block, BlockPos.ZERO, 0);
        }
        return temp;
    }

    public static double getTemperatureAt(World level, BlockPos pos)
    {
        DummyPlayer dummy = getDummyPlayer(level);
        // Move the dummy to the position being tested
        Vector3d newPos = CSMath.getCenterPos(pos);
        dummy.setPos(newPos.x, newPos.y, newPos.z);
        return Temperature.apply(0, dummy, Temperature.Trait.WORLD, Temperature.getModifiers(dummy, Temperature.Trait.WORLD));
    }

    public static DummyPlayer getDummyPlayer(World level)
    {
        ResourceLocation dimension = level.dimension().location();
        // There is one "dummy" entity per world, which TempModifiers are applied to
        DummyPlayer dummy = DUMMIES.get(dimension);
        // If the dummy for this dimension is invalid, make a new one
        if (dummy == null || dummy.level != level)
        {
            WorldHelper.DUMMIES.put(dimension, dummy = new DummyPlayer(level));
            // Use default player modifiers to determine the temperature
            GatherDefaultTempModifiersEvent event = new GatherDefaultTempModifiersEvent(dummy, Temperature.Trait.WORLD);
            MinecraftForge.EVENT_BUS.post(event);
            for (TempModifier modifier : event.getModifiers())
            {   modifier.tickRate(1);
            }
            Temperature.addModifiers(dummy, event.getModifiers(), Temperature.Trait.WORLD, Placement.Duplicates.BY_CLASS);
        }
        return dummy;
    }

    public static boolean allAdjacentBlocksMatch(BlockPos pos, Predicate<BlockPos> predicate)
    {
        BlockPos.Mutable pos2 = pos.mutable();
        for (int i = 0; i < Direction.values().length; i++)
        {
            BlockPos offset = pos2.setWithOffset(pos, Direction.values()[i]);
            if (!predicate.test(offset)) return false;
        }
        return true;
    }

    public static boolean shouldFreeze(IWorld levelReader, BlockPos pos, boolean mustBeAtEdge)
    {
        if (pos.getY() >= 0 && pos.getY() < levelReader.getMaxBuildHeight()
        && levelReader instanceof ServerWorld)
        {
            if (surroundedByIce(levelReader, pos))
            {   return true;
            }
            DynamicHolder<Boolean> freezingTemp = DynamicHolder.create(() -> getRoughTemperatureAt((ServerWorld) levelReader, pos) < 0.15F);

            if (!mustBeAtEdge)
            {   return freezingTemp.get();
            }

            boolean surroundedByWater = levelReader.isWaterAt(pos.north())
                                     && levelReader.isWaterAt(pos.south())
                                     && levelReader.isWaterAt(pos.east())
                                     && levelReader.isWaterAt(pos.west());

            return !surroundedByWater && freezingTemp.get();
        }
        return false;
    }

    public static boolean shouldMelt(IWorld levelReader, BlockPos pos, boolean mustBeAtEdge)
    {
        if (pos.getY() >= 0 && pos.getY() < levelReader.getMaxBuildHeight()
        && levelReader instanceof ServerWorld)
        {
            if (mustBeAtEdge && surroundedByIce(levelReader, pos))
            {   return false;
            }
            return getRoughTemperatureAt((ServerWorld) levelReader, pos) >= 0.15F;
        }
        return false;
    }

    public static boolean surroundedByIce(IWorld level, BlockPos pos)
    {
        return level.getBlockState(pos.north()).is(Blocks.ICE)
            && level.getBlockState(pos.south()).is(Blocks.ICE)
            && level.getBlockState(pos.east()).is(Blocks.ICE)
            && level.getBlockState(pos.west()).is(Blocks.ICE);
    }

    public static boolean nextToSoulFire(IWorld level, BlockPos pos)
    {
        BlockPos.Mutable pos2 = pos.mutable();
        for (int x = -1; x <= 1; x++)
        for (int y =  0; y <= 1; y++)
        for (int z = -1; z <= 1; z++)
        {
            BlockState state = level.getBlockState(pos2.setWithOffset(pos, x, y, z));
            if (state.is(Blocks.SOUL_FIRE) || state.is(Blocks.SOUL_CAMPFIRE) && state.getValue(CampfireBlock.LIT))
            {   return true;
            }
        }
        return false;
    }

    public static class TempSnapshot
    {
        private final World level;
        private final BlockPos pos;
        private final long timestamp;
        private final double temperature;

        public TempSnapshot(World level, BlockPos pos, long timestamp, double temperature)
        {
            this.level = level;
            this.pos = pos;
            this.timestamp = timestamp;
            this.temperature = temperature;
        }

        public World level()
        {   return level;
        }
        public BlockPos pos()
        {   return pos;
        }
        public long timestamp()
        {   return timestamp;
        }
        public double temperature()
        {   return temperature;
        }
    }
}
