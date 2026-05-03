package net.gabriel.monk.spell;

import net.gabriel.monk.Monkmod;
import net.minecraft.util.Identifier;
import net.spell_engine.api.spell.event.SpellHandlers;

public final class MonkCustomSpellImpact {

    public static final Identifier OCCULT_IMPACTION_EFFECTS =
            Identifier.of(Monkmod.MOD_ID, "occult_impaction_effects");

    public static final Identifier THROW_SPIRIT_SPHERE =
            Identifier.of(Monkmod.MOD_ID, "throw_spirit_sphere");

    public static final Identifier ETHEREAL_STEP =
            Identifier.of(Monkmod.MOD_ID, "ethereal_step");

    public static final Identifier SPIRIT_SPHERE_SUMMON =
            Identifier.of(Monkmod.MOD_ID, "spirit_sphere_summon");

    public static final Identifier SOUL_COLLECT =
            Identifier.of(Monkmod.MOD_ID, "soul_collect");

    public static final Identifier SPIRITUAL_ABSORPTION =
            Identifier.of(Monkmod.MOD_ID, "spiritual_absorption");

    public static final Identifier SPIRIT_TRANSFER =
            Identifier.of(Monkmod.MOD_ID, "spirit_transfer");

    public static final Identifier TRIPLE_COMBO =
            Identifier.of(Monkmod.MOD_ID, "triple_combo");

    public static void registerCustomImpacts() {
        SpellHandlers.registerCustomImpact(OCCULT_IMPACTION_EFFECTS, new OccultImpactionEffectsImpact());
        SpellHandlers.registerCustomImpact(THROW_SPIRIT_SPHERE, new ThrowSpiritSphereImpact());
        SpellHandlers.registerCustomImpact(ETHEREAL_STEP, new EtherealStepImpact());
        SpellHandlers.registerCustomImpact(SPIRIT_SPHERE_SUMMON, new SpiritSphereSummonImpact());
        SpellHandlers.registerCustomImpact(SOUL_COLLECT, new SoulCollectImpact());
        SpellHandlers.registerCustomImpact(SPIRITUAL_ABSORPTION, new SpiritualAbsorptionImpact());
        SpellHandlers.registerCustomImpact(SPIRIT_TRANSFER, new SpiritTransferImpact());
        SpellHandlers.registerCustomImpact(TRIPLE_COMBO, new TripleComboImpact());
    }

    private MonkCustomSpellImpact() {
    }
}