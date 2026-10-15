package dev.windblown.selftest.fabric;

import dev.windblown.selftest.WindblownSelfTest;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class WindblownSelfTestFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WindblownSelfTest test = new WindblownSelfTest();
        ClientTickEvents.END_CLIENT_TICK.register(client -> test.tick());
    }
}
