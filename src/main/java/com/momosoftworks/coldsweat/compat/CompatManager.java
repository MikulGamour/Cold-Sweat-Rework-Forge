package com.momosoftworks.coldsweat.compat;

import com.anthonyhilyard.iceberg.util.Tooltips;
import com.mojang.datafixers.util.Either;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.temperature.modifier.compat.SereneSeasonsTempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.core.init.ModItems;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModDamageSources;
import dev.ghen.thirst.content.purity.ContainerWithPurity;
import dev.ghen.thirst.content.purity.WaterPurity;
import dev.ghen.thirst.foundation.common.event.RegisterThirstValueEvent;
import glitchcore.event.EventManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.joml.Vector3d;
import sereneseasons.api.season.SeasonChangedEvent;
import sereneseasons.season.SeasonHooks;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CompatManager
{
    private static final boolean BOP_LOADED = modLoaded("biomesoplenty");
    private static final boolean SEASONS_LOADED = modLoaded("sereneseasons");
    private static final boolean CURIOS_LOADED = modLoaded("curios");
    private static final boolean WEREWOLVES_LOADED = modLoaded("werewolves");
    private static final boolean SPIRIT_LOADED = modLoaded("spirit");
    private static final boolean BYG_LOADED = modLoaded("byg");
    private static final boolean BWG_LOADED = modLoaded("biomeswevegone");
    private static final boolean CREATE_LOADED = modLoaded("create", "0.5.1");
    private static final boolean ATMOSPHERIC_LOADED = modLoaded("atmospheric");
    private static final boolean ENVIRONMENTAL_LOADED = modLoaded("environmental");
    private static final boolean TERRALITH_LOADED = modLoaded("terralith");
    private static final boolean WEATHER_LOADED = modLoaded("weather2");
    private static final boolean WYTHERS_LOADED = modLoaded("wwoo");
    private static final boolean TOOLTIPS_LOADED = modLoaded("legendarytooltips");
    private static final boolean PRIMAL_WINTER_LOADED = modLoaded("primalwinter");
    private static final boolean THIRST_LOADED = modLoaded("thirst", "1.21.0-2.0.0");
    private static final boolean ICEBERG_LOADED = modLoaded("iceberg");
    private static final boolean SPOILED_LOADED = modLoaded("spoiled");
    private static final boolean SUPPLEMENTARIES_LOADED = modLoaded("supplementaries");

    public static boolean modLoaded(String modID, String minVersion, String maxVersion)
    {
        ModFileInfo mod = FMLLoader.getLoadingModList().getModFileById(modID);
        if (mod == null)
        {   return false;
        }

        ArtifactVersion version = mod.getFile().getJarVersion();
        if (!minVersion.isEmpty())
        {
            if (version.compareTo(new DefaultArtifactVersion(minVersion)) >= 0)
            {   return true;
            }
            else
            {   ColdSweat.LOGGER.error("Cold Sweat requires {} {} or higher for compat to be enabled!", modID, version);
                return false;
            }
        }
        if (!maxVersion.isEmpty())
        {
            if (version.compareTo(new DefaultArtifactVersion(maxVersion)) <= 0)
            {   return true;
            }
            else
            {   ColdSweat.LOGGER.error("Cold Sweat requires {} {} or lower for compat to be enabled!", modID, version);
                return false;
            }
        }
        else return true;
    }

    public static boolean modLoaded(String modID, String minVersion)
    {   return modLoaded(modID, minVersion, "");
    }

    public static boolean modLoaded(String modID)
    {   return modLoaded(modID, "");
    }

    public static boolean isBiomesOPlentyLoaded()
    {   return BOP_LOADED;
    }
    public static boolean isSereneSeasonsLoaded()
    {   return SEASONS_LOADED;
    }
    public static boolean isCuriosLoaded()
    {   return CURIOS_LOADED;
    }
    public static boolean isWerewolvesLoaded()
    {   return WEREWOLVES_LOADED;
    }
    public static boolean isSpiritLoaded()
    {   return SPIRIT_LOADED;
    }
    public static boolean isBiomesYoullGoLoaded()
    {   return BYG_LOADED;
    }
    public static boolean isBiomesWeveGoneLoaded()
    {   return BWG_LOADED;
    }
    public static boolean isCreateLoaded()
    {   return CREATE_LOADED;
    }
    public static boolean isAtmosphericLoaded()
    {   return ATMOSPHERIC_LOADED;
    }
    public static boolean isEnvironmentalLoaded()
    {   return ENVIRONMENTAL_LOADED;
    }
    public static boolean isTerralithLoaded()
    {   return TERRALITH_LOADED;
    }
    public static boolean isWeather2Loaded()
    {   return WEATHER_LOADED;
    }
    public static boolean isWythersLoaded()
    {   return WYTHERS_LOADED;
    }
    public static boolean isLegendaryTooltipsLoaded()
    {   return TOOLTIPS_LOADED;
    }
    public static boolean isPrimalWinterLoaded()
    {   return PRIMAL_WINTER_LOADED;
    }
    public static boolean isThirstLoaded()
    {   return THIRST_LOADED;
    }
    public static boolean isIcebergLoaded()
    {   return ICEBERG_LOADED;
    }
    public static boolean isSpoiledLoaded()
    {   return SPOILED_LOADED;
    }
    public static boolean isSupplementariesLoaded()
    {   return SUPPLEMENTARIES_LOADED;
    }

    public static abstract class Curios
    {
        public static boolean hasCurio(Player player, Item curio)
        {
            return CURIOS_LOADED
                && Optional.ofNullable(player.getCapability(CuriosCapability.INVENTORY))
                           .map(cap -> cap.findFirstCurio(curio))
                           .map(Optional::isPresent)
                           .orElse(false);
        }

        public static List<ItemStack> getCurios(LivingEntity entity)
        {
            if (!CURIOS_LOADED) return new ArrayList<>();
            return Optional.ofNullable(entity.getCapability(CuriosCapability.INVENTORY))
                           .map(curiosHandler -> curiosHandler.getEquippedCurios())
                           .map(stacks ->
                           {
                               List<ItemStack> list = new ArrayList<>();
                               for (int i = 0; i < stacks.getSlots(); i++)
                               {   list.add(stacks.getStackInSlot(i));
                               }
                               return list;
                           }).orElse(new ArrayList<>());
        }
    }

    public static abstract class Werewolves
    {
        public static boolean isWerewolf(Player player)
        {   return false;//WEREWOLVES_LOADED && WerewolfPlayer.getOpt(player).filter(w -> w.getLevel() > 0).map(w -> w.getForm().isTransformed()).orElse(false);
        }
    }

    public static abstract class Weather2
    {
        public static boolean isRainstormAt(Level level, BlockPos pos)
        {
            /*if (WEATHER_LOADED)
            {
                WeatherManagerServer weatherManager = ServerTickHandler.getWeatherManagerFor(level.dimension());
                if (weatherManager == null) return false;
                StormObject rainStorm = weatherManager.getClosestStormAny(new Vec3(pos.getX(), pos.getY(), pos.getZ()), 250);
                if (rainStorm == null) return false;

                if (WorldHelper.canSeeSky(level, pos, 60) && rainStorm.isPrecipitating() && rainStorm.levelTemperature > 0.0f
                && Math.sqrt(Math.pow(pos.getX() - rainStorm.pos.x, 2) + Math.pow(pos.getX() - rainStorm.pos.x, 2)) < rainStorm.getSize())
                {   return true;
                }
            }*/
            return false;
        }

        public static Object getClosestStorm(Level level, BlockPos pos)
        {
            if (WEATHER_LOADED)
            {
                /*WeatherManagerServer weatherManager = ServerTickHandler.getWeatherManagerFor(level.dimension());
                if (weatherManager == null) return null;

                double distance = Double.MAX_VALUE;
                WeatherObject closestStorm = null;
                for (WeatherObject stormObject : weatherManager.getStormObjects())
                {
                    double newDistance = stormObject.pos.distanceTo(new Vec3(pos.getX(), pos.getY(), pos.getZ()));
                    if (newDistance < distance)
                    {   distance = newDistance;
                        closestStorm = stormObject;
                    }
                }
                return closestStorm;*/
                return null;
            }
            return null;
        }
    }

    public static abstract class SereneSeasons
    {
        public static boolean isColdEnoughToSnow(Level level, BlockPos pos)
        {
            return SEASONS_LOADED && SeasonHooks.coldEnoughToSnowSeasonal(level, level.getBiome(pos), pos);
        }
    }

    public static abstract class Thirst
    {
        public static boolean hasWaterPurity(ItemStack stack)
        {
            if (THIRST_LOADED)
            {   return WaterPurity.hasPurity(stack);
            }
            return false;
        }

        public static int getWaterPurity(ItemStack stack)
        {
            if (THIRST_LOADED)
            {   return new Object()
            {
                public int getWaterPurity()
                {   return WaterPurity.getPurity(stack);
                }
            }.getWaterPurity();
            }
            return 0;
        }

        public static ItemStack setWaterPurity(ItemStack stack, int purity)
        {
            if (THIRST_LOADED)
            {
            return new Object()
            {
                public ItemStack setWaterPurity()
                {   return WaterPurity.addPurity(stack, purity);
                }
            }.setWaterPurity();
            }
            return stack;
        }

        public static ItemStack setWaterPurity(ItemStack item, BlockPos pos, Level level)
        {
            if (THIRST_LOADED)
            {
            return new Object()
            {
                public ItemStack setWaterPurity()
                {   return WaterPurity.addPurity(item, pos, level);
                }
            }.setWaterPurity();
            }
            return item;
        }
    }

    public static abstract class LegendaryTooltips
    {
        public static int getTooltipStartIndex(List<Either<FormattedText, TooltipComponent>> tooltip)
        {
            if (isIcebergLoaded())
            {
                return new Object()
            {
                public int getLegendaryTTStartIndex()
                {
                    int index = CSMath.getIndexOf(tooltip, element -> element.right().map(component -> component instanceof Tooltips.TitleBreakComponent).orElse(false));
                    if (index == -1) return 0;
                    return index;
                }
            }.getLegendaryTTStartIndex();
            }
            return 0;
        }
    }

    public static abstract class Valkyrien
    {
        /*public static Vec3 translateToShipCoords(Vec3 pos, Ship ship)
        {
            if (ship != null)
            {
                Vector3d posVec = VectorConversionsMCKt.toJOML(pos);
                ship.getWorldToShip().transformPosition(posVec);
                return VectorConversionsMCKt.toMinecraft(posVec);
            }
            return pos;
        }*/

        /**
         * If any ship is managing the given position, translate the position to the corresponding coordinates in the shipyard
         */
        /*public static Vec3 transformIfShipPos(Level level, Vec3 pos)
        {
            if (VALKYRIEN_SKIES_LOADED)
            {
                List<Vector3d> shipTransforms = VSGameUtilsKt.transformToNearbyShipsAndWorld(level, pos.x, pos.y, pos.z, 1);
                if (shipTransforms.isEmpty()) return pos;
                Vector3d shipCoords = shipTransforms.get(0);
                return VectorConversionsMCKt.toMinecraft(shipCoords);
            }
            return pos;
        }*/

        /*public static AABB transformIfShipPos(Level level, AABB aabb)
        {
            Vec3 min = transformIfShipPos(level, new Vec3(aabb.minX, aabb.minY, aabb.minZ));
            Vec3 max = transformIfShipPos(level, new Vec3(aabb.maxX, aabb.maxY, aabb.maxZ));
            return new AABB(min, max);
        }*/

        /*public static BlockPos transformIfShipPos(Level level, BlockPos pos)
        {
            if (VALKYRIEN_SKIES_LOADED)
            {
                List<Vector3d> shipTransforms = VSGameUtilsKt.transformToNearbyShipsAndWorld(level, pos.getX(), pos.getY(), pos.getZ(), 1);
                if (shipTransforms.isEmpty()) return pos;
                Vector3d shipCoords = shipTransforms.get(0);
                return BlockPos.containing(VectorConversionsMCKt.toMinecraft(shipCoords));
            }
            return pos;
        }*/
    }

    /* Compat Events */

    public static void registerEventHandlers()
    {
        if (CURIOS_LOADED)
        {
            NeoForge.EVENT_BUS.register(new Object()
            {
                @SubscribeEvent
                public void onCurioChange(CurioChangeEvent event)
                {
                    EntityTempManager.updateInsulationAttributeModifiers(event.getEntity(), event.getFrom(), event.getTo());
                }
            });
        }

        if (THIRST_LOADED)
        {
            NeoForge.EVENT_BUS.register(new Object()
            {
                @SubscribeEvent
                public void registerThirstItems(RegisterThirstValueEvent event)
                {
                    event.addDrink(ModItems.FILLED_WATERSKIN.value(), 6, 3);
                    event.addContainer(new ContainerWithPurity(ModItems.WATERSKIN.value(),
                                                               ModItems.FILLED_WATERSKIN.value()));
                }
            });
        }

        if (SEASONS_LOADED)
        {
            // Register event to GlitchCore's stupid redundant proprietary event bus
            new Object()
            {
                public void registerListener()
                {
                    EventManager.<SeasonChangedEvent.Standard>addListener(event ->
                    {
                        for (Player player : event.getLevel().players())
                        {
                            Temperature.getModifier(player, Temperature.Trait.WORLD, SereneSeasonsTempModifier.class)
                                       .ifPresent(mod -> mod.update(mod.getLastInput(), player, Temperature.Trait.WORLD));
                        }
                    });
                }
            }.registerListener();
        }
    }

    public static boolean USING_BACKTANK = false;

    //TODO: Reimplement when this mod is updated
    /*@SubscribeEvent
    public static void drainCreateBacktank(PlayerTickEvent.Post event)
    {
        Player player = event.getEntity();
        if (!CompatManager.isCreateLoaded()) return;
        ItemStack backTank = player.getItemBySlot(EquipmentSlot.CHEST);

        // Somehow this makes the indicator render. I have no idea
        if (USING_BACKTANK && player.level().isClientSide)
        {
            player.getPersistentData().putInt("VisualBacktankAir", Math.round(BacktankUtil.getAllWithAir(player).stream()
                                                                                      .map(BacktankUtil::getAir)
                                                                                      .reduce(0f, Float::sum)) - 1);
        }

        if (player.tickCount % 20 != 0 || event.phase == TickEvent.Phase.START)
        {   return;
        }

        if (!player.isCreative() && !player.isInLava()
        && backTank.getItem() instanceof BacktankItem
        && backTank.getItem().isFireResistant()
        && Temperature.get(player, Temperature.Trait.WORLD) > Temperature.get(player, Temperature.Trait.BURNING_POINT))
        {
            // Ensure player is wearing a full set of fire-resistant armor
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!helmet.getItem().isFireResistant() || !(helmet.getItem() instanceof DivingHelmetItem)) return;
            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
            if (!boots.getItem().isFireResistant()) return;
            ItemStack pants = player.getItemBySlot(EquipmentSlot.LEGS);
            if (!pants.getItem().isFireResistant()) return;

            if (player.level().isClientSide)
                USING_BACKTANK = true;

            if (CSMath.getIfNotNull(backTank.getTag(), tag -> tag.getInt("Air"), 0) > 0)
            {   // Drain air
                BacktankUtil.consumeAir(player, backTank, 1);
                //Update backtank air status
                if (player.level().isClientSide)
                {
                    player.getPersistentData().putInt("VisualBacktankAir", Math.round(BacktankUtil.getAllWithAir(player).stream()
                                                                                              .map(BacktankUtil::getAir)
                                                                                              .reduce(0f, Float::sum)));
                }
            }
        }
        else if (player.level().isClientSide)
        {   USING_BACKTANK = false;
        }
    }*/

    @EventBusSubscriber(modid = ColdSweat.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents
    {
        @SubscribeEvent
        public static void setupModEvents(FMLCommonSetupEvent event)
        {
            // TODO: Implement when Create is updated
            /*if (isCreateLoaded())
            {
                ColdSweatDisplayBehaviors.THERMOLITH = AllDisplayBehaviours.register(new ResourceLocation(ColdSweat.MOD_ID, "thermolith"), new ColdSweatDisplayBehaviors.Thermolith());
                AllDisplayBehaviours.assignBlock(ColdSweatDisplayBehaviors.THERMOLITH, ModBlocks.THERMOLITH);
            }*/
        }
    }
}
