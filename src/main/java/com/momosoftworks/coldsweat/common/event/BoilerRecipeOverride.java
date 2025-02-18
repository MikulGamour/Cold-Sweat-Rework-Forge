package com.momosoftworks.coldsweat.common.event;

import com.momosoftworks.coldsweat.ColdSweat;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.container.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

@Mod.EventBusSubscriber
public class BoilerRecipeOverride
{
    @SubscribeEvent
    public static void onCraftingTableOpen(PlayerContainerEvent.Open event)
    {
        if (event.getContainer() instanceof RecipeBookContainer<?>
        && ((RecipeBookContainer<?>) event.getContainer()).getGridWidth() == 3
        && ((RecipeBookContainer<?>) event.getContainer()).getGridHeight() == 3)
        {
            RecipeBookContainer<?> crafting = (RecipeBookContainer<?>) event.getContainer();
            MinecraftServer server = event.getEntity().getServer();
            if (server == null) return;
            IRecipe<CraftingInventory> boilerRecipe = (IRecipe) server.getRecipeManager().byKey(new ResourceLocation(ColdSweat.MOD_ID, "boiler")).orElse(null);
            if (boilerRecipe == null) return;
            
            crafting.addSlotListener(new IContainerListener()
            {
                @Override
                public void slotChanged(Container sendingContainer, int slotId, ItemStack stack)
                {
                    Slot slot = sendingContainer.getSlot(slotId);

                    if (slot instanceof CraftingResultSlot)
                    {
                        if (crafting.recipeMatches((IRecipe) boilerRecipe))
                        {   slot.set(boilerRecipe.assemble(getCraftingContainer(slot)));
                        }
                    }
                }

                @Override
                public void setContainerData(Container pContainerMenu, int pDataSlotIndex, int pValue)
                {}
                @Override
                public void refreshContainer(Container pContainerToSend, NonNullList<ItemStack> pItemsList)
                {}
            });
        }
    }

    private static final Field SLOT_CRAFT_CONTAINER = ObfuscationReflectionHelper.findField(CraftingResultSlot.class, "field_75239_a");
    static
    {   SLOT_CRAFT_CONTAINER.setAccessible(true);
    }

    @Nullable
    private static CraftingInventory getCraftingContainer(Slot slot)
    {
        try
        {   return (CraftingInventory) SLOT_CRAFT_CONTAINER.get(slot);
        }
        catch (IllegalAccessException e)
        {   ColdSweat.LOGGER.error("Failed to get crafting container from ResultSlot", e);
            return null;
        }
    }
}
