package net.gabriel.monk.client.vfx;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.gabriel.monk.effect.ModEffects;
import net.gabriel.monk.util.MonkSpiritSpheres;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

import java.util.List;

public final class SpiritualSpheresParticles {

    private static final int TICK_INTERVAL = 3;
    private static final int PARTICLES_PER_SPHERE = 4;

    private static final double MINI_ORBIT_RADIUS = 0.22d;
    private static final double MINI_ORBIT_Y = 0.06d;

    private static final double TANGENTIAL_SPEED = 0.006d;
    private static final double POSITION_JITTER = 0.018d;

    private static final float SPHERE_ORBIT_PERIOD_TICKS = 140.0f;
    private static final float SPHERE_ORBIT_RADIUS = 1.2f;

    private static final double PLAYER_RENDER_BASE_Y = 1.85d;

    private static final Vector3f BASE_PURPLE = new Vector3f(0.46f, 0.16f, 0.74f);
    private static final Vector3f LIGHT_PURPLE = new Vector3f(0.78f, 0.56f, 0.95f);
    private static final Vector3f PALE_LILAC = new Vector3f(0.90f, 0.80f, 1.00f);
    private static final Vector3f SOFT_WHITE = new Vector3f(0.97f, 0.98f, 1.00f);

    private static final float SPARKLE_CHANCE = 0.10f;
    private static final float NORMAL_FADE_TO_PALE_CHANCE = 0.18f;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(SpiritualSpheresParticles::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.world == null || client.isPaused()) return;

        List<AbstractClientPlayerEntity> players = client.world.getPlayers();

