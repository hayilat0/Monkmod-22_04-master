package net.gabriel.monk.spell;

import net.gabriel.monk.effect.ModEffects;
import net.gabriel.monk.mixin.EntityIFramesAccessor;
import net.gabriel.monk.network.TripleComboVfxNetworking;
import net.gabriel.monk.particle.ModParticles;
import net.gabriel.monk.util.MonkDelayedTasks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.event.SpellHandlers;
import net.spell_engine.internals.SpellHelper;
import net.spell_power.api.SpellPower;

import java.util.ArrayList;
import java.util.List;

public final class TripleComboImpact implements SpellHandlers.CustomImpact {

    private static final int SPHERES_COST = 1;
    private static final int SPHERES_DURATION_TICKS = 14 * 20;

    private static final double RANGE = 3.0d;
    private static final double HALF_ANGLE_DEGREES = 30.0d;
    private static final double CONE_DOT_LIMIT = Math.cos(Math.toRadians(HALF_ANGLE_DEGREES));
    private static final double VERTICAL_TOLERANCE = 1.75d;

    private static final int FIRST_HIT_DELAY_TICKS = 5;
    private static final int HIT_INTERVAL_TICKS = 6;

    private static final float FIRST_HIT_MULTIPLIER = 0.4f;
    private static final float SECOND_HIT_MULTIPLIER = 0.4f;
    private static final float THIRD_HIT_MULTIPLIER = 0.8f;

    private static final float[] HIT_DAMAGE_MULTIPLIERS = {
            FIRST_HIT_MULTIPLIER,
            SECOND_HIT_MULTIPLIER,
            THIRD_HIT_MULTIPLIER
    };

    private static final Identifier KNUCKLE_ATTACK_SOUND =
            Identifier.of("forcemaster_rpg", "knuckle_attack");

    @Override
    public SpellHandlers.ImpactResult onSpellImpact(
            RegistryEntry<Spell> spellEntry,
            SpellPower.Result power,
            LivingEntity caster,
            Entity target,
            SpellHelper.ImpactContext context
    ) {
        if (!(caster.getWorld() instanceof ServerWorld serverWorld)) {
            return new SpellHandlers.ImpactResult(true, false);
        }

        if (!consumeSpiritualSpheresAndResetTimer(caster, SPHERES_COST)) {
            return new SpellHandlers.ImpactResult(false, false);
        }

        for (int hitIndex = 0; hitIndex < HIT_DAMAGE_MULTIPLIERS.length; hitIndex++) {
            int finalHitIndex = hitIndex;
            int delay = FIRST_HIT_DELAY_TICKS + hitIndex * HIT_INTERVAL_TICKS;

            MonkDelayedTasks.runLater(serverWorld.getServer(), delay, () -> {
                if (!caster.isAlive()) {
                    return;
                }

                if (!(caster.getWorld() instanceof ServerWorld currentWorld)) {
                    return;
                }

                executeHit(currentWorld, caster, finalHitIndex);
            });
        }

        return new SpellHandlers.ImpactResult(true, false);
    }

    private static void executeHit(ServerWorld world, LivingEntity caster, int hitIndex) {
        float attackDamage = (float) caster.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        float damage = attackDamage * HIT_DAMAGE_MULTIPLIERS[hitIndex];

        Vec3d forward = horizontalForward(caster);
        List<LivingEntity> targets = findTargetsInCone(world, caster, forward);

        playPunchSound(world, caster, hitIndex);
        spawnMeteorRushVfx(world, caster, forward, hitIndex);

        for (LivingEntity victim : targets) {
            resetIFrames(victim);

            DamageSource source = attackDamageSource(world, caster);
            victim.damage(source, damage);

            spawnTargetImpactVfx(world, caster, victim, hitIndex);
        }
    }

