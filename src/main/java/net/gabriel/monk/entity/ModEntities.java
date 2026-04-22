package net.gabriel.monk.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.gabriel.monk.Monkmod;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {

    public static final EntityType<SpiritualSphereProjectileEntity> SPIRITUAL_SPHERE_PROJECTILE =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    Identifier.of(Monkmod.MOD_ID, "spiritual_sphere_projectile"),
                    FabricEntityTypeBuilder.<SpiritualSphereProjectileEntity>create(
                                    SpawnGroup.MISC,
                                    SpiritualSphereProjectileEntity::new
                            )
                            // ✅ hitbox mais coerente com o visual (antes: 0.35f)
                            .dimensions(EntityDimensions.fixed(0.50f, 0.50f))
                            .trackRangeBlocks(64)
                            .trackedUpdateRate(10)
                            .build()
            );

    public static void registerAll() {
        // chamar isso no onInitialize garante que a classe carregue e registre o EntityType
    }

    private ModEntities() {}
}
