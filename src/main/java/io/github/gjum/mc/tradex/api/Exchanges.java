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
