package com.createosha.harness.harness;

public enum ConnectionType {
    /** Rope anchored to a fence post or other anchor block. */
    BLOCK,
    /** Rope connected to another player entity. */
    ENTITY,
    /** Rope anchored to the player's own position at attachment time (self-safety line). */
    SELF_ANCHOR
}
