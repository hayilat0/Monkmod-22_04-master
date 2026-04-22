package net.gabriel.monk.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.gabriel.monk.effect.ModEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class MonkVfxEvents {

    private static final Set<UUID> prevChakra = new HashSet<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(MonkVfxEvents::tick);
    }

    private static void tick(MinecraftServer server) {
        Set<UUID> nowChakra = new HashSet<>();

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            UUID id = p.getUuid();

            boolean hasChakra = p.hasStatusEffect(ModEffects.VIGOR_CHAKRA);

            // ---- Chakra do Vigor (mantém!) ----
            if (hasChakra) {
                nowChakra.add(id);

                if (!prevChakra.contains(id)) {
                    VigorChakraVfx.onActivated(p);
                }
                VigorChakraVfx.onTick(p);
            }

        }

        prevChakra.clear();
        prevChakra.addAll(nowChakra);
    }

    private MonkVfxEvents() {}
}
