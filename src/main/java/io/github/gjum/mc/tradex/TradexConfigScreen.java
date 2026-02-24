package io.github.gjum.mc.tradex;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Builds the Cloth Config screen for Tradex settings.
 */
public class TradexConfigScreen {

	public static Screen create(Screen parent) {
		TradexConfig config = TradexConfig.getInstance();

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.literal("Tradex Configuration"))
				.setSavingRunnable(config::save);

		ConfigEntryBuilder entryBuilder = builder.entryBuilder();

		ConfigCategory highlights = builder.getOrCreateCategory(
				Component.literal("Highlights"));

		highlights.addEntry(entryBuilder.startIntField(
						Component.literal("Highlight Max Distance (blocks)"),
						config.highlightMaxDistance)
				.setDefaultValue(10)
				.setMin(0)
				.setMax(256)
				.setTooltip(Component.literal(
						"Highlights farther than this distance are permanently removed.\n" +
								"Set to 0 to disable distance-based removal."))
				.setSaveConsumer(val -> config.highlightMaxDistance = val)
				.build());

		highlights.addEntry(entryBuilder.startIntField(
						Component.literal("Highlight Lifetime (seconds)"),
						config.highlightLifetimeSeconds)
				.setDefaultValue(5)
				.setMin(0)
				.setMax(60)
				.setTooltip(Component.literal(
						"Highlights disappear after this many seconds.\n" +
								"Set to 0 to disable time-based removal (never disappear)."))
				.setSaveConsumer(val -> config.highlightLifetimeSeconds = val)
				.build());

		return builder.build();
	}
}
