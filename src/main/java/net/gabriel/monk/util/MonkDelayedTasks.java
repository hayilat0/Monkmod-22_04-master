package net.gabriel.monk.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class MonkDelayedTasks {

    private static final List<DelayedTask> TASKS = new ArrayList<>();
    private static boolean registered = false;

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<DelayedTask> iterator = TASKS.iterator();

            while (iterator.hasNext()) {
                DelayedTask task = iterator.next();

                if (task.server != server) {
                    continue;
                }

                task.ticksRemaining--;

                if (task.ticksRemaining <= 0) {
                    iterator.remove();
                    task.runnable.run();
                }
            }
        });
    }

    public static void runLater(MinecraftServer server, int delayTicks, Runnable runnable) {
        if (delayTicks <= 0) {
            runnable.run();
            return;
        }

        TASKS.add(new DelayedTask(server, delayTicks, runnable));
    }

    private static final class DelayedTask {
        private final MinecraftServer server;
        private int ticksRemaining;
        private final Runnable runnable;

        private DelayedTask(MinecraftServer server, int ticksRemaining, Runnable runnable) {
            this.server = server;
            this.ticksRemaining = ticksRemaining;
            this.runnable = runnable;
        }
    }

    private MonkDelayedTasks() {
    }
}