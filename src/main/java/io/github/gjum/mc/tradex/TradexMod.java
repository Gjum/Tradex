package io.github.gjum.mc.tradex;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.gjum.mc.tradex.api.Exchanges;
import io.github.gjum.mc.tradex.api.MojangAuthProtocol;
import io.github.gjum.mc.tradex.model.Exchange;
import io.github.gjum.mc.tradex.model.ExchangeChest;
import io.github.gjum.mc.tradex.model.Pos;
import io.github.gjum.mc.tradex.model.SearchQuery;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
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
import java.util.HashMap;

import static io.github.gjum.mc.tradex.Utils.mc;
import static io.github.gjum.mc.tradex.api.Api.gson;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding;

public class TradexMod implements ModInitializer, ChatHandler.InfoProvider {
	public static final Logger LOG = LoggerFactory.getLogger("tradex");

	public static final String VERSION = /*$ mod_version*/ "1.0.0";

	public static TradexMod mod;

	public KeyMapping keyOpenGui = new KeyMapping("Open Tradex Search", InputConstants.KEY_X, "Tradex");
	public KeyMapping keyClearHighlights = new KeyMapping("Clear Highlights", InputConstants.UNKNOWN.getValue(), "Tradex");

	private ChatHandler chatHandler = new ChatHandler(this);

	public final HighlightManager highlightManager = new HighlightManager();

	// store all clicked exchanges so the user can go back in chat and search for any previous exchange
	public HashMap<Pos, ExchangeChest> exploredExchanges = new HashMap<>();
	public @Nullable Exchanges.SearchResult lastSearchResult;

	public @NotNull String getCurrentServerName() {
		if (mc.getCurrentServer() == null) return "singleplayer"; // single player
		return mc.getCurrentServer().ip;
	}

	public @NotNull String getCurrentWorldName() {
		return mc.level.dimension().location().getPath();
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

		WorldRenderEvents.AFTER_TRANSLUCENT.register(mod::render);
	}

	public void handleJoinGame(ClientboundLoginPacket packet) {
		try {
			chatHandler = new ChatHandler(this);
			exploredExchanges.clear();
			lastSearchResult = null;
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

		exploredExchanges.computeIfAbsent(exchange.pos, e -> new ExchangeChest()).add(exchange);
		highlightManager.registerHighlight(exchange.pos);

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
