package net.gabriel.monk.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityIFramesAccessor {
    @Accessor("timeUntilRegen")
    void monkmod$setTimeUntilRegen(int value);
}
