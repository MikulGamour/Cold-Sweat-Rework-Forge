package com.momosoftworks.coldsweat.common.item;

import com.momosoftworks.coldsweat.client.event.RegisterModels;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class ChameleonArmorItem extends ArmorItem
{
    public ChameleonArmorItem(ArmorMaterial material, Type type, Properties properties)
    {   super(material, type, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer)
    {
        consumer.accept(new IClientItemExtensions()
        {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlot armorSlot, HumanoidModel<?> playerModel)
            {
                RegisterModels.checkForInitModels();
                if (entityLiving instanceof Player)
                {   return RegisterModels.EMPTY_ARMOR_MODEL; // Custom logic for player models
                }
                else return getRealArmorModel(entityLiving, itemStack, armorSlot);
            }
        });
    }

    public HumanoidModel<?> getRealArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlot armorSlot)
    {
        RegisterModels.checkForInitModels();
        return switch (armorSlot)
        {
            case HEAD -> RegisterModels.CHAMELEON_HELMET_MODEL;
            case CHEST -> RegisterModels.CHAMELEON_CHESTPLATE_MODEL;
            case LEGS -> RegisterModels.CHAMELEON_LEGGINGS_MODEL;
            case FEET -> RegisterModels.CHAMELEON_BOOTS_MODEL;
            default -> RegisterModels.EMPTY_ARMOR_MODEL;
        };
    }
}
