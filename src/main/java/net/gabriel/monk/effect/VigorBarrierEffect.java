package net.gabriel.monk.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;

public final class VigorBarrierEffect extends StatusEffect {

    public static final int DURATION_TICKS = 40; // 2s

    // Scaling desejado:
    // 1 Arcane Power -> 1 coração de shield
    // 1 coração = 2 HP
    private static final double HP_PER_ARCANE_POWER = 2.0;

    public VigorBarrierEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0x3BD6A5);
    }

    /**
     * Dá o "shield" de verdade usando Absorption vanilla, mas com ícone escondido.
     */
    public static void applyShield(ServerPlayerEntity player) {
        // 1) Valor "cru" do atributo (mesma API que você já usa pra modifiers)
        double arcaneAttr = player.getAttributeValue(SpellSchools.ARCANE.getAttributeEntry());

        // 2) Valor calculado pelo SpellPower (ordem no seu projeto: (school, entity))
        var result = SpellPower.getSpellPower(SpellSchools.ARCANE, player);
        double arcaneComputed = result.nonCriticalValue();

        // Usa o maior (robusto e ótimo pra debug)
        double arcaneRaw = Math.max(arcaneAttr, arcaneComputed);

        // MULTIPLICADOR DE CORAÇÕES
        double arcaneForScaling = arcaneRaw;
        if (arcaneForScaling > 0 && arcaneForScaling < 20) {
            arcaneForScaling *= 0.4;
        }

        // 1 AP -> 1 coração -> 2 HP
        double desiredHp = arcaneForScaling * HP_PER_ARCANE_POWER;

        // Absorption vanilla: cada nível = 4 HP (2 corações)
        int amp = Math.max(0, (int) Math.ceil(desiredHp / 4.0) - 1);

        // Absorption sem partículas e sem ícone (pra não aparecer o efeito vanilla)
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.ABSORPTION,
                DURATION_TICKS,
                amp,
                true,
                false,
                false
        ), player);
    }
}
