package com.momosoftworks.coldsweat.util.math;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3d;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.*;

public class CSMath
{
    private CSMath() {}

    /**
     * Runs the given runnable if the given object is not null.
     */
    public static <T> void doIfNotNull(T object, Consumer<T> run)
    {   if (object != null) run.accept(object);
    }

    /**
     * Runs the function if the given object is not null, and returns the result. <br>
     * Otherwise, returns the default value.
     */
    public static <T, R> R getIfNotNull(T object, Function<T, R> valueGetter, R defaultValue)
    {   if (object != null) return valueGetter.apply(object);
        return defaultValue;
    }

    public static float toRadians(float input)
    {   return input * (float) (Math.PI / 180);
    }

    public static float toRadians(double input)
    {   return (float) input * (float) (Math.PI / 180);
    }

    public static float toDegrees(float input)
    {   return input * (float) (180 / Math.PI);
    }

    /**
     * Converts the given Quaternion to a set of pitch-yaw-roll angles.
     * @return The converted rotation.
     */
    public static Vector3d toEulerAngles(Quaternion quat)
    {   double ysqr = quat.i() * quat.i();

        // roll (x-axis rotation)
        double t0 = +2.0 * (quat.r() * quat.i() + quat.j() * quat.k());
        double t1 = +1.0 - 2.0 * (ysqr + quat.j() * quat.j());
        double roll = Math.atan2(t0, t1);

        // pitch (y-axis rotation)
        double t2 = +2.0 * (quat.r() * quat.j() - quat.k() * quat.i());
        t2 = Math.min(t2, 1.0);
        t2 = Math.max(t2, -1.0);
        double pitch = Math.asin(t2);

        // yaw (z-axis rotation)
        double t3 = +2.0 * (quat.r() * quat.k() + quat.i() * quat.j());
        double t4 = +1.0 - 2.0 * (ysqr + quat.k() * quat.k());
        double yaw = Math.atan2(t3, t4);

        return new Vector3d(roll, pitch, yaw);
    }

    public static Quaternion toQuaternion(double x, double y, double z)
    {   double cy = Math.cos(z * 0.5);
        double sy = Math.sin(z * 0.5);
        double cp = Math.cos(y * 0.5);
        double sp = Math.sin(y * 0.5);
        double cr = Math.cos(x * 0.5);
        double sr = Math.sin(x * 0.5);

        return new Quaternion(
            (float) (sr * cp * cy - cr * sp * sy),
            (float) (cr * sp * cy + sr * cp * sy),
            (float) (cr * cp * sy - sr * sp * cy),
            (float) (cr * cp * cy + sr * sp * sy));
    }

