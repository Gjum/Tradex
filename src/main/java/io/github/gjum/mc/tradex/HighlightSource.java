package io.github.gjum.mc.tradex;

import io.github.gjum.mc.tradex.model.Pos;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Predicate;

public interface HighlightSource {
	/**
	 * Returns positions that should be highlighted.
	 *
	 * @param playerPos current player position for distance calculation
	 * @param now current timestamp for time-based filtering
	 * @param extraFilter optional additional filter predicate
	 * @return collection of positions to highlight
	 */
	Collection<Pos> getActivePositions(Pos playerPos, long now, @Nullable Predicate<Pos> extraFilter);

	/**
	 * Mark a position as suppressed (hidden) for this source.
	 * @param pos position to suppress
	 */
	void markSuppressed(Pos pos);

	/**
	 * Unsuppress a position for this source (e.g., user explicitly re-enables).
	 * @param pos position to unsuppress
	 */
	void unsuppress(Pos pos);

	/**
	 * Check if this source considers position suppressed.
	 * @param pos position to check
	 * @return true if suppressed
	 */
	boolean isSuppressed(Pos pos);

	/**
	 * Clear all state for this source (e.g., on world join or "Clear All").
	 */
	void reset();

	/**
	 * Returns whether this source should apply distance/time-based automatic purging.
	 * Chat sources return true; search result sources return false.
	 */
	boolean supportsAutoPurging();
}
