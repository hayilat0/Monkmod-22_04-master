package net.gabriel.monk.client.vfx;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.gabriel.monk.network.EtherealStepVfxPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class EtherealStepVfxClient {
    private static final int LIFE_TICKS = 14;

    private static final float TRAVEL_SPEED = 0.55f;
    private static final int AFTERIMAGE_COUNT = 7;

    private static final float GHOST_ALPHA = 0.30f;
    private static final float FADE_OUT_STRENGTH = 0.65f;

    private static final boolean DISABLE_DEPTH_WRITE = true;

    // Evita fantasma estourando na câmera em 1ª pessoa (player local)
    private static final float LOCAL_FP_MAX_T = 0.78f;
    private static final double LOCAL_FP_MIN_DIST_SQ = 1.44d;

    private static final Map<Integer, Trail> TRAILS = new HashMap<>();

    private EtherealStepVfxClient() {
    }

    public static void init() {
        EtherealStepVfxPayload.register();
        ClientPlayNetworking.registerGlobalReceiver(EtherealStepVfxPayload.ID, EtherealStepVfxClient::onPayload);
        WorldRenderEvents.AFTER_ENTITIES.register(EtherealStepVfxClient::onWorldRender);
    }

    private static void onPayload(EtherealStepVfxPayload payload, ClientPlayNetworking.Context context) {
        MinecraftClient client = context.client();
        client.execute(() -> handlePayload(client, payload));
    }

    private static void handlePayload(MinecraftClient client, EtherealStepVfxPayload payload) {
        if (client.world == null) return;

        Entity entity = client.world.getEntityById(payload.casterEntityId());
        if (!(entity instanceof PlayerEntity caster)) return;

        long tick = client.world.getTime();
        TRAILS.put(payload.casterEntityId(), new Trail(payload.start(), payload.end(), tick));

        // Partículas cosméticas (portal)
        Vec3d delta = payload.end().subtract(payload.start());
        int steps = LIFE_TICKS;

        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3d p = payload.start().add(delta.multiply(t));

            client.world.addParticle(
                    ParticleTypes.PORTAL,
                    p.x, p.y + 1.0, p.z,
                    (client.world.random.nextDouble() - 0.5) * 0.06,
                    (client.world.random.nextDouble() - 0.5) * 0.06,
                    (client.world.random.nextDouble() - 0.5) * 0.06
            );
        }
    }

    private static void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || consumers == null || context.camera() == null) return;

        float tickDelta = context.tickCounter().getTickDelta(true);
        Vec3d cameraPos = context.camera().getPos();
        long worldTime = client.world.getTime();

        boolean isFirstPerson = client.options.getPerspective() == Perspective.FIRST_PERSON;

        Iterator<Map.Entry<Integer, Trail>> it = TRAILS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Trail> entry = it.next();
            int entityId = entry.getKey();
            Trail trail = entry.getValue();

            long age = worldTime - trail.startTick;
            if (age > LIFE_TICKS) {
                it.remove();
                continue;
            }

            Entity e = client.world.getEntityById(entityId);
            if (!(e instanceof PlayerEntity player)) {
                it.remove();
                continue;
            }

            boolean isLocalPlayer = player.getId() == client.player.getId();
            boolean localFirstPerson = isLocalPlayer && isFirstPerson;

            renderAfterimages(client, player, trail, age, tickDelta, matrices, consumers, cameraPos, localFirstPerson);
        }
    }

    private static void renderAfterimages(
            MinecraftClient client,
            PlayerEntity player,
            Trail trail,
            long ageTicks,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Vec3d cameraPos,
            boolean localFirstPerson
    ) {
        Vec3d delta = trail.end.subtract(trail.start);
        if (delta.lengthSquared() < 1.0E-6) return;

        float yaw = (float) (MathHelper.atan2(delta.z, delta.x) * 57.29577951308232D) - 90.0f;

        float t = ageTicks + tickDelta;
        float progress = MathHelper.clamp(t / LIFE_TICKS, 0.0f, 1.0f);

        float phase = MathHelper.clamp(progress * TRAVEL_SPEED, 0.0f, 1.0f);

        float fade = 1.0f - (progress * 0.85f);
        float baseAlpha = MathHelper.clamp(GHOST_ALPHA * fade, 0.05f, 0.75f);

        for (int i = 0; i < AFTERIMAGE_COUNT; i++) {
            float iT = (float) i / AFTERIMAGE_COUNT;

            float ghostT = MathHelper.clamp(iT + phase, 0.0f, 1.0f);

            // Em 1ª pessoa (player local), evita render muito perto do final
            if (localFirstPerson && ghostT > LOCAL_FP_MAX_T) continue;

            // ease-out
            float inv = 1.0f - ghostT;
            ghostT = 1.0f - (inv * inv);

            Vec3d pos = trail.start.add(delta.multiply(ghostT));

            double wiggle = MathHelper.sin(t * TRAVEL_SPEED + i) * 0.03;
            pos = pos.add(wiggle, 0.0, -wiggle);

            // Em 1ª pessoa, evita render dentro da câmera
            if (localFirstPerson && pos.squaredDistanceTo(cameraPos) < LOCAL_FP_MIN_DIST_SQ) continue;

            float alpha = baseAlpha * (1.0f - (iT * FADE_OUT_STRENGTH));
            renderGhost(client, player, pos, yaw, alpha, tickDelta, matrices, consumers, cameraPos);
        }
    }

    /**
     * FIX MODPACK (NOME + TRANSPARÊNCIA):
     *
     * 1) Criamos um GhostPlayerEntity que:
     *    - shouldRenderName() = false
     *    - getName()/getDisplayName() = Text.empty()
     *    Isso BLINDA contra mods que tentam renderizar nameplate de qualquer jeito.
     *
     * 2) Multiplicamos alpha por VÉRTICE (não dependemos de setShaderColor).
     */
    private static void renderGhost(
            MinecraftClient client,
            PlayerEntity original,
            Vec3d worldPos,
            float yaw,
            float alpha,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Vec3d cameraPos
    ) {
        if (client.world == null) return;

        GameProfile profile = original.getGameProfile();

        // ✅ Entidade fantasma blindada contra nameplate
        GhostPlayerEntity ghost = new GhostPlayerEntity(client.world, profile);

        ghost.refreshPositionAndAngles(worldPos.x, worldPos.y, worldPos.z, yaw, original.getPitch());

        ghost.setPose(original.getPose());
        ghost.setSneaking(original.isSneaking());
        ghost.setSprinting(original.isSprinting());

        ghost.setNoGravity(true);
        ghost.setSilent(true);
        ghost.setInvulnerable(true);

        // redundância proposital: alguns mods usam isso
        ghost.setCustomNameVisible(false);

        // Copia armor + mão
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ghost.equipStack(slot, original.getEquippedStack(slot));
        }

        double dx = worldPos.x - cameraPos.x;
        double dy = worldPos.y - cameraPos.y;
        double dz = worldPos.z - cameraPos.z;

        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        EntityRenderer<? super Entity> renderer = dispatcher.getRenderer(ghost);

        VertexConsumerProvider alphaConsumers = new AlphaVertexConsumerProvider(consumers, alpha);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (DISABLE_DEPTH_WRITE) {
            RenderSystem.depthMask(false);
        }

        matrices.push();
        matrices.translate(dx, dy, dz);

        // Full bright
        renderer.render(ghost, yaw, tickDelta, matrices, alphaConsumers, 15728880);

        matrices.pop();

        if (DISABLE_DEPTH_WRITE) {
            RenderSystem.depthMask(true);
        }

        RenderSystem.disableBlend();
    }

    private record Trail(Vec3d start, Vec3d end, long startTick) {
    }

    /**
     * Player fantasma que NUNCA deixa renderizar nome.
     * Isso segura até modpack “agressivo” que força nameplate.
     */
    private static final class GhostPlayerEntity extends OtherClientPlayerEntity {
        private GhostPlayerEntity(ClientWorld world, GameProfile profile) {
            super(world, profile);
        }

        @Override
        public boolean shouldRenderName() {
            return false;
        }

        @Override
        public Text getName() {
            return Text.empty();
        }

        @Override
        public Text getDisplayName() {
            return Text.empty();
        }
    }

    /**
     * Multiplica o alpha de TODO vertex color por alphaMul.
     */
    private static final class AlphaVertexConsumerProvider implements VertexConsumerProvider {
        private final VertexConsumerProvider delegate;
        private final float alphaMul;

        private AlphaVertexConsumerProvider(VertexConsumerProvider delegate, float alphaMul) {
            this.delegate = delegate;
            this.alphaMul = MathHelper.clamp(alphaMul, 0.0f, 1.0f);
        }

        @Override
        public VertexConsumer getBuffer(RenderLayer layer) {
            return new AlphaVertexConsumer(delegate.getBuffer(layer), alphaMul);
        }
    }

    /**
     * Compatível com versões onde VertexConsumer exige vertex(float,float,float).
     */
    private static final class AlphaVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float alphaMul;

        private AlphaVertexConsumer(VertexConsumer delegate, float alphaMul) {
            this.delegate = delegate;
            this.alphaMul = alphaMul;
        }

        private int scaleAlpha(int a) {
            return MathHelper.clamp(Math.round(a * alphaMul), 0, 255);
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            return delegate.vertex(x, y, z);
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return delegate.color(red, green, blue, scaleAlpha(alpha));
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            return delegate.texture(u, v);
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            return delegate.overlay(u, v);
        }

        @Override
        public VertexConsumer light(int u, int v) {
            return delegate.light(u, v);
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return delegate.normal(x, y, z);
        }
    }
}
