package net.gabriel.monk.util;

import net.gabriel.monk.ModItems;
import net.minecraft.entity.LivingEntity;

public final class MonkSpiritSpheres {
    private static final int BASE_MAX = 5;

    private MonkSpiritSpheres() {}

    public static int getMaxSpheres(LivingEntity entity) {
        int bonus = 0;

        if (entity.getMainHandStack().isOf(ModItems.SPIRITUAL_GAUNTLET)
                || entity.getOffHandStack().isOf(ModItems.SPIRITUAL_GAUNTLET)) {
            bonus += 1;
        }

        return BASE_MAX + bonus;
    }
}
