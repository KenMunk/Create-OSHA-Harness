package com.createosha.harness.harness;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Per-player rope harness state, stored as a NeoForge data attachment.
 * Mutable on the server; synced to the owning client via {@link com.createosha.harness.network.SyncRopeStatePacket}.
 */
public class PlayerRopeState implements INBTSerializable<CompoundTag> {

    public static final float MAX_CLIMB_STAMINA = 20.0f;

    private final List<RopeConnection> connections = new ArrayList<>();
    private float climbStamina = MAX_CLIMB_STAMINA;
    /** Transient — not serialised. Reset when the player lands. */
    private boolean ropeSaveFired = false;

    public PlayerRopeState() {}

    public boolean hasRopeSaveFired() { return ropeSaveFired; }
    public void setRopeSaveFired(boolean value) { ropeSaveFired = value; }

    // ── Connection management ────────────────────────────────────────────────

    public List<RopeConnection> getConnections() {
        return Collections.unmodifiableList(connections);
    }

    public boolean addConnection(RopeConnection conn) {
        connections.add(conn);
        return true;
    }

    public boolean removeConnection(UUID connectionId) {
        return connections.removeIf(c -> c.connectionId().equals(connectionId));
    }

    /** Replace the connection at list index {@code idx} (used when updating deployedLength). */
    public void replaceConnection(int idx, RopeConnection updated) {
        connections.set(idx, updated);
    }

    public void clearConnections() {
        connections.clear();
    }

    public int connectionCount() {
        return connections.size();
    }

    // ── Stamina ──────────────────────────────────────────────────────────────

    public float getClimbStamina() { return climbStamina; }

    public void setClimbStamina(float value) {
        climbStamina = Math.max(0.0f, Math.min(MAX_CLIMB_STAMINA, value));
    }

    public boolean consumeStamina(float cost) {
        if (climbStamina < cost) return false;
        climbStamina -= cost;
        return true;
    }

    public void regenStamina(float amount) {
        climbStamina = Math.min(MAX_CLIMB_STAMINA, climbStamina + amount);
    }

    // ── INBTSerializable ─────────────────────────────────────────────────────

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("climb_stamina", climbStamina);
        ListTag list = new ListTag();
        for (RopeConnection conn : connections) {
            list.add(conn.toNBT());
        }
        tag.put("connections", list);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        climbStamina = tag.getFloat("climb_stamina");
        connections.clear();
        ListTag list = tag.getList("connections", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            connections.add(RopeConnection.fromNBT(list.getCompound(i)));
        }
    }
}
