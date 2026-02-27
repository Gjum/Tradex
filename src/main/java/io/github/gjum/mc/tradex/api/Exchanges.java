package io.github.gjum.mc.tradex.api;

import com.google.gson.annotations.Expose;
import io.github.gjum.mc.tradex.model.Exchange;
import io.github.gjum.mc.tradex.model.SearchQuery;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Exchanges {
	public static CompletableFuture<Void> upload(Exchange exchange) {
		return Api.request("/exchanges/upload", exchange)
				.thenAccept(Api::drop)
				.exceptionally(Api.logError("Failed uploading exchange"));
	}

	public static CompletableFuture<SearchResult> search(SearchQuery query) {
		return Api.request("/exchanges/search", query)
				.thenApply(Api.parseJson(SearchResult.class))
				.whenComplete((searchResult, err) -> {
					if (err != null || searchResult == null) return;
					searchResult.ts = System.currentTimeMillis();
					// make mutable, for sorting
					searchResult.exchanges = new ArrayList<>(searchResult.exchanges);
					for (var exchange : searchResult.exchanges) {
						exchange.fixNulls();
					}
					// If client requested "cheapest" sorting, ensure we sort by per-normal-item price
					if (query != null && query.sortMode != null && "cheapest".equalsIgnoreCase(query.sortMode)) {
						searchResult.exchanges.sort(java.util.Comparator.comparingDouble(e -> {
							if (e.output == null) return Double.POSITIVE_INFINITY;
							// use decompacted counts (normalized counts) for math
							double inCount = Math.max(1, (double) e.input.countDecompacted());
							double outCount = Math.max(1, (double) e.output.countDecompacted());
							return inCount / outCount; // cost (input) per single normalized output item
						}));
					}
					// If client requested "closest" sorting, ensure we sort by distance from player position
					if (query != null && query.sortMode != null && "closest".equalsIgnoreCase(query.sortMode) && query.pos != null) {
						searchResult.exchanges.sort(java.util.Comparator.comparingDouble(e -> {
							if (e.pos == null) return Double.POSITIVE_INFINITY;
							return query.pos.distance(e.pos);
						}));
					}
				})
				.exceptionally(Api.logError("Failed searching exchanges"));
	}

	public static class SearchResult {
		@Expose
		public @NotNull List<Exchange> exchanges;

		public long ts;

		public SearchResult(@NotNull List<Exchange> exchanges) {
			this.exchanges = exchanges;
		}
	}
}
