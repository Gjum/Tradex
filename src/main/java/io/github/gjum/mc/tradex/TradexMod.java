package io.github.gjum.mc.tradex;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class TradexMod {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void initialize() {
        LOGGER.info("Hello from MyMod!");
    }
}
