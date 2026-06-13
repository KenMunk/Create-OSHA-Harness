package com.createosha.harness.registry;

import com.createosha.harness.CreateOSHAHarness;
import com.createosha.harness.harness.PlayerRopeState;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {

    private ModAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CreateOSHAHarness.MODID);

    /**
     * Per-player rope harness state.  Serialised to NBT so it persists across
     * log-out and is restored by {@link com.createosha.harness.event.RopePhysicsHandler}.
     */
    public static final Supplier<AttachmentType<PlayerRopeState>> ROPE_STATE =
        ATTACHMENT_TYPES.register("rope_state", () ->
            AttachmentType.serializable(PlayerRopeState::new).build()
        );
}
