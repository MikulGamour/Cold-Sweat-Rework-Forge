package com.momosoftworks.coldsweat.client.event;

import com.momosoftworks.coldsweat.client.renderer.block.IceboxBlockEntityRenderer;
import com.momosoftworks.coldsweat.client.renderer.entity.ChameleonEntityRenderer;
import com.momosoftworks.coldsweat.client.renderer.layer.ChameleonArmorLayer;
import com.momosoftworks.coldsweat.client.renderer.model.armor.*;
import com.momosoftworks.coldsweat.client.renderer.model.entity.ChameleonModel;
import com.momosoftworks.coldsweat.core.init.EntityInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RegisterModels
{
    public static HoglinHeadpieceModel<?> HOGLIN_HEADPIECE_MODEL = null;
    public static HoglinTunicModel<?> HOGLIN_TUNIC_MODEL = null;
    public static HoglinHoovesModel<?> HOGLIN_HOOVES_MODEL = null;
    public static HoglinTrousersModel<?> HOGLIN_TROUSERS_MODEL = null;

    public static GoatCapModel<?> GOAT_CAP_MODEL = null;
    public static GoatParkaModel<?> GOAT_PARKA_MODEL = null;
    public static GoatPantsModel<?> GOAT_PANTS_MODEL = null;
    public static GoatBootsModel<?> GOAT_BOOTS_MODEL = null;

    public static ChameleonHelmetModel<?> CHAMELEON_HELMET_MODEL = null;
    public static ChameleonChestplateModel<?> CHAMELEON_CHESTPLATE_MODEL = null;
    public static ChameleonLeggingsModel<?> CHAMELEON_LEGGINGS_MODEL = null;
    public static ChameleonBootsModel<?> CHAMELEON_BOOTS_MODEL = null;

    public static EmptyArmorModel<?> EMPTY_ARMOR_MODEL = null;

    public static void checkForInitModels()
    {
        if (HOGLIN_HEADPIECE_MODEL != null) return;

        EntityModelSet mcModels = Minecraft.getInstance().getEntityModels();

        HOGLIN_HEADPIECE_MODEL = new HoglinHeadpieceModel<>(mcModels.bakeLayer(HoglinHeadpieceModel.LAYER_LOCATION));
        HOGLIN_TUNIC_MODEL = new HoglinTunicModel<>(mcModels.bakeLayer(HoglinTunicModel.LAYER_LOCATION));
        HOGLIN_HOOVES_MODEL = new HoglinHoovesModel<>(mcModels.bakeLayer(HoglinHoovesModel.LAYER_LOCATION));
        HOGLIN_TROUSERS_MODEL = new HoglinTrousersModel<>(mcModels.bakeLayer(HoglinTrousersModel.LAYER_LOCATION));

        GOAT_CAP_MODEL = new GoatCapModel<>(mcModels.bakeLayer(GoatCapModel.LAYER_LOCATION));
        GOAT_PARKA_MODEL = new GoatParkaModel<>(mcModels.bakeLayer(GoatParkaModel.LAYER_LOCATION));
        GOAT_PANTS_MODEL = new GoatPantsModel<>(mcModels.bakeLayer(GoatPantsModel.LAYER_LOCATION));
        GOAT_BOOTS_MODEL = new GoatBootsModel<>(mcModels.bakeLayer(GoatBootsModel.LAYER_LOCATION));

        CHAMELEON_HELMET_MODEL = new ChameleonHelmetModel<>(mcModels.bakeLayer(ChameleonHelmetModel.LAYER_LOCATION));
        CHAMELEON_CHESTPLATE_MODEL = new ChameleonChestplateModel<>(mcModels.bakeLayer(ChameleonChestplateModel.LAYER_LOCATION));
        CHAMELEON_LEGGINGS_MODEL = new ChameleonLeggingsModel<>(mcModels.bakeLayer(ChameleonLeggingsModel.LAYER_LOCATION));
        CHAMELEON_BOOTS_MODEL = new ChameleonBootsModel<>(mcModels.bakeLayer(ChameleonBootsModel.LAYER_LOCATION));

        EMPTY_ARMOR_MODEL = new EmptyArmorModel<>(mcModels.bakeLayer(EmptyArmorModel.LAYER_LOCATION));
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        event.registerLayerDefinition(ChameleonModel.LAYER_LOCATION, ChameleonModel::createBodyLayer);

        event.registerLayerDefinition(HoglinHeadpieceModel.LAYER_LOCATION, HoglinHeadpieceModel::createArmorLayer);
        event.registerLayerDefinition(HoglinTunicModel.LAYER_LOCATION, HoglinTunicModel::createArmorLayer);
        event.registerLayerDefinition(HoglinHoovesModel.LAYER_LOCATION, HoglinHoovesModel::createArmorLayer);
        event.registerLayerDefinition(HoglinTrousersModel.LAYER_LOCATION, HoglinTrousersModel::createArmorLayer);

        event.registerLayerDefinition(GoatCapModel.LAYER_LOCATION, GoatCapModel::createArmorLayer);
        event.registerLayerDefinition(GoatParkaModel.LAYER_LOCATION, GoatParkaModel::createArmorLayer);
        event.registerLayerDefinition(GoatPantsModel.LAYER_LOCATION, GoatPantsModel::createArmorLayer);
        event.registerLayerDefinition(GoatBootsModel.LAYER_LOCATION, GoatBootsModel::createArmorLayer);

        event.registerLayerDefinition(ChameleonHelmetModel.LAYER_LOCATION, ChameleonHelmetModel::createArmorLayer);
        event.registerLayerDefinition(ChameleonChestplateModel.LAYER_LOCATION, ChameleonChestplateModel::createArmorLayer);
        event.registerLayerDefinition(ChameleonLeggingsModel.LAYER_LOCATION, ChameleonLeggingsModel::createArmorLayer);
        event.registerLayerDefinition(ChameleonBootsModel.LAYER_LOCATION, ChameleonBootsModel::createArmorLayer);

        event.registerLayerDefinition(EmptyArmorModel.LAYER_LOCATION, EmptyArmorModel::createArmorLayer);

        event.registerLayerDefinition(IceboxBlockEntityRenderer.LAYER_LOCATION, IceboxBlockEntityRenderer::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(EntityInit.CHAMELEON.get(), ChameleonEntityRenderer::new);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (event.getSkin("default") instanceof PlayerRenderer playerRenderer)
        {   playerRenderer.addLayer(new ChameleonArmorLayer<>(playerRenderer, mc.getModelManager()));
        }
        if (event.getSkin("slim") instanceof PlayerRenderer playerRenderer)
        {   playerRenderer.addLayer(new ChameleonArmorLayer<>(playerRenderer, mc.getModelManager()));
        }
    }
}
