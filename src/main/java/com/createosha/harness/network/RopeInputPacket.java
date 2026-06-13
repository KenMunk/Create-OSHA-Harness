package com.createosha.harness.network;

import com.createosha.harness.CreateOSHAHarness;
import com.createosha.harness.harness.HarnessConstraintSystem;
import com.createosha.harness.harness.PlayerRopeState;
import com.createosha.harness.registry.ModAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → Server: notifies the server of a harness input action.
 * The server validates the action and updates authoritative rope state.
 */
public record RopeInputPacket(Action action) implements CustomPacketPayload {

    public enum Action { CLIMB_UP, DESCEND, DETACH_ALL }

    public static final Type<RopeInputPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(CreateOSHAHarness.MODID, "rope_input")
    );

    public static final StreamCodec<FriendlyByteBuf, RopeInputPacket> STREAM_CODEC = StreamCodec.of(
        (buf, pkt) -> buf.writeEnum(pkt.action()),
        buf -> new RopeInputPacket(buf.readEnum(Action.class))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Server-side handler — runs on the main server thread via enqueueWork. */
    public static void handle(RopeInputPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            PlayerRopeState state = player.getData(ModAttachments.ROPE_STATE.get());
            boolean stateChanged = false;

            switch (pkt.action()) {
                case CLIMB_UP   -> stateChanged = HarnessConstraintSystem.handleClimbUp(player, state);
                case DESCEND    -> stateChanged = HarnessConstraintSystem.handleDescend(player, state);
                case DETACH_ALL -> {
                    state.clearConnections();
                    stateChanged = true;
                }
            }

            if (stateChanged) {
                ModNetworking.syncStateToPlayer(player, state);
            }
        });
    }
}
