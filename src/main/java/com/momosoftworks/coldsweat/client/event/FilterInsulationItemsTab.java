package com.momosoftworks.coldsweat.client.event;

import com.mojang.datafixers.util.Either;
import com.momosoftworks.coldsweat.api.event.client.InsulatorTabBuildEvent;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class FilterInsulationItemsTab
{
    @SubscribeEvent
    public static void filterItems(InsulatorTabBuildEvent event)
    {
        event.addCheck((item, insulator) ->
        {
            for (Either<TagKey<Item>, Item> either : CSMath.listOrEmpty(insulator.data().items()))
            {
                if (either.left().map(tag -> item.builtInRegistryHolder().is(tag)).orElse(false))
                {
                    TagKey<Item> tagKey = either.left().get();
                    HolderSet<Item> tag = BuiltInRegistries.ITEM.getTag(tagKey).orElse(null);
                    int tagSize = tag.size();

                    if (tagSize > 6 && tag.stream().findFirst().get().value() != item)
                    {   return false;
                    }
                }
            }
            return true;
        });
    }
}
