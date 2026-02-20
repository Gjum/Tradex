package io.github.gjum.mc.tradex;

import io.github.gjum.mc.tradex.model.Exchange;
import io.github.gjum.mc.tradex.model.Pos;
import io.github.gjum.mc.tradex.model.Rule;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.gjum.mc.tradex.TradexMod.LOG;
import static io.github.gjum.mc.tradex.Utils.intOrNull;
import static io.github.gjum.mc.tradex.Utils.nonNullOr;

/// https://github.com/CivMC/Civ/blob/main/plugins/itemexchange-paper/src/main/java/com/untamedears/itemexchange/rules/ShopRule.java#L82
public class ChatHandler {
	private static final String itemSpec = "(\\d+)\\s*([^\"]+)(?:\\s+\"(.*)\")?";
	protected static final Pattern presentPattern = Pattern.compile("^\\(?([0-9]+)/([0-9]+)\\)? +exchanges? +present\\.?$", Pattern.CASE_INSENSITIVE);
	protected static final Pattern availablePattern = Pattern.compile("^([0-9]+) exchanges? +available\\.?$", Pattern.CASE_INSENSITIVE);
	protected static final Pattern inputPattern = Pattern.compile("^Input: +" + itemSpec + "$");
	protected static final Pattern outputPattern = Pattern.compile("^Output: +" + itemSpec + "$");
	protected static final Pattern enchantPattern = Pattern.compile("^!?([ a-z]+)(?: +([0-9]+|%))?$", Pattern.CASE_INSENSITIVE);
	protected static final Pattern conditionPattern = Pattern.compile("^Condition: +(Undamaged|Damaged|Any)$", Pattern.CASE_INSENSITIVE);
	protected static final Pattern repairLevelPattern = Pattern.compile("^Repair level ([0-9]+)( or less)?$", Pattern.CASE_INSENSITIVE);
	protected static final Pattern potionPattern = Pattern.compile("^Potion +Name: +(.+)$", Pattern.CASE_INSENSITIVE);
	protected static final Pattern bookGenPattern = Pattern.compile("^Generation: +(.+)$");
	protected static final Pattern authorPattern = Pattern.compile("^Author: +(.+)$");
	protected static final Pattern transferPattern = Pattern.compile("^Successful +(exchange|donation)s?!?$", Pattern.CASE_INSENSITIVE);
	protected static final Pattern locationPattern = Pattern.compile("Location: ([-0-9]+) ([-0-9]+) ([-0-9]+)");

	public @Nullable Exchange parsingExchange;
	private @Nullable Exchange lastExchange;
	private boolean currentlyParsingInput;
	private final InfoProvider mod;

	public interface InfoProvider {

		@NotNull String getCurrentServerName();

		@NotNull String getCurrentWorldName();

		void handleExchangeFromChat(Exchange exchange);

		void sendTabComplete(int id);
	}

	public ChatHandler(InfoProvider mod) {
		this.mod = mod;
	}

