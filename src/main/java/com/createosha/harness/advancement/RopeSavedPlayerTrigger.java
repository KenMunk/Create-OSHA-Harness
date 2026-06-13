package com.createosha.harness.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Fires when a player who has fallen at least N blocks is caught by an active rope connection.
 *
 * <p>Advancement JSON usage:
 * <pre>{@code
 * "trigger": "create_osha_harness:rope_saved_player",
 * "conditions": { "min_fall_blocks": 10 }
 * }</pre>
 */
public class RopeSavedPlayerTrigger extends SimpleCriterionTrigger<RopeSavedPlayerTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /**
     * Call this on the server when the rope constraint fires on a falling player.
     *
     * @param player     the player being caught
     * @param fallBlocks how many blocks they had fallen (from {@code player.fallDistance})
     */
    public void trigger(ServerPlayer player, int fallBlocks) {
        this.trigger(player, instance -> instance.matches(fallBlocks));
    }

    // ── TriggerInstance ──────────────────────────────────────────────────────

    public record TriggerInstance(
        Optional<ContextAwarePredicate> player,
        int minFallBlocks
    ) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player")
                    .forGetter(TriggerInstance::player),
                Codec.INT.optionalFieldOf("min_fall_blocks", 0)
                    .forGetter(TriggerInstance::minFallBlocks)
            ).apply(instance, TriggerInstance::new)
        );

        @Override
        public Optional<ContextAwarePredicate> player() { return player; }

        public boolean matches(int fallBlocks) { return fallBlocks >= minFallBlocks; }
    }
}
