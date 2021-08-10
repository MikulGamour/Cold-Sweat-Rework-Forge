package net.momostudios.coldsweat.gui;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.momostudios.coldsweat.util.init.ModItems;

public class ColdSweatGroup extends ItemGroup
{
    public static final ColdSweatGroup COLD_SWEAT = new ColdSweatGroup(ItemGroup.GROUPS.length, "cold_sweat");
    public ColdSweatGroup(int index, String label)
    {
        super(index, label);
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(ModItems.FILLED_WATERSKIN.get());
    }
}
