package net.gabriel.monk.spell;

import net.gabriel.monk.effect.ModEffects;
import net.gabriel.monk.entity.ModEntities;
import net.gabriel.monk.entity.SpiritualSphereProjectileEntity;
import net.gabriel.monk.util.MonkSpiritSpheres;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.event.SpellHandlers;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCastSyncHelper;
import net.spell_engine.internals.casting.SpellCasterEntity;
import net.spell_power.api.SpellPower;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ThrowSpiritSphereImpact implements SpellHandlers.CustomImpact {
    private static final float BASE_DAMAGE = 3.0f;
    private static final float DAMAGE_COEF = 0.7f;

    private static final float BASE_HEAL = 2.0f;
    private static final float HEAL_COEF = 0.2f;

    private static final float SPLASH_MULT = 0.7f;
    private static final float SPLASH_RADIUS = 2.0f;

    private static final int PROJECTILE_MAX_LIFE_TICKS = 60;
    private static final int SPHERES_DURATION_TICKS = 280;

    private static final float AIM_ASSIST_ANGLE_DEGREES = 25.0f;
    private static final float AIM_ASSIST_STRENGTH = 0.25f;
    private static final float PROJECTILE_SPEED = 1.15f;

    private static final Map<UUID, CastState> CAST_STATE = new HashMap<>();

    private static final class CastState {
        long startedAt;
        int firedCount;
        long lastFireTick;

        CastState(long startedAt) {
            this.startedAt = startedAt;
            this.firedCount = 0;
            this.lastFireTick = Long.MIN_VALUE;
        }
    }

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

        UUID uuid = caster.getUuid();
        long now = caster.getWorld().getTime();
        int maxShots = MonkSpiritSpheres.getMaxSpheres(caster);

        SpellCasterEntity casterEntity = (SpellCasterEntity) (Object) caster;
        SpellCast.Process process = casterEntity.getSpellCastProcess();

        CastState state = CAST_STATE.get(uuid);
        if (process != null) {
            long startedAt = process.startedAt();
            if (state == null || state.startedAt != startedAt) {
                state = new CastState(startedAt);
                CAST_STATE.put(uuid, state);
            }
        } else if (state == null) {
            return new SpellHandlers.ImpactResult(true, false);
        }

        if (state.firedCount < maxShots && state.lastFireTick != now) {
            boolean fired = tryFireSphere(spellEntry, power, caster, target, serverWorld, state);
            if (fired) {
                state.lastFireTick = now;
            }
        }

        if (!context.isChanneled() || state.firedCount >= maxShots) {
            if (caster instanceof PlayerEntity player) {
                if (state.firedCount > 0) {
                    stopCastingAndApplyCooldownIfPlayer(caster, spellEntry);
                } else {
                    SpellCastSyncHelper.clearCasting(player);
                }
            }
            CAST_STATE.remove(uuid);
        }

        return new SpellHandlers.ImpactResult(true, false);
    }

    private static boolean tryFireSphere(
            RegistryEntry<Spell> spellEntry,
            SpellPower.Result power,
            LivingEntity caster,
            Entity target,
            ServerWorld serverWorld,
            CastState state
    ) {
        LivingEntity livingTarget = getValidTargetInFront(caster, target);
        if (livingTarget == null) {
            return false;
        }

        if (!consumeSpiritualSpheresAndResetTimer(caster, 1)) {
            if (caster instanceof PlayerEntity player) {
                if (state.firedCount == 0) {
                    SpellCastSyncHelper.clearCasting(player);
                } else {
                    stopCastingAndApplyCooldownIfPlayer(caster, spellEntry);
                }
            }
            return false;
        }

        float spellPowerValue = (float) power.nonCriticalValue();
        float damage = BASE_DAMAGE + (DAMAGE_COEF * spellPowerValue);
        float heal = BASE_HEAL + (HEAL_COEF * spellPowerValue);

        SpiritualSphereProjectileEntity projectile =
                new SpiritualSphereProjectileEntity(ModEntities.SPIRITUAL_SPHERE_PROJECTILE, caster, serverWorld);

        projectile.configure(damage, heal, SPLASH_MULT, SPLASH_RADIUS, PROJECTILE_MAX_LIFE_TICKS);
        setVelocityWithAimAssist(projectile, caster, livingTarget);
        projectile.setHomingTarget(livingTarget);

        serverWorld.spawnEntity(projectile);
        state.firedCount++;
        return true;
    }

    private static LivingEntity getValidTargetInFront(LivingEntity caster, Entity target) {
        if (!(target instanceof LivingEntity living)) {
            return null;
        }
        if (!living.isAlive()) {
            return null;
        }
        if (living == caster) {
            return null;
        }
        if (isAlly(caster, living)) {
            return null;
        }
        if (!isTargetInsideAimCone(caster, living, AIM_ASSIST_ANGLE_DEGREES)) {
            return null;
        }
        return living;
    }

    private static boolean isTargetInsideAimCone(LivingEntity caster, LivingEntity target, float degrees) {
        Vec3d forward = caster.getRotationVec(1.0f).normalize();

        Vec3d from = caster.getEyePos();
        Vec3d to = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);

        Vec3d dirToTarget = to.subtract(from).normalize();

        double dot = forward.dotProduct(dirToTarget);
        double minDot = Math.cos(Math.toRadians(degrees));
        return dot >= minDot;
    }

    private static void setVelocityWithAimAssist(SpiritualSphereProjectileEntity projectile, LivingEntity caster, LivingEntity target) {
        Vec3d forward = caster.getRotationVec(1.0f).normalize();

        Vec3d from = caster.getEyePos();
        Vec3d to = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);

        Vec3d dirToTarget = to.subtract(from).normalize();

        Vec3d mixed = forward.multiply(1.0 - AIM_ASSIST_STRENGTH)
                .add(dirToTarget.multiply(AIM_ASSIST_STRENGTH))
                .normalize();

        projectile.setVelocity(mixed.x, mixed.y, mixed.z, PROJECTILE_SPEED, 0.0f);
    }

    private static boolean consumeSpiritualSpheresAndResetTimer(LivingEntity caster, int amount) {
        StatusEffectInstance instance = caster.getStatusEffect(ModEffects.SPIRITUAL_SPHERES);
        if (instance == null) {
            return false;
        }

        int amplifier = instance.getAmplifier();
        int spheres = amplifier + 1;
        if (spheres < amount) {
            return false;
        }

        int newAmplifier = amplifier - amount;

        boolean ambient = instance.isAmbient();
        boolean showParticles = instance.shouldShowParticles();
        boolean showIcon = instance.shouldShowIcon();

        caster.removeStatusEffect(ModEffects.SPIRITUAL_SPHERES);

        if (newAmplifier < 0) {
            return true;
        }

        caster.addStatusEffect(
                new StatusEffectInstance(ModEffects.SPIRITUAL_SPHERES, SPHERES_DURATION_TICKS, newAmplifier, ambient, showParticles, showIcon),
                caster
        );

        return true;
    }

    private static void stopCastingAndApplyCooldownIfPlayer(LivingEntity caster, RegistryEntry<Spell> spellEntry) {
        if (!(caster instanceof PlayerEntity player)) {
            return;
        }

        float cooldownSeconds = SpellHelper.getCooldownDuration(player, spellEntry);
        int cooldownTicks = Math.round(cooldownSeconds * 20.0f);
        if (cooldownTicks > 0) {
            SpellCasterEntity casterEntity = (SpellCasterEntity) (Object) player;
            var cooldownManager = casterEntity.getCooldownManager();
            cooldownManager.set(spellEntry, cooldownTicks);
        }

        SpellCastSyncHelper.clearCasting(player);
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