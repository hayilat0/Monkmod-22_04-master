package net.gabriel.monk.effect;

import net.gabriel.monk.Monkmod;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;
import net.spell_power.api.SpellPowerMechanics;
import net.spell_power.api.SpellSchools;

public final class SpiritualSpheresEffect extends StatusEffect {
    public SpiritualSpheresEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0x7B61FF);

        // 5/10/15/20/25% via amplifier 0/1/2/3/4/5 (multiplica pelo (amp+1))

        this.addAttributeModifier(
                SpellPowerMechanics.HASTE.attributeEntry,
                Identifier.of(Monkmod.MOD_ID, "spiritual_spheres_haste"),
                0.05,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        this.addAttributeModifier(
                SpellSchools.ARCANE.getAttributeEntry(),
                Identifier.of(Monkmod.MOD_ID, "spiritual_spheres_arcane_power"),
                0.05,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        this.addAttributeModifier(
                EntityAttributes.GENERIC_ATTACK_DAMAGE,
                Identifier.of(Monkmod.MOD_ID, "spiritual_spheres_attack_damage"),
                0.05,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }
}
