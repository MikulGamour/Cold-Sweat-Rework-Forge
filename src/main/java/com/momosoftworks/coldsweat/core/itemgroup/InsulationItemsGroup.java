package com.momosoftworks.coldsweat.core.itemgroup;

import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.InsulatorData;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class InsulationItemsGroup extends CreativeModeTab
{
    public static final InsulationItemsGroup INSULATION_ITEMS = new InsulationItemsGroup("cs_insulation_items");
    public InsulationItemsGroup(String label)
    {   super(label);
    }

    @Override
    public ItemStack makeIcon()
    {   return new ItemStack(ModItems.CHAMELEON_MOLT);
    }

    public void register()
    {   return;
    }

    @Override
    public void fillItemList(@NotNull NonNullList<ItemStack> items)
    {
        // Spoof the item categories to allow items to be added to the tab
        List<List<ItemStack>> itemCategories = List.of(
                sort(ConfigSettings.INSULATION_ITEMS.get().entries()),
                sort(ConfigSettings.INSULATING_ARMORS.get().entries()),
                sort(ConfigSettings.INSULATING_CURIOS.get().entries())
        );

        for (List<ItemStack> category : itemCategories)
        {
            for (ItemStack stack : category)
            {
                // Make a dummy item list to get the result of the item's fillItemCategory() method
                NonNullList<ItemStack> dummyList = NonNullList.create();
                stack.getItem().fillItemCategory(stack.getItem().getItemCategory(), dummyList);

                // Skip if this item is already in the tab
                if (!dummyList.isEmpty() && items.stream().noneMatch(item -> item.sameItem(dummyList.get(0))))
                {   items.add(dummyList.get(0));
                }
            }
        }
    }

    private static List<ItemStack> sort(Collection<Map.Entry<Item, InsulatorData>> items)
    {   List<Map.Entry<Item, InsulatorData>> list = new ArrayList<>(items);
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
