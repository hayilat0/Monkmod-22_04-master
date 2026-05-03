package net.gabriel.monk.mixin;

import net.forcemaster_rpg.item.weapons.WeaponsRegister;
import net.minecraft.util.Identifier;
import net.spell_engine.api.spell.container.SpellChoice;
import net.spell_engine.api.spell.container.SpellContainers;
import net.spell_engine.rpg_series.item.Weapon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(value = WeaponsRegister.class, remap = false)
public abstract class ForcemasterWeaponsRegisterMixin {

    private static final String MONKMOD_KNUCKLE_POOL =
            "monkmod:weapon/forcemaster_knuckle";

    private static final String FORCEMASTER_KNUCKLE_ARCANE_OVERFLOW =
            "forcemaster_rpg:knuckle_arcane_overflow";

    @Inject(method = "register", at = @At("HEAD"))
    private static void monkmod$addSpellChoicesToForcemasterKnuckles(Map configs, CallbackInfo ci) {
        patchKnuckle(WeaponsRegister.wooden_knuckle);
        patchKnuckle(WeaponsRegister.stone_knuckle);
        patchKnuckle(WeaponsRegister.iron_knuckle);
        patchKnuckle(WeaponsRegister.golden_knuckle);
        patchKnuckle(WeaponsRegister.diamond_knuckle);
        patchKnuckle(WeaponsRegister.netherite_knuckle);
    }

    private static void patchKnuckle(Weapon.Entry entry) {
        entry.spellContainer(
                SpellContainers.forMeleeWeapon()
                        .withAdditionalSpell(List.of(FORCEMASTER_KNUCKLE_ARCANE_OVERFLOW))
                        .withBindingPool(Identifier.of(MONKMOD_KNUCKLE_POOL))
                        .withMaxSpellCount(1)
        );

        entry.spellChoice(
                SpellChoice.of(MONKMOD_KNUCKLE_POOL)
        );
    }
}