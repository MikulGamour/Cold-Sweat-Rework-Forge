package com.momosoftworks.coldsweat.api.temperature.modifier;

import com.momosoftworks.coldsweat.api.registry.BlockTempRegistry;
import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTemp;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.init.ModAdvancementTriggers;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import oshi.util.tuples.Triplet;

import java.util.*;
import java.util.function.Function;

public class BlockTempModifier extends TempModifier
{
    public BlockTempModifier() {}

    public BlockTempModifier(int range)
    {   if (range > 0) this.getNBT().putInt("RangeOverride", range);
    }

    Map<ChunkPos, ChunkAccess> chunks = new HashMap<>(16);
    Map<BlockTemp, Double> blockTempEffects = new HashMap<>(128);
    Map<BlockPos, BlockState> stateCache = new HashMap<>(4096);
    List<Triplet<BlockPos, BlockTemp, Double>> triggers = new ArrayList<>(128);

    @Override
    public Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait)
    {
        blockTempEffects.clear();
        stateCache.clear();
        triggers.clear();

        Level level = entity.level();
        int range = this.getNBT().contains("RangeOverride", 3) ? this.getNBT().getInt("RangeOverride") : ConfigSettings.BLOCK_RANGE.get();
        BlockPos blockPos = entity.blockPosition();

        int entX = blockPos.getX();
        int entY = blockPos.getY();
        int entZ = blockPos.getZ();
        BlockPos.MutableBlockPos blockpos = new BlockPos.MutableBlockPos();

        // Only tick advancements every second, because Minecraft advancements are not performant at all
        boolean shouldTickAdvancements = this.getTicksExisted() % 20 == 0;

        for (int x = -range; x < range; x++)
        {
            for (int z = -range; z < range; z++)
            {
                ChunkPos chunkPos = new ChunkPos((entX + x) >> 4, (entZ + z) >> 4);
                ChunkAccess chunk = chunks.get(chunkPos);
                if (chunk == null) chunks.put(chunkPos, chunk = WorldHelper.getChunk(level, chunkPos));
                if (chunk == null) continue;

                for (int y = -range; y < range; y++)
                {
                        blockpos.set(entX + x, entY + y, entZ + z);

                        BlockState state = stateCache.get(blockpos);
                        if (state == null)
                        {   LevelChunkSection section = WorldHelper.getChunkSection(chunk, blockpos.getY());
                            state = section.getBlockState(blockpos.getX() & 15, blockpos.getY() & 15, blockpos.getZ() & 15);
                            stateCache.put(blockpos.immutable(), state);
                        }

                        if (state.isAir()) continue;

                        // Get the BlockTemp associated with the block
                        Collection<BlockTemp> blockTemps = BlockTempRegistry.getBlockTempsFor(state);

                        if (blockTemps.isEmpty() || (blockTemps.size() == 1 && blockTemps.contains(BlockTempRegistry.DEFAULT_BLOCK_TEMP))) continue;

                        // Get the amount that this block has affected the entity so far

                        // Are any of the block temps able to affect the entity?
                        // This check prevents costly calculations if the block can't affect the entity anyway
                        if (areAnyBlockTempsInRange(blockTempEffects, blockTemps))
                        {
                            // Get Vector positions of the centers of the source block and player
                            Vec3 pos = Vec3.atCenterOf(blockpos);

                            // Gets the closest point in the player's BB to the block
                            Vec3 playerClosest = WorldHelper.getClosestPointOnEntity(entity, pos);

                            // Cast a ray between the player and the block
                            // Lessen the effect with each block between the player and the block
                            int[] blocks = new int[1];
                            Vec3 ray = pos.subtract(playerClosest);
                            Direction direction = Direction.getNearest(ray.x, ray.y, ray.z);

                            WorldHelper.forBlocksInRay(playerClosest, pos, level, chunk, stateCache,
                            (rayState, bpos) ->
                            {   if (!bpos.equals(blockpos) && WorldHelper.isSpreadBlocked(level, rayState, bpos, direction, direction))
                                {   blocks[0]++;
                                }
                            }, 3);

                            // Get the temperature of the block given the player's distance
                            double distance = CSMath.getDistance(playerClosest, pos);

                            for (BlockTemp blockTemp : blockTemps)
                            {
                                if (!blockTemp.isValid(level, blockpos, state)) continue;
                                double temperature = blockTemp.getTemperature(level, entity, state, blockpos, distance);
                                double tempToAdd = blockTemp.fade()
                                                   ? CSMath.blend(temperature, 0, distance, 0, blockTemp.range())
                                                   : temperature;

                                // Store this block type's total effect on the player
                                // Dampen the effect with each block between the player and the block
                                double blockTempTotal = blockTempEffects.getOrDefault(blockTemp, 0d) + tempToAdd / (blocks[0] + 1);
                                blockTempEffects.put(blockTemp, CSMath.clamp(blockTempTotal, blockTemp.minEffect(), blockTemp.maxEffect()));
                                // Used to trigger advancements
                                if (shouldTickAdvancements)
                                {   triggers.add(new Triplet<>(blockpos, blockTemp, distance));
                                }
                            }
                        }
                }
            }
        }
        // Trigger advancements at every BlockPos with a BlockEffect attached to it
        if (entity instanceof ServerPlayer player && shouldTickAdvancements)
        {
            for (Triplet<BlockPos, BlockTemp, Double> trigger : triggers)
            {   ModAdvancementTriggers.BLOCK_AFFECTS_TEMP.value().trigger(player, trigger.getA(), trigger.getC(), blockTempEffects.get(trigger.getB()));
            }
        }

        // Remove old chunks from the cache
        while (chunks.size() >= 16)
        {   chunks.remove(chunks.keySet().iterator().next());
        }

        // Add the effects of all the blocks together and return the result
        return temp ->
        {
            for (Map.Entry<BlockTemp, Double> effect : blockTempEffects.entrySet())
            {
                BlockTemp be = effect.getKey();
                double min = be.minTemperature();
                double max = be.maxTemperature();
                if (!CSMath.betweenInclusive(temp, min, max)) continue;
                temp = CSMath.clamp(temp + effect.getValue(), min, max);
            }
            return temp;
        };
    }

    private static boolean areAnyBlockTempsInRange(Map<BlockTemp, Double> blockTempEffects, Collection<BlockTemp> blockTemps)
    {
        boolean isInTempRange = blockTempEffects.isEmpty();
        if (!isInTempRange)
        {
            for (Map.Entry<BlockTemp, Double> entry : blockTempEffects.entrySet())
            {   BlockTemp key = entry.getKey();
                Double value = entry.getValue();

                if (!blockTemps.contains(key) || CSMath.betweenInclusive(value, key.minEffect(), key.maxEffect()))
                {   isInTempRange = true;
                    break;
                }
            }
        }
        return isInTempRange;
    }
}