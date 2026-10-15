package dev.windblown.fabric;

import dev.windblown.client.WindblownClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class WindblownFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WindblownClient.init(FabricLoader.getInstance().getConfigDir());
    }
}
