package io.github.gjum.mc.tradex;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import io.github.gjum.mc.tradex.api.Exchanges;
import java.text.SimpleDateFormat;
import java.util.Date;
import io.github.gjum.mc.tradex.api.MojangAuthProtocol;
import io.github.gjum.mc.tradex.model.Exchange;
import io.github.gjum.mc.tradex.model.Pos;
import io.github.gjum.mc.tradex.model.SearchQuery;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//? if >=1.21.11 {
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
//?} else if >=1.21.6 {
/*import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
*///?} else {
/*import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
*///?}
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static io.github.gjum.mc.tradex.Utils.mc;
import static io.github.gjum.mc.tradex.api.Api.gson;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding;

public class TradexMod implements ModInitializer, ChatHandler.InfoProvider {
	public static final Logger LOG = LoggerFactory.getLogger("tradex");

	public static final String VERSION = /*$ mod_version*/ "1.0.0";

	public static TradexMod mod;

	//? if >=1.21.11 {
	public KeyMapping keyOpenGui = new KeyMapping("Open Tradex Search", InputConstants.KEY_X, KeyMapping.Category.MISC);
	public KeyMapping keyClearHighlights = new KeyMapping("Clear Highlights", InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC);
	//?} else {
	/*public KeyMapping keyOpenGui = new KeyMapping("Open Tradex Search", InputConstants.KEY_X, "Tradex");
	public KeyMapping keyClearHighlights = new KeyMapping("Clear Highlights", InputConstants.UNKNOWN.getValue(), "Tradex");
	*///?}

	private ChatHandler chatHandler = new ChatHandler(this);

	public final HighlightManager highlightManager = new HighlightManager();

	public final ExploredExchangesSource exploredSource = highlightManager.exploredSource;
	public final SearchResultSource searchSource = highlightManager.searchSource;
	public final NearbyExchangesSource nearbySource = highlightManager.nearbySource;

	public @NotNull String getCurrentServerName() {
		if (mc.getCurrentServer() == null) return "singleplayer"; // single player
		return mc.getCurrentServer().ip;
	}

	public @NotNull String getCurrentWorldName() {
		//? if >=1.21.11 {
		return mc.level.dimension().identifier().getPath();
		//?} else {
		/*return mc.level.dimension().location().getPath();
		*///?}
	}

	public @NotNull Pos getPlayerPos() {
		var p = mc.player.blockPosition();
		return new Pos(getCurrentServerName(), getCurrentWorldName(),
				p.getX(), p.getY(), p.getZ());
	}

	@Override
	public void onInitialize() {
		if (mod != null) throw new IllegalStateException("Constructor called twice");
		mod = new TradexMod();
		TradexConfig.load();
		MojangAuthProtocol.obtainToken();

		registerKeyBinding(mod.keyOpenGui);
		registerKeyBinding(mod.keyClearHighlights);

		ClientCommandRegistrationCallback.EVENT.register(mod::onRegisterSlashCommands);

		ClientTickEvents.START_CLIENT_TICK.register(mod::handleTick);

		//? if >=1.21.11 {
		WorldRenderEvents.AFTER_ENTITIES.register(mod::render);
		//?} else if >=1.21.6 {
		/*WorldRenderEvents.AFTER_TRANSLUCENT.register(mod::render);
		// Register HUD overlay for search/nearby highlights (renders through walls)
		HudRenderCallback.EVENT.register(OverlayRender::renderHudOverlay);
		*///?} else {
		/*WorldRenderEvents.AFTER_TRANSLUCENT.register(mod::render);
		*///?}
	}

	public void handleJoinGame(ClientboundLoginPacket packet) {
		try {
			chatHandler = new ChatHandler(this);
			highlightManager.reset();
		} catch (Throwable err) {
			err.printStackTrace();
		}
	}

