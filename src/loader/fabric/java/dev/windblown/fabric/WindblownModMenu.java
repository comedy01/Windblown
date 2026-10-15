package dev.windblown.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.windblown.client.gui.WindblownSettingsScreen;
import net.minecraft.client.Minecraft;

public final class WindblownModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new WindblownSettingsScreen(parent, Minecraft.getInstance().options);
    }
}
