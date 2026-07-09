package net.gabriel.monk.client.vfx;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.gabriel.monk.network.TripleComboVfxPayload;
import net.gabriel.monk.particle.ModParticles;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * VFX client-side do Combo Triplo.
 *
 * Ajustes desta versão:
 * - 6 golpes visuais por hit (total 18);
 * - menos partículas;
 * - cubos menores;
 * - os cubos ficam "deitados", paralelos ao chão;
 * - movimento reto e mais sutil.
 */
public final class TripleComboVfxClient {

    private static final List<MeteorRushEffect> ACTIVE_RUSHES = new ArrayList<>();
    private static final List<ImpactEffect> ACTIVE_IMPACTS = new ArrayList<>();

    private static boolean initialized = false;

    private TripleComboVfxClient() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        TripleComboVfxPayload.register();

        ClientPlayNetworking.registerGlobalReceiver(TripleComboVfxPayload.ID, TripleComboVfxClient::onPayload);
        ClientTickEvents.END_CLIENT_TICK.register(TripleComboVfxClient::tick);
    }

    private static void onPayload(TripleComboVfxPayload payload, ClientPlayNetworking.Context context) {
        MinecraftClient client = context.client();
        client.execute(() -> handlePayload(client, payload));
    }

    private static void handlePayload(MinecraftClient client, TripleComboVfxPayload payload) {
        if (client.world == null) return;

        int hitIndex = MathHelper.clamp(payload.hitIndex(), 0, 2);

        if (payload.mode() == TripleComboVfxPayload.MODE_METEOR_RUSH) {
            ACTIVE_RUSHES.add(new MeteorRushEffect(
                    payload.casterEntityId(),
                    payload.start(),
                    payload.end(),
                    hitIndex
            ));
            return;
        }

        if (payload.mode() == TripleComboVfxPayload.MODE_IMPACT) {
            ACTIVE_IMPACTS.add(new ImpactEffect(
                    payload.start(),
                    payload.end(),
                    hitIndex
            ));
        }
    }

    private static void tick(MinecraftClient client) {
        if (client.world == null || client.isPaused()) {
            return;
        }

        ClientWorld world = client.world;

        Iterator<MeteorRushEffect> rushIterator = ACTIVE_RUSHES.iterator();
        while (rushIterator.hasNext()) {
            MeteorRushEffect effect = rushIterator.next();
            effect.tick(world);

            if (effect.finished()) {
                rushIterator.remove();
            }
        }

        Iterator<ImpactEffect> impactIterator = ACTIVE_IMPACTS.iterator();
        while (impactIterator.hasNext()) {
            ImpactEffect effect = impactIterator.next();
            effect.tick(world);

            if (effect.finished()) {
                impactIterator.remove();
            }
        }
    }

    /**
     * Cada hit do combo gera exatamente 6 golpes visuais.
     */
    private static final class MeteorRushEffect {
        private static final int PULSES_PER_HIT = 6;

        private final int casterEntityId;
        private final Vec3d start;
        private final Vec3d end;
        private final Vec3d directionFlat;
        private final Vec3d right;
        private final int hitIndex;

        private final List<FlatSquarePulse> pulses = new ArrayList<>();

        private boolean built = false;
        private int age = 0;
        private int durationTicks = 10;

        private MeteorRushEffect(int casterEntityId, Vec3d start, Vec3d end, int hitIndex) {
            this.casterEntityId = casterEntityId;
            this.start = start;
            this.end = end;
            this.hitIndex = hitIndex;

            Vec3d raw = end.subtract(start);
            Vec3d horizontal = new Vec3d(raw.x, 0.0d, raw.z);

            if (horizontal.lengthSquared() < 1.0E-6d) {
                horizontal = new Vec3d(0.0d, 0.0d, 1.0d);
            }

            this.directionFlat = horizontal.normalize();
            this.right = horizontalRight(this.directionFlat);
        }

        private void build(Random random) {
            built = true;

            double coneSpread = switch (hitIndex) {
                case 0 -> 0.42d;
                case 1 -> 0.54d;
                default -> 0.68d;
            };

            double ySpread = switch (hitIndex) {
                case 0 -> 0.05d;
                case 1 -> 0.07d;
                default -> 0.09d;
            };

            int travelTicks = switch (hitIndex) {
                case 0 -> 6;
                case 1 -> 7;
                default -> 8;
            };

            float halfWidth = switch (hitIndex) {
                case 0 -> 0.08f;
                case 1 -> 0.09f;
                default -> 0.11f;
            };

            float halfLength = switch (hitIndex) {
                case 0 -> 0.10f;
                case 1 -> 0.12f;
                default -> 0.14f;
            };

            float halfHeight = switch (hitIndex) {
                case 0 -> 0.012f;
                case 1 -> 0.015f;
                default -> 0.018f;
            };

            int maxEnd = 0;

            for (int i = 0; i < PULSES_PER_HIT; i++) {
                double sideOffset = randomTriangular(random) * coneSpread;
                double yOffset = randomTriangular(random) * ySpread;

                // Mantém o movimento paralelo ao chão:
                // start e end do golpe ficam na mesma altura.
                Vec3d pulseStart = new Vec3d(
                        start.x + right.x * sideOffset * 0.10d,
                        start.y + yOffset,
                        start.z + right.z * sideOffset * 0.10d
                );

                Vec3d pulseEnd = new Vec3d(
                        end.x + right.x * sideOffset,
                        start.y + yOffset,
                        end.z + right.z * sideOffset
                );

                int startDelay = i / 2; // 0,0,1,1,2,2

                FlatSquarePulse pulse = new FlatSquarePulse(
                        pulseStart,
                        pulseEnd,
                        halfWidth,
                        halfLength,
                        halfHeight,
                        travelTicks,
                        startDelay,
                        hitIndex
                );

                pulses.add(pulse);
                maxEnd = Math.max(maxEnd, startDelay + travelTicks);
            }

            durationTicks = maxEnd + 2;
        }

        private void tick(ClientWorld world) {
            Random random = world.random;

            if (!built) {
                build(random);
            }

            spawnHandFlash(world, random);

            for (FlatSquarePulse pulse : pulses) {
                pulse.render(world, age);
            }

            age++;
        }

        private void spawnHandFlash(ClientWorld world, Random random) {
            if (age > 0) return;

            Entity caster = world.getEntityById(casterEntityId);

            Vec3d handCenter = start;
            if (caster != null) {
                handCenter = caster.getPos()
                        .add(0.0d, caster.getHeight() * 0.62d, 0.0d)
                        .add(directionFlat.multiply(0.45d));
            }

            int count = hitIndex == 2 ? 2 : 1;

            for (int i = 0; i < count; i++) {
                Vec3d p = handCenter
                        .add(right.multiply(randomTriangular(random) * 0.06d));

                world.addParticle(
                        ModParticles.SPIRITUAL_METEOR,
                        p.x,
                        p.y,
                        p.z,
                        directionFlat.x * 0.010d,
                        0.0d,
                        directionFlat.z * 0.010d
                );
            }
        }

        private boolean finished() {
            return age > durationTicks;
        }
    }

    /**
     * Um golpe visual "deitado":
     * uma moldura retangular horizontal, com pouca espessura vertical,
     * movendo-se em linha reta paralela ao chão.
     */
    private static final class FlatSquarePulse {
        private final Vec3d start;
        private final Vec3d end;
        private final Vec3d directionFlat;
        private final Vec3d right;
        private final float halfWidth;
        private final float halfLength;
        private final float halfHeight;
        private final int travelTicks;
        private final int startDelay;
        private final int hitIndex;

        private FlatSquarePulse(
                Vec3d start,
                Vec3d end,
                float halfWidth,
                float halfLength,
                float halfHeight,
                int travelTicks,
                int startDelay,
                int hitIndex
        ) {
            this.start = start;
            this.end = end;
            this.halfWidth = halfWidth;
            this.halfLength = halfLength;
            this.halfHeight = halfHeight;
            this.travelTicks = travelTicks;
            this.startDelay = startDelay;
            this.hitIndex = hitIndex;

            Vec3d raw = end.subtract(start);
            Vec3d horizontal = new Vec3d(raw.x, 0.0d, raw.z);

            if (horizontal.lengthSquared() < 1.0E-6d) {
                horizontal = new Vec3d(0.0d, 0.0d, 1.0d);
            }

            this.directionFlat = horizontal.normalize();
            this.right = horizontalRight(this.directionFlat);
        }

        private void render(ClientWorld world, int globalAge) {
            if (globalAge < startDelay) {
                return;
            }

            int localAge = globalAge - startDelay;
            if (localAge > travelTicks) {
                return;
            }

            double t = localAge / (double) Math.max(1, travelTicks);
            double eased = easeOutCubic(t);

            Vec3d center = start.lerp(end, eased);

            int layerCount = hitIndex == 2 ? 2 : 1;
            int edgeSamples = hitIndex == 2 ? 2 : 1;

            double speed = switch (hitIndex) {
                case 0 -> 0.010d;
                case 1 -> 0.012d;
                default -> 0.015d;
            };

            for (int layer = 0; layer < layerCount; layer++) {
                double yOffset = layerCount == 1
                        ? 0.0d
                        : MathHelper.lerp(layer / (double) (layerCount - 1), -halfHeight, halfHeight);

                Vec3d layerCenter = center.add(0.0d, yOffset, 0.0d);
                spawnFlatRectOutline(world, layerCenter, edgeSamples, speed);
            }
        }

        /**
         * Desenha um retângulo vazado HORIZONTAL (deitado),
         * usando os eixos:
         * - largura = right
         * - comprimento = directionFlat
         *
         * Isso faz o "cubo" ficar paralelo ao chão.
         */
        private void spawnFlatRectOutline(ClientWorld world, Vec3d center, int edgeSamples, double speed) {
            for (int i = 0; i <= edgeSamples; i++) {
                double s = -1.0d + (2.0d * i / (double) Math.max(1, edgeSamples));

                Vec3d front = center
                        .add(right.multiply(s * halfWidth))
                        .add(directionFlat.multiply(halfLength));

                Vec3d back = center
                        .add(right.multiply(s * halfWidth))
                        .add(directionFlat.multiply(-halfLength));

                Vec3d left = center
                        .add(right.multiply(-halfWidth))
                        .add(directionFlat.multiply(s * halfLength));

                Vec3d rightPos = center
                        .add(right.multiply(halfWidth))
                        .add(directionFlat.multiply(s * halfLength));

                spawnMeteor(world, front, speed);
                spawnMeteor(world, back, speed);
                spawnMeteor(world, left, speed);
                spawnMeteor(world, rightPos, speed);
            }
        }

        private void spawnMeteor(ClientWorld world, Vec3d pos, double speed) {
            world.addParticle(
                    ModParticles.SPIRITUAL_METEOR,
                    pos.x,
                    pos.y,
                    pos.z,
                    directionFlat.x * speed,
                    0.0d,
                    directionFlat.z * speed
            );
        }
    }

    /**
     * Impacto final bem sutil.
     */
    private static final class ImpactEffect {
        private final Vec3d impactPos;
        private final Vec3d directionFlat;
        private final Vec3d right;
        private final int hitIndex;
        private final int durationTicks;
        private int age;

        private ImpactEffect(Vec3d casterPos, Vec3d impactPos, int hitIndex) {
            this.impactPos = impactPos;
            this.hitIndex = hitIndex;

            Vec3d raw = impactPos.subtract(casterPos);
            Vec3d horizontal = new Vec3d(raw.x, 0.0d, raw.z);

            if (horizontal.lengthSquared() < 1.0E-6d) {
                horizontal = new Vec3d(0.0d, 0.0d, 1.0d);
            }

            this.directionFlat = horizontal.normalize();
            this.right = horizontalRight(this.directionFlat);

            this.durationTicks = hitIndex == 2 ? 4 : 2;
        }

        private void tick(ClientWorld world) {
            Random random = world.random;

            if (age == 0) {
                spawnOpeningFlash(world, random);
            }

            if (hitIndex == 2) {
                spawnTinyFlatRing(world);
            }

            age++;
        }

        private void spawnOpeningFlash(ClientWorld world, Random random) {
            int count = switch (hitIndex) {
                case 0 -> 4;
                case 1 -> 5;
                default -> 7;
            };

            for (int i = 0; i < count; i++) {
                Vec3d dir = randomImpactDirection(random);
                Vec3d p = impactPos.add(dir.multiply(random.nextDouble() * 0.05d));

                world.addParticle(
                        ModParticles.SPIRITUAL_METEOR,
                        p.x,
                        p.y,
                        p.z,
                        dir.x * 0.015d,
                        0.0d,
                        dir.z * 0.015d
                );
            }

            if (hitIndex == 2) {
                world.addParticle(
                        ParticleTypes.END_ROD,
                        impactPos.x,
                        impactPos.y,
                        impactPos.z,
                        0.0d,
                        0.0d,
                        0.0d
                );
            }
        }

        private void spawnTinyFlatRing(ClientWorld world) {
            if (age > 1) return;

            int ringCount = 6;
            double radius = 0.05d + age * 0.04d;

            for (int i = 0; i < ringCount; i++) {
                double angle = (i / (double) ringCount) * Math.PI * 2.0d;

                Vec3d dir = right.multiply(Math.cos(angle))
                        .add(directionFlat.multiply(Math.sin(angle)));

                Vec3d p = impactPos.add(dir.multiply(radius));

                world.addParticle(
                        ModParticles.SPIRITUAL_METEOR,
                        p.x,
                        p.y,
                        p.z,
                        dir.x * 0.010d,
                        0.0d,
                        dir.z * 0.010d
                );
            }
        }

        private Vec3d randomImpactDirection(Random random) {
            Vec3d v = right.multiply(randomTriangular(random))
                    .add(directionFlat.multiply(randomTriangular(random)));

            if (v.lengthSquared() < 1.0E-6d) {
                return directionFlat;
            }

            return v.normalize();
        }

        private boolean finished() {
            return age > durationTicks;
        }
    }

    private static Vec3d horizontalRight(Vec3d direction) {
        Vec3d right = new Vec3d(-direction.z, 0.0d, direction.x);

        if (right.lengthSquared() < 1.0E-6d) {
            return new Vec3d(1.0d, 0.0d, 0.0d);
        }

        return right.normalize();
    }

    private static double easeOutCubic(double t) {
        t = MathHelper.clamp(t, 0.0d, 1.0d);
        double inv = 1.0d - t;
        return 1.0d - inv * inv * inv;
    }

    private static double randomTriangular(Random random) {
        return random.nextDouble() - random.nextDouble();
    }
}