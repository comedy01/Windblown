package dev.windblown.selftest.neoforge;

import dev.windblown.selftest.WindblownSelfTest;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "windblown_selftest", dist = Dist.CLIENT)
public final class WindblownSelfTestNeoForge {
    public WindblownSelfTestNeoForge() {
        WindblownSelfTest test = new WindblownSelfTest();
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> test.tick());
    }
}
