package com.momosoftworks.coldsweat.common.event;

import com.momosoftworks.coldsweat.ColdSweat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

@EventBusSubscriber
public class BoilerRecipeOverride
{
    @SubscribeEvent
    public static void onCraftingTableOpen(PlayerContainerEvent.Open event)
    {
        if (event.getContainer() instanceof RecipeBookMenu<?, ?> crafting
        && crafting.getGridWidth() == 3 && crafting.getGridHeight() == 3)
        {
            MinecraftServer server = event.getEntity().getServer();
            if (server == null) return;
            RecipeHolder boilerRecipe = server.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "boiler")).orElse(null);
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
                        {
                            CraftingContainer craftSlots = getCraftingContainer(resultSlot);
                            if (craftSlots != null)
                            {   slot.set(boilerRecipe.value().assemble(craftSlots.asCraftInput(), server.registryAccess()));
                            }
                        }
                    }
                }

                @Override
                public void dataChanged(AbstractContainerMenu pContainerMenu, int pDataSlotIndex, int pValue)
                {}
            });
        }
    }

    private static final Field SLOT_CRAFT_CONTAINER = ObfuscationReflectionHelper.findField(ResultSlot.class, "craftSlots");
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
