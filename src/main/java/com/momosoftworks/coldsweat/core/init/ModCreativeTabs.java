package com.momosoftworks.coldsweat.core.init;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.event.client.InsulatorTabBuildEvent;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.data.codec.configuration.InsulatorData;
import com.momosoftworks.coldsweat.util.entity.DummyPlayer;
import com.momosoftworks.coldsweat.util.serialization.ObjectBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.*;
import java.util.stream.Stream;

public class ModCreativeTabs
{
    public static final DeferredRegister<CreativeModeTab> ITEM_GROUPS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ColdSweat.MOD_ID);

    public static DeferredHolder<CreativeModeTab, CreativeModeTab> COLD_SWEAT_TAB = ITEM_GROUPS.register("cold_sweat", () -> CreativeModeTab.builder()
            .icon(() -> ModItems.FILLED_WATERSKIN.value().getDefaultInstance())
            .displayItems((params, list) ->
            {
                list.acceptAll(List.of(
                        ModItems.WATERSKIN.value().getDefaultInstance(),
                        ObjectBuilder.build(() ->
                        {   ItemStack stack = ModItems.FILLED_WATERSKIN.value().getDefaultInstance();
                            stack = CompatManager.Thirst.setWaterPurity(stack, 3);
                            return stack;
                        }),
                        ModItems.GOAT_FUR.value().getDefaultInstance(),
                        ModItems.HOGLIN_HIDE.value().getDefaultInstance(),
                        ModItems.CHAMELEON_MOLT.value().getDefaultInstance(),
                        ModItems.MINECART_INSULATION.value().getDefaultInstance(),
                        ModItems.INSULATED_MINECART.value().getDefaultInstance(),
                        ObjectBuilder.build(() ->
                        {   ItemStack stack = ModItems.SOULSPRING_LAMP.value().getDefaultInstance();
                            stack.set(ModItemComponents.SOULSPRING_LAMP_LIT, true);
                            stack.set(ModItemComponents.SOULSPRING_LAMP_FUEL, 64d);
                            return stack;
                        }),
                        ModItems.SOUL_SPROUT.value().getDefaultInstance(),
                        ModItems.THERMOMETER.value().getDefaultInstance(),
                        ModItems.THERMOLITH.value().getDefaultInstance(),
                        ModItems.HEARTH.value().getDefaultInstance(),
                        ModItems.BOILER.value().getDefaultInstance(),
                        ModItems.ICEBOX.value().getDefaultInstance(),
                        ModItems.SMOKESTACK.value().getDefaultInstance(),
                        ModItems.SEWING_TABLE.value().getDefaultInstance(),
                        ModItems.HOGLIN_HEADPIECE.value().getDefaultInstance(),
                        ModItems.HOGLIN_TUNIC.value().getDefaultInstance(),
                        ModItems.HOGLIN_TROUSERS.value().getDefaultInstance(),
                        ModItems.HOGLIN_HOOVES.value().getDefaultInstance(),
                        ModItems.GOAT_FUR_CAP.value().getDefaultInstance(),
                        ModItems.GOAT_FUR_PARKA.value().getDefaultInstance(),
                        ModItems.GOAT_FUR_PANTS.value().getDefaultInstance(),
                        ModItems.GOAT_FUR_BOOTS.value().getDefaultInstance(),
                        ModItems.CHAMELEON_SPAWN_EGG.value().getDefaultInstance()
                ));
            })
            .title(Component.translatable("itemGroup.cold_sweat"))
            .build());

    public static DeferredHolder<CreativeModeTab, CreativeModeTab> INSULATION_ITEMS_TAB = ITEM_GROUPS.register("cs_insulation_items", () -> CreativeModeTab.builder()
            .icon(() -> ModItems.CHAMELEON_MOLT.value().getDefaultInstance())
            .displayItems((params, list) ->
            {
                List<ItemStack> allInsulators = new ArrayList<>();
                Stream.of(
                        sort(ConfigSettings.INSULATION_ITEMS.get().entries()),
                        sort(ConfigSettings.INSULATING_ARMORS.get().entries()),
                        sort(ConfigSettings.INSULATING_CURIOS.get().entries())
                ).flatMap(Collection::stream).forEach(stack ->
                {
                    if (allInsulators.stream().noneMatch(s -> s.getItem() == stack.getItem()))
                    {   allInsulators.add(stack);
                    }
                });

                list.acceptAll(allInsulators);
            })
            .title(Component.translatable("itemGroup.cs_insulation_items"))
            .build());

    private static List<ItemStack> sort(Collection<Map.Entry<Item, InsulatorData>> items)
    {
        List<Map.Entry<Item, InsulatorData>> list = new ArrayList<>(items);
        DummyPlayer slotGetter = new DummyPlayer();

        // Sort by tags the items are in
        list.sort(Comparator.comparing(entry -> entry.getKey().builtInRegistryHolder().tags().sequential().map(tag -> tag.location().toString()).reduce("", (a, b) -> a + b)));
        // Sort by insulation value
        list.sort(Comparator.comparingInt(entry -> entry.getValue().insulation().getCompareValue()));
        // Sort by armor material and slot
        list.sort(Comparator.comparing(entry -> entry.getKey() instanceof ArmorItem armor
                                               ? armor.getMaterial().getKey().location().toString() + (3 - slotGetter.getEquipmentSlotForItem(entry.getKey().getDefaultInstance()).getIndex())
                                               : ""));

        InsulatorTabBuildEvent event = new InsulatorTabBuildEvent(list);
        NeoForge.EVENT_BUS.post(event);

        return event.getItems().stream().map(entry ->
        {
            ItemStack stack = new ItemStack(entry.getKey());
            DataComponentMap components = entry.getValue().data().components().components();
            stack.applyComponents(components);
            return stack;
        }).toList();
    }
}
