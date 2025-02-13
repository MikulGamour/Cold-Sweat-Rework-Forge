package com.momosoftworks.coldsweat.common.event;

import com.momosoftworks.coldsweat.ColdSweat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

@Mod.EventBusSubscriber
public class BoilerRecipeOverride
{
    @SubscribeEvent
    public static void onCraftingTableOpen(PlayerContainerEvent.Open event)
    {
        if (event.getContainer() instanceof RecipeBookMenu<?> crafting
        && crafting.getGridWidth() == 3 && crafting.getGridHeight() == 3)
        {
            MinecraftServer server = event.getEntity().getServer();
            if (server == null) return;
            Recipe<Container> boilerRecipe = (Recipe) server.getRecipeManager().byKey(new ResourceLocation(ColdSweat.MOD_ID, "boiler")).orElse(null);
            if (boilerRecipe == null) return;
            
            crafting.addSlotListener(new ContainerListener()
            {
                @Override
                public void slotChanged(AbstractContainerMenu sendingContainer, int slotId, ItemStack stack)
                {
                    Slot slot = sendingContainer.getSlot(slotId);

                    if (slot instanceof ResultSlot resultSlot)
                    {
                        if (crafting.recipeMatches(boilerRecipe))
                        {   slot.set(boilerRecipe.assemble(getCraftingContainer(resultSlot)));
                        }
                    }
                }

                @Override
                public void dataChanged(AbstractContainerMenu pContainerMenu, int pDataSlotIndex, int pValue)
                {}
            });
        }
    }

    private static final Field SLOT_CRAFT_CONTAINER = ObfuscationReflectionHelper.findField(ResultSlot.class, "f_40162_");
    static
    {   SLOT_CRAFT_CONTAINER.setAccessible(true);
    }

    @Nullable
    private static CraftingContainer getCraftingContainer(ResultSlot slot)
    {
        try
        {   return (CraftingContainer) SLOT_CRAFT_CONTAINER.get(slot);
        }
        catch (IllegalAccessException e)
        {   ColdSweat.LOGGER.error("Failed to get crafting container from ResultSlot", e);
            return null;
        }
    }
}
