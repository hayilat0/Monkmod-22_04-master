package net.gabriel.monk.client.render;

import net.gabriel.monk.Monkmod;
import net.gabriel.monk.effect.ModEffects;
import net.gabriel.monk.util.MonkSpiritSpheres;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SpiritualSpheresFeatureRenderer
        extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    private static final Identifier TEXTURE =
            Identifier.of(Monkmod.MOD_ID, "textures/entity/spiritual_sphere.png");

    private static final int FULLBRIGHT = 0xF000F0;

    private static final float SPHERE_SCALE = 0.90f;
    private static final float HALO_SCALE = 1.2f;

    private static final float GROW_TICKS = 32.0f;
    private static final float MIN_GROW_SCALE = 0.12f;

    // Órbita
    private static final float ORBIT_PERIOD_TICKS = 140.0f;
    private static final float BASE_RADIUS = 1.15f;
    private static final float RADIUS_WAVE = 0.08f;
    private static final float ANGLE_WOBBLE_DEGREES = 6.0f;

    // Bob vertical reduzido
    private static final float BASE_HEIGHT = -0.16f;
    private static final float GLOBAL_BOB = 0.018f;
    private static final float INDIVIDUAL_BOB = 0.050f;
    private static final float SECONDARY_BOB = 0.016f;

    // Rotação própria completa, mas ainda suave
    private static final float SELF_SPIN_Y_SPEED = 7.2f;
    private static final float SELF_SPIN_X_SPEED = 3.2f;
    private static final float SELF_SPIN_Z_SPEED = 2.1f;
    private static final float SELF_TILT_BASE = 18.0f;
    private static final float SELF_TILT_VARIATION = 4.0f;

    private final ModelPart sphere;

    private static final Map<UUID, PlayerSphereVisualState> VISUAL_STATES = new HashMap<>();

    private static final class PlayerSphereVisualState {
        int lastCount = 0;
        float[] spawnAges = new float[0];
        float lastSeenAge = 0.0f;
    }

    public SpiritualSpheresFeatureRenderer(
            FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context,
            EntityModelLoader loader
    ) {
        super(context);
        this.sphere = loader.getModelPart(SpiritualSphereModel.LAYER).getChild("sphere");
    }

    @Override
    public void render(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            AbstractClientPlayerEntity player,
            float limbAngle,
            float limbDistance,
            float tickDelta,
            float animationProgress,
            float headYaw,
            float headPitch
    ) {
        StatusEffectInstance inst = player.getStatusEffect(ModEffects.SPIRITUAL_SPHERES);
        if (inst == null) {
            VISUAL_STATES.remove(player.getUuid());
            return;
        }

        int maxAllowed = MonkSpiritSpheres.getMaxSpheres(player);
        int count = Math.min(maxAllowed, inst.getAmplifier() + 1);
        if (count <= 0) {
            VISUAL_STATES.remove(player.getUuid());
            return;
        }

        if (player.isSpectator() || player.isInvisible()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && player == client.player && client.options.getPerspective().isFirstPerson()) {
            return;
        }

        float age = player.age + tickDelta;

        PlayerSphereVisualState state = VISUAL_STATES.computeIfAbsent(player.getUuid(), uuid -> new PlayerSphereVisualState());
        updateVisualState(state, count, age);

        float baseOrbitAngleDeg = (age / ORBIT_PERIOD_TICKS) * 360.0f;

        VertexConsumer core = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
        VertexConsumer halo = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));

        for (int i = 0; i < count; i++) {
            float normalizedIndex = (float) i / (float) count;
            float phase = normalizedIndex * (float) (Math.PI * 2.0);

            float angleWobble = (float) Math.sin(age * 0.050f + phase * 1.5f) * ANGLE_WOBBLE_DEGREES;
            float angleDeg = baseOrbitAngleDeg + (360.0f * i / count) + angleWobble;
            double angleRad = Math.toRadians(angleDeg);

            float radiusPulse = (float) Math.sin(age * 0.070f + phase * 1.25f) * RADIUS_WAVE;
            float radius = BASE_RADIUS + radiusPulse;

            float x = (float) (Math.cos(angleRad) * radius);
            float z = (float) (Math.sin(angleRad) * radius);

            float y =
                    BASE_HEIGHT
                            + (float) Math.sin(age * 0.080f) * GLOBAL_BOB
                            + (float) Math.sin(age * 0.145f + phase * 1.55f) * INDIVIDUAL_BOB
                            + (float) Math.cos(age * 0.060f + phase * 0.90f) * SECONDARY_BOB;

            float growScale = computeGrowScale(state, i, age);

            float selfTilt = SELF_TILT_BASE + (float) Math.sin(phase * 1.20f) * SELF_TILT_VARIATION;
            float selfSpinY = age * SELF_SPIN_Y_SPEED + i * 19.0f;
            float selfSpinX = age * SELF_SPIN_X_SPEED + i * 11.0f;
            float selfSpinZ = age * SELF_SPIN_Z_SPEED + i * 7.0f;

            matrices.push();

            matrices.translate(x, y, z);

            // Translação orbital refinada
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-angleDeg + 90.0f));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) Math.sin(age * 0.120f + phase) * 4.0f));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) Math.cos(age * 0.105f + phase * 1.25f) * 3.0f));

            // Rotação própria completa
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(selfTilt));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(selfSpinY));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(selfSpinX));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(selfSpinZ));

            // núcleo
            matrices.push();
            float coreScale = SPHERE_SCALE * growScale;
            matrices.scale(coreScale, coreScale, coreScale);
            sphere.render(matrices, core, FULLBRIGHT, OverlayTexture.DEFAULT_UV);
            matrices.pop();

            // halo
            matrices.push();
            float haloScale = SPHERE_SCALE * HALO_SCALE * growScale;
            matrices.scale(haloScale, haloScale, haloScale);

            sphere.render(matrices, halo, FULLBRIGHT, OverlayTexture.DEFAULT_UV);

            float blur = 0.02f * SPHERE_SCALE * growScale;

            matrices.translate( blur, 0f, 0f);
            sphere.render(matrices, halo, FULLBRIGHT, OverlayTexture.DEFAULT_UV);

            matrices.translate(-2f * blur, 0f, 0f);
            sphere.render(matrices, halo, FULLBRIGHT, OverlayTexture.DEFAULT_UV);

            matrices.translate( blur, 0f,  blur);
            sphere.render(matrices, halo, FULLBRIGHT, OverlayTexture.DEFAULT_UV);

            matrices.translate(0f, 0f, -2f * blur);
            sphere.render(matrices, halo, FULLBRIGHT, OverlayTexture.DEFAULT_UV);

            matrices.pop();
            matrices.pop();
        }

        state.lastSeenAge = age;
    }

    private static void updateVisualState(PlayerSphereVisualState state, int count, float currentAge) {
        if (state.spawnAges.length < count) {
            float[] newSpawnAges = new float[count];
            System.arraycopy(state.spawnAges, 0, newSpawnAges, 0, state.spawnAges.length);

            for (int i = state.spawnAges.length; i < count; i++) {
                newSpawnAges[i] = currentAge;
            }

            state.spawnAges = newSpawnAges;
        }

        if (count > state.lastCount) {
            for (int i = state.lastCount; i < count; i++) {
                state.spawnAges[i] = currentAge;
            }
        } else if (count < state.lastCount) {
            float[] newSpawnAges = new float[count];
            if (count > 0) {
                System.arraycopy(state.spawnAges, 0, newSpawnAges, 0, count);
            }
            state.spawnAges = newSpawnAges;
        }

        state.lastCount = count;
    }

    private static float computeGrowScale(PlayerSphereVisualState state, int index, float currentAge) {
        if (index < 0 || index >= state.spawnAges.length) {
            return 1.0f;
        }

        float elapsed = currentAge - state.spawnAges[index];
        float t = MathHelper.clamp(elapsed / GROW_TICKS, 0.0f, 1.0f);

        float eased = 1.0f - (1.0f - t) * (1.0f - t);

        return MathHelper.lerp(eased, MIN_GROW_SCALE, 1.0f);
    }
}