package net.gabriel.monk.spell;

import net.gabriel.monk.effect.ModEffects;
import net.gabriel.monk.util.MonkSpiritSpheres;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.event.SpellHandlers;
import net.spell_engine.internals.SpellHelper;
import net.spell_power.api.SpellPower;

public final class SpiritSphereSummonImpact implements SpellHandlers.CustomImpact {

    private static final int DURATION_TICKS = 14 * 20;
    private static final int GAIN = 1;

    @Override
    public SpellHandlers.ImpactResult onSpellImpact(
            RegistryEntry<Spell> spellEntry,
            SpellPower.Result power,
            LivingEntity caster,
            Entity target,
            SpellHelper.ImpactContext context
    ) {
        if (!(caster.getWorld() instanceof ServerWorld)) {
            return new SpellHandlers.ImpactResult(true, false);
        }

        int maxAllowed = MonkSpiritSpheres.getMaxSpheres(caster);

        StatusEffectInstance inst = caster.getStatusEffect(ModEffects.SPIRITUAL_SPHERES);
        int currentStacks = (inst == null) ? 0 : (inst.getAmplifier() + 1);

        int newStacks = Math.min(maxAllowed, currentStacks + GAIN);
        if (newStacks <= 0) {
            caster.removeStatusEffect(ModEffects.SPIRITUAL_SPHERES);
            return new SpellHandlers.ImpactResult(true, false);
        }

        // Reaplica pra sempre resetar o timer (igual era no JSON)
        caster.removeStatusEffect(ModEffects.SPIRITUAL_SPHERES);

        caster.addStatusEffect(
                new StatusEffectInstance(
                        ModEffects.SPIRITUAL_SPHERES,
                        DURATION_TICKS,
                        newStacks - 1,
                        true,
                        true,
                        true
                ),
                caster
        );

        return new SpellHandlers.ImpactResult(true, false);
    }
}
