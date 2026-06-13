package com.createosha.harness.network;

import com.createosha.harness.CreateOSHAHarness;
import com.createosha.harness.client.ClientEventHandler;
import com.createosha.harness.harness.RopeConnection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → Client: carries the full rope state for the receiving player.
 * Sent whenever the server-side state changes (connection added/removed, length updated).
 */
public record SyncRopeStatePacket(List<RopeConnection> connections, float stamina)
        implements CustomPacketPayload {

    public static final Type<SyncRopeStatePacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(CreateOSHAHarness.MODID, "sync_rope_state")
    );

    public static final StreamCodec<FriendlyByteBuf, SyncRopeStatePacket> STREAM_CODEC = StreamCodec.of(
        (buf, pkt) -> {
            buf.writeFloat(pkt.stamina());
            buf.writeVarInt(pkt.connections().size());
            for (RopeConnection conn : pkt.connections()) {
                conn.toNetwork(buf);
            }
        },
        buf -> {
            float stamina = buf.readFloat();
            int count = buf.readVarInt();
            List<RopeConnection> conns = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                conns.add(RopeConnection.fromNetwork(buf));
            }
            return new SyncRopeStatePacket(conns, stamina);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Client-side handler — runs on the main client thread via enqueueWork. */
    public static void handle(SyncRopeStatePacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientEventHandler.onSyncRopeState(pkt));
    }
}
