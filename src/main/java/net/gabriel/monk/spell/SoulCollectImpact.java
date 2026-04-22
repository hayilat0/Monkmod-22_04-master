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

public final class SoulCollectImpact implements SpellHandlers.CustomImpact {
    private static final int SPHERES_DURATION_TICKS = 280;

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

        int maxSpheres = MonkSpiritSpheres.getMaxSpheres(caster);
        int amplifier = maxSpheres - 1;

        caster.removeStatusEffect(ModEffects.SPIRITUAL_SPHERES);
        caster.addStatusEffect(
                new StatusEffectInstance(
                        ModEffects.SPIRITUAL_SPHERES,
                        SPHERES_DURATION_TICKS,
                        amplifier,
                        true,
                        true,
                        true
                ),
                caster
        );

        return new SpellHandlers.ImpactResult(true, false);
    }
}