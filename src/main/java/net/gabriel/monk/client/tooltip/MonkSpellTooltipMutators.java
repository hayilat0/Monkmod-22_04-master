package net.gabriel.monk.client.tooltip;

import net.gabriel.monk.Monkmod;
import net.minecraft.util.Identifier;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.client.gui.SpellTooltip;
import net.spell_power.api.SpellPower;

public final class MonkSpellTooltipMutators {
    private MonkSpellTooltipMutators() { }

    private static final Identifier OCCULT_IMPACTION = Identifier.of(Monkmod.MOD_ID, "occult_impaction");
    private static final Identifier THROW_SPIRIT_SPHERE = Identifier.of(Monkmod.MOD_ID, "throw_spirit_sphere");

    public static void register() {
        SpellTooltip.addDescriptionMutator(OCCULT_IMPACTION, MonkSpellTooltipMutators::mutateOccultImpaction);
        SpellTooltip.addDescriptionMutator(THROW_SPIRIT_SPHERE, MonkSpellTooltipMutators::mutateThrowSpiritSphere);
    }

    private static String mutateOccultImpaction(SpellTooltip.DescriptionMutator.Args args) {
        Spell spell = args.spellEntry().value();

        // Spell Power atual do player (dinâmico, igual os mods Wizards/Forcemaster)
        SpellPower.Result power = SpellPower.getSpellPower(spell.school, args.player());
        float p = (float) power.nonCriticalValue();

        // >>> Mesma fórmula do seu OccultImpactionEffectsImpact <<<
        float mainDamage = 4.0f + (0.8f * p);
        float splashDamage = mainDamage * 0.6f;

        String description = args.description();
        description = description.replace(SpellTooltip.placeholder("damage"), SpellTooltip.formattedNumber(mainDamage));
        description = description.replace(SpellTooltip.placeholder("splash_damage"), SpellTooltip.formattedNumber(splashDamage));

        return description;
    }

    private static String mutateThrowSpiritSphere(SpellTooltip.DescriptionMutator.Args args) {
        Spell spell = args.spellEntry().value();

        SpellPower.Result power = SpellPower.getSpellPower(spell.school, args.player());
        float p = (float) power.nonCriticalValue();

        // >>> Mesma fórmula do seu ThrowSpiritSphereImpact <<<
        float damage = 3.0f + (0.7f * p);
        float heal = 2.0f + (0.2f * p);

        String description = args.description();
        description = description.replace(SpellTooltip.placeholder("damage"), SpellTooltip.formattedNumber(damage));
        description = description.replace(SpellTooltip.placeholder("heal"), SpellTooltip.formattedNumber(heal));

        return description;
    }
}
