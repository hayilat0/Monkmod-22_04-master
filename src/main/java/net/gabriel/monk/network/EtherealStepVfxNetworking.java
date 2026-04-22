package net.gabriel.monk.network;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Helper do servidor para disparar o VFX do Passo Etéreo nos clientes próximos (tracking) + no próprio player.
 */
public final class EtherealStepVfxNetworking {

    private EtherealStepVfxNetworking() {}

    public static void send(LivingEntity caster, Vec3d start, Vec3d end) {
        EtherealStepVfxPayload.register();

        EtherealStepVfxPayload payload = new EtherealStepVfxPayload(caster.getId(), start, end);

        ServerPlayerEntity self = (caster instanceof ServerPlayerEntity sp) ? sp : null;
        boolean sentToSelfInTracking = false;

        for (ServerPlayerEntity player : PlayerLookup.tracking(caster)) {
            ServerPlayNetworking.send(player, payload);
            if (self != null && player == self) {
                sentToSelfInTracking = true;
            }
        }

        // garante que o caster também vê (nem sempre ele entra no "tracking" do próprio entity)
        if (self != null && !sentToSelfInTracking) {
            ServerPlayNetworking.send(self, payload);
        }
    }
}
