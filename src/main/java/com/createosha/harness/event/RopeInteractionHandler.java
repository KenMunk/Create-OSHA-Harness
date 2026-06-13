package com.createosha.harness.event;

import com.createosha.harness.Config;
import com.createosha.harness.ModTags;
import com.createosha.harness.harness.PlayerRopeState;
import com.createosha.harness.harness.RopeConnection;
import com.createosha.harness.network.ModNetworking;
import com.createosha.harness.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = com.createosha.harness.CreateOSHAHarness.MODID)
public final class RopeInteractionHandler {

    private RopeInteractionHandler() {}

    // ── Right-click on block ────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ItemStack held = event.getItemStack();
        if (!held.is(ModTags.ROPE_ITEMS)) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState blockState = level.getBlockState(pos);

        // Fence post / solid anchor blocks only
        if (!blockState.is(net.minecraft.tags.BlockTags.FENCES)
                && !blockState.is(net.minecraft.tags.BlockTags.FENCE_GATES)) {
            return;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;

        if (player.isShiftKeyDown()) {
            detachAll(serverPlayer);
        } else {
            attachToBlock(serverPlayer, pos);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
    }

    // ── Right-click on player entity ────────────────────────────────────────

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ItemStack held = event.getItemStack();
        if (!held.is(ModTags.ROPE_ITEMS)) return;
        if (!(event.getTarget() instanceof Player targetPlayer)) return;
        if (targetPlayer == player) return;

        ServerPlayer serverPlayer = (ServerPlayer) player;

        if (player.isShiftKeyDown()) {
            detachAll(serverPlayer);
        } else {
            attachToEntity(serverPlayer, targetPlayer.getUUID());
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
    }

    // ── Right-click in air (self safety line) ──────────────────────────────

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ItemStack held = event.getItemStack();
        if (!held.is(ModTags.ROPE_ITEMS)) return;

        ServerPlayer serverPlayer = (ServerPlayer) player;

        if (player.isShiftKeyDown()) {
            detachAll(serverPlayer);
        } else {
            // Anchor the rope to the player's current standing position
            BlockPos anchorPos = player.blockPosition();
            attachToSelf(serverPlayer, anchorPos);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
    }

    // ── Clear all connections on player death ──────────────────────────────

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerRopeState state = player.getData(ModAttachments.ROPE_STATE.get());
        state.clearConnections();
        ModNetworking.syncStateToPlayer(player, state);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static void attachToBlock(ServerPlayer player, BlockPos pos) {
        PlayerRopeState state = player.getData(ModAttachments.ROPE_STATE.get());
        if (state.connectionCount() >= Config.MAX_CONNECTIONS.get()) {
            sendFeedback(player, "Too many rope connections.");
            return;
        }
        float maxLen = Config.MAX_ROPE_LENGTH.get().floatValue();
        state.addConnection(RopeConnection.toBlock(pos, maxLen));
        ModNetworking.syncStateToPlayer(player, state);
        sendFeedback(player, "Rope attached to fence post.");
    }

    private static void attachToEntity(ServerPlayer player, java.util.UUID targetId) {
        PlayerRopeState state = player.getData(ModAttachments.ROPE_STATE.get());
        if (state.connectionCount() >= Config.MAX_CONNECTIONS.get()) {
            sendFeedback(player, "Too many rope connections.");
            return;
        }
        float maxLen = Config.MAX_ROPE_LENGTH.get().floatValue();
        state.addConnection(RopeConnection.toEntity(targetId, maxLen));
        ModNetworking.syncStateToPlayer(player, state);
        sendFeedback(player, "Rope attached to player.");
    }

    private static void attachToSelf(ServerPlayer player, BlockPos anchorPos) {
        PlayerRopeState state = player.getData(ModAttachments.ROPE_STATE.get());
        if (state.connectionCount() >= Config.MAX_CONNECTIONS.get()) {
            sendFeedback(player, "Too many rope connections.");
            return;
        }
        float maxLen = Config.MAX_ROPE_LENGTH.get().floatValue();
        state.addConnection(RopeConnection.toSelf(anchorPos, maxLen));
        ModNetworking.syncStateToPlayer(player, state);
        sendFeedback(player, "Safety line anchored.");
    }

    private static void detachAll(ServerPlayer player) {
        PlayerRopeState state = player.getData(ModAttachments.ROPE_STATE.get());
        state.clearConnections();
        state.setClimbStamina(PlayerRopeState.MAX_CLIMB_STAMINA);
        ModNetworking.syncStateToPlayer(player, state);
        sendFeedback(player, "Rope detached.");
    }

    private static void sendFeedback(ServerPlayer player, String message) {
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(message), true);
    }
}
