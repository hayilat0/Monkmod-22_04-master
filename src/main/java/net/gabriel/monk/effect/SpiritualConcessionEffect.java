package net.gabriel.monk.effect;

import net.gabriel.monk.Monkmod;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

public final class SpiritualConcessionEffect extends StatusEffect {

    // ID fixo do modifier (tem que ser único e constante)
    private static final Identifier DAMAGE_MODIFIER_ID =
            Identifier.of(Monkmod.MOD_ID, "spiritual_concession_attack_damage");

    public SpiritualConcessionEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0x7b4de8);

        // ✅ +30% de dano de ataque enquanto o efeito estiver ativo
        // ADD_MULTIPLIED_TOTAL = aplica multiplicador no total do atributo
        this.addAttributeModifier(
                EntityAttributes.GENERIC_ATTACK_DAMAGE,
                DAMAGE_MODIFIER_ID,
                0.30,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }
}
