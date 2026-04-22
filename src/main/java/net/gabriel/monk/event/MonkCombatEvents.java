package net.gabriel.monk.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.gabriel.monk.effect.ModEffects;
import net.gabriel.monk.effect.VigorBarrierEffect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.spell_engine.api.spell.fx.ParticleBatch;
import net.spell_engine.fx.ParticleHelper;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MonkCombatEvents {

    // ICD 1s (20 ticks) — (seu)
    private static final Map<UUID, Integer> LAST_VIGOR_PROC_TICK = new HashMap<>();
    private static final int VIGOR_ICD_TICKS = 20;

    // ===== Concessão Espiritual =====
    // Mesma cor roxa que você usa no ethereal_step.json (color_rgba: 2974679039)
    private static final long ARCANE_PURPLE_RGBA = 2974679039L;

    // Remove o efeito no fim do tick (assim multi-hit/sweep no mesmo ataque ainda fica buffado)
    private static final Set<UUID> CONCESSION_CLEAR_END_TICK = new HashSet<>();

    // Garante que só recupera 1 esfera por ataque/tick
    private static final Set<UUID> CONCESSION_RESTORE_ONCE = new HashSet<>();

    // duração padrão das esferas (igual seu EtherealStepImpact)
    private static final int SPHERES_DURATION_TICKS = 14 * 20;

    // ✅ Marca o "alvo principal" do ataque (clique esquerdo)
    private static final Map<UUID, PrimaryTargetMark> PRIMARY_TARGET = new HashMap<>();

    private record PrimaryTargetMark(UUID targetUuid, int tick) {}

    public static void register() {

        // =========================================================
        // ✅ Captura o alvo principal do clique/ataque
        // =========================================================
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity living)) return ActionResult.PASS;

            // Guarda o alvo clicado neste tick (alvo principal)
            PRIMARY_TARGET.put(sp.getUuid(), new PrimaryTargetMark(living.getUuid(), sp.age));
            return ActionResult.PASS;
        });

        // =========================================================
        // ✅ Lógica de combate/efeitos
        // =========================================================
        ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, baseDamageTaken, damageTaken, blocked) -> {
            if (damageTaken <= 0) return;
            if (!(target instanceof LivingEntity livingTarget)) return;

            Entity attackerEntity = source.getAttacker();
            if (!(attackerEntity instanceof ServerPlayerEntity attacker)) return;

            // Só consome em hit melee direto (não gasta em DOT/magia/projétil)
            Entity direct = source.getSource();
            boolean isDirectMelee = (direct == attacker);

            // Determina se esse target é o alvo principal clicado neste tick
            boolean isPrimaryTarget = false;
            PrimaryTargetMark mark = PRIMARY_TARGET.get(attacker.getUuid());
            if (mark != null && mark.tick == attacker.age) {
                isPrimaryTarget = livingTarget.getUuid().equals(mark.targetUuid);
            }

            // =========================
            // ✅ CONCESSÃO ESPIRITUAL
            // =========================
            if (isDirectMelee && attacker.hasStatusEffect(ModEffects.SPIRITUAL_CONCESSION)) {

                // ✅ Agora: TODOS os efeitos visuais apenas no alvo principal
                if (isPrimaryTarget) {
                    spawnSpiritualConcessionHitVfx(attacker, livingTarget);
                }

                UUID id = attacker.getUuid();

                // Recupera 1 esfera (somente 1 vez por ataque/tick)
                if (CONCESSION_RESTORE_ONCE.add(id)) {
                    restoreOneSpiritualSphere(attacker);
                }

                // Consome o buff no fim do tick (melhor para hits múltiplos do mesmo ataque)
                CONCESSION_CLEAR_END_TICK.add(id);
            }

            // =========================
            // ✅ CHAKRA DO VIGOR (SEU)
            // =========================
            if (!attacker.hasStatusEffect(ModEffects.VIGOR_CHAKRA)) return;

            // ICD
            int now = attacker.age;
            Integer last = LAST_VIGOR_PROC_TICK.get(attacker.getUuid());
            if (last != null && (now - last) < VIGOR_ICD_TICKS) return;

            // 30% chance
            if (attacker.getRandom().nextFloat() > 0.3f) return;

            LAST_VIGOR_PROC_TICK.put(attacker.getUuid(), now);

            // Aplica o seu efeito "vigor_barrier"
            attacker.addStatusEffect(new StatusEffectInstance(
                    ModEffects.VIGOR_BARRIER,
                    VigorBarrierEffect.DURATION_TICKS,
                    0,
                    true,
                    true,
                    true
            ), attacker);

            // Dá o shield (corações dourados)
            VigorBarrierEffect.applyShield(attacker);
        });

        // ✅ Consumir Concessão no fim do tick
        ServerTickEvents.END_SERVER_TICK.register(MonkCombatEvents::endServerTick);
    }

    private static void endServerTick(MinecraftServer server) {
        if (!CONCESSION_CLEAR_END_TICK.isEmpty()) {
            for (UUID id : CONCESSION_CLEAR_END_TICK) {
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
                if (p == null) continue;
                p.removeStatusEffect(ModEffects.SPIRITUAL_CONCESSION);
            }
            CONCESSION_CLEAR_END_TICK.clear();
        }

        // limpa por tick
        CONCESSION_RESTORE_ONCE.clear();

        // limpa marcações antigas
        PRIMARY_TARGET.entrySet().removeIf(e -> {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(e.getKey());
            if (p == null) return true;
            return (p.age - e.getValue().tick) > 2;
        });
    }

    /**
     * Recupera 1 esfera espiritual (stack +1), até o limite de 5.
     * Reseta o timer para 14s (igual seu comportamento no Passo Etéreo).
     */
    private static void restoreOneSpiritualSphere(ServerPlayerEntity player) {
        StatusEffectInstance inst = player.getStatusEffect(ModEffects.SPIRITUAL_SPHERES);

        int currentAmp = (inst == null) ? -1 : inst.getAmplifier(); // amp 0 = 1 esfera
        int newAmp = Math.min(4, currentAmp + 1); // max 5 esferas -> amp 4

        boolean ambient = (inst != null) && inst.isAmbient();
        boolean showParticles = (inst == null) || inst.shouldShowParticles();
        boolean showIcon = (inst == null) || inst.shouldShowIcon();

        if (inst != null) {
            player.removeStatusEffect(ModEffects.SPIRITUAL_SPHERES);
        }

        player.addStatusEffect(new StatusEffectInstance(
                ModEffects.SPIRITUAL_SPHERES,
                SPHERES_DURATION_TICKS,
                newAmp,
                ambient,
                showParticles,
                showIcon
        ), player);
    }

    /**
     * ✅ Partículas Spell Engine roxas (agora 100% só no alvo principal):
     * - spell_engine:magic_arcane_burst
     * - spell_engine:magic_arcane_float
     * - spell_engine:magic_arcane_decelerate
     * - spell_engine:aura_effect_649
     */
    private static void spawnSpiritualConcessionHitVfx(ServerPlayerEntity attacker, LivingEntity target) {

        ParticleBatch burst = new ParticleBatch(
                "spell_engine:magic_arcane_burst",
                ParticleBatch.Shape.SPHERE,
                ParticleBatch.Origin.CENTER,
                14.0f,
                0.04f,
                0.14f
        )
                .color(ARCANE_PURPLE_RGBA)
                .scale(1.15f)
                .extent(0.60f);

        ParticleBatch floaty = new ParticleBatch(
                "spell_engine:magic_arcane_float",
                ParticleBatch.Shape.SPHERE,
                ParticleBatch.Origin.CENTER,
                10.0f,
                0.02f,
                0.08f
        )
                .color(ARCANE_PURPLE_RGBA)
                .scale(1.05f)
                .maxAge(1.00f)
                .extent(0.55f);

        ParticleBatch decel = new ParticleBatch(
                "spell_engine:magic_arcane_ascend",
                ParticleBatch.Shape.SPHERE,
                ParticleBatch.Origin.CENTER,
                4.0f,
                0.02f,
                0.08f
        )
                .color(ARCANE_PURPLE_RGBA)
                .scale(1.00f)
                .maxAge(0.90f)
                .extent(0.45f);

        ParticleBatch auraInner = new ParticleBatch(
                "spell_engine:aura_effect_649",
                ParticleBatch.Shape.SPHERE,
                ParticleBatch.Origin.CENTER,
                6.0f,
                0.00f,
                0.00f
        )
                .color(ARCANE_PURPLE_RGBA)
                .scale(1.30f)
                .maxAge(1.10f)
                .extent(0.75f);

        ParticleBatch auraOuter = new ParticleBatch(
                "spell_engine:aura_effect_649",
                ParticleBatch.Shape.SPHERE,
                ParticleBatch.Origin.CENTER,
                4.0f,
                0.00f,
                0.00f
        )
                .color(ARCANE_PURPLE_RGBA)
                .scale(1.60f)
                .maxAge(1.25f)
                .extent(1.00f);

        ParticleBatch[] batches = new ParticleBatch[]{burst, floaty, decel, auraInner, auraOuter};

        // manda para quem está vendo o alvo + garante atacante
        Collection<ServerPlayerEntity> viewers = new HashSet<>(PlayerLookup.tracking(target));
        viewers.add(attacker);
        if (target instanceof ServerPlayerEntity sp) viewers.add(sp);

        ParticleHelper.sendBatches(target, batches, 1.0f, viewers);
    }

    private MonkCombatEvents() {}
}
