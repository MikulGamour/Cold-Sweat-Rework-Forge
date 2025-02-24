package com.momosoftworks.coldsweat.core.init;

import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.common.block.*;
import com.momosoftworks.coldsweat.common.item.*;
import com.momosoftworks.coldsweat.util.registries.ModArmorMaterials;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemInit
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ColdSweat.MOD_ID);

    // Items
    public static final RegistryObject<Item> WATERSKIN = ITEMS.register("waterskin", WaterskinItem::new);
    public static final RegistryObject<Item> FILLED_WATERSKIN = ITEMS.register("filled_waterskin", FilledWaterskinItem::new);
    public static final RegistryObject<Item> MINECART_INSULATION = ITEMS.register("minecart_insulation", MinecartInsulationItem::new);
    public static final RegistryObject<Item> THERMOMETER = ITEMS.register("thermometer", () ->
            new ThermometerItem(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(1)));
    public static final RegistryObject<Item> SOULSPRING_LAMP = ITEMS.register("soulspring_lamp", SoulspringLampItem::new);
    public static final RegistryObject<Item> GOAT_FUR = ITEMS.register("goat_fur", () ->
            new Item(new Item.Properties()));
    public static final RegistryObject<Item> HOGLIN_HIDE = ITEMS.register("hoglin_hide", () ->
            new Item(new Item.Properties()));
    public static final RegistryObject<Item> INSULATED_MINECART = ITEMS.register("insulated_minecart", () ->
            new InsulatedMinecartItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CHAMELEON_MOLT = ITEMS.register("chameleon_molt", () ->
            new Item(new Item.Properties()));

    // Armor Items
    public static final RegistryObject<Item> HOGLIN_HEADPIECE = ITEMS.register("hoglin_headpiece", () ->
            new HoglinArmorItem(ModArmorMaterials.HOGLIN, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> HOGLIN_TUNIC = ITEMS.register("hoglin_tunic", () ->
            new HoglinArmorItem(ModArmorMaterials.HOGLIN, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> HOGLIN_TROUSERS = ITEMS.register("hoglin_trousers", () ->
            new HoglinArmorItem(ModArmorMaterials.HOGLIN, ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistryObject<Item> HOGLIN_HOOVES = ITEMS.register("hoglin_hooves", () ->
            new HoglinArmorItem(ModArmorMaterials.HOGLIN, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> GOAT_FUR_CAP = ITEMS.register("goat_fur_cap", () ->
            new GoatArmorItem(ModArmorMaterials.GOAT_FUR, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> GOAT_FUR_PARKA = ITEMS.register("goat_fur_parka", () ->
            new GoatArmorItem(ModArmorMaterials.GOAT_FUR, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> GOAT_FUR_PANTS = ITEMS.register("goat_fur_pants", () ->
            new GoatArmorItem(ModArmorMaterials.GOAT_FUR, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> GOAT_FUR_BOOTS = ITEMS.register("goat_fur_boots", () ->
            new GoatArmorItem(ModArmorMaterials.GOAT_FUR, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> CHAMELEON_HELMET = ITEMS.register("chameleon_scale_helmet", () ->
            new ChameleonArmorItem(ModArmorMaterials.CHAMELEON, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> CHAMELEON_CHESTPLATE = ITEMS.register("chameleon_scale_chestplate", () ->
            new ChameleonArmorItem(ModArmorMaterials.CHAMELEON, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> CHAMELEON_LEGGINGS = ITEMS.register("chameleon_scale_leggings", () ->
            new ChameleonArmorItem(ModArmorMaterials.CHAMELEON, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> CHAMELEON_BOOTS = ITEMS.register("chameleon_scale_boots", () ->
            new ChameleonArmorItem(ModArmorMaterials.CHAMELEON, ArmorItem.Type.BOOTS, new Item.Properties()));

    // Block Items
    public static final RegistryObject<BlockItem> BOILER = ITEMS.register("boiler", () -> new BlockItem(BlockInit.BOILER.get(), BoilerBlock.getItemProperties()));
    public static final RegistryObject<BlockItem> ICEBOX = ITEMS.register("icebox", () -> new BlockItem(BlockInit.ICEBOX.get(), IceboxBlock.getItemProperties()));
    public static final RegistryObject<BlockItem> SEWING_TABLE = ITEMS.register("sewing_table", () -> new BlockItem(BlockInit.SEWING_TABLE.get(), SewingTableBlock.getItemProperties()));
    public static final RegistryObject<BlockItem> HEARTH = ITEMS.register("hearth", () -> new BlockItem(BlockInit.HEARTH_BOTTOM.get(), HearthBottomBlock.getItemProperties()));
    public static final RegistryObject<BlockItem> THERMOLITH = ITEMS.register("thermolith", () -> new BlockItem(BlockInit.THERMOLITH.get(), ThermolithBlock.getItemProperties()));
    public static final RegistryObject<BlockItem> SOUL_SPROUT = ITEMS.register("soul_sprout", () -> new SoulSproutItem(BlockInit.SOUL_STALK.get(),
            SoulStalkBlock.getItemProperties().food(new FoodProperties.Builder().nutrition(3).saturationMod(0.5f).alwaysEat().fast().build())));
    public static final RegistryObject<BlockItem> SMOKESTACK = ITEMS.register("smokestack", () -> new BlockItem(BlockInit.SMOKESTACK.get(), SmokestackBlock.getItemProperties()));

    // Spawn Eggs
    public static final RegistryObject<ForgeSpawnEggItem> CHAMELEON_SPAWN_EGG = ITEMS.register("chameleon_spawn_egg", () ->
            new ForgeSpawnEggItem(EntityInit.CHAMELEON, 0x82C841, 0x1C9170, new Item.Properties()));
}
