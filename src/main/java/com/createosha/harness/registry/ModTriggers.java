package com.createosha.harness.registry;

import com.createosha.harness.advancement.RopeSavedPlayerTrigger;
import net.minecraft.advancements.CriteriaTriggers;

public final class ModTriggers {

    private ModTriggers() {}

    public static RopeSavedPlayerTrigger ROPE_SAVED_PLAYER;

    /**
     * Called from {@code FMLCommonSetupEvent.enqueueWork} so it runs on the main
     * thread before the trigger registry is frozen.
     */
    public static void register() {
        ROPE_SAVED_PLAYER = CriteriaTriggers.register(
            "create_osha_harness:rope_saved_player",
            new RopeSavedPlayerTrigger()
        );
    }
}
