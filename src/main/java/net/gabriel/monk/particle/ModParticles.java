package net.gabriel.monk.particle;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.gabriel.monk.Monkmod;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModParticles {

    public static final SimpleParticleType SPIRITUAL_METEOR =
            Registry.register(
                    Registries.PARTICLE_TYPE,
                    Identifier.of(Monkmod.MOD_ID, "spiritual_meteor"),
                    FabricParticleTypes.simple()
            );

    public static void registerAll() {
        Monkmod.LOGGER.info("Registrando partículas do Monkmod.");
    }

    private ModParticles() {
    }
}