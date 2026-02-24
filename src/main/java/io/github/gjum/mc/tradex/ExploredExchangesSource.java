package io.github.gjum.mc.tradex;

import io.github.gjum.mc.tradex.model.Exchange;
import io.github.gjum.mc.tradex.model.ExchangeChest;
import io.github.gjum.mc.tradex.model.Pos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static io.github.gjum.mc.tradex.Utils.mc;

/**
 * Highlight source for exchanges discovered through chat parsing.
 * Supports distance/time-based automatic purging.
 */
public class ExploredExchangesSource implements HighlightSource {
	private final HashMap<Pos, ExchangeChest> exchanges = new HashMap<>();
	private final Map<Pos, Long> highlightCreationTimes = new HashMap<>();
	private final HashSet<Pos> suppressedPositions = new HashSet<>();

	@Override
	public Collection<Pos> getActivePositions(Pos playerPos, long now, @Nullable Predicate<Pos> extraFilter) {
		if (TradexMod.mod == null || mc.player == null) {
			return new ArrayList<>();
		}

		var config = TradexConfig.getInstance();
		Set<Pos> result = new HashSet<>();

		int maxDist = config.highlightMaxDistance;
		int lifetimeSec = config.highlightLifetimeSeconds;
		var playerBlockPos = mc.player.blockPosition();

		// Iterate using iterator for safe removal without copying
		var it = exchanges.entrySet().iterator();
		while (it.hasNext()) {
			var entry = it.next();
			Pos pos = entry.getKey();

			// Skip suppressed positions
			if (suppressedPositions.contains(pos)) {
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
				// Purge this position using iterator for safe removal
				it.remove();
				highlightCreationTimes.remove(pos);
				suppressedPositions.add(pos);
			} else {
				// Apply extra filter if provided
				if (extraFilter == null || extraFilter.test(pos)) {
					result.add(pos);
				}
			}
		}

		return result;
	}

	@Override
	public void markSuppressed(Pos pos) {
		suppressedPositions.add(pos);
		highlightCreationTimes.remove(pos);
	}

	@Override
	public void unsuppress(Pos pos) {
		suppressedPositions.remove(pos);
		highlightCreationTimes.put(pos, System.currentTimeMillis());
	}

	@Override
	public boolean isSuppressed(Pos pos) {
		return suppressedPositions.contains(pos);
	}

	@Override
	public void reset() {
		exchanges.clear();
		highlightCreationTimes.clear();
		suppressedPositions.clear();
	}

	@Override
	public boolean supportsAutoPurging() {
		return true;
	}

	/**
	 * Add an exchange to this source.
	 * @param exchange the exchange to add
	 */
	public void addExchange(@NotNull Exchange exchange) {
		exchanges.computeIfAbsent(exchange.pos, e -> new ExchangeChest()).add(exchange);
	}

	/**
	 * Get the ExchangeChest at a position, if it exists.
	 * @param pos the position to look up
	 * @return the ExchangeChest, or null if not found
	 */
	@Nullable
	public ExchangeChest getChest(Pos pos) {
		return exchanges.get(pos);
	}

	/**
	 * Get all exchanges map.
	 * @return the map of positions to ExchangeChests
	 */
	public HashMap<Pos, ExchangeChest> getAllExchanges() {
		return new HashMap<>(exchanges);
	}
}
