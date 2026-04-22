package net.gabriel.monk.spell;

import net.gabriel.monk.effect.ModEffects;
import net.gabriel.monk.entity.ModEntities;
import net.gabriel.monk.entity.SpiritualSphereProjectileEntity;
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
import net.spell_power.api.SpellPower;

public final class SpiritTransferImpact implements SpellHandlers.CustomImpact {
    private static final float HEAL_COEF = 0.85f;
    private static final float SPLASH_RADIUS = 2.0f;
    private static final int PROJECTILE_MAX_LIFE_TICKS = 60;
    private static final int VIGOR_CHAKRA_DURATION_TICKS = 80;

    private static final float AIM_ASSIST_ANGLE_DEGREES = 25.0f;
    private static final float AIM_ASSIST_STRENGTH = 0.25f;
    private static final float PROJECTILE_SPEED = 1.15f;

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

        LivingEntity allyTarget = getValidAllyTargetInFront(caster, target);
        if (allyTarget == null) {
            return new SpellHandlers.ImpactResult(false, false);
        }

        if (!consumeSpiritualSpheresAndResetTimer(caster, 1)) {
            return new SpellHandlers.ImpactResult(false, false);
        }

        float healAmount = HEAL_COEF * (float) power.nonCriticalValue();

        SpiritualSphereProjectileEntity projectile =
                new SpiritualSphereProjectileEntity(ModEntities.SPIRITUAL_SPHERE_PROJECTILE, caster, serverWorld);

        projectile.configureSpiritTransfer(
                healAmount,
                SPLASH_RADIUS,
                VIGOR_CHAKRA_DURATION_TICKS,
                PROJECTILE_MAX_LIFE_TICKS
        );

        setVelocityWithAimAssist(projectile, caster, allyTarget);
        projectile.setHomingTarget(allyTarget);

        serverWorld.spawnEntity(projectile);

        return new SpellHandlers.ImpactResult(true, false);
    }

    private static LivingEntity getValidAllyTargetInFront(LivingEntity caster, Entity target) {
        if (!(target instanceof LivingEntity living)) return null;
        if (!living.isAlive()) return null;
        if (living == caster) return null;
        if (!isAlly(caster, living)) return null;

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
        if (instance == null) return false;

        int amplifier = instance.getAmplifier();
        int spheres = amplifier + 1;

        if (spheres < amount) return false;

        int newAmplifier = amplifier - amount;

        boolean ambient = instance.isAmbient();
        boolean showParticles = instance.shouldShowParticles();
        boolean showIcon = instance.shouldShowIcon();

        caster.removeStatusEffect(ModEffects.SPIRITUAL_SPHERES);

        if (newAmplifier < 0) {
            return true;
        }

        caster.addStatusEffect(
                new StatusEffectInstance(ModEffects.SPIRITUAL_SPHERES, 280, newAmplifier, ambient, showParticles, showIcon),
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