package io.github.gjum.mc.tradex;

import io.github.gjum.mc.tradex.model.Exchange;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
//? if >=1.21.6 {
import net.minecraft.network.chat.ComponentSerialization;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
//?}
import net.minecraft.server.Bootstrap;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ChatHandlerTest {

	@BeforeAll
	static void beforeAll() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private static class ModMock implements ChatHandler.InfoProvider {
		Exchange createdExchange;

		public void handleExchangeFromChat(Exchange exchange) {
			if (createdExchange != null) {
				throw new Error("Double set: already had " + createdExchange + " then got " + exchange);
			}
			createdExchange = exchange;
		}

		public @NotNull String getCurrentServerName() {
			return "SERVER";
		}

		public @NotNull String getCurrentWorldName() {
			return "WORLD";
		}

		public void sendTabComplete(int id) {
		}
	}

	@Test
	void readsExchangeChat() {
		var mod = new ModMock();
		var handler = new ChatHandler(mod);

		//? if >=1.21.6 {
		// 1.21.5+ uses hover_event with value instead of hoverEvent with contents
		var locationJson = "{\"hover_event\":{\"action\":\"show_text\",\"value\":{\"text\":\"Location: -123 64 -234\"}},\"text\":\"§aReinforced at §a100% (2000/2000)§a health with §bDiamond §aon §dGjum\"}";
		//?} else {
		/*var locationJson = "{\"hoverEvent\":{\"action\":\"show_text\",\"contents\":{\"text\":\"Location: -123 64 -234\"}},\"text\":\"§aReinforced at §a100% (2000/2000)§a health with §bDiamond §aon §dGjum\"}";
		*///?}

		var chats = new String[]{
				"{\"extra\":[{\"color\":\"yellow\",\"text\":\"(2/3) exchanges present.\"}],\"text\":\"\"}",
				"{\"extra\":[{\"color\":\"yellow\",\"text\":\"Input: \"},{\"color\":\"white\",\"text\":\"42 Diamond Pickaxe\"},{\"italic\":true,\"color\":\"white\",\"text\":\" \\\"custom name\"},{\"italic\":true,\"color\":\"white\",\"text\":\"\\\"\"}],\"text\":\"\"}",
				"{\"extra\":[{\"color\":\"aqua\",\"text\":\"Efficiency 3\"}],\"text\":\"\"}",
				"{\"extra\":[{\"color\":\"red\",\"text\":\"!Unbreaking\"}],\"text\":\"\"}",
				"{\"extra\":[{\"color\":\"green\",\"text\":\"Other enchantments allowed\"}],\"text\":\"\"}",
				"{\"extra\":[{\"italic\":true,\"color\":\"dark_purple\",\"text\":\"Hi there\"}],\"text\":\"\"}",
				"{\"extra\":[{\"italic\":true,\"color\":\"dark_purple\",\"text\":\"Hello\"}],\"text\":\"\"}",
				"{\"extra\":[{\"color\":\"gold\",\"text\":\"Condition: Damaged\"}],\"text\":\"\"}",
				"{\"extra\":[{\"color\":\"gold\",\"text\":\"Repair level 23\"}],\"text\":\"\"}",
				"{\"extra\":[{\"color\":\"yellow\",\"text\":\"Output: \"},{\"color\":\"white\",\"text\":\"2 Stone\"},{\"italic\":true,\"color\":\"white\",\"text\":\" \\\"Shadno Stone\"},{\"italic\":true,\"color\":\"white\",\"text\":\"\\\"\"}],\"text\":\"\"}",
				"{\"extra\":[{\"color\":\"gold\",\"text\":\"Repair level 23 or less\"}],\"text\":\"\"}",
				"{\"extra\":[{\"text\":\"Group: Gjum\"}],\"text\":\"\"}",
				"{\"extra\":[{\"color\":\"yellow\",\"text\":\"1 exchange available.\"}],\"text\":\"\"}",
				locationJson,
		};
		for (String json : chats) {
			handler.handeChat(parseChat(json));
		}

		assertNull(handler.parsingExchange);
		var exchange = mod.createdExchange;
		assertNotNull(exchange);
		assertEquals(2, exchange.index);
		assertEquals(3, exchange.multi);
		assertEquals(1, exchange.stock);
		assertEquals("Diamond Pickaxe", exchange.input.material);
		assertEquals(42, exchange.input.count);
		assertEquals("custom name", exchange.input.customName);
		assertEquals(1, exchange.input.requiredEnchants.size());
		assertEquals(3, exchange.input.requiredEnchants.get("Efficiency"));
		assertEquals(1, exchange.input.excludedEnchants.size());
		assertEquals(23, exchange.input.repairLevel);
		assertTrue(exchange.input.excludedEnchants.contains("Unbreaking"));
		assertNotNull(exchange.output);
		assertEquals("Stone", exchange.output.material);
		assertEquals(-23, exchange.output.repairLevel);
	}

	@Test
	void missingCti() {
		var mod = new ModMock();
		var handler = new ChatHandler(mod);

		handler.handeChat(parseChat("{\"extra\":[{\"color\":\"yellow\",\"text\":\"(2/3) exchanges present.\"}],\"text\":\"\"}"));
		handler.handeChat(parseChat("{\"extra\":[{\"color\":\"yellow\",\"text\":\"Input: \"},{\"color\":\"white\",\"text\":\"42 Diamond Pickaxe\"},{\"italic\":true,\"color\":\"white\",\"text\":\" \\\"custom name\"},{\"italic\":true,\"color\":\"white\",\"text\":\"\\\"\"}],\"text\":\"\"}"));
		handler.handeChat(parseChat("{\"extra\":[{\"color\":\"yellow\",\"text\":\"Output: \"},{\"color\":\"white\",\"text\":\"2 Stone\"},{\"italic\":true,\"color\":\"white\",\"text\":\" \\\"Shadno Stone\"},{\"italic\":true,\"color\":\"white\",\"text\":\"\\\"\"}],\"text\":\"\"}"));
		handler.handeChat(parseChat("{\"extra\":[{\"color\":\"yellow\",\"text\":\"1 exchange available.\"}],\"text\":\"\"}"));

		assertNotNull(handler.parsingExchange);
	}

	private static Component parseChat(String json) {
		//? if >=1.21.6 {
		var registryOps = HolderLookup.Provider.create(Stream.of()).createSerializationContext(JsonOps.INSTANCE);
		return ComponentSerialization.CODEC.parse(registryOps, JsonParser.parseString(json)).getOrThrow();
		//?} else {
		/*return Component.Serializer.fromJson(json, HolderLookup.Provider.create(Stream.of()));
		*///?}
	}
}
