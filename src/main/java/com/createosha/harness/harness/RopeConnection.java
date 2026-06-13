package com.createosha.harness.harness;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents one active rope connection for a player.
 * Immutable — use the {@code with*} factory methods when the state must change.
 */
public final class RopeConnection {

    private final UUID connectionId;
    private final ConnectionType type;
    @Nullable private final BlockPos anchorBlock;
    @Nullable private final UUID anchorEntityId;
    /** Currently deployed rope length; player is constrained to this distance from anchor. */
    private final float deployedLength;
    /** Maximum rope that can ever be deployed. Clamped by Config.MAX_ROPE_LENGTH. */
    private final float maxDeployedLength;

    public RopeConnection(
            UUID connectionId,
            ConnectionType type,
            @Nullable BlockPos anchorBlock,
            @Nullable UUID anchorEntityId,
            float deployedLength,
            float maxDeployedLength) {
        this.connectionId    = connectionId;
        this.type            = type;
        this.anchorBlock     = anchorBlock;
        this.anchorEntityId  = anchorEntityId;
        this.deployedLength  = deployedLength;
        this.maxDeployedLength = maxDeployedLength;
    }

    // ── Named constructors ───────────────────────────────────────────────────

    public static RopeConnection toBlock(BlockPos pos, float maxLength) {
        return new RopeConnection(UUID.randomUUID(), ConnectionType.BLOCK, pos, null, maxLength, maxLength);
    }

    public static RopeConnection toEntity(UUID entityId, float maxLength) {
        return new RopeConnection(UUID.randomUUID(), ConnectionType.ENTITY, null, entityId, maxLength, maxLength);
    }

    public static RopeConnection toSelf(BlockPos anchorPos, float maxLength) {
        return new RopeConnection(UUID.randomUUID(), ConnectionType.SELF_ANCHOR, anchorPos, null, maxLength, maxLength);
    }

    // ── Wither methods ───────────────────────────────────────────────────────

    public RopeConnection withDeployedLength(float newLength) {
        return new RopeConnection(connectionId, type, anchorBlock, anchorEntityId, newLength, maxDeployedLength);
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public UUID connectionId()    { return connectionId; }
    public ConnectionType type()  { return type; }
    @Nullable public BlockPos anchorBlock()    { return anchorBlock; }
    @Nullable public UUID anchorEntityId()     { return anchorEntityId; }
    public float deployedLength()              { return deployedLength; }
    public float maxDeployedLength()           { return maxDeployedLength; }

    // ── NBT serialisation ────────────────────────────────────────────────────

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", connectionId);
        tag.putString("type", type.name());
        if (anchorBlock != null) {
            tag.putLong("anchor_block", anchorBlock.asLong());
        }
        if (anchorEntityId != null) {
            tag.putUUID("anchor_entity", anchorEntityId);
        }
        tag.putFloat("deployed",     deployedLength);
        tag.putFloat("max_deployed", maxDeployedLength);
        return tag;
    }

    public static RopeConnection fromNBT(CompoundTag tag) {
        UUID id       = tag.getUUID("id");
        ConnectionType type = ConnectionType.valueOf(tag.getString("type"));
        BlockPos block = tag.contains("anchor_block") ? BlockPos.of(tag.getLong("anchor_block")) : null;
        UUID entity    = tag.contains("anchor_entity")  ? tag.getUUID("anchor_entity") : null;
        float deployed    = tag.getFloat("deployed");
        float maxDeployed = tag.getFloat("max_deployed");
        return new RopeConnection(id, type, block, entity, deployed, maxDeployed);
    }

    // ── Network serialisation ────────────────────────────────────────────────

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeUUID(connectionId);
        buf.writeEnum(type);
        buf.writeBoolean(anchorBlock != null);
        if (anchorBlock != null) buf.writeBlockPos(anchorBlock);
        buf.writeBoolean(anchorEntityId != null);
        if (anchorEntityId != null) buf.writeUUID(anchorEntityId);
        buf.writeFloat(deployedLength);
        buf.writeFloat(maxDeployedLength);
    }

    public static RopeConnection fromNetwork(FriendlyByteBuf buf) {
        UUID id            = buf.readUUID();
        ConnectionType type = buf.readEnum(ConnectionType.class);
        BlockPos block     = buf.readBoolean() ? buf.readBlockPos() : null;
        UUID entity        = buf.readBoolean() ? buf.readUUID() : null;
        float deployed     = buf.readFloat();
        float maxDeployed  = buf.readFloat();
        return new RopeConnection(id, type, block, entity, deployed, maxDeployed);
    }
}
