package com.momosoftworks.coldsweat.client.event;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.client.gui.Overlays;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.common.item.FilledWaterskinItem;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.init.ItemInit;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RegisterItemOverrides
{
    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event)
    {
        event.enqueueWork(() ->
        {
            ItemModelsProperties.register(ItemInit.SOULSPRING_LAMP.get(), new ResourceLocation(ColdSweat.MOD_ID, "soulspring_state"), (stack, level, entity) ->
            {
                CompoundNBT tag = stack.getOrCreateTag();
                if (tag.getBoolean("Lit"))
                {
                    return tag.getInt("Fuel") > 43 ? 3 :
                           tag.getInt("Fuel") > 22 ? 2 : 1;
                }
                return 0;
            });

            ItemModelsProperties.register(ItemInit.FILLED_WATERSKIN.get(), new ResourceLocation(ColdSweat.MOD_ID, "water_temperature"), (stack, level, entity) ->
            {
                return stack.getOrCreateTag().getFloat(FilledWaterskinItem.NBT_TEMPERATURE);
            });

            ItemModelsProperties.register(ItemInit.THERMOMETER.get(), new ResourceLocation(ColdSweat.MOD_ID, "temperature"), (stack, level, livingEntity) ->
            {
                Entity entity = (livingEntity != null ? livingEntity : stack.getEntityRepresentation());
                if (entity != null)
                {
                    double minTemp = livingEntity != null ? Temperature.get(livingEntity, Temperature.Trait.FREEZING_POINT) : ConfigSettings.MIN_TEMP.get();
                    double maxTemp = livingEntity != null ? Temperature.get(livingEntity, Temperature.Trait.BURNING_POINT)  : ConfigSettings.MAX_TEMP.get();

                    double worldTemp;
                    if (!entity.getPersistentData().contains("WorldTempTimestamp")
                    || (entity.tickCount % 20 == 0 || (entity instanceof PlayerEntity && entity.tickCount % 2 == 0)) && entity.getPersistentData().getInt("WorldTempTimestamp") != entity.tickCount)
                    {
                        worldTemp = entity instanceof LivingEntity
                                ? EntityTempManager.getTemperatureCap(entity).map(cap -> cap.getTrait(Temperature.Trait.WORLD)).orElse(0.0)
                                : WorldHelper.getTemperatureAt(entity.level, entity.blockPosition());

                        entity.getPersistentData().putDouble("WorldTemp", worldTemp);
                        entity.getPersistentData().putInt("WorldTempTimestamp", entity.tickCount);
                    }
                    else worldTemp = entity.getPersistentData().getDouble("WorldTemp");

                    if (entity instanceof ItemFrameEntity)
                    {
                        ItemFrameEntity frame = (ItemFrameEntity) entity;
                        if (Minecraft.getInstance().getEntityRenderDispatcher().crosshairPickEntity == frame)
                        {
                            boolean celsius = ConfigSettings.CELSIUS.get();
                            TextFormatting tempColor;
                            switch (Overlays.getGaugeSeverity(worldTemp, minTemp, maxTemp))
                            {
                                case 0 : tempColor = TextFormatting.WHITE; break;
                                case 2 : case 3 : tempColor = TextFormatting.GOLD; break;
                                case 4 : tempColor = TextFormatting.RED; break;
                                case -2 : case -3 : tempColor = TextFormatting.AQUA; break;
                                case -4 : tempColor = TextFormatting.BLUE; break;
                                default : tempColor = TextFormatting.RESET; break;
                            };
                            int convertedTemp = (int) Temperature.convert(worldTemp, Temperature.Units.MC, celsius ? Temperature.Units.C : Temperature.Units.F, true) + ConfigSettings.TEMP_OFFSET.get();
                            frame.getItem().setHoverName(new StringTextComponent(convertedTemp + " " + (celsius ? Temperature.Units.C.getFormattedName()
                                                                                                          : Temperature.Units.F.getFormattedName())).withStyle(tempColor));
                        }
                    }

                    double worldTempAdjusted = Overlays.getWorldSeverity(worldTemp, minTemp, maxTemp) * 1.01;
                    return (float) worldTempAdjusted;
                }
                return 0;
            });
        });
    }
}
