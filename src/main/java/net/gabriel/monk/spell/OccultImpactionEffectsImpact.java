package net.gabriel.monk.spell;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.gabriel.monk.effect.ModEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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

/**
 * Impacto Psíquico / Occult Impaction:
 * - Consome 1 esfera
 * - Reseta o timer das esferas para 14s
 *
 * ✅ Solta partículas Spell Engine em CADA alvo acertado:
 * - alvo principal (dano máximo): círculo + partículas arcanas
 * - alvos splash: APENAS partículas arcanas (sem círculo)
 */
public class OccultImpactionEffectsImpact implements SpellHandlers.CustomImpact {

    private static final float BASE_DAMAGE = 4.0f;
    private static final float ARCANE_COEF = 0.8f;

    private static final float SPLASH_MULT = 0.6f;
    private static final double SPLASH_RADIUS = 2.0d;

    private static final int VIGOR_CHAKRA_DURATION_TICKS = 80;
    private static final int SPHERES_COST = 1;

    private static final int SPHERES_DURATION_TICKS = 14 * 20;

    // ===============================
    // ✅ VFX Spell Engine (arcano roxo)
    // ===============================
    private static final long ARCANE_PURPLE_RGBA = 2974679039L;
    private static final float MAIN_SCALE = 1.00f;
    private static final float SPLASH_SCALE = 0.78f;

    @Override
    public SpellHandlers.ImpactResult onSpellImpact(RegistryEntry<Spell> spell,
                                                    SpellPower.Result power,
                                                    LivingEntity caster,
                                                    Entity target,
                                                    SpellHelper.ImpactContext context) {

        if (!(caster.getWorld() instanceof ServerWorld serverWorld)) {
            // client-side: não faz lógica, só deixa o cast fluir
            return new SpellHandlers.ImpactResult(true, false);
        }

        if (!(target instanceof LivingEntity livingTarget) || !livingTarget.isAlive()) {
            return new SpellHandlers.ImpactResult(false, false);
        }

        if (!consumeSpiritualSpheresAndResetTimer(caster, SPHERES_COST)) {
            return new SpellHandlers.ImpactResult(false, false);
        }

        float arc = (float) power.nonCriticalValue();
        float damage = BASE_DAMAGE + (ARCANE_COEF * arc);

        // ===============================
        // ✅ Dano no alvo principal
        // ===============================
        livingTarget.damage(serverWorld.getDamageSources().magic(), damage);

        // ✅ VFX no alvo principal (COM círculo)
        spawnOccultImpactionHitVfx(caster, livingTarget, MAIN_SCALE, true);

        // ===============================
        // ✅ Splash
        // ===============================
        Vec3d center = livingTarget.getPos();
        Box area = new Box(center, center).expand(SPLASH_RADIUS);

        float splashDamage = damage * SPLASH_MULT;

        List<LivingEntity> entities = serverWorld.getEntitiesByClass(
                LivingEntity.class,
                area,
                e -> e.isAlive() && e != caster && e != livingTarget
        );

        for (LivingEntity e : entities) {
            e.damage(serverWorld.getDamageSources().magic(), splashDamage);

            // ✅ VFX nos alvos splash (SEM círculo — só partículas arcanas)
            spawnOccultImpactionHitVfx(caster, e, SPLASH_SCALE, false);
        }

        // ===============================
        // ✅ Buff no caster
        // ===============================
        caster.addStatusEffect(new StatusEffectInstance(
                ModEffects.VIGOR_CHAKRA,
                VIGOR_CHAKRA_DURATION_TICKS,
                0,
                true,
                true,
                true
        ), caster);

        return new SpellHandlers.ImpactResult(true, false);
    }

