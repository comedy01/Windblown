package dev.windblown.selftest.forge;

import dev.windblown.selftest.WindblownSelfTest;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("windblown_selftest")
public final class WindblownSelfTestForge {
    public WindblownSelfTestForge() {
        WindblownSelfTest test = new WindblownSelfTest();
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                test.tick();
            }
        });
    }
}
