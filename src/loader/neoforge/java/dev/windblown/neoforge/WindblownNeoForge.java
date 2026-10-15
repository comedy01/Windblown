package dev.windblown.neoforge;

import dev.windblown.client.WindblownClient;
import dev.windblown.client.gui.WindblownSettingsScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = WindblownClient.MOD_ID, dist = Dist.CLIENT)
public final class WindblownNeoForge {
    public WindblownNeoForge(IEventBus modBus, ModContainer container) {
        WindblownClient.init(FMLPaths.CONFIGDIR.get());

        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> new WindblownSettingsScreen(parent, Minecraft.getInstance().options));
    }
}
