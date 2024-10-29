package com.momosoftworks.coldsweat.util.compat;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.core.init.BlockInit;
import com.momosoftworks.coldsweat.util.compat.create.ColdSweatDisplayBehaviors;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModBlocks;
import com.momosoftworks.coldsweat.util.registries.ModDamageSources;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import com.simibubi.create.content.equipment.armor.BacktankItem;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.equipment.armor.DivingHelmetItem;
import com.simibubi.create.content.redstone.displayLink.AllDisplayBehaviours;
import com.simibubi.create.foundation.ponder.PonderRegistry;
import com.simibubi.create.infrastructure.ponder.AllPonderTags;
import de.teamlapen.werewolves.entities.player.werewolf.WerewolfPlayer;
import dev.ghen.thirst.api.ThirstHelper;
import dev.ghen.thirst.content.purity.ContainerWithPurity;
import dev.ghen.thirst.content.purity.WaterPurity;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.jwaresoftware.mcmods.lib.api.combat.Armory;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import sereneseasons.season.SeasonHooks;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import weather2.ServerTickHandler;
import weather2.weathersystem.WeatherManagerServer;
import weather2.weathersystem.storm.StormObject;
import weather2.weathersystem.storm.WeatherObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mod.EventBusSubscriber
public class CompatManager
{
    private static final boolean BOP_LOADED = modLoaded("biomesoplenty");
    private static final boolean SEASONS_LOADED = modLoaded("sereneseasons");
    private static final boolean CURIOS_LOADED = modLoaded("curios");
    private static final boolean WEREWOLVES_LOADED = modLoaded("werewolves");
    private static final boolean SPIRIT_LOADED = modLoaded("spirit");
    private static final boolean ARMOR_UNDERWEAR_LOADED = modLoaded("armorunder");
    private static final boolean BYG_LOADED = modLoaded("byg");
    private static final boolean CREATE_LOADED = modLoaded("create", "0.5.1");
    private static final boolean ATMOSPHERIC_LOADED = modLoaded("atmospheric");
    private static final boolean ENVIRONMENTAL_LOADED = modLoaded("environmental");
    private static final boolean TERRALITH_LOADED = modLoaded("terralith");
    private static final boolean WEATHER_LOADED = modLoaded("weather2");
    private static final boolean WYTHERS_LOADED = modLoaded("wwoo");
    private static final boolean TOOLTIPS_LOADED = modLoaded("legendarytooltips");
    private static final boolean PRIMAL_WINTER_LOADED = modLoaded("primalwinter");
    private static final boolean THIRST_LOADED = modLoaded("thirst", "1.18.2-1.3.8");
    private static final boolean ICEBERG_LOADED = modLoaded("iceberg");
    private static final boolean SPOILED_LOADED = modLoaded("spoiled");
    private static final boolean SUPPLEMENTARIES_LOADED = modLoaded("supplementaries");
    private static final boolean VALKYRIEN_SKIES_LOADED = modLoaded("valkyrienskies");

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
    public static boolean isArmorUnderwearLoaded()
    {   return ARMOR_UNDERWEAR_LOADED;
    }
    public static boolean isBiomesYoullGoLoaded()
    {   return BYG_LOADED;
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
    public static boolean isValkyrienSkiesLoaded()
    {   return VALKYRIEN_SKIES_LOADED;
    }

    public static boolean hasCurio(Player player, Item curio)
    {   return CURIOS_LOADED && getCurios(player).stream().map(ItemStack::getItem).anyMatch(item -> item == curio);
    }

    public static List<ItemStack> getCurios(LivingEntity entity)
    {
        if (!CURIOS_LOADED) return new ArrayList<>();
        return entity.getCapability(CuriosCapability.INVENTORY)
                     .map(handler -> handler.getCurios().values()).stream().flatMap(Collection::stream)
                     .map(ICurioStacksHandler::getStacks)
                     .map(stacks ->
                     {
                         List<ItemStack> list = new ArrayList<>();
                         for (int i = 0; i < stacks.getSlots(); i++)
                         {   list.add(stacks.getStackInSlot(i));
                         }
                         return list;
                     }).flatMap(List::stream).toList();
    }

    public static boolean hasOzzyLiner(ItemStack stack)
    {
        return ARMOR_UNDERWEAR_LOADED && Armory.getXLining(stack).has(Armory.XLining.TEMPERATURE_REGULATOR);
    }
    public static boolean hasOttoLiner(ItemStack stack)
    {
        return ARMOR_UNDERWEAR_LOADED && Armory.getXLining(stack).has(Armory.XLining.ANTIFREEZE_SHIELD);
    }
    public static boolean hasOllieLiner(ItemStack stack)
    {
        return ARMOR_UNDERWEAR_LOADED && Armory.getXLining(stack).has(Armory.XLining.ANTIBURN_SHIELD);
    }

    public static boolean isWerewolf(Player player)
    {
        return WEREWOLVES_LOADED && WerewolfPlayer.getOpt(player).filter(w -> w.getLevel() > 0).map(w -> w.getForm().isTransformed()).orElse(false);
    }

    public static boolean isRainstormAt(Level level, BlockPos pos)
    {
        if (WEATHER_LOADED)
        {
            WeatherManagerServer weatherManager = ServerTickHandler.getWeatherManagerFor(level.dimension());
            if (weatherManager == null) return false;
            StormObject rainStorm = weatherManager.getClosestStormAny(new Vec3(pos.getX(), pos.getY(), pos.getZ()), 250);
            if (rainStorm == null) return false;

            if (WorldHelper.canSeeSky(level, pos, 60) && rainStorm.isPrecipitating() && rainStorm.levelTemperature > 0.0f
            && Math.sqrt(Math.pow(pos.getX() - rainStorm.pos.x, 2) + Math.pow(pos.getX() - rainStorm.pos.x, 2)) < rainStorm.getSize())
            {   return true;
            }
        }
        return false;
    }

    public static Object getClosestStorm(Level level, BlockPos pos)
    {
        if (WEATHER_LOADED)
        {
            WeatherManagerServer weatherManager = ServerTickHandler.getWeatherManagerFor(level.dimension());
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
            return closestStorm;
        }
        return null;
    }

    public static boolean isColdEnoughToSnow(Level level, BlockPos pos)
    {
        return SEASONS_LOADED && SeasonHooks.coldEnoughToSnowHook(level.getBiome(pos).value(), pos, level);
    }

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
        {   return WaterPurity.getPurity(stack);
        }
        return 0;
    }

