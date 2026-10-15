package dev.windblown.forge;

import dev.windblown.client.WindblownClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(WindblownClient.MOD_ID)
public final class WindblownForge {
    public WindblownForge() {
        ModLoadingContext context = ModLoadingContext.get();
        ForgeClient.clientOnly(context);
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        WindblownClient.init(FMLPaths.CONFIGDIR.get());
        ForgeClient.register(context);
    }
}
