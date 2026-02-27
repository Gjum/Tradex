package io.github.gjum.mc.tradex;

import io.github.gjum.mc.tradex.model.Pos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static io.github.gjum.mc.tradex.Utils.mc;

/**
 * Manages highlight lifecycle by coordinating multiple highlight sources.
 * Each source owns its own data and suppression state.
 */
public class HighlightManager {
	private final List<HighlightSource> sources = new ArrayList<>();

	public final ExploredExchangesSource exploredSource = new ExploredExchangesSource();
	public final SearchResultSource searchSource = new SearchResultSource();
	public final NearbyExchangesSource nearbySource = new NearbyExchangesSource();

	public HighlightManager() {
		sources.add(exploredSource);
		sources.add(searchSource);
		sources.add(nearbySource);
	}

	/**
	 * Called every render tick to update highlight state.
	 * Auto-purging logic only applies to sources that support it (e.g., explored exchanges).
	 */
	public void tick() {
		if (mc.player == null) return;
		long now = System.currentTimeMillis();
		var playerPos = TradexMod.mod.getPlayerPos();

		// Only sources that support auto-purging get their positions filtered during tick
		// This is mainly for exploredExchanges - search results are filtered by TTL in getActivePositions()
		for (HighlightSource source : sources) {
			if (source.supportsAutoPurging()) {
				source.getActivePositions(playerPos, now, null);
			}
		}
	}

	/**
	 * Registers a position as newly highlighted.
	 * Calls unsuppress on all sources - individual sources efficiently skip
	 * positions they don't track (no-op in most cases).
	 */
	public void registerHighlight(Pos pos) {
		for (HighlightSource source : sources) {
			source.unsuppress(pos);
		}
	}

	/**
	 * Clears ALL highlights immediately. Called by the "Clear Highlights" button/hotkey.
	 */
	public void clearAll() {
		for (HighlightSource source : sources) {
			source.reset();
		}
	}

	/**
	 * Resets the manager state entirely (e.g. on world join).
	 */
	public void reset() {
		for (HighlightSource source : sources) {
			source.reset();
		}
	}

	/**
	 * Get all active positions from all sources.
	 */
	public Collection<Pos> getAllActivePositions(Pos playerPos, long now, @Nullable Predicate<Pos> extraFilter) {
		Set<Pos> result = new HashSet<>();
		for (HighlightSource source : sources) {
			result.addAll(source.getActivePositions(playerPos, now, extraFilter));
		}
		return result;
	}
}
