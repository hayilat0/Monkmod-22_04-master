package net.gabriel.monk.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.gabriel.monk.Monkmod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * Payload S2C do VFX personalizado do Combo Triplo.
 *
 * mode:
 * 0 = rajada de meteoros entre start e end
 * 1 = impacto/estouro no end
 */
public record TripleComboVfxPayload(
        int casterEntityId,
        Vec3d start,
        Vec3d end,
        int hitIndex,
        int mode
) implements CustomPayload {

    public static final int MODE_METEOR_RUSH = 0;
    public static final int MODE_IMPACT = 1;

    public static final Id<TripleComboVfxPayload> ID =
            new Id<>(Identifier.of(Monkmod.MOD_ID, "triple_combo_vfx"));

    public static final PacketCodec<PacketByteBuf, TripleComboVfxPayload> CODEC =
            PacketCodec.of(TripleComboVfxPayload::write, TripleComboVfxPayload::new);

    private static boolean REGISTERED = false;

    public static void register() {
        if (REGISTERED) return;
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
        REGISTERED = true;
    }

    public TripleComboVfxPayload(PacketByteBuf buf) {
        this(
                buf.readVarInt(),
                readVec3d(buf),
                readVec3d(buf),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(casterEntityId);
        writeVec3d(buf, start);
        writeVec3d(buf, end);
        buf.writeVarInt(hitIndex);
        buf.writeVarInt(mode);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static Vec3d readVec3d(PacketByteBuf buf) {
        return new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    private static void writeVec3d(PacketByteBuf buf, Vec3d v) {
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
        buf.writeDouble(v.z);
    }
}