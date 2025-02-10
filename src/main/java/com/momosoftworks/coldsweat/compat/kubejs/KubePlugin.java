package com.momosoftworks.coldsweat.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import net.neoforged.neoforge.common.NeoForge;

public class KubePlugin implements KubeJSPlugin
{
    @Override
    public void registerEvents(EventGroupRegistry registry)
    {
        registry.register(KubeEventHandlers.COLD_SWEAT);
        NeoForge.EVENT_BUS.register(KubeEventHandlers.class);
    }

    @Override
    public void registerBindings(BindingRegistry bindings)
    {
        bindings.add("coldsweat", new KubeBindings());
    }
}
