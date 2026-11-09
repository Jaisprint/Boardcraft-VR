package com.jay.cardboardvr;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CardboardVrClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("cardboardvr");

    @Override
    public void onInitializeClient() {
        VrConfig.load();
        GyroReceiver.I.start(VrConfig.I.port);
        ClientTickEvents.END_CLIENT_TICK.register(InputHandler::tick);
        LOG.info("Cardboard VR loaded (port {}). Press F9 for VR settings.", VrConfig.I.port);
    }
}
