package net.gabriel.monk.network;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.gabriel.monk.Monkmod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Helper de servidor para enviar o VFX do Combo Triplo aos clientes próximos.
 *
 * Os métodos retornam boolean para que o TripleComboImpact consiga usar fallback
 * caso o envio do pacote de VFX falhe.
 */
public final class TripleComboVfxNetworking {

    private TripleComboVfxNetworking() {
    }

    public static boolean sendMeteorRush(LivingEntity caster, Vec3d start, Vec3d end, int hitIndex) {
        try {
            TripleComboVfxPayload payload = new TripleComboVfxPayload(
                    caster.getId(),
                    start,
                    end,
                    hitIndex,
                    TripleComboVfxPayload.MODE_METEOR_RUSH
            );

            sendToTracking(caster, payload);
            return true;
        } catch (Exception e) {
            Monkmod.LOGGER.warn("Falha ao enviar VFX de rajada do Combo Triplo.", e);
            return false;
        }
    }

    public static boolean sendImpact(LivingEntity caster, LivingEntity target, Vec3d impactPos, int hitIndex) {
        try {
            TripleComboVfxPayload payload = new TripleComboVfxPayload(
                    caster.getId(),
                    caster.getPos().add(0.0d, caster.getHeight() * 0.58d, 0.0d),
                    impactPos,
                    hitIndex,
                    TripleComboVfxPayload.MODE_IMPACT
            );

            sendToTracking(caster, target, payload);
            return true;
        } catch (Exception e) {
            Monkmod.LOGGER.warn("Falha ao enviar VFX de impacto do Combo Triplo.", e);
            return false;
        }
    }

    private static void sendToTracking(LivingEntity anchor, TripleComboVfxPayload payload) {
        Set<ServerPlayerEntity> viewers = new HashSet<>(PlayerLookup.tracking(anchor));

        if (anchor instanceof ServerPlayerEntity self) {
            viewers.add(self);
        }

        for (ServerPlayerEntity viewer : viewers) {
            ServerPlayNetworking.send(viewer, payload);
        }
    }

    private static void sendToTracking(LivingEntity caster, LivingEntity target, TripleComboVfxPayload payload) {
        Set<ServerPlayerEntity> viewers = new HashSet<>();

        addAll(viewers, PlayerLookup.tracking(caster));
        addAll(viewers, PlayerLookup.tracking(target));

        if (caster instanceof ServerPlayerEntity serverCaster) {
            viewers.add(serverCaster);
        }

        if (target instanceof ServerPlayerEntity serverTarget) {
            viewers.add(serverTarget);
        }

        for (ServerPlayerEntity viewer : viewers) {
            ServerPlayNetworking.send(viewer, payload);
        }
    }

    private static void addAll(Set<ServerPlayerEntity> viewers, Collection<ServerPlayerEntity> players) {
        viewers.addAll(players);
    }
}