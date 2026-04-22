package net.gabriel.monk.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.gabriel.monk.Monkmod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * Payload S2C do VFX do Passo Etéreo.
 * Usa o sistema novo de networking (CustomPayload + PacketCodec).
 */
public record EtherealStepVfxPayload(int casterEntityId, Vec3d start, Vec3d end) implements CustomPayload {

    public static final Id<EtherealStepVfxPayload> ID =
            new Id<>(Identifier.of(Monkmod.MOD_ID, "ethereal_step_vfx"));

    // Integra com o estilo "clássico" (PacketByteBuf) via PacketCodec.of(write, constructor(buf))
    public static final PacketCodec<PacketByteBuf, EtherealStepVfxPayload> CODEC =
            PacketCodec.of(EtherealStepVfxPayload::write, EtherealStepVfxPayload::new);

    private static boolean REGISTERED = false;

    /**
     * Precisa ser registrado nos dois lados (cliente e servidor) ANTES de registrar receiver / enviar.
     * Aqui eu coloco um guard pra você poder chamar de qualquer lugar sem duplicar.
     */
    public static void register() {
        if (REGISTERED) return;
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
        REGISTERED = true;
    }

    public EtherealStepVfxPayload(PacketByteBuf buf) {
        this(
                buf.readVarInt(),
                readVec3d(buf),
                readVec3d(buf)
        );
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(casterEntityId);
        writeVec3d(buf, start);
        writeVec3d(buf, end);
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
