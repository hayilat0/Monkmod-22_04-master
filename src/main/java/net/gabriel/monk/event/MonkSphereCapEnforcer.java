package net.gabriel.monk.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.gabriel.monk.effect.ModEffects;
import net.gabriel.monk.util.MonkSpiritSpheres;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class MonkSphereCapEnforcer {
    private static int ticker = 0;

    private MonkSphereCapEnforcer() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(MonkSphereCapEnforcer::onEndServerTick);
    }

    private static void onEndServerTick(MinecraftServer server) {
        // Checa só 1x por segundo (20 ticks)
        ticker++;
        if (ticker < 20) return;
        ticker = 0;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            clampPlayerSpheres(player);
        }
    }

    private static void clampPlayerSpheres(ServerPlayerEntity player) {
        StatusEffectInstance spheres = player.getStatusEffect(ModEffects.SPIRITUAL_SPHERES);
        if (spheres == null) return;

        int stacks = spheres.getAmplifier() + 1;
        int maxAllowed = MonkSpiritSpheres.getMaxSpheres(player);

        if (stacks <= maxAllowed) return;

        // Reaplica o efeito com amplifier reduzido
        int newAmplifier = maxAllowed - 1;

        player.removeStatusEffect(ModEffects.SPIRITUAL_SPHERES);
        player.addStatusEffect(new StatusEffectInstance(
                ModEffects.SPIRITUAL_SPHERES,
                spheres.getDuration(),
                newAmplifier,
                spheres.isAmbient(),
                spheres.shouldShowParticles(),
                spheres.shouldShowIcon()
        ));
    }
}
