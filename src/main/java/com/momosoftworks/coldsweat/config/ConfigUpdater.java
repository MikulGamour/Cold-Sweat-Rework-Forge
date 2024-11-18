package com.momosoftworks.coldsweat.config;

import com.google.common.io.Files;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.config.spec.ItemSettingsConfig;
import com.momosoftworks.coldsweat.config.spec.MainSettingsConfig;
import com.momosoftworks.coldsweat.config.spec.WorldSettingsConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ConfigUpdater
{
    public static void updateConfigs()
    {
        String version = getVersionString(ModList.get().getModContainerById(ColdSweat.MOD_ID).get().getModInfo().getVersion());
        if (version.equals("0.0-NONE")) return;

        String configVersion = MainSettingsConfig.getInstance().getVersion();
        ItemSettingsConfig itemSettings = ItemSettingsConfig.getInstance();
        WorldSettingsConfig worldSettings = WorldSettingsConfig.getInstance();

        /*
         2.3-b01a
         */
        if (isBehind(configVersion, "2.3-b01a"))
        {
            // Update chameleon molt insulation ingredient entry
            addConfigSetting(ItemSettingsConfig.INSULATION_ITEMS,
                             Arrays.asList("cold_sweat:chameleon_molt", 2, 0.0085, "adaptive"));
        }

        /*
         2.3-b04a
         */
        if (isBehind(configVersion, "2.3-b04a"))
        {
            // Update soul sprout food item
            addConfigSetting(ItemSettingsConfig.FOOD_TEMPERATURES,
                             Arrays.asList("cold_sweat:soul_sprout", 0.0, "{}", 1200));
            addConfigSetting(WorldSettingsConfig.BLOCK_TEMPERATURES,
                             Arrays.asList("minecraft:lava_cauldron", 0.5, 7, true, 1.5));
        }

        /*
         2.3-b05b
         */
        if (isBehind(configVersion, "2.3-b05b"))
        {
            // Remove water as fuel item for hearth
            removeConfigSetting(ItemSettingsConfig.HEARTH_FUELS, "minecraft:water_bucket");
            // Remove water as fuel item for icebox
            removeConfigSetting(ItemSettingsConfig.ICEBOX_FUELS, "minecraft:water_bucket");
        }

        /*
         2.3
         */
        if (isBehind(configVersion, "2.3"))
        {
            // Update magma block temperature
            replaceConfigSetting(WorldSettingsConfig.BLOCK_TEMPERATURES, "minecraft:magma_block", blockTemp -> {
                blockTemp.clear();
                blockTemp.addAll(Arrays.asList("minecraft:magma_block", 0.25, 3, 1.0));
            });

            // Update ice fuel value
            replaceConfigSetting(ItemSettingsConfig.ICEBOX_FUELS, "minecraft:ice", iceFuel -> {
                iceFuel.set(1, 250);
            });
            replaceConfigSetting(ItemSettingsConfig.HEARTH_FUELS, "minecraft:ice", iceFuel -> {
                iceFuel.set(1, -250);
            });

            // Update snow fuel value
            replaceConfigSetting(ItemSettingsConfig.ICEBOX_FUELS, "minecraft:snow_block", snowFuel -> {
                snowFuel.set(1, 100);
            });
            replaceConfigSetting(ItemSettingsConfig.HEARTH_FUELS, "minecraft:snow_block", snowFuel -> {
                snowFuel.set(1, -100);
            });

            // Update powder snow fuel value
            replaceConfigSetting(ItemSettingsConfig.ICEBOX_FUELS, "minecraft:powder_snow_bucket", powderSnowFuel -> {
                powderSnowFuel.set(1, 100);
            });
            replaceConfigSetting(ItemSettingsConfig.HEARTH_FUELS, "minecraft:powder_snow_bucket", powderSnowFuel -> {
                powderSnowFuel.set(1, -100);
            });

            // Update snowball fuel value
            replaceConfigSetting(ItemSettingsConfig.ICEBOX_FUELS, "minecraft:snowball", snowballFuel -> {
                snowballFuel.set(1, 10);
            });
            replaceConfigSetting(ItemSettingsConfig.HEARTH_FUELS, "minecraft:snowball", snowballFuel -> {
                snowballFuel.set(1, -10);
            });
        }

        /*
         2.3.1
         */
        if (isBehind(configVersion, "2.3.1"))
        {
            removeConfigSetting(WorldSettingsConfig.BLOCK_TEMPERATURES, "minecraft:ice");
            removeConfigSetting(WorldSettingsConfig.BLOCK_TEMPERATURES, "minecraft:packed_ice");
            removeConfigSetting(WorldSettingsConfig.BLOCK_TEMPERATURES, "minecraft:blue_ice");
        }

        /*
         2.3.2
         */
        if (isBehind(configVersion, "2.3.2"))
        {
            // Goat fur insulation value
            replaceConfigSetting(ItemSettingsConfig.INSULATION_ITEMS, "cold_sweat:fur", insulator ->
            {   insulator.set(0, "cold_sweat:goat_fur");
            });

            // Goat fur armor insulation values
            replaceConfigSetting(ItemSettingsConfig.INSULATION_ITEMS, "cold_sweat:fur_cap", insulator ->
            {   insulator.set(0, "cold_sweat:goat_fur_cap");
            });
            replaceConfigSetting(ItemSettingsConfig.INSULATION_ITEMS, "cold_sweat:fur_parka", insulator ->
            {   insulator.set(0, "cold_sweat:goat_fur_parka");
            });
            replaceConfigSetting(ItemSettingsConfig.INSULATION_ITEMS, "cold_sweat:fur_pants", insulator ->
            {   insulator.set(0, "cold_sweat:goat_fur_pants");
            });
            replaceConfigSetting(ItemSettingsConfig.INSULATION_ITEMS, "cold_sweat:fur_boots", insulator ->
            {   insulator.set(0, "cold_sweat:goat_fur_boots");
            });

            // Goat fur armor worn insulation value
            replaceConfigSetting(ItemSettingsConfig.INSULATING_ARMOR, "cold_sweat:fur_cap", insulator ->
            {   insulator.set(0, "cold_sweat:goat_fur_cap");
            });
            replaceConfigSetting(ItemSettingsConfig.INSULATING_ARMOR, "cold_sweat:fur_parka", insulator ->
            {   insulator.set(0, "cold_sweat:goat_fur_parka");
            });
            replaceConfigSetting(ItemSettingsConfig.INSULATING_ARMOR, "cold_sweat:fur_pants", insulator ->
            {   insulator.set(0, "cold_sweat:goat_fur_pants");
            });
            replaceConfigSetting(ItemSettingsConfig.INSULATING_ARMOR, "cold_sweat:fur_boots", insulator ->
            {   insulator.set(0, "cold_sweat:goat_fur_boots");
            });
        }

        /*
         2.3.4
         */
        if (isBehind(configVersion, "2.3.4"))
        {
            removeConfigSetting(WorldSettingsConfig.BLOCK_TEMPERATURES, "minecraft:soul_fire");
            removeConfigSetting(WorldSettingsConfig.BLOCK_TEMPERATURES, "minecraft:soul_campfire");
            // Add block temperatures converted from Java
            addConfigSetting(WorldSettingsConfig.BLOCK_TEMPERATURES, Arrays.asList("cold_sweat:boiler", 0.27, 7, 0.88, "lit=true", "{}", 4));
            addConfigSetting(WorldSettingsConfig.BLOCK_TEMPERATURES, Arrays.asList("cold_sweat:icebox", -0.27, 7, 0.88, "frosted=true", "{}", 0));
            addConfigSetting(WorldSettingsConfig.BLOCK_TEMPERATURES, Arrays.asList("minecraft:ice", -0.15, 4, 0.6, "", "{}", -0.7));
            addConfigSetting(WorldSettingsConfig.BLOCK_TEMPERATURES, Arrays.asList("minecraft:packed_ice", -0.25, 4, 1.0, "", "{}", -0.7));
            addConfigSetting(WorldSettingsConfig.BLOCK_TEMPERATURES, Arrays.asList("minecraft:blue_ice", -0.35, 4, 1.4, "", "{}", -0.7));
            addConfigSetting(WorldSettingsConfig.BLOCK_TEMPERATURES, Arrays.asList("#minecraft:ice", -0.15, 4, 0.6, "", "{}", -0.7));
            addConfigSetting(WorldSettingsConfig.BLOCK_TEMPERATURES, Arrays.asList("#minecraft:campfires", 0.476, 7, 0.9, "lit=true", " ", 8));
        }

        // Update config version
        MainSettingsConfig.getInstance().setVersion(version);

        MainSettingsConfig.getInstance().save();
        itemSettings.save();
        worldSettings.save();
    }

    public static void updateFileNames()
    {
        for (File file : ConfigLoadingHandler.findFilesRecursive(FMLPaths.CONFIGDIR.get().resolve("coldsweat").toFile()))
        {
            if (!file.isFile()) continue;
            switch (file.getName())
            {
                case "world_settings.toml"  : renameFile(file, "world.toml"); break;
                case "entity_settings.toml" : renameFile(file, "entity.toml"); break;
                case "item_settings.toml"   : renameFile(file, "item.toml"); break;
            }
        }
    }

    private static void renameFile(File file, String newName)
    {
        File newFile = file.toPath().resolveSibling(newName).toFile();
        try
        {   Files.move(file, newFile);
        }
        catch (Exception e)
        {   ColdSweat.LOGGER.error("Failed to rename file {} to {}", file, newFile, e);
        }
    }

    public static String getVersionString(ArtifactVersion version)
    {
        String ver = version.getMajorVersion() + "." + version.getMinorVersion();
        if (version.getIncrementalVersion() != 0)
        {   ver += "." + version.getIncrementalVersion();
        }
        if (version.getQualifier() != null && !version.getQualifier().isEmpty())
        {   ver += "-" + version.getQualifier();
        }
        return ver;
    }

    private static boolean isBehind(String version, String comparedTo)
    {
        boolean isBehind = compareVersions(version, comparedTo) < 0;
        if (isBehind)
        {   ColdSweat.LOGGER.warn("Last launched version {} is less than {}. Updating config settings...", version, comparedTo);
        }
        return isBehind;
    }

    public static int compareVersions(String version, String comparedTo)
    {
        String[] v1Parts = version.split("\\.|\\-");
        String[] v2Parts = comparedTo.split("\\.|\\-");

        int i = 0;
        while (i < v1Parts.length && i < v2Parts.length)
        {
            if (v1Parts[i].matches("\\d+") && v2Parts[i].matches("\\d+"))
            {
                int num1 = Integer.parseInt(v1Parts[i]);
                int num2 = Integer.parseInt(v2Parts[i]);
                if (num1 != num2)
                {   return Integer.compare(num1, num2);
                }
            }
            else
            {
                // If one version is a full release and the other is a beta, the full release is greater
                if (!v1Parts[i].startsWith("b") && v2Parts[i].startsWith("b"))
                {   return 1;
                }
                else if (v1Parts[i].startsWith("b") && !v2Parts[i].startsWith("b"))
                {   return -1;
                }

                int result = comparePreReleaseVersions(v1Parts[i], v2Parts[i]);
                if (result != 0)
                {   return result;
                }
            }
            i++;
        }

        // If all parts are equal so far, but one version has more parts, it's greater
        // unless the shorter one is a full release and the longer one is a beta
        if (v1Parts.length != v2Parts.length)
        {
            if (i == v1Parts.length && i < v2Parts.length && v2Parts[i].startsWith("b"))
            {   return 1;
            }
            else if (i == v2Parts.length && i < v1Parts.length && v1Parts[i].startsWith("b"))
            {   return -1;
            }
        }

        return Integer.compare(v1Parts.length, v2Parts.length);
    }

    private static int comparePreReleaseVersions(String v1, String v2)
    {
        if (v1.startsWith("b") && v2.startsWith("b"))
        {   return compareWithSubVersions(v1.substring(1), v2.substring(1));
        }
        return v1.compareTo(v2);
    }

    private static int compareWithSubVersions(String v1, String v2)
    {
        String[] parts1 = v1.split("(?<=\\d)(?=\\D)|(?<=\\D)(?=\\d)");
        String[] parts2 = v2.split("(?<=\\d)(?=\\D)|(?<=\\D)(?=\\d)");

        int i = 0;
        while (i < parts1.length && i < parts2.length)
        {
            if (parts1[i].matches("\\d+") && parts2[i].matches("\\d+"))
            {
                int num1 = Integer.parseInt(parts1[i]);
                int num2 = Integer.parseInt(parts2[i]);
                if (num1 != num2)
                {   return Integer.compare(num1, num2);
                }
            }
            else
            {
                int result = parts1[i].compareTo(parts2[i]);
                if (result != 0)
                {   return result;
                }
            }
            i++;
        }

        return Integer.compare(parts1.length, parts2.length);
    }

    public static void replaceConfigSetting(ForgeConfigSpec.ConfigValue<List<? extends List<?>>> config, String key,
                                            Consumer<List<Object>> modifier)
    {
        List<List<?>> setting = new ArrayList<>(config.get());
        for (int i = 0; i < setting.size(); i++)
        {
            List<Object> element = new ArrayList<>(setting.get(i));
            if (!element.isEmpty() && element.get(0).equals(key))
            {
                try
                {
                    modifier.accept(element);
                    setting.set(i, element);
                    config.set(setting);
                }
                catch (Exception e)
                {   ColdSweat.LOGGER.error("Failed to update config setting {} for key '{}'", config.getPath(), key, e);
                }
                break;
            }
        }
    }

    public static void addConfigSetting(ForgeConfigSpec.ConfigValue<List<? extends List<?>>> config, List<?> newSetting)
    {
        List<List<? extends Object>> setting = new ArrayList<>(config.get());
        if (setting.stream().noneMatch(entry -> !entry.isEmpty() && entry.get(0).equals(newSetting.get(0))))
        {
            config.clearCache();
            setting.add(newSetting);
            config.set(setting);
        }
    }

    public static void removeConfigSetting(ForgeConfigSpec.ConfigValue<List<? extends List<? extends Object>>> config, String key)
    {
        List<? extends List<? extends Object>> setting = new ArrayList<>(config.get());
        setting.removeIf(entry -> !entry.isEmpty() && entry.get(0).equals(key));
        config.set(setting);
    }
}
