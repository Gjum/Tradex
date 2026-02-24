package io.github.gjum.mc.tradex;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu integration — provides the config screen factory so users
 * can open the Tradex settings from the Mod Menu.
 */
public class TradexModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return TradexConfigScreen::create;
	}
}
