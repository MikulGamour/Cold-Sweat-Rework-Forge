package com.momosoftworks.coldsweat.client.event;

import com.mojang.datafixers.util.Either;
import com.momosoftworks.coldsweat.api.event.client.InsulatorTabBuildEvent;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.item.Item;
import net.minecraft.tags.ITag;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class FilterInsulationItemsTab
{
    @SubscribeEvent
    public static void filterItems(InsulatorTabBuildEvent event)
    {
        event.addCheck((item, insulator) ->
        {
            for (Either<ITag<Item>, Item> either : CSMath.listOrEmpty(insulator.data().items()))
            {
                if (either.left().map(tag -> tag.contains(item)).orElse(false))
                {
                    ITag<Item> tag = either.left().get();
                    int tagSize = tag.getValues().size();

                    if (tagSize > 6 && tag.getValues().stream().findFirst().get() != item)
                    {   return false;
                    }
                }
            }
            return true;
        });
    }
}