    /**
     * VFX Spell Engine no alvo atingido.
     *
     * includeCircle:
     * - true  -> inclui o "círculo" (area_effect_609) + area effect extra
     * - false -> só partículas arcanas (burst + ascend)
     */
    private static void spawnOccultImpactionHitVfx(LivingEntity caster, LivingEntity victim, float scaleMul, boolean includeCircle) {

        // ✅ Partículas arcanas (sempre)
        ParticleBatch burst = new ParticleBatch(
                "spell_engine:magic_arcane_burst",
                ParticleBatch.Shape.SPHERE,
                ParticleBatch.Origin.CENTER,
                14.0f * scaleMul,
                0.05f,
                0.16f
        )
                .color(ARCANE_PURPLE_RGBA)
                .scale(1.15f * scaleMul)
                .extent(0.65f * scaleMul);

        ParticleBatch ascend = new ParticleBatch(
                "spell_engine:magic_arcane_ascend",
                ParticleBatch.Shape.SPHERE,
                ParticleBatch.Origin.CENTER,
                5.0f * scaleMul,
                0.02f,
                0.08f
        )
                .color(ARCANE_PURPLE_RGBA)
                .scale(1.00f * scaleMul)
                .maxAge(0.95f)
                .extent(0.50f * scaleMul);

        ArrayList<ParticleBatch> list = new ArrayList<>();
        list.add(burst);
        list.add(ascend);

        // ✅ Círculo / area effect (apenas no alvo principal)
        if (includeCircle) {

            ParticleBatch area = new ParticleBatch(
                    "spell_engine:area_effect_658",
                    ParticleBatch.Shape.SPHERE,
                    ParticleBatch.Origin.CENTER,
                    1.0f,
                    0.0f,
                    0.0f
            )
                    .color(ARCANE_PURPLE_RGBA)
                    .scale(1.75f * scaleMul)
                    .maxAge(1.10f)
                    .extent(0.95f * scaleMul);

            ParticleBatch area609 = new ParticleBatch(
                    "spell_engine:area_effect_609",
                    ParticleBatch.Shape.SPHERE,
                    ParticleBatch.Origin.CENTER,
                    2.0f,
                    0.0f,
                    0.0f
            )
                    .color(ARCANE_PURPLE_RGBA)
                    .scale(1.55f * scaleMul)
                    .maxAge(1.25f)
                    .extent(0.85f * scaleMul);

            // coloca primeiro pra aparecer como "impact ring" antes do burst
            list.add(0, area);
            list.add(area609);
        }

        ParticleBatch[] batches = list.toArray(new ParticleBatch[0]);

        // manda pra quem está vendo o alvo + garante caster
        Collection<ServerPlayerEntity> viewers = new HashSet<>(PlayerLookup.tracking(victim));
        if (caster instanceof ServerPlayerEntity sp) viewers.add(sp);
        if (victim instanceof ServerPlayerEntity sp2) viewers.add(sp2);

        ParticleHelper.sendBatches(victim, batches, 1.0f, viewers);
    }

    /**
     * Consome N esferas e, se ainda sobrar >= 1 esfera, reaplica o efeito com duração cheia (14s).
     *
     * IMPORTANTE:
     * - precisa REMOVER o efeito antes de reaplicar, senão o jogo pode ignorar a versão "mais fraca"
     *   (amplifier menor) e você não perde esferas.
     */
    private static boolean consumeSpiritualSpheresAndResetTimer(LivingEntity caster, int cost) {
        StatusEffectInstance inst = caster.getStatusEffect(ModEffects.SPIRITUAL_SPHERES);
        if (inst == null) return false;

        int currentAmp = inst.getAmplifier();          // stacks = amp + 1
        int stacks = currentAmp + 1;
        if (stacks < cost) return false;

        int newAmp = currentAmp - cost;                // reduz cost esferas

        boolean ambient = inst.isAmbient();
        boolean showParticles = inst.shouldShowParticles();
        boolean showIcon = inst.shouldShowIcon();

        // REMOVE primeiro para garantir que a redução de amplifier aplica
        caster.removeStatusEffect(ModEffects.SPIRITUAL_SPHERES);

        // Se acabou (newAmp < 0), não reaplica
        if (newAmp < 0) {
            return true;
        }

        caster.addStatusEffect(new StatusEffectInstance(
                ModEffects.SPIRITUAL_SPHERES,
                SPHERES_DURATION_TICKS,
                newAmp,
                ambient,
                showParticles,
                showIcon
        ), caster);

        return true;
    }
}
