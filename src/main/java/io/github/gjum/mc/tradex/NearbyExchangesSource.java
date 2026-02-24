package io.github.gjum.mc.tradex;

import io.github.gjum.mc.tradex.api.Exchanges;
import io.github.gjum.mc.tradex.model.Exchange;
import io.github.gjum.mc.tradex.model.Pos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Highlight source for nearby exchanges (found via tradex search).
 * Does NOT support distance/time-based automatic purging - similar to SearchResultSource.
 */
public class NearbyExchangesSource implements HighlightSource {
	private @Nullable Exchanges.SearchResult searchResult;
	private final HashSet<Pos> suppressedPositions = new HashSet<>();
	private static final long TTL_MS = 3600_000L; // 1 hour

	@Override
	public Collection<Pos> getActivePositions(Pos playerPos, long now, @Nullable Predicate<Pos> extraFilter) {
		if (searchResult == null || searchResult.exchanges.isEmpty()) {
			return new ArrayList<>();
		}

		// Check result TTL (1 hour)
		if (now - searchResult.ts > TTL_MS) {
			searchResult = null;
			return new ArrayList<>();
		}

		Set<Pos> result = new HashSet<>();
		for (var exchange : searchResult.exchanges) {
			if (exchange.pos == null) continue;

			// Skip suppressed positions
			if (suppressedPositions.contains(exchange.pos)) {
				continue;
			}

			// Apply extra filter if provided
			if (extraFilter == null || extraFilter.test(exchange.pos)) {
				result.add(exchange.pos);
			}
		}

		return result;
	}

	@Override
	public void markSuppressed(Pos pos) {
		suppressedPositions.add(pos);
	}

	@Override
	public void unsuppress(Pos pos) {
		suppressedPositions.remove(pos);
	}

	@Override
	public boolean isSuppressed(Pos pos) {
		return suppressedPositions.contains(pos);
	}

	@Override
	public void reset() {
		searchResult = null;
		suppressedPositions.clear();
	}

	@Override
	public boolean supportsAutoPurging() {
		return false;
	}

	/**
	 * Set the search result for this source.
	 * @param result the search result to set
	 */
	public void setSearchResult(@Nullable Exchanges.SearchResult result) {
		this.searchResult = result;
		// Clear suppressed positions when new results come in
		suppressedPositions.clear();
	}

	/**
	 * Get the current search result.
	 * @return the current search result, or null if none
	 */
	@Nullable
	public Exchanges.SearchResult getSearchResult() {
		return searchResult;
	}

	/**
	 * Check if there are any active nearby highlights.
	 * @return true if there are active results
	 */
	public boolean hasActiveHighlights() {
		return searchResult != null && !searchResult.exchanges.isEmpty();
	}

	/**
	 * Get the exchange at a specific position from current result.
	 * @param pos the position to look up
	 * @return the exchange at that position, or null if not found
	 */
	@Nullable
	public Exchange getExchangeAt(Pos pos) {
		if (searchResult == null || searchResult.exchanges.isEmpty()) {
			return null;
		}
		for (var exchange : searchResult.exchanges) {
			if (pos.equals(exchange.pos)) {
				return exchange;
			}
		}
		return null;
	}

	/**
	 * Add or update an exchange in the nearby search result.
	 * If an exchange at the same position exists, it gets replaced with the new (more recent) data.
	 * If no search result exists yet, this method does nothing.
	 * @param exchange the exchange to add or update
	 */
	public void addOrUpdateExchange(@NotNull Exchange exchange) {
		if (searchResult == null) {
			return; // No active nearby highlighting, nothing to update
		}

		// Check if exchange at this position already exists
		for (int i = 0; i < searchResult.exchanges.size(); i++) {
			var existing = searchResult.exchanges.get(i);
			if (existing != null && exchange.pos.equals(existing.pos)) {
				// Replace existing exchange with the updated one
				searchResult.exchanges.set(i, exchange);
				// Unsuppress it so it will render with updated colors
				suppressedPositions.remove(exchange.pos);
				return;
			}
		}

		// Not found in current results - add it
		// This can happen if the exchange exists in Tradex but wasn't in the nearby search results
		// (e.g., it was beyond the search range/limit but got updated via chat parse)
		searchResult.exchanges.add(exchange);
		suppressedPositions.remove(exchange.pos);
	}
}
