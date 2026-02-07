package io.github.gjum.mc.tradex;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Holds all configurable settings for Tradex highlight behavior.
 * Persisted as JSON in the config directory.
 */
public class TradexConfig {
	private static final Path CONFIG_PATH = Path.of("config", "tradex.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static TradexConfig INSTANCE;

	/**
	 * Maximum distance (in blocks) from the player before a highlight is permanently removed.
	 * 0 = disabled (no distance-based removal).
	 */
	public int highlightMaxDistance = 0;

	/**
	 * Time in seconds before a highlight permanently disappears.
	 * 0 = never disappear by time.
	 * Range: 0 (never), 1–60 seconds.
	 */
	public int highlightLifetimeSeconds = 15;

	public static TradexConfig getInstance() {
		if (INSTANCE == null) {
			INSTANCE = load();
		}
		return INSTANCE;
	}

	public static TradexConfig load() {
		try {
			if (Files.exists(CONFIG_PATH)) {
				String json = Files.readString(CONFIG_PATH);
				TradexConfig config = GSON.fromJson(json, TradexConfig.class);
				if (config != null) {
					INSTANCE = config;
					return config;
				}
			}
		} catch (IOException e) {
			TradexMod.LOG.warn("Failed to load Tradex config", e);
		}
		INSTANCE = new TradexConfig();
		INSTANCE.save();
		return INSTANCE;
	}

	public void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			Files.writeString(CONFIG_PATH, GSON.toJson(this));
		} catch (IOException e) {
			TradexMod.LOG.warn("Failed to save Tradex config", e);
		}
	}
}
