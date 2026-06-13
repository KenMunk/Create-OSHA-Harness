package com.createosha.harness;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {

    private Config() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_CONNECTIONS;
    public static final ModConfigSpec.DoubleValue MAX_ROPE_LENGTH;
    public static final ModConfigSpec.DoubleValue CLIMB_SPEED;
    public static final ModConfigSpec.DoubleValue DESCEND_SPEED;
    public static final ModConfigSpec.DoubleValue CLIMB_STAMINA_COST;
    public static final ModConfigSpec.DoubleValue DESCEND_STAMINA_COST;
    public static final ModConfigSpec.DoubleValue STAMINA_REGEN_PER_SECOND;

    static {
        BUILDER.comment("Create: OSHA Harness server configuration").push("harness");

        MAX_CONNECTIONS = BUILDER
            .comment("Maximum active rope connections per player.")
            .defineInRange("maxConnections", 2, 1, 8);

        MAX_ROPE_LENGTH = BUILDER
            .comment("Default maximum rope length in blocks.")
            .defineInRange("maxRopeLength", 10.0, 1.0, 64.0);

        CLIMB_SPEED = BUILDER
            .comment("Upward velocity added per tick while climbing (blocks/tick).")
            .defineInRange("climbSpeed", 0.15, 0.01, 1.0);

        DESCEND_SPEED = BUILDER
            .comment("Rope deployed per input tick while descending (blocks/tick).")
            .defineInRange("descendSpeed", 0.15, 0.01, 1.0);

        CLIMB_STAMINA_COST = BUILDER
            .comment("Stamina consumed per climb tick (0–20 scale).")
            .defineInRange("climbStaminaCost", 0.4, 0.0, 5.0);

        DESCEND_STAMINA_COST = BUILDER
            .comment("Stamina consumed per descend tick (0–20 scale).")
            .defineInRange("descendStaminaCost", 0.1, 0.0, 5.0);

        STAMINA_REGEN_PER_SECOND = BUILDER
            .comment("Stamina regained per second when not climbing.")
            .defineInRange("staminaRegenPerSecond", 2.0, 0.0, 20.0);

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
