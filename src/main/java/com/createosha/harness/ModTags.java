package com.createosha.harness;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {

    private ModTags() {}

    /**
     * Items in this tag trigger harness behaviour when right-clicked.
     * Populate data/create_osha_harness/tags/items/ropes.json with the
     * exact Create Aeronautics rope item IDs.
     */
    public static final TagKey<Item> ROPE_ITEMS = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(CreateOSHAHarness.MODID, "ropes")
    );
}
