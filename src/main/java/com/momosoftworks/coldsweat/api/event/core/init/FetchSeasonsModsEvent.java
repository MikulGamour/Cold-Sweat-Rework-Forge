package com.momosoftworks.coldsweat.api.event.core.init;

import com.google.common.collect.ImmutableList;
import net.neoforged.bus.api.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Gathers a list of mods that handle seasons. Add your mod's ID to this list if you want Cold Sweat to load seasonal temperature configs for you.<br>
 * <br>
 * Subscribe to this event with priority LOWEST to view the entire list of mods.<br>
 * Otherwise, subscribe with priority NORMAL (default).
 */
public class FetchSeasonsModsEvent extends Event
{
    private final List<String> seasonsMods = new ArrayList<>();

    public FetchSeasonsModsEvent()
    {}

    /**
     * @return an IMMUTABLE list of seasons mods that are loaded so far
     */
    public List<String> getSeasonsMods()
    {   return ImmutableList.copyOf(seasonsMods);
    }

    /**
     *
     * @param mod
     */
    public void addSeasonsMod(String mod)
    {   seasonsMods.add(mod);
    }
}
