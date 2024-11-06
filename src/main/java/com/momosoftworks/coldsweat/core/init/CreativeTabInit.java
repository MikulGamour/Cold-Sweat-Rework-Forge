package com.momosoftworks.coldsweat.core.init;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.config.type.Insulator;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import com.momosoftworks.coldsweat.util.serialization.ObjectBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.*;

public class CreativeTabInit
{
    public static final DeferredRegister<CreativeModeTab> ITEM_GROUPS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ColdSweat.MOD_ID);

    public static RegistryObject<CreativeModeTab> COLD_SWEAT_TAB = ITEM_GROUPS.register("cold_sweat", () -> CreativeModeTab.builder()
            .icon(() -> ModItems.FILLED_WATERSKIN.getDefaultInstance())
            .displayItems((params, list) ->
            {
                list.acceptAll(List.of(
                        ModItems.WATERSKIN.getDefaultInstance(),
                        ObjectBuilder.build(() ->
                        {   ItemStack stack = ModItems.FILLED_WATERSKIN.getDefaultInstance();
                            stack = CompatManager.setWaterPurity(stack, 3);
                            return stack;
                        }),
                        ModItems.FUR.getDefaultInstance(),
                        ModItems.HOGLIN_HIDE.getDefaultInstance(),
                        ModItems.CHAMELEON_MOLT.getDefaultInstance(),
                        ModItems.MINECART_INSULATION.getDefaultInstance(),
                        ModItems.INSULATED_MINECART.getDefaultInstance(),
                        ObjectBuilder.build(() ->
                        {   ItemStack stack = ModItems.SOULSPRING_LAMP.getDefaultInstance();
                            stack.getOrCreateTag().putBoolean("Lit", true);
                            stack.getOrCreateTag().putDouble("Fuel", 64);
                            return stack;
                        }),
                        ModItems.SOUL_SPROUT.getDefaultInstance(),
                        ModItems.THERMOMETER.getDefaultInstance(),
                        ModItems.THERMOLITH.getDefaultInstance(),
                        ModItems.HEARTH.getDefaultInstance(),
                        ModItems.BOILER.getDefaultInstance(),
                        ModItems.ICEBOX.getDefaultInstance(),
                        ModItems.SMOKESTACK.getDefaultInstance(),
                        ModItems.SEWING_TABLE.getDefaultInstance(),
                        ModItems.HOGLIN_HEADPIECE.getDefaultInstance(),
                        ModItems.HOGLIN_TUNIC.getDefaultInstance(),
                        ModItems.HOGLIN_TROUSERS.getDefaultInstance(),
                        ModItems.HOGLIN_HOOVES.getDefaultInstance(),
                        ModItems.FUR_CAP.getDefaultInstance(),
                        ModItems.FUR_PARKA.getDefaultInstance(),
                        ModItems.FUR_PANTS.getDefaultInstance(),
                        ModItems.FUR_BOOTS.getDefaultInstance(),
                        ModItems.CHAMELEON_SPAWN_EGG.getDefaultInstance()
                ));
            })
            .title(Component.translatable("itemGroup.cold_sweat"))
            .build());

    public static RegistryObject<CreativeModeTab> INSULATION_ITEMS_TAB = ITEM_GROUPS.register("cs_insulation_items", () -> CreativeModeTab.builder()
            .icon(() -> ModItems.CHAMELEON_MOLT.getDefaultInstance())
            .displayItems((params, list) ->
            {
                list.acceptAll(sort(ConfigSettings.INSULATION_ITEMS.get().entries()));
                list.acceptAll(sort(ConfigSettings.INSULATING_ARMORS.get().entries()));
                list.acceptAll(sort(ConfigSettings.INSULATING_CURIOS.get().entries()));
            })
            .title(Component.translatable("itemGroup.cs_insulation_items"))
            .build());

    private static List<ItemStack> sort(Collection<Map.Entry<Item, Insulator>> items)
    {   List<Map.Entry<Item, Insulator>> list = new ArrayList<>(items);
        // Sort by name first
        list.sort(Comparator.comparing(item -> item.getKey().getDefaultInstance().getDisplayName().getString()));
        // Sort by tags the items are in
        list.sort(Comparator.comparing(item -> ForgeRegistries.ITEMS.tags().getReverseTag(item.getKey()).orElse(null).getTagKeys().sequential().map(tag -> tag.location().toString()).reduce("", (a, b) -> a + b)));
        // Sort by armor material and slot
        list.sort(Comparator.comparing(item -> item.getKey() instanceof ArmorItem armor
                                               ? armor.getMaterial().getName() + (3 - LivingEntity.getEquipmentSlotForItem(armor.getDefaultInstance()).getIndex())
                                               : ""));
        return list.stream().map(data -> new ItemStack(data.getKey(), 1, data.getValue().data().nbt().tag())).toList();
    }
}
