package net.gabriel.monk.spell;

import net.gabriel.monk.effect.ModEffects;
import net.gabriel.monk.network.EtherealStepVfxNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.event.SpellHandlers;
import net.spell_engine.internals.SpellHelper;
import net.spell_power.api.SpellPower;

import java.util.Set;

/**
 * Passo Etéreo:
 * - Requer >= 2 esferas
 * - Consome 2 esferas
 * - Se sobrar >= 1, RESETA timer pra 14s (com novo amplifier)
 *
 * NOVO:
 * - Aplica Concessão Espiritual por 4s
 */
public final class EtherealStepImpact implements SpellHandlers.CustomImpact {

    private static final double MAX_DISTANCE = 7.0d;
    private static final int COST_STACKS = 2;

    private static final int SPHERES_DURATION_TICKS = 14 * 20;

    // ✅ Concessão Espiritual: 4s
    private static final int CONCESSION_DURATION_TICKS = 4 * 20;

    @Override
    public SpellHandlers.ImpactResult onSpellImpact(RegistryEntry<Spell> spell,
                                                    SpellPower.Result power,
                                                    LivingEntity caster,
                                                    Entity target,
                                                    SpellHelper.ImpactContext context) {

        if (!(caster.getWorld() instanceof ServerWorld serverWorld)) {
            return new SpellHandlers.ImpactResult(true, false);
        }

        StatusEffectInstance inst = caster.getStatusEffect(ModEffects.SPIRITUAL_SPHERES);
        int stacks = (inst == null) ? 0 : (inst.getAmplifier() + 1);
        if (stacks < COST_STACKS) {
            return new SpellHandlers.ImpactResult(false, false);
        }

        Vec3d dir = caster.getRotationVec(1.0f);
        if (dir.lengthSquared() < 1.0E-6) {
            return new SpellHandlers.ImpactResult(false, false);
        }
        dir = dir.normalize();

        Vec3d startEye = caster.getEyePos();
        Vec3d endEye = startEye.add(dir.multiply(MAX_DISTANCE));

        // Raycast em blocos
        BlockHitResult hit = serverWorld.raycast(new RaycastContext(
                startEye,
                endEye,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                caster
        ));

        Vec3d desiredEye;
        if (hit.getType() == HitResult.Type.MISS) {
            desiredEye = endEye;
        } else {
            // recua um pouco pra evitar enfiar no bloco
            desiredEye = hit.getPos().subtract(dir.multiply(0.55d));
        }

        // Converte de posição do olho -> posição dos pés
        double eyeOffset = caster.getEyeY() - caster.getY();
        Vec3d desiredFeet = new Vec3d(desiredEye.x, desiredEye.y - eyeOffset, desiredEye.z);

        Vec3d safeSpot = findSafeSpot(serverWorld, caster, desiredFeet, dir);
        if (safeSpot == null) {
            return new SpellHandlers.ImpactResult(false, false);
        }

        // Evita "teleport inútil" (quase no mesmo lugar)
        if (safeSpot.squaredDistanceTo(caster.getPos()) < 0.12d) {
            return new SpellHandlers.ImpactResult(false, false);
        }

        Vec3d from = caster.getPos();

        teleportServer(serverWorld, caster, safeSpot);

        // Consome 2 e reseta duração pra 14s com novo amp
        consumeSpiritualSpheresAndResetTimer(caster, inst, COST_STACKS);

        caster.setVelocity(Vec3d.ZERO);
        caster.velocityModified = true;
        caster.fallDistance = 0.0f;

        // VFX original (blink start/end)
        blinkVfx(serverWorld, from);
        blinkVfx(serverWorld, safeSpot);

        // NOVO: rastro no caminho (server-side, todo mundo vê)
        trailVfx(serverWorld, from, safeSpot);

        // NOVO: afterimages "andando" no trajeto (client-side)
        EtherealStepVfxNetworking.send(caster, from, safeSpot);

        // ✅ NOVO: aplica Concessão Espiritual por 4s
        caster.addStatusEffect(new StatusEffectInstance(
                ModEffects.SPIRITUAL_CONCESSION,
                CONCESSION_DURATION_TICKS,
                0,
                true,
                true,
                true
        ), caster);

        return new SpellHandlers.ImpactResult(true, false);
    }

