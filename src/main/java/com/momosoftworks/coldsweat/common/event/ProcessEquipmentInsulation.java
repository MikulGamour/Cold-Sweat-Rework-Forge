package com.momosoftworks.coldsweat.common.event;

import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.api.event.common.insulation.InsulationTickEvent;
import com.momosoftworks.coldsweat.api.insulation.AdaptiveInsulation;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.insulation.StaticInsulation;
import com.momosoftworks.coldsweat.api.temperature.modifier.ArmorInsulationTempModifier;
import com.momosoftworks.coldsweat.api.util.Placement;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.insulation.IInsulatableCap;
import com.momosoftworks.coldsweat.common.capability.insulation.ItemInsulationCap;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.data.codec.configuration.InsulatorData;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.FastMap;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.advancements.Advancement;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber
public class ProcessEquipmentInsulation
{
    @SubscribeEvent
    public static void applyArmorInsulation(TickEvent.PlayerTickEvent event)
    {
        PlayerEntity player = event.player;
        if (event.phase == TickEvent.Phase.END && player instanceof ServerPlayerEntity && player.tickCount % 20 == 0)
        {
            ServerPlayerEntity serverPlayer = ((ServerPlayerEntity) player);
            int fullyInsulatedSlots = 0;
            Map<String, Double> armorInsulation = new FastMap<>();

            double worldTemp = Temperature.get(player, Temperature.Trait.WORLD);
            double minTemp = Temperature.get(player, Temperature.Trait.FREEZING_POINT);
            double maxTemp = Temperature.get(player, Temperature.Trait.BURNING_POINT);

            for (ItemStack armorStack : player.getArmorSlots())
            {
                if (armorStack.getItem() instanceof ArmorItem)
                {
                    // Add the armor's intrinsic insulation value (defined in configs)
                    // Mutually exclusive with Sewing Table insulation
                    Collection<InsulatorData> armorInsulators = ConfigSettings.INSULATING_ARMORS.get().get(armorStack.getItem());
                    if (!armorInsulators.isEmpty())
                    {
                        for (InsulatorData armorInsulator : armorInsulators)
                        {
                            // Check if the player meets the predicate for the insulation
                            if (!armorInsulator.test(player, armorStack))
                            {   continue;
                            }
                            mapAdd(armorInsulation, "cold_armor", armorInsulator.insulation().getCold());
                            mapAdd(armorInsulation, "heat_armor", armorInsulator.insulation().getHeat());
                        }
                    }
                    else
                    {   // Add the armor's insulation value from the Sewing Table
                        LazyOptional<IInsulatableCap> iCap = ItemInsulationManager.getInsulationCap(armorStack);
                        List<Insulation> insulation = ItemInsulationManager.getAllEffectiveInsulation(armorStack, player);

                        // Get the armor's insulation values
                        for (Insulation value : insulation)
                        {
                            if (value instanceof StaticInsulation)
                            {
                                mapAdd(armorInsulation, "cold_insulators", ((StaticInsulation) value).getCold());
                                mapAdd(armorInsulation, "heat_insulators", ((StaticInsulation) value).getHeat());
                            }
                            else if (value instanceof AdaptiveInsulation)
                            {
                                mapAdd(armorInsulation, "cold_insulators", CSMath.blend(((AdaptiveInsulation) value).getInsulation() * 0.75, 0, ((AdaptiveInsulation) value).getFactor(), -1, 1));
                                mapAdd(armorInsulation, "heat_insulators", CSMath.blend(0, ((AdaptiveInsulation) value).getInsulation() * 0.75, ((AdaptiveInsulation) value).getFactor(), -1, 1));
                            }
                        }

                        // Used for tracking "fully_insulated" advancement
                        if ((armorInsulation.get("cold_insulators") + armorInsulation.get("heat_insulators")) / 2 >= ItemInsulationManager.getInsulationSlots(armorStack))
                        {   fullyInsulatedSlots++;
                        }

                        if (iCap.resolve().isPresent() && iCap.resolve().get() instanceof ItemInsulationCap)
                        {
                            ItemInsulationCap cap = ((ItemInsulationCap) iCap.resolve().get());
                            // Calculate adaptive insulation adaptation state
                            cap.calcAdaptiveInsulation(worldTemp, minTemp, maxTemp);

                            // Remove insulation items if the player has too many
                            List<Pair<ItemStack, Multimap<InsulatorData, Insulation>>> totalInsulation = cap.getInsulation();
                            int filledInsulationSlots = (int) totalInsulation.stream().map(Pair::getSecond).flatMap(map -> map.values().stream()).map(Insulation::split).flatMap(List::stream).count();
                            if (filledInsulationSlots > ItemInsulationManager.getInsulationSlots(armorStack))
                            {   WorldHelper.playEntitySound(SoundEvents.ITEM_FRAME_REMOVE_ITEM, player, SoundCategory.PLAYERS, 1.0F, 1.0F);
                            }
                            while (filledInsulationSlots > ItemInsulationManager.getInsulationSlots(armorStack))
                            {
                                ItemStack removedItem = cap.removeInsulationItem(totalInsulation.get(totalInsulation.size() - 1).getFirst());
                                ItemEntity droppedInsulation = new ItemEntity(player.level, player.getX(), player.getY() + player.getBbHeight() / 2, player.getZ(), removedItem);
                                droppedInsulation.setPickUpDelay(8);
                                droppedInsulation.setDeltaMovement(new Vector3d(player.getRandom().nextGaussian() * 0.05,
                                                                                player.getRandom().nextGaussian() * 0.05 + 0.2,
                                                                                player.getRandom().nextGaussian() * 0.05));
                                player.level.addFreshEntity(droppedInsulation);

                                filledInsulationSlots--;
                            }
                        }
                    }

                    // Add the armor's defense value to the insulation value.
                    double armorAmount = armorStack.getAttributeModifiers(MobEntity.getEquipmentSlotForItem(armorStack)).entries()
                                         .stream().filter(entry -> entry.getKey().equals(Attributes.ARMOR))
                                         .map(entry -> entry.getValue().getAmount())
                                         .mapToDouble(Double::doubleValue).sum();
                    mapAdd(armorInsulation, "cold_protection", Math.min(armorAmount, 20));
                    mapAdd(armorInsulation, "heat_protection", Math.min(armorAmount, 20));
                }
            }

            /* Get insulation from curios */

            for (ItemStack curio : CompatManager.Curios.getCurios(player))
            {
                for (InsulatorData insulator : ConfigSettings.INSULATING_CURIOS.get().get(curio.getItem()))
                {
                    if (insulator.test(player, curio))
                    {
                        mapAdd(armorInsulation, "cold_curios", insulator.insulation().getCold());
                        mapAdd(armorInsulation, "heat_curios", insulator.insulation().getHeat());
                    }
                }
            }

            InsulationTickEvent insulationEvent = new InsulationTickEvent(player, armorInsulation);
            MinecraftForge.EVENT_BUS.post(insulationEvent);
            if (!insulationEvent.isCanceled())
            {
                double cold = insulationEvent.getProperty("cold");
                double heat = insulationEvent.getProperty("heat");

                Temperature.addOrReplaceModifier(player, new ArmorInsulationTempModifier(cold, heat).tickRate(20).expires(20), Temperature.Trait.RATE, Placement.Duplicates.BY_CLASS);
            }

            // Award advancement for full insulation
            if (fullyInsulatedSlots >= 4)
            {
                if (serverPlayer.getServer() != null)
                {
                    Advancement advancement = serverPlayer.getServer().getAdvancements().getAdvancement(new ResourceLocation("cold_sweat:full_insulation"));
                    if (advancement != null)
                    {   serverPlayer.getAdvancements().award(advancement, "requirement");
                    }
                }
            }
        }
    }

    private static void mapAdd(Map<String, Double> map, String key, double value)
    {
        map.put(key, map.getOrDefault(key, 0d) + value);
    }

    /**
     * Prevent damage by magma blocks if the player has hoglin hooves
     */
    @SubscribeEvent
    public static void onDamageTaken(LivingAttackEvent event)
    {
        DamageSource source = event.getSource();
        if (source == DamageSource.HOT_FLOOR && event.getEntityLiving().getItemBySlot(EquipmentSlotType.FEET).getItem() == ModItems.HOGLIN_HOOVES)
        {   event.setCanceled(true);
        }
    }
}
