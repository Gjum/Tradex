package io.github.gjum.mc.tradex;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.github.gjum.mc.tradex.model.Exchange;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static io.github.gjum.mc.tradex.TradexMod.LOG;

/**
 * Manages a persistent set of favorite exchange keys.
 * Each exchange is identified by "server|world|x|y|z|index".
 */
public class FavoritesManager {
	private static final Path FAVORITES_FILE = Path.of("tradex-favorites.json");
	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private static final Type SET_TYPE = new TypeToken<HashSet<String>>() {}.getType();

	private final Set<String> favoriteKeys = new HashSet<>();

	public FavoritesManager() {
		load();
	}

	/**
	 * Build a unique key for the given exchange.
	 */
	public static @NotNull String keyOf(@NotNull Exchange exchange) {
		return exchange.pos.server + "|" + exchange.pos.world + "|"
				+ exchange.pos.x + "|" + exchange.pos.y + "|" + exchange.pos.z
				+ "|" + exchange.index;
	}

	public boolean isFavorite(@NotNull Exchange exchange) {
		return favoriteKeys.contains(keyOf(exchange));
	}

	public void setFavorite(@NotNull Exchange exchange, boolean favorite) {
		if (favorite) {
			favoriteKeys.add(keyOf(exchange));
		} else {
			favoriteKeys.remove(keyOf(exchange));
		}
		save();
	}

	public void toggleFavorite(@NotNull Exchange exchange) {
		setFavorite(exchange, !isFavorite(exchange));
	}

	private void load() {
		try {
			if (Files.exists(FAVORITES_FILE)) {
				String json = Files.readString(FAVORITES_FILE);
				Set<String> loaded = gson.fromJson(json, SET_TYPE);
				if (loaded != null) {
					favoriteKeys.addAll(loaded);
				}
			}
		} catch (Exception e) {
			LOG.warn("Failed to load favorites", e);
		}
	}

	private void save() {
		try {
			Files.writeString(FAVORITES_FILE, gson.toJson(favoriteKeys));
		} catch (IOException e) {
			LOG.warn("Failed to save favorites", e);
		}
	}
}
