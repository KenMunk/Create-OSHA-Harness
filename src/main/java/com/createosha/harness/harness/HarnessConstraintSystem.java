package com.createosha.harness.harness;

import com.createosha.harness.Config;
import com.createosha.harness.registry.ModTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-side rope physics.  Called every player tick to enforce the constraint
 * that a player cannot be further than {@link RopeConnection#deployedLength()} blocks
 * from each of their rope anchors.
 */
public final class HarnessConstraintSystem {

    private HarnessConstraintSystem() {}

    /**
     * Enforces all rope constraints for {@code player}.  Stale connections (anchor block
     * destroyed, anchor player disconnected) are automatically removed.
     */
    public static void enforceConstraints(ServerPlayer player, PlayerRopeState state) {
        Level level = player.level();
        MinecraftServer server = player.server;

        List<UUID> toRemove = new ArrayList<>();

        for (int i = 0; i < state.getConnections().size(); i++) {
            RopeConnection conn = state.getConnections().get(i);
            Vec3 anchor = resolveAnchorPosition(conn, level, server);

            if (anchor == null) {
                toRemove.add(conn.connectionId());
                continue;
            }

            Vec3 playerPos = player.position();
            Vec3 fromAnchor = playerPos.subtract(anchor); // vector pointing away from anchor
            double dist = fromAnchor.length();

            if (dist > conn.deployedLength() && dist > 0.001) {
                Vec3 ropeDir = fromAnchor.normalize(); // direction player is from anchor

                // "Hanging Out" advancement: rope catches a player mid-fall of 10+ blocks.
                // Only fire once per fall event (ropeSaveFired resets on landing).
                if (!state.hasRopeSaveFired()) {
                    Vec3 vel = player.getDeltaMovement();
                    if (vel.y < -0.5 && player.fallDistance >= 10.0f) {
                        ModTriggers.ROPE_SAVED_PLAYER.trigger(player, (int) player.fallDistance);
                        state.setRopeSaveFired(true);
                    }
                }

                // Clamp position to the surface of the rope sphere
                Vec3 clampedPos = anchor.add(ropeDir.scale(conn.deployedLength()));
                player.setPos(clampedPos.x, clampedPos.y, clampedPos.z);

                // Cancel the velocity component pointing further away from anchor
                Vec3 vel = player.getDeltaMovement();
                double outwardComponent = vel.dot(ropeDir);
                if (outwardComponent > 0) {
                    player.setDeltaMovement(vel.subtract(ropeDir.scale(outwardComponent)));
                }
            }

            // Apply multiplayer tension if this is an entity (player-to-player) connection
            if (conn.type() == ConnectionType.ENTITY && conn.anchorEntityId() != null && server != null) {
                ServerPlayer other = server.getPlayerList().getPlayer(conn.anchorEntityId());
                if (other != null) {
                    applyTensionToAnchoredPlayer(player, other, conn);
                }
            }
        }

        for (UUID id : toRemove) {
            state.removeConnection(id);
        }
    }

    /**
     * When player A is falling and connected to player B, player B feels a pull force
     * proportional to A's downward velocity.
     */
    private static void applyTensionToAnchoredPlayer(ServerPlayer falling, ServerPlayer anchored, RopeConnection conn) {
        Vec3 fallVel = falling.getDeltaMovement();
        if (fallVel.y >= 0) return; // not actually falling

        Vec3 toFaller = falling.position().subtract(anchored.position()).normalize();
        double pullMagnitude = Math.min(Math.abs(fallVel.y) * 0.25, 0.2);

        Vec3 anchoredVel = anchored.getDeltaMovement();
        anchored.setDeltaMovement(anchoredVel.add(toFaller.scale(pullMagnitude)));
    }

    /**
     * Returns whether the player is currently "hanging" — airborne with at least one
     * taut connection that has an anchor above them.
     */
    public static boolean isHanging(ServerPlayer player, PlayerRopeState state) {
        if (player.onGround()) return false;

        Vec3 playerPos = player.position();
        Level level = player.level();

        for (RopeConnection conn : state.getConnections()) {
            Vec3 anchor = resolveAnchorPosition(conn, level, player.server);
            if (anchor == null || anchor.y <= playerPos.y) continue;

            double dist = playerPos.distanceTo(anchor);
            if (dist >= conn.deployedLength() - 0.25) return true;
        }
        return false;
    }

    /**
     * Resolves the world-space anchor position for a connection.
     * Returns {@code null} if the anchor no longer exists (block broken, player left).
     */
    public static Vec3 resolveAnchorPosition(RopeConnection conn, Level level, MinecraftServer server) {
        return switch (conn.type()) {
            case BLOCK, SELF_ANCHOR -> {
                BlockPos pos = conn.anchorBlock();
                if (pos == null) yield null;
                // For BLOCK type, verify the block is still a valid anchor
                if (conn.type() == ConnectionType.BLOCK && !isValidAnchorBlock(level, pos)) {
                    yield null;
                }
                yield Vec3.atCenterOf(pos);
            }
            case ENTITY -> {
                if (conn.anchorEntityId() == null || server == null) yield null;
                ServerPlayer other = server.getPlayerList().getPlayer(conn.anchorEntityId());
                yield other != null ? other.position().add(0, 0.9, 0) : null;
            }
        };
    }

    private static boolean isValidAnchorBlock(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.is(BlockTags.FENCES)
            || state.is(BlockTags.FENCE_GATES)
            || (!state.isAir() && state.isSolidRender(level, pos));
    }

    /**
     * Handles a climb-up input from the player.
     * Applies an upward impulse without retracting rope length.
     * Returns {@code false} if the player has no stamina.
     */
    public static boolean handleClimbUp(ServerPlayer player, PlayerRopeState state) {
        if (!isHanging(player, state)) return false;
        if (!state.consumeStamina(Config.CLIMB_STAMINA_COST.get().floatValue())) return false;

        double climbSpeed = Config.CLIMB_SPEED.get();
        Vec3 vel = player.getDeltaMovement();
        player.setDeltaMovement(vel.x, Math.max(vel.y + climbSpeed, climbSpeed), vel.z);
        return true;
    }

    /**
     * Handles a descend input: pays out more rope so gravity can lower the player.
     * Returns {@code false} if rope is fully deployed.
     */
    public static boolean handleDescend(ServerPlayer player, PlayerRopeState state) {
        if (!isHanging(player, state)) return false;
        if (!state.consumeStamina(Config.DESCEND_STAMINA_COST.get().floatValue())) return false;

        Level level = player.level();
        Vec3 playerPos = player.position();

        for (int i = 0; i < state.getConnections().size(); i++) {
            RopeConnection conn = state.getConnections().get(i);
            Vec3 anchor = resolveAnchorPosition(conn, level, player.server);
            if (anchor == null || anchor.y <= playerPos.y) continue;

            double dist = playerPos.distanceTo(anchor);
            if (dist >= conn.deployedLength() - 0.5) {
                float newLength = (float) Math.min(
                    conn.deployedLength() + Config.DESCEND_SPEED.get(),
                    conn.maxDeployedLength()
                );
                state.replaceConnection(i, conn.withDeployedLength(newLength));
                return true;
            }
        }
        return false;
    }
}
