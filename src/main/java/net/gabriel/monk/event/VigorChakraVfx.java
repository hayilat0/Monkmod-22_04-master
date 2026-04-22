package net.gabriel.monk.event;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public final class VigorChakraVfx {

    // ---------- AJUSTES ----------
    // Menos partículas: aumente CHAKRA_SPAWN_EVERY_TICKS (ex: 3 ou 4)
    private static final int CHAKRA_SPAWN_EVERY_TICKS = 2;

    // 3/5/7... menor = menos partículas por tick
    private static final int CHAKRA_ARC_POINTS = 5;

    // “Tamanho geral” da órbita (multiplica os raios abaixo)
    private static final double CHAKRA_RADIUS = 0.85;

    // formato da órbita (elipse no plano YZ)
    private static final double CHAKRA_Y_RADIUS = 0.95;
    private static final double CHAKRA_Z_RADIUS = 0.75;

    // inclinação dos anéis (um inclina pra um lado, o outro pro lado oposto)
    private static final double CHAKRA_TILT_RAD = Math.toRadians(55);

    // distância lateral entre as duas órbitas (garante “sem cruzar” no centro)
    private static final double CHAKRA_RING_OFFSET = 0.55;

    // “velocidade” do giro (maior = mais rápido)
    private static final double CHAKRA_TIME_SPEED = 0.35;

    // jitter sutil pra ficar vivo; diminua se quiser mais “limpo”
    private static final double JITTER_XZ = 0.04;
    private static final double JITTER_Y  = 0.06;

    private VigorChakraVfx() {}

    /** 1x quando o Chakra do Vigor é aplicado.
     *  Agora é intencionalmente vazio: você pediu SEM partículas na ativação. */
    public static void onActivated(ServerPlayerEntity p) {
        // no-op
    }

    /** chamado todo tick enquanto o efeito estiver ativo. */
    public static void onTick(ServerPlayerEntity p) {
        if (p.age % CHAKRA_SPAWN_EVERY_TICKS != 0) return;
        spawnTwoOrbitsNoCross(p);
    }

    // =========================================================
    // 2 órbitas separadas e intercaladas (sem cruzar)
    // =========================================================
    private static void spawnTwoOrbitsNoCross(ServerPlayerEntity p) {
        ServerWorld w = (ServerWorld) p.getWorld();

        // altura do “núcleo” do chakra no corpo
        Vec3d center = p.getPos().add(0, 1.0, 0);

        // vetor "direita" do jogador (as órbitas ficam realmente lado-a-lado conforme ele gira)
        double yawRad = Math.toRadians(p.getYaw());
        Vec3d right = new Vec3d(-Math.sin(yawRad), 0, Math.cos(yawRad));

        // centros separados (não cruzam)
        Vec3d centerA = center.add(right.multiply(+CHAKRA_RING_OFFSET));
        Vec3d centerB = center.add(right.multiply(-CHAKRA_RING_OFFSET));

        // tempo pra animar (giro)
        double time = p.age * CHAKRA_TIME_SPEED;

        // intercalado: B sempre “oposto” de A (meia volta)
        spawnOrbitArc(w, p, centerA, time, +CHAKRA_TILT_RAD);
        spawnOrbitArc(w, p, centerB, time + Math.PI, -CHAKRA_TILT_RAD);
    }

    private static void spawnOrbitArc(ServerWorld w, ServerPlayerEntity p, Vec3d orbitCenter,
                                      double theta0, double tiltZ) {

        int half = CHAKRA_ARC_POINTS / 2; // 3 -> -1,0,1
        double delta = 0.22;

        for (int i = -half; i <= half; i++) {
            double theta = theta0 + i * delta;

            // elipse no plano YZ (x = 0 local) — depois inclinamos e escalamos
            Vec3d local = new Vec3d(
                    0,
                    Math.cos(theta) * CHAKRA_Y_RADIUS,
                    Math.sin(theta) * CHAKRA_Z_RADIUS
            );

            // inclina o anel pra dar o “átomo”
            local = rotateZ(local, tiltZ);

            // escala geral
            local = local.multiply(CHAKRA_RADIUS);

            Vec3d jitter = new Vec3d(
                    (p.getRandom().nextDouble() - 0.5) * JITTER_XZ,
                    (p.getRandom().nextDouble() - 0.5) * JITTER_Y,
                    (p.getRandom().nextDouble() - 0.5) * JITTER_XZ
            );

            Vec3d pos = orbitCenter.add(local).add(jitter);

            w.spawnParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    pos.x, pos.y, pos.z,
                    1,
                    0, 0, 0,
                    0.0
            );
        }
    }

    private static Vec3d rotateZ(Vec3d v, double a) {
        double cos = Math.cos(a);
        double sin = Math.sin(a);
        double x = v.x * cos - v.y * sin;
        double y = v.x * sin + v.y * cos;
        return new Vec3d(x, y, v.z);
    }
}
