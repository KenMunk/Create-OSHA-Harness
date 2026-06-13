package com.createosha.harness.event;

import com.createosha.harness.Config;
import com.createosha.harness.harness.HarnessConstraintSystem;
import com.createosha.harness.harness.PlayerRopeState;
import com.createosha.harness.network.ModNetworking;
import com.createosha.harness.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = com.createosha.harness.CreateOSHAHarness.MODID)
public final class RopePhysicsHandler {

    private RopePhysicsHandler() {}

    /** Ticks at which state is force-synced to the client even without changes (20 ticks = 1 s). */
    private static final int SYNC_INTERVAL = 10;

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        PlayerRopeState state = serverPlayer.getData(ModAttachments.ROPE_STATE.get());
        if (state.connectionCount() == 0) {
            regenStamina(state, serverPlayer);
            return;
        }

        // Reset the advancement flag once the player has landed
        if (serverPlayer.onGround() && state.hasRopeSaveFired()) {
            state.setRopeSaveFired(false);
        }

        int sizeBefore = state.connectionCount();

        // Core rope physics
        HarnessConstraintSystem.enforceConstraints(serverPlayer, state);

        // Stamina regen when not actively doing anything
        if (!HarnessConstraintSystem.isHanging(serverPlayer, state)) {
            regenStamina(state, serverPlayer);
        }

        // Sync to client periodically or if a connection was pruned
        boolean connectionLost = state.connectionCount() != sizeBefore;
        long gameTick = serverPlayer.level().getGameTime();
        if (connectionLost || gameTick % SYNC_INTERVAL == 0) {
            ModNetworking.syncStateToPlayer(serverPlayer, state);
        }
    }

    private static void regenStamina(PlayerRopeState state, ServerPlayer player) {
        // Config value is per second; tick rate is 20/s
        float regenPerTick = (float) (Config.STAMINA_REGEN_PER_SECOND.get() / 20.0);
        state.regenStamina(regenPerTick);
    }
}