    private static void blinkVfx(ServerWorld world, Vec3d pos) {
        world.spawnParticles(
                ParticleTypes.PORTAL,
                pos.x, pos.y + 1.0d, pos.z,
                30,
                0.25d, 0.45d, 0.25d,
                0.12d
        );

        world.playSound(
                null,
                pos.x, pos.y, pos.z,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS,
                0.75f,
                1.05f
        );
    }

    /**
     * Rastro “etéreo” no trajeto. Isso dá a sensação de deslocamento/corrida.
     * (Server-side -> todo mundo vê)
     */
    private static void trailVfx(ServerWorld world, Vec3d start, Vec3d end) {
        Vec3d delta = end.subtract(start);
        double dist = delta.length();
        if (dist < 0.01d) return;

        Vec3d dir = delta.normalize();
        int steps = Math.max(10, (int) Math.ceil(dist * 8.0d));

        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;

            // wobble lateral discreto pra ficar mais “orgânico/etéreo”
            double wobble = Math.sin((t * Math.PI) * 2.0d) * 0.18d;

            Vec3d base = start.add(delta.multiply(t));
            Vec3d side = new Vec3d(-dir.z, 0.0d, dir.x).multiply(wobble);

            double x = base.x + side.x;
            double y = base.y + 1.0d + (Math.sin(t * Math.PI) * 0.15d);
            double z = base.z + side.z;

            // “brilhos” finos + “distorção” tipo portal
            world.spawnParticles(ParticleTypes.END_ROD, x, y, z,
                    1, 0.02d, 0.04d, 0.02d, 0.0d);
            world.spawnParticles(ParticleTypes.PORTAL, x, y, z,
                    1, 0.03d, 0.06d, 0.03d, 0.02d);
        }
    }

    private static void teleportServer(ServerWorld world, LivingEntity entity, Vec3d pos) {
        float yaw = entity.getYaw();
        float pitch = entity.getPitch();
        entity.teleport(world, pos.x, pos.y, pos.z, Set.of(), yaw, pitch);
    }

    private static Vec3d findSafeSpot(ServerWorld world, LivingEntity entity, Vec3d desiredFeet, Vec3d dir) {
        double d = 0.0d;

        // offsets Y: 0, +0.5, +1, -0.5
        double[] yOffsets = new double[]{0.0d, 0.5d, 1.0d, -0.5d};

        while (d <= MAX_DISTANCE) {
            // ✅ CORRIGIDO: volta em direção ao caster
            Vec3d base = desiredFeet.subtract(dir.multiply(d));

            for (double yOff : yOffsets) {
                Vec3d candidate = base.add(0.0d, yOff, 0.0d);

                BlockPos bp = BlockPos.ofFloored(candidate);

                // garante que chunk está carregado (evita teleport pra nada)
                if (!world.isChunkLoaded(bp)) continue;

                Box movedBox = entity.getBoundingBox().offset(
                        candidate.x - entity.getX(),
                        candidate.y - entity.getY(),
                        candidate.z - entity.getZ()
                );

                if (world.isSpaceEmpty(entity, movedBox)) {
                    return candidate;
                }
            }

            d += 0.35d;
        }

        return null;
    }

    private static void consumeSpiritualSpheresAndResetTimer(LivingEntity caster, StatusEffectInstance inst, int cost) {
        if (inst == null) return;

        int stacks = inst.getAmplifier() + 1;
        int newStacks = stacks - cost;

        caster.removeStatusEffect(ModEffects.SPIRITUAL_SPHERES);

        if (newStacks <= 0) {
            return;
        }

        int newAmp = newStacks - 1;

        caster.addStatusEffect(new StatusEffectInstance(
                ModEffects.SPIRITUAL_SPHERES,
                SPHERES_DURATION_TICKS,
                newAmp,
                inst.isAmbient(),
                inst.shouldShowParticles(),
                inst.shouldShowIcon()
        ));
    }
}
