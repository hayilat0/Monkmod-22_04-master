package net.gabriel.monk.effect;

import net.gabriel.monk.Monkmod;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public final class ModEffects {

    public static final RegistryEntry.Reference<StatusEffect> SPIRITUAL_SPHERES = register(
            "spiritual_spheres",
            new SpiritualSpheresEffect()
    );

    public static final RegistryEntry.Reference<StatusEffect> VIGOR_BARRIER = register(
            "vigor_barrier",
            new VigorBarrierEffect()
    );

    public static final RegistryEntry.Reference<StatusEffect> VIGOR_CHAKRA = register(
            "vigor_chakra",
            new VigorChakraEffect()
    );

    // ✅ NOVO: Concessão Espiritual
    public static final RegistryEntry.Reference<StatusEffect> SPIRITUAL_CONCESSION = register(
            "spiritual_concession",
            new SpiritualConcessionEffect()
    );

    private static RegistryEntry.Reference<StatusEffect> register(String path, StatusEffect effect) {
        return Registry.registerReference(
                Registries.STATUS_EFFECT,
                Identifier.of(Monkmod.MOD_ID, path),
                effect
        );
    }

    public static void registerAll() {
        // vazio de propósito
    }

    private ModEffects() {}
}
