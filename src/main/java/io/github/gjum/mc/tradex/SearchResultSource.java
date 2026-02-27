package io.github.gjum.mc.tradex;

import io.github.gjum.mc.tradex.api.Exchanges;
import io.github.gjum.mc.tradex.model.Exchange;
import io.github.gjum.mc.tradex.model.Pos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Highlight source for search results.
 * Does NOT support distance/time-based automatic purging - only TTL-based filtering.
 */
public class SearchResultSource implements HighlightSource {
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
	 * Get the exchange at a specific position from current search result.
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
}
