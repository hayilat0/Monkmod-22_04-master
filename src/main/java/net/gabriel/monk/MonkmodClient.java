package net.gabriel.monk;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.gabriel.monk.client.hud.SpiritualSpheresHud;
import net.gabriel.monk.client.render.SpiritualSphereModel;
import net.gabriel.monk.client.render.SpiritualSphereProjectileRenderer;
import net.gabriel.monk.client.render.SpiritualSpheresFeatureRenderer;
import net.gabriel.monk.client.tooltip.MonkSpellTooltipMutators;
import net.gabriel.monk.client.vfx.EtherealStepVfxClient;
import net.gabriel.monk.client.vfx.SpiritualSpheresParticles;
import net.gabriel.monk.entity.ModEntities;
import net.minecraft.client.render.entity.PlayerEntityRenderer;

public class MonkmodClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        EntityModelLayerRegistry.registerModelLayer(
                SpiritualSphereModel.LAYER,
                SpiritualSphereModel::getTexturedModelData
        );

        EntityRendererRegistry.register(
                ModEntities.SPIRITUAL_SPHERE_PROJECTILE,
                SpiritualSphereProjectileRenderer::new
        );

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (entityType, entityRenderer, registrationHelper, context) -> {
                    if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
                        registrationHelper.register(
                                new SpiritualSpheresFeatureRenderer(playerRenderer, context.getModelLoader())
                        );
                    }
                }
        );

        SpiritualSpheresParticles.register();

        HudRenderCallback.EVENT.register(SpiritualSpheresHud::render);

        EtherealStepVfxClient.init();
        MonkSpellTooltipMutators.register();
    }
}
