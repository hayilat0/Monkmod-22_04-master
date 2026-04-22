package net.gabriel.monk.client.render;

import net.gabriel.monk.Monkmod;
import net.gabriel.monk.entity.SpiritualSphereProjectileEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public final class SpiritualSphereProjectileRenderer extends EntityRenderer<SpiritualSphereProjectileEntity> {

    private static final Identifier TEXTURE =
            Identifier.of(Monkmod.MOD_ID, "textures/entity/spiritual_sphere.png");

    private static final int FULLBRIGHT = 0xF000F0;

    // ✅ Ajuste aqui o tamanho do projétil (1.0f = atual). Ex.: 0.70f deixa bem menor.
    private static final float SPHERE_SCALE = 1.0f;

    private final ModelPart sphere;

    public SpiritualSphereProjectileRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.sphere = ctx.getModelLoader().getModelPart(SpiritualSphereModel.LAYER).getChild("sphere");
    }

    @Override
    public void render(
            SpiritualSphereProjectileEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        matrices.push();

        float age = (entity.age + tickDelta);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(age * 14.0f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(age * 10.0f));

        // ✅ Tamanho do projétil
        matrices.scale(SPHERE_SCALE, SPHERE_SCALE, SPHERE_SCALE);

        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
        sphere.render(matrices, vc, FULLBRIGHT, OverlayTexture.DEFAULT_UV);

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(SpiritualSphereProjectileEntity entity) {
        return TEXTURE;
    }
}
