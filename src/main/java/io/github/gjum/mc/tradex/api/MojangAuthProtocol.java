package io.github.gjum.mc.tradex.api;

import com.google.gson.annotations.Expose;
import com.mojang.authlib.exceptions.AuthenticationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static io.github.gjum.mc.tradex.Utils.mc;
import static io.github.gjum.mc.tradex.api.Api.API_ROOT;

/// Class serves as response payload parser
public class MojangAuthProtocol {
	@Expose
	public @Nullable String username;
	@Expose
	public @Nullable String challenge;
	@Expose
	public @Nullable String token;
	@Expose
	public @Nullable String error;

	public static CompletableFuture<String> obtainToken() {
		return getChallenge()
				.thenApply(MojangAuthProtocol::joinServerMojangApi)
				.thenCompose(MojangAuthProtocol::getTokenFromChallenge)
				.exceptionally(Api.logError("Failed obtaining token"));
	}

	private static CompletableFuture<String> getChallenge() {
		var payload = new MojangAuthProtocol();
		payload.username = mc.getUser().getName();
		return get("/login/mojang", payload);
	}

	private static @NotNull String joinServerMojangApi(@NotNull String challenge) {
		var hostname = URI.create(API_ROOT).getHost();
		var secret = challenge + "\n" + hostname;
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			digest.update(secret.getBytes());
			String sha = new BigInteger(digest.digest()).toString(16);

			//? if >=1.21.11 {
			mc.services().sessionService().joinServer(
			//?} else {
			/*mc.getMinecraftSessionService().joinServer(
			*///?}
					mc.getUser().getProfileId(),
					mc.getUser().getAccessToken(),
					sha);
			return challenge;
		} catch (NoSuchAlgorithmException | AuthenticationException e) {
			throw new RuntimeException(e);
		}
	}

	private static CompletableFuture<String> getTokenFromChallenge(@NotNull String challenge) {
		var payload = new MojangAuthProtocol();
		payload.username = mc.getUser().getName();
		payload.challenge = challenge;
		return get("/login/mojang", payload);
	}

	private static CompletableFuture<String> get(String path, MojangAuthProtocol payload) {
		HttpRequest request = HttpRequest.newBuilder()
				.timeout(Duration.ofSeconds(5))
				.uri(URI.create(API_ROOT + path))
				.setHeader("User-Agent", Api.getUserAgent())
				.setHeader("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(Api.gson.toJson(payload)))
				.build();
		return Api.client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(MojangAuthProtocol::handle);
	}

	private static @NotNull String handle(HttpResponse<String> res) {
		var parsed = Api.gson.fromJson(res.body(), MojangAuthProtocol.class);
		if (parsed.token != null) {
			return parsed.token;
		} else if (parsed.challenge != null) {
			return parsed.challenge;
		} else if (parsed.error != null) {
			throw new Error("Failed to authenticate: %d %s".formatted(
					res.statusCode(), parsed.error));
		} else {
			throw new IllegalArgumentException("Unexpected response: " + res.body());
		}
	}
}
