package net.gabriel.monk.spell;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.gabriel.monk.effect.ModEffects;
import net.gabriel.monk.mixin.EntityIFramesAccessor;
import net.gabriel.monk.util.MonkDelayedTasks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.event.SpellHandlers;
import net.spell_engine.api.spell.fx.ParticleBatch;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.internals.SpellHelper;
import net.spell_power.api.SpellPower;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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

    private static final long FIST_COLOR = 4289003775L;
    private static final long FIST_COLOR_BRIGHT = 4291575039L;

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

        List<LivingEntity> targets = findTargetsInCone(world, caster);

        playPunchSound(world, caster, hitIndex);
        spawnCasterPunchVfx(caster, hitIndex);

        for (LivingEntity victim : targets) {
            resetIFrames(victim);

            DamageSource source = attackDamageSource(world, caster);
            victim.damage(source, damage);

            spawnTargetImpactVfx(caster, victim, hitIndex);
        }
    }

    private static List<LivingEntity> findTargetsInCone(ServerWorld world, LivingEntity caster) {
        Vec3d origin = caster.getPos().add(0.0d, caster.getHeight() * 0.55d, 0.0d);
        Vec3d forward = horizontalForward(caster);

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

    private static void spawnCasterPunchVfx(LivingEntity caster, int hitIndex) {
        float scale = switch (hitIndex) {
            case 0 -> 0.85f;
            case 1 -> 0.85f;
            default -> 1.10f;
        };

        long color = hitIndex == 2 ? FIST_COLOR_BRIGHT : FIST_COLOR;

        List<ParticleBatch> batches = new ArrayList<>();

        ParticleBatch fist = new ParticleBatch(
                "spell_engine:sign_fist",
                ParticleBatch.Shape.LINE_VERTICAL,
                ParticleBatch.Origin.CENTER,
                1.0f,
                0.42f,
                0.42f
        )
                .scale(scale)
                .color(color)
                .followEntity(true);

        batches.add(fist);

        if (hitIndex == 2) {
            ParticleBatch burst = new ParticleBatch(
                    "spell_engine:area_effect_658",
                    ParticleBatch.Shape.SPHERE,
                    ParticleBatch.Origin.CENTER,
                    1.0f,
                    0.0f,
                    0.0f
            )
                    .scale(0.60f)
                    .extent(0.35f)
                    .color(color)
                    .followEntity(true);

            batches.add(burst);
        }

        sendBatches(caster, batches.toArray(new ParticleBatch[0]));
    }

    private static void spawnTargetImpactVfx(LivingEntity caster, LivingEntity victim, int hitIndex) {
        float fistScale = switch (hitIndex) {
            case 0 -> 0.90f;
            case 1 -> 0.90f;
            default -> 1.20f;
        };

        float ringScale = switch (hitIndex) {
            case 0 -> 0.45f;
            case 1 -> 0.45f;
            default -> 0.80f;
        };

        long color = hitIndex == 2 ? FIST_COLOR_BRIGHT : FIST_COLOR;

        List<ParticleBatch> batches = new ArrayList<>();

        ParticleBatch fist = new ParticleBatch(
                "spell_engine:sign_fist",
                ParticleBatch.Shape.LINE_VERTICAL,
                ParticleBatch.Origin.CENTER,
                1.0f,
                0.50f,
                0.50f
        )
                .scale(fistScale)
                .color(color);

        batches.add(fist);

        ParticleBatch impactRing = new ParticleBatch(
                "spell_engine:area_effect_658",
                ParticleBatch.Shape.SPHERE,
                ParticleBatch.Origin.CENTER,
                1.0f,
                0.0f,
                0.0f
        )
                .scale(ringScale)
                .extent(0.35f + ringScale * 0.25f)
                .color(color);

        batches.add(impactRing);

        if (hitIndex == 2) {
            ParticleBatch secondFist = new ParticleBatch(
                    "spell_engine:sign_fist",
                    ParticleBatch.Shape.LINE_VERTICAL,
                    ParticleBatch.Origin.CENTER,
                    1.0f,
                    0.58f,
                    0.58f
            )
                    .scale(0.95f)
                    .color(color);

            batches.add(secondFist);
        }

        sendBatchesFromCasterToTarget(caster, victim, batches.toArray(new ParticleBatch[0]));
    }

    private static void sendBatches(LivingEntity anchor, ParticleBatch[] batches) {
        Collection<ServerPlayerEntity> viewers = new HashSet<>(PlayerLookup.tracking(anchor));

        if (anchor instanceof ServerPlayerEntity serverPlayer) {
            viewers.add(serverPlayer);
        }

        ParticleHelper.sendBatches(anchor, batches, 1.0f, viewers);
    }

    private static void sendBatchesFromCasterToTarget(LivingEntity caster, LivingEntity target, ParticleBatch[] batches) {
        Collection<ServerPlayerEntity> viewers = new HashSet<>(PlayerLookup.tracking(target));

        if (caster instanceof ServerPlayerEntity serverCaster) {
            viewers.add(serverCaster);
        }

        if (target instanceof ServerPlayerEntity serverTarget) {
            viewers.add(serverTarget);
        }

        ParticleHelper.sendBatches(target, batches, 1.0f, viewers);
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