package com.momosoftworks.coldsweat.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

public class KubePlugin implements KubeJSPlugin
{
    @Override
    public void registerEvents(EventGroupRegistry registry)
    {
        registry.register(KubeEventHandlers.COLD_SWEAT);
    }

    @Override
    public void registerBindings(BindingRegistry bindings)
    {
        bindings.add("coldsweat", new KubeBindings());
    }
}