    private static List<LivingEntity> findTargetsInCone(ServerWorld world, LivingEntity caster, Vec3d forward) {
        Vec3d origin = caster.getPos().add(0.0d, caster.getHeight() * 0.55d, 0.0d);

        if (forward.lengthSquared() < 0.0001d) {
            return List.of();
        }

        Box searchBox = caster.getBoundingBox().expand(RANGE, VERTICAL_TOLERANCE, RANGE);

        List<LivingEntity> nearby = world.getEntitiesByClass(
                LivingEntity.class,
                searchBox,
                entity -> entity.isAlive() && entity != caster
        );

        List<LivingEntity> result = new ArrayList<>();

        for (LivingEntity entity : nearby) {
            if (isAlly(caster, entity)) {
                continue;
            }

            Vec3d targetCenter = entity.getPos().add(0.0d, entity.getHeight() * 0.5d, 0.0d);
            Vec3d delta = targetCenter.subtract(origin);

            if (Math.abs(delta.y) > VERTICAL_TOLERANCE) {
                continue;
            }

            Vec3d horizontalDelta = new Vec3d(delta.x, 0.0d, delta.z);
            double horizontalDistance = horizontalDelta.length();

            if (horizontalDistance > RANGE) {
                continue;
            }

            if (horizontalDistance < 0.15d) {
                result.add(entity);
                continue;
            }

            Vec3d directionToTarget = horizontalDelta.normalize();
            double dot = forward.dotProduct(directionToTarget);

            if (dot >= CONE_DOT_LIMIT) {
                result.add(entity);
            }
        }

        return result;
    }

    private static void spawnMeteorRushVfx(ServerWorld world, LivingEntity caster, Vec3d forward, int hitIndex) {
        if (forward.lengthSquared() < 0.0001d) {
            return;
        }

        Vec3d start = caster.getPos()
                .add(0.0d, caster.getHeight() * 0.58d, 0.0d)
                .add(forward.multiply(0.45d));

        Vec3d end = start.add(forward.multiply(RANGE));

        boolean sent = TripleComboVfxNetworking.sendMeteorRush(caster, start, end, hitIndex);

        if (!sent) {
            spawnServerFallbackMeteorRush(world, start, end, hitIndex);
        }
    }

    private static void spawnTargetImpactVfx(ServerWorld world, LivingEntity caster, LivingEntity victim, int hitIndex) {
        Vec3d impactPos = victim.getPos().add(0.0d, victim.getHeight() * 0.55d, 0.0d);

        boolean sent = TripleComboVfxNetworking.sendImpact(caster, victim, impactPos, hitIndex);

        if (!sent) {
            spawnServerFallbackImpact(world, impactPos, hitIndex);
        }
    }