    /**
     * Clamps the given value to be between {@code min} and {@code max}.
     * @return A value within the given bounds.
     */
    public static double clamp(double value, double min, double max)
    {   if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    /**
     * Floating-point overload for {@link #clamp(double, double, double)}.
     */
    public static float clamp(float value, float min, float max)
    {   if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    /**
     * Integer overload for {@link #clamp(double, double, double)}.
     */
    public static int clamp(int value, int min, int max)
    {   if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    /**
     * An "absolute" floor method that has inverted behavior for negative numbers. <br>
     * <br>
     * This is more logical in some cases, when the floor of the number should be the integer farther from 0, rather than the technically larger one. <br>
     * Ex: {@code Math.ceil(-2.5) = -2}, but {@code CSMath.ceil(-2.5) = -3}
     * @return The adjusted floor of the given value as an integer.
     */
    public static int ceil(double value)
    {
        if (value >= 0)
        {   return (int) Math.ceil(value);
        }
        else return (int) Math.floor(value);
    }

    /**
     * An "absolute" floor method that has inverted behavior for negative numbers. <br>
     * <br>
     * This is more logical in some cases, when the floor of the number should be the integer closer to 0, rather than the technically smaller one. <br>
     * Ex: {@code Math.floor(-2.5) = -3}, but {@code CSMath.floor(-2.5) = -2}
     * @return The adjusted floor of the given value as an integer.
     */
    public static int floor(double value)
    {
        if (value >= 0)
        {   return (int) Math.floor(value);
        }
        else return (int) Math.ceil(value);
    }

    /**
     * A generalized min function that works with any set of comparable objects.
     * @return The smallest value according to the object's {@code compareTo} method.
     */
    @SafeVarargs
    public static <T extends Comparable<T>> T min(T... values)
    {
        T min = values[0];
        for (T value : values)
        {   if (value.compareTo(min) < 0) min = value;
        }
        return min;
    }

    /**
     * A generalized max function that works with any set of comparable objects.
     * @return The largest value according to the object's {@code compareTo} method.
     */
    @SafeVarargs
    public static <T extends Comparable<T>> T max(T... values)
    {
        T max = values[0];
        for (T value : values)
        {   if (value.compareTo(max) > 0) max = value;
        }
        return max;
    }

    public static double min(double... values)
    {
        double min = values[0];
        for (double value : values)
        {   if (value < min) min = value;
        }
        return min;
    }

    public static double max(double... values)
    {
        double max = values[0];
        for (double value : values)
        {   if (value > max) max = value;
        }
        return max;
    }

    /**
     * Calculates if the given value is between two values (inclusive)
     */
    public static boolean betweenInclusive(double value, double min, double max)
    {   return value >= min && value <= max;
    }

    /**
     * Calculates if the given value is between two values (exclusive)
     */
    public static boolean betweenExclusive(double value, double min, double max)
    {   return value > min && value < max;
    }

    /**
     * Returns a number between the two given values {@code blendFrom} and {@code blendTo}, based on factor.<br>
     * If {@code factor} = rangeMin, returns {@code blendFrom}. If {@code factor} = {@code rangeMax}, returns {@code blendTo}.<br>
     * @param blendFrom The minimum value.
     * @param blendTo The maximum value.
     * @param factor The "progress" between blendFrom and blendTo.
     * @param rangeMin The minimum of the range of values over which to interpolate.
     * @param rangeMax The maximum of the range of values over which to interpolate.
     * @return The interpolated value.
     */
    public static double blend(double blendFrom, double blendTo, double factor, double rangeMin, double rangeMax)
    {
        if (rangeMin > rangeMax) return blend(blendTo, blendFrom, factor, rangeMax, rangeMin);

        if (factor <= rangeMin) return blendFrom;
        if (factor >= rangeMax) return blendTo;
        return (blendTo - blendFrom) / (rangeMax - rangeMin) * (factor - rangeMin) + blendFrom;
    }

    /**
     * Floating-point overload for {@link #blend(double, double, double, double, double)}.
     */
    public static float blend(float blendFrom, float blendTo, float factor, float rangeMin, float rangeMax)
    {
        if (rangeMin > rangeMax) return blend(blendTo, blendFrom, factor, rangeMax, rangeMin);

        if (factor <= rangeMin) return blendFrom;
        if (factor >= rangeMax) return blendTo;
        return (blendTo - blendFrom) / (rangeMax - rangeMin) * (factor - rangeMin) + blendFrom;
    }

    /**
     * A blend function with a logarithmic curve (starts fast, then slows down).<br>
     * @return The interpolated value.
     */
    public static double blendLog(double blendFrom, double blendTo, double factor, double rangeMin, double rangeMax)
    {
        if (factor <= rangeMin) return blendFrom;
        if (factor >= rangeMax) return blendTo;
        return (blendTo - blendFrom) / Math.sqrt(rangeMax - rangeMin) * Math.sqrt(factor - rangeMin) + blendFrom;
    }

    /**
     * Floating-point overload for {@link #blendLog(double, double, double, double, double)}.
     */
    public static float blendLog(float blendFrom, float blendTo, float factor, float rangeMin, float rangeMax)
    {
        if (factor <= rangeMin) return blendFrom;
        if (factor >= rangeMax) return blendTo;
        return (blendTo - blendFrom) / (float) Math.sqrt(rangeMax - rangeMin) * (float) Math.sqrt(factor - rangeMin) + blendFrom;
    }

    public static double blendExp(double blendFrom, double blendTo, double factor, double rangeMin, double rangeMax, double intensity)
    {
        factor = clamp(factor, rangeMin, rangeMax);

        double normalizedFactor = (factor - rangeMin) / (rangeMax - rangeMin);
        double expFactor = (Math.pow(intensity, normalizedFactor) - 1) / (intensity - 1);

        return blendFrom + (blendTo - blendFrom) * expFactor;
    }
    public static float blendExp(float blendFrom, float blendTo, float factor, float rangeMin, float rangeMax, double intensity)
    {   return (float) blendExp((double) blendFrom, blendTo, factor, rangeMin, rangeMax, intensity);
    }
    public static double blendExp(double blendFrom, double blendTo, double factor, double rangeMin, double rangeMax)
    {   return blendExp(blendFrom, blendTo, factor, rangeMin, rangeMax, Math.E);
    }
    public static float blendExp(float blendFrom, float blendTo, float factor, float rangeMin, float rangeMax)
    {   return blendExp(blendFrom, blendTo, factor, rangeMin, rangeMax, Math.E);
    }

    // Eases in and out, like a combination of blendLog and blendExp
    public static float blendInOut(float blendFrom, float blendTo, float factor, float rangeMin, float rangeMax)
    {
        // Ensure factor is within the specified range
        factor = Math.max(rangeMin, Math.min(factor, rangeMax));

        // Normalize the factor to a 0-1 range
        float normalizedFactor = (factor - rangeMin) / (rangeMax - rangeMin);

        // Apply ease-in-out curve to the normalized factor
        float expFactor = (float) (Math.pow(normalizedFactor, 2) / (Math.pow(normalizedFactor, 2) + Math.pow(1 - normalizedFactor, 2)));

        // Perform the blend
        return blendFrom + (blendTo - blendFrom) * expFactor;
    }

    /**
     * Gets the average of two numbers contained within a {@link Pair}.
     */
    public static double averagePair(Pair<? extends Number, ? extends Number> pair)
    {   return (pair.getFirst().doubleValue() + pair.getSecond().doubleValue()) / 2;
    }

    /**
     * Adds the values of the given pairs together.
     * @return A pair containing the sum of all the left values and of all the right values.
     */
    @SafeVarargs
    public static Pair<Double, Double> addPairs(Pair<? extends Number, ? extends Number>... pairs)
    {
        double first = 0;
        double second = 0;
        for (Pair<? extends Number, ? extends Number> pair : pairs)
        {   first += pair.getFirst().doubleValue();
            second += pair.getSecond().doubleValue();
        }
        return Pair.of(first, second);
    }

    /**
     * Gets the squared distance between two 3D points.
     * @return The distance, squared.
     */
    public static double getDistanceSqr(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        double xDistance = Math.abs(x1 - x2);
        double yDistance = Math.abs(y1 - y2);
        double zDistance = Math.abs(z1 - z2);
        return xDistance * xDistance + yDistance * yDistance + zDistance * zDistance;
    }

    /**
     * Gets the distance between an entity and a {@link Vec3} 3D point.
     * @return The distance.
     */
    public static double getDistance(Entity entity, Vec3 pos)
    {   return getDistance(entity, pos.x, pos.y, pos.z);
    }

    /**
     * Gets the distance between two 3D points (not squared).
     * @return The distance.
     */
    public static double getDistance(double x1, double y1, double z1, double x2, double y2, double z2)
    {   return Math.sqrt(getDistanceSqr(x1, y1, z1, x2, y2, z2));
    }

    /**
     * Returns the distance between two {@link Vec3} 3D coordinates.
     * @return The distance.
     */
    public static double getDistance(Vec3 pos1, Vec3 pos2)
    {   return getDistance(pos1.x, pos1.y, pos1.z, pos2.x, pos2.y, pos2.z);
    }

    public static double getDistance(Entity entity, double x, double y, double z)
    {   return getDistance(entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), x, y, z);
    }

    public static double getDistance(Vec3i pos1, Vec3i pos2)
    {   return Math.sqrt(pos1.distSqr(pos2));
    }

    public static double average(Number... values)
    {
        double sum = 0;
        for (Number value : values)
        {   sum += value.doubleValue();
        }
        return sum / values.length;
    }

    /**
     * Takes an average of the two values, with weight<br>
     * @param val1 The first value.
     * @param val2 The second value.
     * @param weight1 The weight of the first value.
     * @param weight2 The weight of the second value.
     * @return The weighted average.
     */
    public static double weightedAverage(double val1, double val2, double weight1, double weight2)
    {   return (val1 * weight1 + val2 * weight2) / (weight1 + weight2);
    }

    /**
     * Takes an average of all the values in the given list, with weight<br>
     * <br>
     * @param values The map of values to average (value, weight).
     * @return The average of the values in the given array.
     */
    public static <A extends Number, B extends Number> double weightedAverage(List<Pair<A, B>> values)
    {
        double sum = 0;
        double weightSum = 0;
        for (Pair<A, B> entry : values)
        {
            double weight = entry.getSecond().doubleValue();
            sum += entry.getFirst().doubleValue() * weight;
            weightSum += weight;
        }
        return sum / Math.max(1, weightSum);
    }

    public static Vec3 vectorToVec(Vector3d vec)
    {   return new Vec3(vec.x, vec.y, vec.z);
    }

    /**
     * Returns a {@link Direction} from the given vector.
     * @return The direction.
     */
    public static Direction getDirectionFrom(double x, double y, double z)
    {
        Direction direction = Direction.NORTH;
        double f = Float.MIN_VALUE;

        for (Direction direction1 : Direction.values())
        {
            double f1 = x * direction1.getStepX() + y * direction1.getStepY() + z * direction1.getStepZ();

            if (f1 > f)
            {
                f = f1;
                direction = direction1;
            }
        }

        return direction;
    }

    public static Direction getDirectionFrom(Vec3 vec3)
    {   return getDirectionFrom(vec3.x, vec3.y, vec3.z);
    }

    public static Direction getDirectionFrom(BlockPos from, BlockPos to)
    {   return getDirectionFrom(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());
    }

    public static <T> void breakableForEach(Collection<T> collection, BiConsumer<T, InterruptibleIterator<T>> consumer)
    {   new InterruptibleIterator<T>(collection).run(consumer);
    }

    /**
     * Simple try/catch block that ignores errors.
     * @param runnable The code to run upon success.
     */
    public static void tryCatch(Runnable runnable)
    {
        try
        {   runnable.run();
        }
        catch (Throwable ignored) {}
    }

    public static <T> T tryCatch(Supplier<T> supplier)
    {
        try
        {   return supplier.get();
        }
        catch (Throwable ignored) {}
        return null;
    }

    /**
     * @return 1 if the given value is positive, -1 if it is negative, and 0 if it is 0.
     */
    public static int sign(double value)
    {
        if (value == 0) return 0;
        return value < 0 ? -1 : 1;
    }

    /**
     * @return 1 if the given value is above the range, -1 if it is below the range, and 0 if it is within the range.
     */
    public static int signForRange(double value, double min, double max)
    {
        return value > max ? 1 : value < min ? -1 : 0;
    }

    /**
     * Limits the decimal places of the value to the given amount.
     * @param value The value to limit.
     * @param sigFigs The amount of decimal places to limit to.
     * @return The value with the decimal places limited.
     */
    public static double truncate(double value, int sigFigs)
    {
        return (int) (value * Math.pow(10.0, sigFigs)) / Math.pow(10.0, sigFigs);
    }

    public static boolean isInteger(Number value)
    {   return Math.abs(value.doubleValue() - value.intValue()) < 0.0001;
    }

    public static double round(double value, int places)
    {
        if (places < 0) throw new IllegalArgumentException("Argument \"places\" must be a positive integer.");
        if (isInteger(value)) return value;

        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);

        return bd.doubleValue();
    }

    /**
     * Rounds the given value to the nearest multiple of the given number.
     * @return The rounded value.
     */
    public static double roundNearest(double value, double multipleOf)
    {   return Math.round(value / multipleOf) * multipleOf;
    }

    /**
     * Rounds down the given value to the nearest multiple of the given number.
     * @return The rounded value.
     */
    public static double roundDownNearest(double value, double multipleOf)
    {   return Math.floor(value / multipleOf) * multipleOf;
    }

    /**
     * Rounds up the given value to the nearest multiple of the given number.
     * @return The rounded value.
     */
    public static double roundUpNearest(double value, double multipleOf)
    {   return Math.ceil(value / multipleOf) * multipleOf;
    }

    public static int blendColors(int colorA, int colorB, float ratio)
    {
        int aFrom = (colorA >> 24) & 0xff;
        int rFrom = (colorA >> 16) & 0xff;
        int gFrom = (colorA >> 8) & 0xff;
        int bFrom = colorA & 0xff;

        int aTo = (colorB >> 24) & 0xff;
        int rTo = (colorB >> 16) & 0xff;
        int gTo = (colorB >> 8) & 0xff;
        int bTo = colorB & 0xff;

        int a = (int) (aFrom + (aTo - aFrom) * ratio);
        int r = (int) (rFrom + (rTo - rFrom) * ratio);
        int g = (int) (gFrom + (gTo - gFrom) * ratio);
        int b = (int) (bFrom + (bTo - bFrom) * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Calculates the number that is farthest from zero.
     * @return The absolute maximum value.
     */
    public static double maxAbs(double... values)
    {
        double mostExtreme = 0;
        for (double value : values)
        {
            if (Math.abs(value) > Math.abs(mostExtreme))
            {   mostExtreme = value;
            }
        }
        return mostExtreme;
    }

    /**
     * Calculates the number that is closest to zero.
     * @return The absolute minimum value.
     */
    public static double minAbs(double... values)
    {
        double smallest = values[0];
        for (double value : values)
        {
            if (Math.abs(value) < Math.abs(smallest))
            {
                smallest = value;
            }
        }
        return smallest;
    }

    public static boolean equalAbs(double value1, double value2)
    {   return Math.abs(value1) == Math.abs(value2);
    }

    public static boolean greaterAbs(double value1, double value2)
    {   return Math.abs(value1) > Math.abs(value2);
    }

    public static boolean lessAbs(double value1, double value2)
    {   return Math.abs(value1) < Math.abs(value2);
    }

    public static boolean greaterEqualAbs(double value1, double value2)
    {   return Math.abs(value1) >= Math.abs(value2);
    }

    public static boolean lessEqualAbs(double value1, double value2)
    {   return Math.abs(value1) <= Math.abs(value2);
    }

    /**
     * Lowers the absolute value of the given number by {@code amount}.
     */
    public static double shrink(double value, double amount)
    {   return Math.max(0, Math.abs(value) - amount) * sign(value);
    }

    /**
     * Raises the absolute value of the given number by {@code amount}.
     */
    public static double grow(double value, double amount)
    {   return Math.abs(value) + amount * sign(value);
    }

    /**
     * Integer overload for {@link #shrink(double, double)}
     */
    public static int shrink(int value, int amount)
    {   return value > 0 ? Math.max(0, value - amount) : Math.min(0, value + amount);
    }

    /**
     * Integer overload for {@link #grow(double, double)}
     */
    public static int grow(int value, int amount)
    {   return value > 0 ? value + amount : value - amount;
    }

    /**
     * @return A Vec3 at the center of the given BlockPos.
     */
    public static Vec3 getCenterPos(BlockPos pos)
    {   return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    /**
     * Rotates a VoxelShape to the given direction, assuming it is facing north.
     * @param to The direction to rotate to.
     * @param shape The shape to rotate.
     * @return The rotated shape.
     */
    public static VoxelShape rotateShape(Direction to, VoxelShape shape)
    {   // shapeHolder[0] is the old shape, shapeHolder[1] is the new shape
        VoxelShape[] shapeHolder = new VoxelShape[] {shape, Shapes.empty() };

        int times = (to.get2DDataValue() - Direction.NORTH.get2DDataValue() + 4) % 4;
        for (int i = 0; i < times; i++)
        {
            shapeHolder[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
            {   shapeHolder[1] = Shapes.or(shapeHolder[1], Shapes.create(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX));
            });
            shapeHolder[0] = shapeHolder[1];
            shapeHolder[1] = Shapes.empty();
        }

        return shapeHolder[0];
    }

    /**
     * "Flattens" the given VoxelShape into a 2D projection along the given axis.
     * @param axis The axis to flatten along.
     * @param shape The shape to flatten.
     * @return The flattened shape.
     */
    public static VoxelShape flattenShape(Direction.Axis axis, VoxelShape shape)
    {   // Flatten the shape into a 2D projection
        // shapeHolder[0] is the old shape, shapeHolder[1] is the new shape
        VoxelShape[] shapeHolder = new VoxelShape[] {shape, Shapes.empty()};
        switch (axis)
        {
            case X ->
            shapeHolder[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
            {    shapeHolder[1] = Shapes.or(shapeHolder[1], Shapes.box(0, minY, minZ, 1, maxY, maxZ));
            });

            case Y ->
            shapeHolder[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
            {    shapeHolder[1] = Shapes.or(shapeHolder[1], Shapes.box(minX, 0, minZ, maxX, 1, maxZ));
            });

            case Z ->
            shapeHolder[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
            {    shapeHolder[1] = Shapes.or(shapeHolder[1], Shapes.box(minX, minY, 0, maxX, maxY, 1));
            });
        }
        return shapeHolder[1];
    }

    /**
     * Checks if a cube with size {@code [size * 2]} centered at {@code pos1} contains the position {@code pos2}.
     * @return Whether the position is contained within the cube.
     */
    public static boolean withinCubeDistance(BlockPos pos1, BlockPos pos2, double maxDistance)
    {
        return Math.abs(pos1.getX() - pos2.getX()) <= maxDistance
            && Math.abs(pos1.getY() - pos2.getY()) <= maxDistance
            && Math.abs(pos1.getZ() - pos2.getZ()) <= maxDistance;
    }

    /**
     * Returns an optional containing the value. <br>
     * If the value is a non-usable (NaN, null, or infinite), the returned optional is empty
     * @return An Optional containing the value, or empty if the value is invalid.
     */
    public static Optional<Double> safeDouble(Double value)
    {   return value == null || Double.isNaN(value) || Double.isInfinite(value)
               ? Optional.empty()
               : Optional.of(value);
    }

    /**
     * Returns the exact object used as the key for this map entry.
     * @return The map's key object.
     */
    @Nullable
    public static <Key> Key getExactKey(Map<Key, ?> map, Key key)
    {   return map.keySet().stream().filter(key::equals).findFirst().orElse(null);
    }

    /**
     * Finds the first non-null value out of the given ones.
     * @return The first non-null value, or null if all values are null.
     */
    @SafeVarargs
    @Nullable
    public static <T> T orElse(T... values)
    {   for (int i = 0; i < values.length; i++)
        {   if (values[i] != null) return values[i];
        }
        return null;
    }

    public static <T> List<T> listOrEmpty(Optional<List<T>> list)
    {   return list.orElseGet(Collections::emptyList);
    }

    public static Class<?> getCallerClass(int depth)
    {
        StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
        String callerClassName = null;
        for (int i = 1 + depth; i < stElements.length; i++)
        {
            StackTraceElement ste = stElements[i];
            if (ste.getClassName().indexOf("java.lang.Thread") != 0)
            {
                if (callerClassName == null)
                {   callerClassName = ste.getClassName();
                }
                else if (!callerClassName.equals(ste.getClassName()))
                {   try
                    {   return Class.forName(ste.getClassName());
                    }
                    catch (ClassNotFoundException e)
                    {   return null;
                    }
                }
            }
        }

        return null;
    }

    public static Class<?> getClass(String className)
    {
        try
        {   return Class.forName(className);
        }
        catch (ClassNotFoundException e)
        {   return null;
        }
    }

    public static <T> int getIndexOf(T o, List<T> list, BiPredicate<T, T> equals)
    {
        T[] es = list.toArray((T[]) new Object[0]);
        int size = list.size();
        if (o == null)
        {
            for (int i = 0; i < size - 1; i++)
            {
                if (es[i] == null)
                {   return i;
                }
            }
        }
        else for (int i = 0; i < size - 1; i++)
        {
            if (equals.test(o, es[i]))
            {   return i;
            }
        }

        return -1;
    }

    public static <T> int getIndexOf(List<T> list, Predicate<T> equals)
    {
        T[] es = list.toArray((T[]) new Object[0]);
        int size = list.size();
        for (int i = 0; i < size - 1; i++)
        {
            if (equals.test(es[i]))
            {   return i;
            }
        }

        return -1;
    }

    public static String formatDoubleOrInt(double value)
    {   return isInteger(value) ? String.valueOf((int) value) : String.valueOf(value);
    }

    @SafeVarargs
    public static <T> boolean containsAny(List<T> list, T... values)
    {
        for (T value : values)
        {   if (list.contains(value)) return true;
        }
        return false;
    }

    /**
     * Returns true if the given string contains any of the provided substrings.
     */
    public static boolean containsAny(String string, String... values)
    {
        for (String value : values)
        {   if (string.contains(value)) return true;
        }
        return false;
    }

    /**
     * Optimized anyMatch() method for lists using a basic for loop, because Iterators are slow.
     */
    public static <T> boolean anyMatch(List<T> list, Predicate<T> predicate)
    {
        for (int i = 0; i < list.size(); i++)
        {
            if (predicate.test(list.get(i)))
            {   return true;
            }
        }
        return false;
    }

    /**
     * Generic anyMatch() method for collections using an enhanced for loop.
     */
    public static <T> boolean anyMatch(Collection<T> collection, Predicate<T> predicate)
    {
        for (T t : collection)
        {
            if (predicate.test(t))
            {   return true;
            }
        }
        return false;
    }

    public static void fillHorizontalGradient(PoseStack ps, int x1, int y1, int x2, int y2, int colorFrom, int colorTo)
    {
        int width = x2 - x1;
        // Break into multiple thin strips (more strips = smoother gradient)
        int strips = 50; // Adjust for desired smoothness

        for (int i = 0; i < strips; i++)
        {
            int stripX = x1 + (width * i / strips);
            int nextStripX = x1 + (width * (i + 1) / strips);

            // Calculate interpolated color for this strip
            float fraction = (float) i / strips;
            int color = blendColors(colorFrom, colorTo, fraction);

            // Draw the vertical strip
            Screen.fill(ps, stripX, y1, nextStripX, y2, color);
        }
    }
}
