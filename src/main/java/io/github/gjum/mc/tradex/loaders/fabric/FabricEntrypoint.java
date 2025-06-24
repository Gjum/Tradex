//? if fabric {
package io.github.gjum.mc.tradex.loaders.fabric;

import io.github.gjum.mc.tradex.TradexMod;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;

public class FabricEntrypoint implements ModInitializer {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        LOGGER.info("Hello from FabricEntrypoint!");
        TradexMod.initialize();
    }
}
//?}
