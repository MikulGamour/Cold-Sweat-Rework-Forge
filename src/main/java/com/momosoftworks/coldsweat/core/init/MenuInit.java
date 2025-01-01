package com.momosoftworks.coldsweat.core.init;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.common.container.*;
import net.minecraftforge.registries.RegistryObject;

public class MenuInit
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.CONTAINERS, ColdSweat.MOD_ID);

    public static final RegistryObject<MenuType<BoilerContainer>> BOILER_CONTAINER_TYPE =
            MENU_TYPES.register("boiler", () -> IForgeMenuType.create(BoilerContainer::new));

    public static final RegistryObject<MenuType<IceboxContainer>> ICEBOX_CONTAINER_TYPE =
        MENU_TYPES.register("ice_box", () -> IForgeMenuType.create(IceboxContainer::new));

    public static final RegistryObject<MenuType<SewingContainer>> SEWING_CONTAINER_TYPE =
        MENU_TYPES.register("sewing_table", () -> IForgeMenuType.create(SewingContainer::new));

    public static final RegistryObject<MenuType<HearthContainer>> HEARTH_CONTAINER_TYPE =
        MENU_TYPES.register("hearth", () -> IForgeMenuType.create(HearthContainer::new));
}