    public static ItemStack setWaterPurity(ItemStack stack, int purity)
    {
        if (THIRST_LOADED)
        {   return WaterPurity.addPurity(stack, purity);
        }
        return stack;
    }

    public static ItemStack setWaterPurity(ItemStack item, BlockPos pos, Level level)
    {
        if (THIRST_LOADED)
        {   return WaterPurity.addPurity(item, pos, level);
        }
        return item;
    }

    /* Compat Events */

    public static void registerEventHandlers()
    {
        if (CURIOS_LOADED)
        {
            MinecraftForge.EVENT_BUS.register(new Object()
            {
                @SubscribeEvent
                public void onCurioChange(CurioChangeEvent event)
                {
                    EntityTempManager.updateInsulationAttributeModifiers(event.getEntityLiving());
                }
            });
        }
    }

    @SubscribeEvent
    public static void onLivingTempDamage(LivingEvent event)
    {
        if (!(event instanceof LivingDamageEvent || event instanceof LivingAttackEvent)) return;
        // Armor Underwear compat
        if (ARMOR_UNDERWEAR_LOADED && !event.getEntityLiving().level.isClientSide)
        {
            // Get the damage source from the event (different methods for LivingDamage/LivingAttack)
            DamageSource source = event instanceof LivingDamageEvent
                                  ? ((LivingDamageEvent) event).getSource()
                                  : ((LivingAttackEvent) event).getSource();
            if (source == null) return;

            boolean isDamageCold;
            if (((isDamageCold = source == ModDamageSources.COLD) || source == ModDamageSources.HOT))
            {
                int liners = 0;
                for (ItemStack stack : event.getEntityLiving().getArmorSlots())
                {
                    if (isDamageCold ? hasOttoLiner(stack) : hasOllieLiner(stack))
                        liners++;
                }
                // Cancel the event if full liners
                if (liners >= 4)
                {   event.setCanceled(true);
                    return;
                }
                // Dampen the damage as the number of liners increases
                if (event instanceof LivingDamageEvent damageEvent)
                    damageEvent.setAmount(CSMath.blend(damageEvent.getAmount(), 0, liners, 0, 4));
            }
        }
    }

    public static boolean USING_BACKTANK = false;

    @SubscribeEvent
    public static void drainCreateBacktank(TickEvent.PlayerTickEvent event)
    {
        Player player = event.player;
        if (!CompatManager.isCreateLoaded()) return;
        ItemStack backTank = player.getItemBySlot(EquipmentSlot.CHEST);

        // Somehow this makes the indicator render. I have no idea
        if (USING_BACKTANK && player.level.isClientSide)
        {   player.getPersistentData().putInt("VisualBacktankAir", Math.round(BacktankUtil.getAir(backTank)) - 1);
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

            if (player.level.isClientSide)
                USING_BACKTANK = true;

            if (CSMath.getIfNotNull(backTank.getTag(), tag -> tag.getInt("Air"), 0) > 0)
            {   // Drain air
                BacktankUtil.consumeAir(player, backTank, 1);
                //Update backtank air status
                if (player.level.isClientSide)
                {   player.getPersistentData().putInt("VisualBacktankAir", Math.round(BacktankUtil.getAir(backTank)) - 1);
                }
            }
        }
        else if (player.level.isClientSide)
        {   USING_BACKTANK = false;
        }
    }

    @Mod.EventBusSubscriber(modid = ColdSweat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents
    {
        @SubscribeEvent
        public static void addThirstDrinks(FMLCommonSetupEvent event)
        {
            if (isThirstLoaded())
            {   ThirstHelper.addDrink(ModItems.FILLED_WATERSKIN, 6, 12);
                WaterPurity.addContainer(new ContainerWithPurity(ModItems.WATERSKIN.getDefaultInstance(),
                                                                 ModItems.FILLED_WATERSKIN.getDefaultInstance()));
            }
            if (isCreateLoaded())
            {
                new Object()
                {
                    public void registerDisplayBehaviors()
                    {
                        ColdSweatDisplayBehaviors.THERMOLITH = AllDisplayBehaviours.register(new ResourceLocation(ColdSweat.MOD_ID, "thermolith"), new ColdSweatDisplayBehaviors.Thermolith());
                        AllDisplayBehaviours.assignBlock(ColdSweatDisplayBehaviors.THERMOLITH, ModBlocks.THERMOLITH);
                    }
                }.registerDisplayBehaviors();
            }
        }

        @SubscribeEvent
        public static void setupModClientEvents(FMLClientSetupEvent event)
        {
            if (isCreateLoaded())
            {
                new Object()
                {
                    public void registerPonderTags()
                    {
                        PonderRegistry.TAGS.forTag(AllPonderTags.DISPLAY_SOURCES).add(BlockInit.THERMOLITH.get());
                    }
                }.registerPonderTags();
            }
        }
    }
}
