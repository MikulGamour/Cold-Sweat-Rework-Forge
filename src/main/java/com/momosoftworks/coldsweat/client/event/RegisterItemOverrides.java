package com.momosoftworks.coldsweat.client.event;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.client.gui.Overlays;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.common.item.FilledWaterskinItem;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.init.ItemInit;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
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
            ItemProperties.register(ItemInit.SOULSPRING_LAMP.get(), new ResourceLocation(ColdSweat.MOD_ID, "soulspring_state"), (stack, level, entity, id) ->
            {
                CompoundTag tag = stack.getOrCreateTag();
                if (tag.getBoolean("Lit"))
                {
                    return tag.getInt("Fuel") > 43 ? 3 :
                           tag.getInt("Fuel") > 22 ? 2 : 1;
                }
                return 0;
            });

            ItemProperties.register(ItemInit.FILLED_WATERSKIN.get(), new ResourceLocation(ColdSweat.MOD_ID, "water_temperature"), (stack, level, entity, id) ->
            {
                return stack.getOrCreateTag().getFloat(FilledWaterskinItem.NBT_TEMPERATURE);
            });

            ItemProperties.register(ItemInit.THERMOMETER.get(), new ResourceLocation(ColdSweat.MOD_ID, "temperature"), (stack, level, livingEntity, id) ->
            {
                Entity entity = (livingEntity != null ? livingEntity : stack.getEntityRepresentation());
                if (entity != null)
                {
                    double minTemp = livingEntity != null ? Temperature.get(livingEntity, Temperature.Trait.FREEZING_POINT) : ConfigSettings.MIN_TEMP.get();
                    double maxTemp = livingEntity != null ? Temperature.get(livingEntity, Temperature.Trait.BURNING_POINT)  : ConfigSettings.MAX_TEMP.get();

                    double worldTemp;
                    if (!entity.getPersistentData().contains("WorldTempTimestamp")
                    || (entity.tickCount % 20 == 0 || (entity instanceof Player && entity.tickCount % 2 == 0)) && entity.getPersistentData().getInt("WorldTempTimestamp") != entity.tickCount)
                    {
                        worldTemp = entity instanceof LivingEntity living
                                ? EntityTempManager.getTemperatureCap(living).map(cap -> cap.getTrait(Temperature.Trait.WORLD)).orElse(0.0)
                                : WorldHelper.getTemperatureAt(entity.level, entity.blockPosition());

                        entity.getPersistentData().putDouble("WorldTemp", worldTemp);
                        entity.getPersistentData().putInt("WorldTempTimestamp", entity.tickCount);
                    }
                    else worldTemp = entity.getPersistentData().getDouble("WorldTemp");

                    if (entity instanceof ItemFrame frame)
                    {
                        if (Minecraft.getInstance().getEntityRenderDispatcher().crosshairPickEntity == frame)
                        {
                            boolean celsius = ConfigSettings.CELSIUS.get();
                            ChatFormatting tempColor = switch (Overlays.getGaugeSeverity(worldTemp, minTemp, maxTemp))
                            {
                                case 0 -> ChatFormatting.WHITE;
                                case 2,3 -> ChatFormatting.GOLD;
                                case 4 -> ChatFormatting.RED;
                                case -2,-3 -> ChatFormatting.AQUA;
                                case -4 -> ChatFormatting.BLUE;
                                default -> ChatFormatting.RESET;
                            };
                            int convertedTemp = (int) Temperature.convert(worldTemp, Temperature.Units.MC, celsius ? Temperature.Units.C : Temperature.Units.F, true) + ConfigSettings.TEMP_OFFSET.get();
                            frame.getItem().setHoverName(new TextComponent(convertedTemp + " " + (celsius ? Temperature.Units.C.getFormattedName()
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
