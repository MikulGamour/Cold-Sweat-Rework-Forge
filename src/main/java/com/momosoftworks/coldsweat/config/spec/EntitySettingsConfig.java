package com.momosoftworks.coldsweat.config.spec;

import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.serialization.ListBuilder;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class EntitySettingsConfig
{
    private static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> INSULATED_MOUNTS;
    public static final ForgeConfigSpec.ConfigValue<List<?>> GOAT_FUR_GROWTH_STATS;
    public static final ForgeConfigSpec.ConfigValue<List<?>> CHAMELEON_SHED_STATS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> CHAMELEON_SPAWN_BIOMES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> GOAT_SPAWN_BIOMES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> ENTITY_TEMPERATURES;

    static
    {
        /*
         Insulated Entities
         */
        BUILDER.push("Entity Temperature");
        INSULATED_MOUNTS = BUILDER
                .comment("List of entities that will insulate the player when riding them",
                         "The rate at which the player's temperature changes is divided by the resistance value",
                         "Format: [[\"entity_id\", coldResistance, hotResistance], [\"entity_id\", coldResistance, hotResistance], etc...]")
                .defineListAllowEmpty(List.of("Insulated Mounts"), () -> Arrays.asList(
                ),
                it -> it instanceof List<?> list
                      && list.size() == 3
                      && list.get(0) instanceof String
                      && list.get(1) instanceof Number
                      && list.get(2) instanceof Number);

        ENTITY_TEMPERATURES = BUILDER
                .comment("Defines temperature-emitting properties for entities",
                         "Format: [[\"entity_id\", temperature, range, *units], [\"entity_id\", temperature, range, *units], etc...]",
                         "temperature: The temperature emitted by the entity",
                         "range: The range of the effect, in blocks",
                         "units: (Optional) The units of the temperature value (MC, F, or C). Defaults to MC")
                .defineListAllowEmpty(List.of("Entity Temperatures"), () -> Arrays.asList(),
                it -> it instanceof List<?> list
                      && list.size() >= 3
                      && list.get(0) instanceof String
                      && list.get(1) instanceof Number
                      && list.get(2) instanceof Number
                      && (list.size() < 4 || list.get(3) instanceof String));

        BUILDER.pop();

        BUILDER.push("Fur Growth & Shedding");

        GOAT_FUR_GROWTH_STATS = BUILDER
                .comment("Defines how often a goat will try to grow its fur, the growth cooldown after shearing, and the chance of it succeeding",
                        "Format: [ticks, cooldown, chance]")
                .defineList("Goat Fur Growth Timings", List.of(
                        1200, 2400, 0.20
                ),
                it -> it instanceof Number);

        CHAMELEON_SHED_STATS = BUILDER
                .comment("Defines how often a chameleon will try to shed its skin, the cooldown after shedding, and the chance of it succeeding",
                        "Format: [ticks, cooldown, chance]")
                .defineList("Chameleon Shedding Timings", List.of(
                        100, 36000, 0.10
                ),
                it -> it instanceof Number);

        BUILDER.pop();

        BUILDER.push("Mob Spawning");
        CHAMELEON_SPAWN_BIOMES = BUILDER
                .comment("Defines the biomes that Chameleons can spawn in",
                         "Format: [[\"biome_id\", weight], [\"biome_id\", weight], etc...]")
                .defineListAllowEmpty(List.of("Chameleon Spawn Biomes"), () -> ListBuilder.begin(
                                List.of("minecraft:bamboo_jungle", 80),
                                List.of("minecraft:jungle", 80),
                                List.of("minecraft:sparse_jungle", 35),
                                List.of("minecraft:desert", 1))
                            .addIf(CompatManager.isBiomesOPlentyLoaded(),
                                () -> List.of("biomesoplenty:lush_desert", 3),
                                () -> List.of("biomesoplenty:rainforest", 20),
                                () -> List.of("biomesoplenty:rocky_rainforest", 15),
                                () -> List.of("biomesoplenty:fungal_jungle", 10),
                                () -> List.of("biomesoplenty:tropics", 8),
                                () -> List.of("biomesoplenty:outback", 2))
                            .addIf(CompatManager.isBiomesYoullGoLoaded(),
                                () -> List.of("byg:tropical_rainforest", 60),
                                () -> List.of("byg:jacaranda_forest", 3),
                                () -> List.of("byg:guiana_shield", 3),
                                () -> List.of("byg:crag_gardens", 4),
                                () -> List.of("byg:atacama_desert", 1),
                                () -> List.of("byg:cypress_swamplands", 3),
                                () -> List.of("byg:mojave_desert", 1),
                                () -> List.of("byg:windswept_desert", 2))
                            .addIf(CompatManager.isAtmosphericLoaded(),
                                () -> List.of("atmospheric:dunes", 0.75),
                                () -> List.of("atmospheric:flourishing_dunes", 1.5),
                                () -> List.of("atmospheric:rocky_dunes", 0.75),
                                () -> List.of("atmospheric:petrified_dunes", 0.5),
                                () -> List.of("atmospheric:rainforest", 50),
                                () -> List.of("atmospheric:sparse_rainforest", 40),
                                () -> List.of("atmospheric:rainforest_basin", 50),
                                () -> List.of("atmospheric:sparse_rainforest_basin", 30))
                           .addIf(CompatManager.isTerralithLoaded(),
                                () -> List.of("terralith:red_oasis", 3),
                                () -> List.of("terralith:desert_oasis", 3),
                                () -> List.of("terralith:tropical_jungle", 80),
                                () -> List.of("terralith:arid_highlands", 1.5),
                                () -> List.of("terralith:rocky_jungle", 80),
                                () -> List.of("terralith:brushland", 1.5))
                           .addIf(CompatManager.isWythersLoaded(),
                                () -> List.of("wythers:cactus_desert", 1),
                                () -> List.of("wythers:tropical_forest", 10),
                                () -> List.of("wythers:tropical_rainforest", 80)
                        ).build(),
                        it -> it instanceof List<?> list && list.size() == 2 && list.get(0) instanceof String && list.get(1) instanceof Number);

        GOAT_SPAWN_BIOMES = BUILDER
                .comment("Defines additional biomes that goats can spawn in",
                         "Format: [[\"biome_id\", weight], [\"biome_id\", weight], etc...]",
                         "Not affected by the \"Increase Goat Spawns\" option")
                .defineListAllowEmpty(List.of("Goat Spawn Biomes"), () -> ListBuilder.begin(
                                List.of("minecraft:frozen_peaks", 8),
                                List.of("minecraft:jagged_peaks", 8),
                                List.of("minecraft:snowy_slopes", 8),
                                List.of("minecraft:meadow", 3),
                                List.of("minecraft:windswept_hills", 6),
                                List.of("minecraft:windswept_forest", 6),
                                List.of("minecraft:windswept_gravelly_hills", 4),
                                List.of("minecraft:grove", 5),
                                List.of("minecraft:stony_peaks", 8))
                            .addIf(CompatManager.isBiomesOPlentyLoaded(),
                                () -> List.of("biomesoplenty:boreal_forest", 5),
                                () -> List.of("biomesoplenty:jade_cliffs", 4),
                                () -> List.of("biomesoplenty:crag", 3))
                            .addIf(CompatManager.isBiomesYoullGoLoaded(),
                                () -> List.of("byg:canadian_shield", 3),
                                () -> List.of("byg:guiana_shield", 3),
                                () -> List.of("byg:fragment_forest", 128),
                                () -> List.of("byg:howling_peaks", 6),
                                () -> List.of("byg:shattered_glacier", 6),
                                () -> List.of("byg:dacite_ridges", 5))
                            .addIf(CompatManager.isTerralithLoaded(),
                                () -> List.of("terralith:blooming_plateau", 5),
                                () -> List.of("terralith:rocky_mountains", 6),
                                () -> List.of("terralith:alpine_grove", 6),
                                () -> List.of("terralith:scarlet_mountains", 4),
                                () -> List.of("terralith:windswept_spires", 16),
                                () -> List.of("terralith:cloud_forest", 4),
                                () -> List.of("terralith:haze_mountain", 4)
                        ).build(),
                        it -> it instanceof List<?> list && list.size() == 2 && list.get(0) instanceof String && list.get(1) instanceof Number);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void setup()
    {
        Path configPath = FMLPaths.CONFIGDIR.get();
        Path csConfigPath = Paths.get(configPath.toAbsolutePath().toString(), "coldsweat");

        // Create the config folder
        try
        {   Files.createDirectory(csConfigPath);
        }
        catch (Exception ignored) {}

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "coldsweat/entity.toml");
    }

    public static void save()
    {   SPEC.save();
    }
}
