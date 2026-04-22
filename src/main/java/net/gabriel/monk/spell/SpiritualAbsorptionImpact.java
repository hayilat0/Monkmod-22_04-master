package net.gabriel.monk.spell;

import net.gabriel.monk.entity.ModEntities;
import net.gabriel.monk.entity.SpiritualSphereProjectileEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.event.SpellHandlers;
import net.spell_engine.internals.SpellHelper;
import net.spell_power.api.SpellPower;

import java.util.List;

public final class SpiritualAbsorptionImpact implements SpellHandlers.CustomImpact {

    private static final float DAMAGE_COEF = 0.3f;
    private static final float HEAL_COEF = 0.3f;
    private static final double AREA_RADIUS = 1.0d;

    private static final int RETURN_SPHERE_MAX_LIFE_TICKS = 36;

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

        if (!(target instanceof LivingEntity livingTarget) || !livingTarget.isAlive()) {
            return new SpellHandlers.ImpactResult(true, false);
        }

        Vec3d center = livingTarget.getPos();
        Box area = new Box(center, center).expand(AREA_RADIUS);

        List<LivingEntity> entities = serverWorld.getEntitiesByClass(
                LivingEntity.class,
                area,
                e -> e.isAlive() && e != caster
        );

        float arcanePower = (float) power.nonCriticalValue();
        float damage = DAMAGE_COEF * arcanePower;
        float healPerEnemy = HEAL_COEF * arcanePower;

        for (LivingEntity entity : entities) {
            if (isAlly(caster, entity)) {
                continue;
            }

            entity.damage(serverWorld.getDamageSources().magic(), damage);

            // Agora a esfera visual é quem vai conceder a cura + 1 stack ao chegar no usuário
            spawnReturnSphere(serverWorld, caster, entity, healPerEnemy);
        }

        return new SpellHandlers.ImpactResult(true, false);
    }

    private static void spawnReturnSphere(ServerWorld world, LivingEntity caster, LivingEntity enemy, float healAmount) {
        Vec3d enemyCenter = enemy.getPos().add(0.0, enemy.getHeight() * 0.68, 0.0);

        SpiritualSphereProjectileEntity projectile =
                new SpiritualSphereProjectileEntity(ModEntities.SPIRITUAL_SPHERE_PROJECTILE, caster, world);

        projectile.configureVisualReturn(healAmount, RETURN_SPHERE_MAX_LIFE_TICKS);
        projectile.setPosition(enemyCenter.x, enemyCenter.y, enemyCenter.z);
        projectile.setHomingTarget(caster);

        world.spawnEntity(projectile);
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