	public void onRegisterSlashCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext _context) {
		dispatcher.register(
				literal("tradex"
				).executes(context -> {
					var gui = new SearchGui(null);
					mc.setScreen(gui);
					return 1;
				}));

		dispatcher.register(
				literal("tradex"
				).then(literal("searchcoord"
				).then(argument("x", IntegerArgumentType.integer()
				).then(argument("y", IntegerArgumentType.integer()
				).then(argument("z", IntegerArgumentType.integer()).executes(context -> {
					int x = IntegerArgumentType.getInteger(context, "x");
					int y = IntegerArgumentType.getInteger(context, "y");
					int z = IntegerArgumentType.getInteger(context, "z");
					int limit = 100;
					// Build a SearchQuery centered on the provided coordinates (use y=64 by default)
					var pos = new Pos(getCurrentServerName(), getCurrentWorldName(), x, y, z);
					var query = new SearchQuery(
						"", "",
						pos,
						0L, // updatedAfter: 0 to not filter by time
						true, // allow unstocked
						limit,
						"closest"
					);
					Exchanges.search(query).thenAccept(result -> {
						if (result == null || result.exchanges == null || result.exchanges.isEmpty()) {
							Utils.showChat(Component.literal("Tradex: no exchanges found for coords " + x + "," + z));
							return;
						}
						// Post a summary to chat on the main thread
						mc.doRunTask(() -> {
							Utils.showChat(Component.literal("Tradex: found " + result.exchanges.size() + " exchanges (closest first)"));
							int shown = Math.min(100, result.exchanges.size());
							for (int i = 0; i < shown; i++) {
								var ex = result.exchanges.get(i);
								String world = ex.pos != null ? ex.pos.world : "?";
								int exx = ex.pos != null ? ex.pos.x : 0;
								int exy = ex.pos != null ? ex.pos.y : 0;
								int exz = ex.pos != null ? ex.pos.z : 0;
								double dist = ex.pos != null ? pos.distance(ex.pos) : Double.NaN;
								String exName = "";
								if (ex.output != null) exName = ex.output.count + " " + ex.output.material;
								else if (ex.input != null) exName = ex.input.count + " " + ex.input.material;
								Utils.showChat(Component.literal(String.format("%d) %s @ %d,%d,%d (%s) %s", i+1, exName, exx, exy, exz, world, Utils.distanceToText(dist))));
							}
							if (result.exchanges.size() > shown) {
								Utils.showChat(Component.literal("...and " + (result.exchanges.size() - shown) + " more"));
							}
						});
					}).exceptionally(e -> {
						e.printStackTrace();
						mc.doRunTask(() -> Utils.showChat(Component.literal("Tradex: search failed: " + e.getMessage())));
						return null;
					});
					return 1;
				}))))));

		dispatcher.register(
				literal("tradex"
				).then(literal("search"
				).then(argument("output", StringArgumentType.greedyString()
				).executes(context -> {
					String output = context.getArgument("output", String.class);
					var gui = new SearchGui(null);
					gui.inputQuery = "";
					gui.outputQuery = output;
					gui.sortMode = SearchGui.SortMode.closest;
					gui.performSearch();
					mc.setScreen(gui);
					return 1;
				}))));

		// Comprehensive test command: /tradex test <x> <y> <z> <sortMode> <limit> <print> <maxAgeDays> <allowUnstocked>
		// sortMode: closest, cheapest, latest, stock
		// maxAgeDays: 0 = no filter, otherwise filters to entries updated within that many days
		dispatcher.register(
				literal("tradex")
				.then(literal("test")
				.then(argument("x", IntegerArgumentType.integer())
				.then(argument("y", IntegerArgumentType.integer())
				.then(argument("z", IntegerArgumentType.integer())
				.then(argument("sortMode", StringArgumentType.word())
				.then(argument("limit", IntegerArgumentType.integer(1, 10000))
				.then(argument("print", IntegerArgumentType.integer(1, 1000))
				.then(argument("maxAgeDays", IntegerArgumentType.integer(0, 3650))
				.then(argument("allowUnstocked", BoolArgumentType.bool())
				.executes(context -> {
					int x = IntegerArgumentType.getInteger(context, "x");
					int y = IntegerArgumentType.getInteger(context, "y");
					int z = IntegerArgumentType.getInteger(context, "z");
					String sortModeStr = StringArgumentType.getString(context, "sortMode");
					int limit = IntegerArgumentType.getInteger(context, "limit");
					int print = IntegerArgumentType.getInteger(context, "print");
					int maxAgeDays = IntegerArgumentType.getInteger(context, "maxAgeDays");
					boolean allowUnstocked = BoolArgumentType.getBool(context, "allowUnstocked");
					
					// Calculate updatedAfter timestamp
					long updatedAfter = 0L;
					if (maxAgeDays > 0) {
						updatedAfter = System.currentTimeMillis() - (maxAgeDays * 24L * 60 * 60 * 1000);
					}
					
					var pos = new Pos(getCurrentServerName(), getCurrentWorldName(), x, y, z);
					var query = new SearchQuery(
						"", "",
						pos,
						updatedAfter,
						allowUnstocked,
						limit,
						sortModeStr
					);
					
					final long finalUpdatedAfter = updatedAfter;
					Utils.showChat(Component.literal(String.format(
						"Tradex test: pos=(%d,%d,%d) sort=%s limit=%d print=%d maxAge=%dd unstocked=%b",
						x, y, z, sortModeStr, limit, print, maxAgeDays, allowUnstocked
					)));
					
					Exchanges.search(query).thenAccept(result -> {
						if (result == null || result.exchanges == null || result.exchanges.isEmpty()) {
							mc.doRunTask(() -> Utils.showChat(Component.literal("Tradex: no exchanges found")));
							return;
						}
						mc.doRunTask(() -> {
							SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
							Utils.showChat(Component.literal("Tradex: found " + result.exchanges.size() + " exchanges (sort: " + sortModeStr + ")"));
							int shown = Math.min(print, result.exchanges.size());
							for (int i = 0; i < shown; i++) {
								var ex = result.exchanges.get(i);
								String world = ex.pos != null ? ex.pos.world : "?";
								int exx = ex.pos != null ? ex.pos.x : 0;
								int exy = ex.pos != null ? ex.pos.y : 0;
								int exz = ex.pos != null ? ex.pos.z : 0;
								double dist = ex.pos != null ? pos.distance(ex.pos) : Double.NaN;
								String exName = "";
								if (ex.output != null) exName = ex.output.count + " " + ex.output.material;
								else if (ex.input != null) exName = ex.input.count + " " + ex.input.material;
								String lastUpdated = dateFormat.format(new Date(ex.time));
								String stockInfo = ex.stock >= 0 ? " stock=" + ex.stock : "";
								Utils.showChat(Component.literal(String.format(
									"%d) %s @ %d,%d,%d (%s) %s%s updated=%s",
									i+1, exName, exx, exy, exz, world, Utils.distanceToText(dist), stockInfo, lastUpdated
								)));
							}
							if (result.exchanges.size() > shown) {
								Utils.showChat(Component.literal("...and " + (result.exchanges.size() - shown) + " more"));
							}
						});
					}).exceptionally(e -> {
						e.printStackTrace();
						mc.doRunTask(() -> Utils.showChat(Component.literal("Tradex: search failed: " + e.getMessage())));
						return null;
					});
					return 1;
				})))))))))));
	}

	public void handleTick(Minecraft minecraft) {
		try {
			if (keyOpenGui.consumeClick()) {
				mc.setScreen(new SearchGui(null));
			}
			if (keyClearHighlights.consumeClick()) {
				highlightManager.clearAll();
			}
			highlightManager.tick();
		} catch (Throwable err) {
			err.printStackTrace();
		}
	}

	public void handleReceivedChat(Component chat) {
		try {
			chatHandler.handeChat(chat);
		} catch (Throwable err) {
			err.printStackTrace();
		}
	}

	public void handleReceivedTabComplete(int id) {
		try {
			if (id == -3 && chatHandler.parsingExchange != null) {
				mc.player.connection.sendCommand("cti");
			}
		} catch (Throwable err) {
			err.printStackTrace();
		}
	}

	public void handleExchangeFromChat(Exchange exchange) {
		Exchanges.upload(exchange);

		exploredSource.addExchange(exchange);
		exploredSource.unsuppress(exchange.pos);

		// If nearby highlighting is active, update it with the new exchange data
		// This ensures that recently clicked exchanges show with updated colors (red/orange/cyan)
		// in addition to the temporary yellow/green explored highlighting
		if (nearbySource.hasActiveHighlights()) {
			nearbySource.addOrUpdateExchange(exchange);
		}

		var cmd = "/tradex search %s".formatted(SearchQuery.getSpecForRule(exchange.output));
		//? if >=1.21.6 {
		var text = Component.literal("[Tradex] Find other shops selling " + exchange.output.material).copy().withStyle(s -> s
				.withUnderlined(true)
				.withClickEvent(new ClickEvent.RunCommand(cmd))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to open the search screen")))
		);
		//?} else {
		/*var text = Component.literal("[Tradex] Find other shops selling " + exchange.output.material).copy().withStyle(s -> s
				.withUnderlined(true)
				.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to open the search screen")))
		);
		*///?}
		Utils.showChat(text);

		try {
			var path = Path.of("exchanges-log.jsonl");
			if (Files.exists(path)) {
				var line = gson.toJson(exchange) + "\n";
				Files.write(path, line.getBytes(), StandardOpenOption.APPEND);
			}
		} catch (IOException ignored) {
		}
	}

	public void sendTabComplete(int id) {
		mc.getConnection().send(new ServerboundCommandSuggestionPacket(id, "hel"));
	}

	public void render(WorldRenderContext context) {
		try {
			Render.render(context);
		} catch (Throwable err) {
			// TODO log unique error once
		}
	}
}
