package com.createosha.harness.network;

import com.createosha.harness.harness.PlayerRopeState;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModNetworking {

    private ModNetworking() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModNetworking::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        registrar.playToClient(
            SyncRopeStatePacket.TYPE,
            SyncRopeStatePacket.STREAM_CODEC,
            SyncRopeStatePacket::handle
        );

        registrar.playToServer(
            RopeInputPacket.TYPE,
            RopeInputPacket.STREAM_CODEC,
            RopeInputPacket::handle
        );
    }

    /** Convenience: push the current rope state to a specific player's client. */
    public static void syncStateToPlayer(ServerPlayer player, PlayerRopeState state) {
        PacketDistributor.sendToPlayer(player,
            new SyncRopeStatePacket(state.getConnections(), state.getClimbStamina())
        );
    }
}
