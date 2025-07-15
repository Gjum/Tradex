package io.github.gjum.mc.tradex.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.gjum.mc.tradex.TradexMod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static io.github.gjum.mc.tradex.TradexMod.LOG;
import static io.github.gjum.mc.tradex.Utils.mc;

public class Api {
	static final String API_ROOT = "https://api.tradex.civinfo.net";

	public static Gson gson = new GsonBuilder()
			.excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT)
			.excludeFieldsWithoutExposeAnnotation()
			.create();

	public static HttpClient client = HttpClient.newHttpClient();

	public static String getUserAgent() {
		return "TradexMod-fabric-v%s-mc%s".formatted(TradexMod.VERSION, mc.getLaunchedVersion());
	}

	private static @Nullable CompletableFuture<String> token;

	synchronized
	private static CompletableFuture<String> getToken() {
		if (token == null || token.isCompletedExceptionally()) token = MojangAuthProtocol.obtainToken();
		return token;
	}

	public static CompletableFuture<HttpResponse<String>> request(String path, @Nullable Object payload) {
		return request(URI.create(API_ROOT + path), payload);
	}

	public static CompletableFuture<HttpResponse<String>> request(URI uri, @Nullable Object payload) {
		return getToken().thenCompose(token -> {
			var request = HttpRequest.newBuilder()
					.timeout(Duration.ofSeconds(5))
					.uri(uri)
					.setHeader("User-Agent", getUserAgent())
					.setHeader("Authorization", "Bearer " + token);
			if (payload != null) {
				var payloadStr = gson.toJson(payload);
				request.setHeader("Content-Type", "application/json");
				request.POST(HttpRequest.BodyPublishers.ofString(payloadStr));
			}
			return client.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString());
		});
	}

	// utils

	public static <T> Function<HttpResponse<String>, T> parseJson(Class<T> clazz) {
		return res -> gson.fromJson(res.body(), clazz);
	}

	public static void drop(HttpResponse<String> ignored) {
	}

	public static <T> @NotNull Function<Throwable, T> logError(String msg) {
		return e -> {
			LOG.warn(msg);
			e.printStackTrace();
			throw new RuntimeException(e);
		};
	}
}