        for (AbstractClientPlayerEntity player : players) {
            StatusEffectInstance inst = player.getStatusEffect(ModEffects.SPIRITUAL_SPHERES);
            if (inst == null) continue;

            int maxAllowed = MonkSpiritSpheres.getMaxSpheres(player);
            int stacks = Math.min(maxAllowed, inst.getAmplifier() + 1);
            if (stacks <= 0) continue;

            if (player.isSpectator() || player.isInvisible()) continue;

            if (client.player != null && player == client.player && client.options.getPerspective().isFirstPerson()) {
                continue;
            }

            if (client.player != null && client.player.squaredDistanceTo(player) > 2304.0d) continue;
            if ((player.age % TICK_INTERVAL) != 0) continue;

            float age = player.age;

            float baseAngleDeg = (age / SPHERE_ORBIT_PERIOD_TICKS) * 360.0f;
            float yWobble = -0.2f + (float) Math.sin(age * 0.15f) * 0.04f;

            double baseY = player.getY() + PLAYER_RENDER_BASE_Y + (double) yWobble;

            float bodyYaw = player.getBodyYaw();
            double yawRad = Math.toRadians(bodyYaw);
            double yawCos = Math.cos(yawRad);
            double yawSin = Math.sin(yawRad);

            Random random = client.world.random;

            for (int sphereIndex = 0; sphereIndex < stacks; sphereIndex++) {
                float angleDeg = baseAngleDeg + (360.0f * (float) sphereIndex / (float) stacks);
                double angleRad = -Math.toRadians(angleDeg);

                double localX = Math.cos(angleRad) * SPHERE_ORBIT_RADIUS;
                double localZ = Math.sin(angleRad) * SPHERE_ORBIT_RADIUS;

                double rotX = (localX * yawCos) - (localZ * yawSin);
                double rotZ = (localX * yawSin) + (localZ * yawCos);

                double x = player.getX() + rotX;
                double z = player.getZ() + rotZ;

                spawnMiniOrbit(client, random, x, baseY, z, age, sphereIndex);
            }
        }
    }

    private static void spawnMiniOrbit(MinecraftClient client,
                                       Random random,
                                       double x, double y, double z,
                                       float age,
                                       int sphereIndex) {

        for (int i = 0; i < PARTICLES_PER_SPHERE; i++) {
            double theta = Math.toRadians((age * 18.0f) + (sphereIndex * 90.0f) + (i * 90.0f));

            double dx = Math.cos(theta) * MINI_ORBIT_RADIUS;
            double dz = Math.sin(theta) * MINI_ORBIT_RADIUS;
            double dy = Math.sin(theta * 2.0d) * MINI_ORBIT_Y;

            double jx = (random.nextFloat() - 0.5d) * 2.0d * POSITION_JITTER;
            double jy = (random.nextFloat() - 0.5d) * 2.0d * POSITION_JITTER;
            double jz = (random.nextFloat() - 0.5d) * 2.0d * POSITION_JITTER;

            double vx = -Math.sin(theta) * TANGENTIAL_SPEED;
            double vy = 0.0012d;
            double vz = Math.cos(theta) * TANGENTIAL_SPEED;

            ParticleEffect main = pickHarmonizedParticle(random);

            client.world.addParticle(
                    main,
                    x + dx + jx,
                    y + dy + jy,
                    z + dz + jz,
                    vx, vy, vz
            );

            if (random.nextFloat() < SPARKLE_CHANCE) {
                ParticleEffect sparkle = pickSparkle(random);

                client.world.addParticle(
                        sparkle,
                        x + (dx * 0.65d) + (jx * 0.5d),
                        y + (dy * 0.65d) + (jy * 0.5d),
                        z + (dz * 0.65d) + (jz * 0.5d),
                        0.0d, 8.0e-4d, 0.0d
                );
            }
        }
    }

    private static ParticleEffect pickHarmonizedParticle(Random random) {
        float size = MathHelper.lerp(random.nextFloat(), 0.20f, 0.44f);
        float mix = (random.nextFloat() + random.nextFloat()) * 0.5f;

        Vector3f c = lerpColor(BASE_PURPLE, LIGHT_PURPLE, mix);
        c = nudge(c, random, 0.03f);

        if (random.nextFloat() < NORMAL_FADE_TO_PALE_CHANCE) {
            float toPale = MathHelper.lerp(random.nextFloat(), 0.25f, 0.65f);
            Vector3f c2 = lerpColor(c, PALE_LILAC, toPale);

            return new DustColorTransitionParticleEffect(
                    new Vector3f(c),
                    new Vector3f(c2),
                    size
            );
        }

        return new DustParticleEffect(new Vector3f(c), size);
    }

    private static ParticleEffect pickSparkle(Random random) {
        float size = MathHelper.lerp(random.nextFloat(), 0.10f, 0.20f);

        float mix = (random.nextFloat() + random.nextFloat()) * 0.5f;
        float t = MathHelper.lerp(mix, 0.65f, 0.92f);

        Vector3f c = lerpColor(LIGHT_PURPLE, SOFT_WHITE, t);
        c = nudge(c, random, 0.02f);

        if (random.nextFloat() < 0.70f) {
            Vector3f c2 = lerpColor(c, SOFT_WHITE, 0.85f);

            return new DustColorTransitionParticleEffect(
                    new Vector3f(c),
                    new Vector3f(c2),
                    size
            );
        }

        return new DustParticleEffect(new Vector3f(c), size);
    }

    private static Vector3f nudge(Vector3f c, Random random, float amount) {
        float nx = (random.nextFloat() - 0.5f) * 2.0f * amount;
        float ny = (random.nextFloat() - 0.5f) * 2.0f * amount;
        float nz = (random.nextFloat() - 0.5f) * 2.0f * amount;

        return new Vector3f(
                MathHelper.clamp(c.x + nx, 0.0f, 1.0f),
                MathHelper.clamp(c.y + ny, 0.0f, 1.0f),
                MathHelper.clamp(c.z + nz, 0.0f, 1.0f)
        );
    }

    private static Vector3f lerpColor(Vector3f a, Vector3f b, float t) {
        t = MathHelper.clamp(t, 0.0f, 1.0f);
        return new Vector3f(
                MathHelper.lerp(t, a.x, b.x),
                MathHelper.lerp(t, a.y, b.y),
                MathHelper.lerp(t, a.z, b.z)
        );
    }

    private SpiritualSpheresParticles() {}
}