    private static void spawnServerFallbackMeteorRush(ServerWorld world, Vec3d start, Vec3d end, int hitIndex) {
        Vec3d delta = end.subtract(start);

        if (delta.lengthSquared() < 0.0001d) {
            return;
        }

        Vec3d direction = delta.normalize();
        Vec3d right = new Vec3d(-direction.z, 0.0d, direction.x);

        if (right.lengthSquared() < 0.0001d) {
            right = new Vec3d(1.0d, 0.0d, 0.0d);
        } else {
            right = right.normalize();
        }

        Random random = world.random;

        int lines = switch (hitIndex) {
            case 0 -> 5;
            case 1 -> 7;
            default -> 12;
        };

        int pointsPerLine = hitIndex == 2 ? 9 : 7;
        double spread = hitIndex == 2 ? 0.85d : 0.55d;

        for (int line = 0; line < lines; line++) {
            double side = (random.nextDouble() - random.nextDouble()) * spread;
            double vertical = (random.nextDouble() - random.nextDouble()) * spread * 0.35d;

            Vec3d lineEnd = end.add(right.multiply(side)).add(0.0d, vertical, 0.0d);

            for (int i = 0; i < pointsPerLine; i++) {
                double t = i / (double) Math.max(1, pointsPerLine - 1);
                Vec3d p = start.lerp(lineEnd, t);

                Vec3d velocity = direction.multiply(hitIndex == 2 ? 0.07d : 0.04d);

                world.spawnParticles(
                        ModParticles.SPIRITUAL_METEOR,
                        p.x,
                        p.y,
                        p.z,
                        1,
                        0.0d,
                        0.0d,
                        0.0d,
                        0.0d
                );

                if (i == 0 || i == 1) {
                    world.spawnParticles(
                            ParticleTypes.END_ROD,
                            p.x,
                            p.y,
                            p.z,
                            1,
                            velocity.x * 0.25d,
                            0.01d,
                            velocity.z * 0.25d,
                            0.0d
                    );
                }
            }
        }

        if (hitIndex == 2) {
            world.spawnParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    end.x,
                    end.y,
                    end.z,
                    12,
                    0.35d,
                    0.25d,
                    0.35d,
                    0.04d
            );
        }
    }

    private static void spawnServerFallbackImpact(ServerWorld world, Vec3d impactPos, int hitIndex) {
        int count = hitIndex == 2 ? 30 : 18;
        double spread = hitIndex == 2 ? 0.45d : 0.28d;

        world.spawnParticles(
                ModParticles.SPIRITUAL_METEOR,
                impactPos.x,
                impactPos.y,
                impactPos.z,
                count,
                spread,
                spread * 0.65d,
                spread,
                hitIndex == 2 ? 0.08d : 0.05d
        );

        world.spawnParticles(
                ParticleTypes.END_ROD,
                impactPos.x,
                impactPos.y,
                impactPos.z,
                hitIndex == 2 ? 12 : 5,
                spread * 0.8d,
                spread * 0.55d,
                spread * 0.8d,
                0.04d
        );

        if (hitIndex == 2) {
            world.spawnParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    impactPos.x,
                    impactPos.y,
                    impactPos.z,
                    18,
                    0.25d,
                    0.22d,
                    0.25d,
                    0.055d
            );
        }
    }

    private static Vec3d horizontalForward(LivingEntity entity) {
        Vec3d look = entity.getRotationVec(1.0f);
        Vec3d forward = new Vec3d(look.x, 0.0d, look.z);

        if (forward.lengthSquared() < 0.0001d) {
            return Vec3d.ZERO;
        }

        return forward.normalize();
    }

    private static void playPunchSound(ServerWorld world, LivingEntity caster, int hitIndex) {
        float pitch = switch (hitIndex) {
            case 0 -> 0.98f;
            case 1 -> 1.06f;
            default -> 0.84f;
        };

        float volume = hitIndex == 2 ? 0.78f : 0.58f;

        world.playSound(
                null,
                caster.getX(),
                caster.getY(),
                caster.getZ(),
                SoundEvent.of(KNUCKLE_ATTACK_SOUND),
                SoundCategory.PLAYERS,
                volume,
                pitch
        );
    }

    private static DamageSource attackDamageSource(ServerWorld world, LivingEntity caster) {
        if (caster instanceof PlayerEntity player) {
            return world.getDamageSources().playerAttack(player);
        }

        return world.getDamageSources().mobAttack(caster);
    }

    private static void resetIFrames(LivingEntity entity) {
        if (entity instanceof EntityIFramesAccessor accessor) {
            accessor.monkmod$setTimeUntilRegen(0);
        }
    }

    private static boolean consumeSpiritualSpheresAndResetTimer(LivingEntity caster, int cost) {
        var instance = caster.getStatusEffect(ModEffects.SPIRITUAL_SPHERES);

        if (instance == null) {
            return false;
        }

        int currentAmplifier = instance.getAmplifier();
        int stacks = currentAmplifier + 1;

        if (stacks < cost) {
            return false;
        }

        int newAmplifier = currentAmplifier - cost;

        boolean ambient = instance.isAmbient();
        boolean showParticles = instance.shouldShowParticles();
        boolean showIcon = instance.shouldShowIcon();

        caster.removeStatusEffect(ModEffects.SPIRITUAL_SPHERES);

        if (newAmplifier < 0) {
            return true;
        }

        caster.addStatusEffect(
                new net.minecraft.entity.effect.StatusEffectInstance(
                        ModEffects.SPIRITUAL_SPHERES,
                        SPHERES_DURATION_TICKS,
                        newAmplifier,
                        ambient,
                        showParticles,
                        showIcon
                ),
                caster
        );

        return true;
    }

    private static boolean isAlly(LivingEntity a, LivingEntity b) {
        if (a.isTeammate(b) || b.isTeammate(a)) {
            return true;
        }

        if (a instanceof PlayerEntity p1 && b instanceof PlayerEntity p2) {
            return !p1.shouldDamagePlayer(p2);
        }

        if (b instanceof TameableEntity tame && tame.isTamed() && tame.getOwnerUuid() != null) {
            return tame.getOwnerUuid().equals(a.getUuid());
        }

        return false;
    }
}