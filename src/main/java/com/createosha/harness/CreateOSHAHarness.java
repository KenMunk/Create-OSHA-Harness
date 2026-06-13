package com.createosha.harness;

import com.createosha.harness.event.RopeInteractionHandler;
import com.createosha.harness.event.RopePhysicsHandler;
import com.createosha.harness.network.ModNetworking;
import com.createosha.harness.registry.ModAttachments;
import com.createosha.harness.registry.ModTriggers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(CreateOSHAHarness.MODID)
public class CreateOSHAHarness {

    public static final String MODID = "create_osha_harness";

    public CreateOSHAHarness(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModNetworking.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);

        modEventBus.addListener((FMLCommonSetupEvent event) ->
            event.enqueueWork(ModTriggers::register));

        NeoForge.EVENT_BUS.register(RopeInteractionHandler.class);
        NeoForge.EVENT_BUS.register(RopePhysicsHandler.class);
    }
}