	public void handeChat(Component chat) {
		final String msg = chat.getString();
		var firstColor = getFirstColor(chat);

		Matcher presentMatcher = presentPattern.matcher(msg);
		if (presentMatcher.matches() && isColor(firstColor, "yellow")) {
			final Integer index = intOrNull(presentMatcher.group(1));
			final Integer multi = intOrNull(presentMatcher.group(2));
			if (index != null) {
				parsingExchange = new Exchange();
				parsingExchange.time = System.currentTimeMillis();
				parsingExchange.index = index; // 1-based
				if (multi != null) {
					parsingExchange.multi = multi;
				}
				mod.sendTabComplete(-3);
			}
			return;
		}

		if (parsingExchange == null) {
			return;
		}

		Matcher inputMatcher = inputPattern.matcher(msg);
		if (inputMatcher.matches() && isColor(firstColor, "yellow")) {
			currentlyParsingInput = true;
			final Integer count = intOrNull(inputMatcher.group(1));
			final String material = inputMatcher.group(2);
			final String customName = inputMatcher.group(3);
			if (count == null || material == null || material.isEmpty()) {
				LOG.warn("Failed to parse exchange input: " + msg);
			} else {
				parsingExchange.input = new Rule(count, material);
				parsingExchange.input.customName = customName;
			}
			return;
		}
		Matcher outputMatcher = outputPattern.matcher(msg);
		if (outputMatcher.matches() && isColor(firstColor, "yellow")) {
			currentlyParsingInput = false;
			final Integer count = intOrNull(outputMatcher.group(1));
			final String material = outputMatcher.group(2);
			final String customName = outputMatcher.group(3);
			if (count == null || material == null || material.isEmpty()) {
				LOG.warn("Failed to parse exchange output: " + msg);
			} else {
				parsingExchange.output = new Rule(count, material);
				parsingExchange.output.customName = customName;
			}
			return;
		}

		final Rule rule = currentlyParsingInput
				? parsingExchange.input
				: parsingExchange.output;
		if (rule != null) {
			Matcher conditionMatcher = conditionPattern.matcher(msg);
			if (conditionMatcher.matches() && isColor(firstColor, "gold")) {
				rule.condition = conditionMatcher.group(1);
				return;
			}
			Matcher repairLevelMatcher = repairLevelPattern.matcher(msg);
			if (repairLevelMatcher.matches() && isColor(firstColor, "gold")) {
				int num = Integer.parseInt(repairLevelMatcher.group(1));
				// num -= 2;
				if (repairLevelMatcher.group(2) != null) {
					num = -num; // "or less"
				}
				rule.repairLevel = num;
				return;
			}
			if ("Never repaired".equals(msg) && isColor(firstColor, "gold")) {
				rule.repairLevel = 0;
				return;
			}
			Matcher potionMatcher = potionPattern.matcher(msg);
			if (potionMatcher.matches() && isColor(firstColor, "aqua")) {
				rule.potionName = potionMatcher.group(1);
				return;
			}
			Matcher authorMatcher = authorPattern.matcher(msg);
			if (authorMatcher.matches() && isColor(firstColor, "dark_aqua")) {
				rule.bookAuthor = authorMatcher.group(1);
				return;
			}
			Matcher bookGenMatcher = bookGenPattern.matcher(msg);
			if (bookGenMatcher.matches() && isColor(firstColor, "dark_aqua")) {
				rule.bookGeneration = bookGenMatcher.group(1);
				return;
			}
			Matcher enchantMatcher = enchantPattern.matcher(msg);
			if (enchantMatcher.matches()) {
				if (isColor(firstColor, "aqua")) {
					var enchant = enchantMatcher.group(1);
					var levelStr = nonNullOr(enchantMatcher.group(2), "0");
					int level = "%".equals(levelStr) ? 0 : Integer.parseInt(levelStr);
					rule.requiredEnchants.put(enchant, level);
					return;
				}
				if (msg.startsWith("!") && isColor(firstColor, "red")) {
					var enchant = enchantMatcher.group(1);
					rule.excludedEnchants.add(enchant);
					return;
				}
			}
			if ("Other enchantments allowed".equals(msg) && isColor(firstColor, "green")) {
				rule.unlistedEnchantsAllowed = true;
				return;
			}
			if (isColor(firstColor, "dark_purple")) {
				rule.lore.add(msg);
				return;
			}
		}

		Matcher availableMatcher = availablePattern.matcher(msg);
		if (availableMatcher.matches() && isColor(firstColor, "yellow")) {
			final Integer available = intOrNull(availableMatcher.group(1));
			if (available != null) {
				parsingExchange.stock = available;
			}
			return;
		}

		var hover = getHoverText(chat);
		if (hover != null && hover.getString().startsWith("Location: ")) {
			var locationMatcher = locationPattern.matcher(hover.getString());
			if (locationMatcher.matches()) {
				// TODO handle double chests. See `Exchange.adjacent`
				parsingExchange.pos = new Pos(
						mod.getCurrentServerName(),
						mod.getCurrentWorldName(),
						Integer.parseInt(locationMatcher.group(1)),
						Integer.parseInt(locationMatcher.group(2)),
						Integer.parseInt(locationMatcher.group(3))
				);

				try {
					mod.handleExchangeFromChat(parsingExchange);
				} catch (Throwable err) {
					err.printStackTrace();
				}
				// mark completed
				lastExchange = parsingExchange;
				parsingExchange = null;
				return;
			}
		}

		Matcher transferMatcher = transferPattern.matcher(msg);
		if (lastExchange != null && transferMatcher.matches() && isColor(firstColor, "green")) {
			if ("exchange".equals(transferMatcher.group(1))) lastExchange.stock--;
			mod.handleExchangeFromChat(lastExchange);
			return;
		}
	}

	private @Nullable Component getHoverText(@NotNull Component chat) {
		var hover = chat.getStyle().getHoverEvent();
		if (hover == null) return null;
		//? if >=1.21.6 {
		if (!(hover instanceof HoverEvent.ShowText showText)) return null;
		return showText.value();
		//?} else {
		/*if (hover.getAction() != HoverEvent.Action.SHOW_TEXT) return null;
		//noinspection unchecked
		return hover.getValue((HoverEvent.Action<Component>) hover.getAction());
		*///?}
	}

	private @Nullable TextColor getFirstColor(@NotNull Component chat) {
		var color = chat.getStyle().getColor();
		if (color != null) return color;
		if (chat.getSiblings().isEmpty()) return null;
		return getFirstColor(chat.getSiblings().getFirst());
	}

	private static boolean isColor(TextColor color, String name) {
		return Objects.equals(color, TextColor.parseColor(name).getOrThrow());
	}
}
