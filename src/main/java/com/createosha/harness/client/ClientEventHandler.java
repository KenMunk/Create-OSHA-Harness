package com.createosha.harness.client;

import com.createosha.harness.CreateOSHAHarness;
import com.createosha.harness.harness.ConnectionType;
import com.createosha.harness.harness.PlayerRopeState;
import com.createosha.harness.harness.RopeConnection;
import com.createosha.harness.network.RopeInputPacket;
import com.createosha.harness.network.SyncRopeStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client-only event handler.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Receives synced rope state from the server.</li>
 *   <li>Translates jump/sneak key presses into climb/descend packets while hanging.</li>
 * </ul>
 */
@EventBusSubscriber(modid = CreateOSHAHarness.MODID, value = Dist.CLIENT)
public final class ClientEventHandler {

    private ClientEventHandler() {}

    /** Last rope state received from the server; {@code null} until first sync. */
    static volatile PlayerRopeState localRopeState = null;

    /** Prevents flooding the server with input packets every tick. */
    private static int inputCooldown = 0;

    // ── Packet handler (called from SyncRopeStatePacket.handle) ─────────────

    public static void onSyncRopeState(SyncRopeStatePacket pkt) {
        PlayerRopeState fresh = new PlayerRopeState();
        pkt.connections().forEach(fresh::addConnection);
        fresh.setClimbStamina(pkt.stamina());
        localRopeState = fresh;
    }

    // ── Client tick: send climb/descend input to server ─────────────────────

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (localRopeState == null || localRopeState.connectionCount() == 0) return;

        if (inputCooldown > 0) {
            inputCooldown--;
            return;
        }

        boolean jumpDown  = mc.options.keyJump.isDown();
        boolean sneakDown = mc.options.keyShift.isDown();

        if (jumpDown && isClientHanging()) {
            PacketDistributor.sendToServer(new RopeInputPacket(RopeInputPacket.Action.CLIMB_UP));
            inputCooldown = 2;
        } else if (sneakDown && isClientHanging()) {
            PacketDistributor.sendToServer(new RopeInputPacket(RopeInputPacket.Action.DESCEND));
            inputCooldown = 2;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Lightweight client-side hanging check.
     * Returns {@code true} if the local player is airborne and at least one BLOCK or
     * SELF_ANCHOR connection is taut above them.  Entity connections are excluded because
     * the other player's position is not reliably known client-side.
     */
    private static boolean isClientHanging() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.onGround()) return false;
        if (localRopeState == null) return false;

        Vec3 playerPos = mc.player.position();
        for (RopeConnection conn : localRopeState.getConnections()) {
            if (conn.type() == ConnectionType.ENTITY) continue;
            if (conn.anchorBlock() == null) continue;

            Vec3 anchor = Vec3.atCenterOf(conn.anchorBlock());
            if (anchor.y <= playerPos.y) continue;

            double dist = playerPos.distanceTo(anchor);
            if (dist >= conn.deployedLength() - 0.25) return true;
        }
        return false;
    }

    /** Accessor for the renderer. */
    static PlayerRopeState getLocalRopeState() { return localRopeState; }
}
