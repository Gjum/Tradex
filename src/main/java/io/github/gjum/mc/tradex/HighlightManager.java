package io.github.gjum.mc.tradex;

import io.github.gjum.mc.tradex.model.ExchangeChest;
import io.github.gjum.mc.tradex.model.Pos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import static io.github.gjum.mc.tradex.Utils.mc;

/**
 * Manages highlight lifecycle: tracks when each highlight was created,
 * and purges highlights based on distance and time settings from config.
 */
public class HighlightManager {

	/**
	 * Tracks the creation timestamp (System.currentTimeMillis()) for each highlighted position.
	 * Entries are added when a highlight is first rendered / registered,
	 * and removed when the highlight is purged.
	 */
	private final Map<Pos, Long> highlightCreationTimes = new HashMap<>();

	/**
	 * Positions that have been permanently purged (by distance or time).
	 * They will not be re-shown until the user triggers highlight creation again
	 * (e.g. clicking a chest or pressing "Highlight search results").
	 */
	private final HashSet<Pos> purgedPositions = new HashSet<>();

	/**
	 * Called every render tick to purge highlights that exceed distance or time limits.
	 * Modifies mod.exploredExchanges and mod.lastSearchResult in-place.
	 */
	public void tick() {
		if (mc.player == null) return;

		var config = TradexConfig.getInstance();
		var mod = TradexMod.mod;
		long now = System.currentTimeMillis();
		var playerBlockPos = mc.player.blockPosition();

		int maxDist = config.highlightMaxDistance;
		int lifetimeSec = config.highlightLifetimeSeconds;

		// --- Purge from exploredExchanges ---
		Iterator<Map.Entry<Pos, ExchangeChest>> it = mod.exploredExchanges.entrySet().iterator();
		while (it.hasNext()) {
			var entry = it.next();
			Pos pos = entry.getKey();

			if (purgedPositions.contains(pos)) {
				it.remove();
				highlightCreationTimes.remove(pos);
				continue;
			}

			// Track creation time if not yet tracked
			highlightCreationTimes.putIfAbsent(pos, now);
			long createdAt = highlightCreationTimes.get(pos);

			boolean shouldPurge = false;

			// Distance check
			if (maxDist > 0) {
				double distSq = pos.block().distSqr(playerBlockPos);
				if (distSq > (double) maxDist * maxDist) {
					shouldPurge = true;
				}
			}

			// Time check
			if (lifetimeSec > 0) {
				long elapsedMs = now - createdAt;
				if (elapsedMs > lifetimeSec * 1000L) {
					shouldPurge = true;
				}
			}

			if (shouldPurge) {
				it.remove();
				highlightCreationTimes.remove(pos);
				purgedPositions.add(pos);
			}
		}

		// --- Purge from lastSearchResult ---
		if (mod.lastSearchResult != null) {
			mod.lastSearchResult.exchanges.removeIf(exchange -> {
				Pos pos = exchange.pos;
				if (pos == null) return false;

				if (purgedPositions.contains(pos)) {
					highlightCreationTimes.remove(pos);
					return true;
				}

				highlightCreationTimes.putIfAbsent(pos, now);
				long createdAt = highlightCreationTimes.get(pos);

				boolean shouldPurge = false;

				if (maxDist > 0) {
					double distSq = pos.block().distSqr(playerBlockPos);
					if (distSq > (double) maxDist * maxDist) {
						shouldPurge = true;
					}
				}

				if (lifetimeSec > 0) {
					long elapsedMs = now - createdAt;
					if (elapsedMs > lifetimeSec * 1000L) {
						shouldPurge = true;
					}
				}

				if (shouldPurge) {
					highlightCreationTimes.remove(pos);
					purgedPositions.add(pos);
					return true;
				}
				return false;
			});

			// Clear result object if all exchanges removed
			if (mod.lastSearchResult.exchanges.isEmpty()) {
				mod.lastSearchResult = null;
			}
		}
	}

	/**
	 * Registers a position as newly highlighted (resets its timer, removes from purged set).
	 */
	public void registerHighlight(Pos pos) {
		purgedPositions.remove(pos);
		highlightCreationTimes.put(pos, System.currentTimeMillis());
	}

	/**
	 * Clears ALL highlights immediately. Called by the "Clear Highlights" button/hotkey.
	 */
	public void clearAll() {
		var mod = TradexMod.mod;

		// Add all current positions to purged set
		for (Pos pos : mod.exploredExchanges.keySet()) {
			purgedPositions.add(pos);
		}
		if (mod.lastSearchResult != null) {
			for (var exchange : mod.lastSearchResult.exchanges) {
				if (exchange.pos != null) purgedPositions.add(exchange.pos);
			}
		}

		mod.exploredExchanges.clear();
		mod.lastSearchResult = null;
		highlightCreationTimes.clear();
	}

	/**
	 * Resets the manager state entirely (e.g. on world join).
	 */
	public void reset() {
		highlightCreationTimes.clear();
		purgedPositions.clear();
	}
}
