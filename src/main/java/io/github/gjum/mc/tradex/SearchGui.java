package io.github.gjum.mc.tradex;

import io.github.gjum.mc.tradex.model.Exchange;
import io.github.gjum.mc.tradex.model.Pos;
import io.github.gjum.mc.tradex.model.Rule;
import io.github.gjum.mc.tradex.model.SearchQuery;
import io.github.gjum.mc.tradex.api.Exchanges;
import io.github.gjum.mc.gui.*;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.gjum.mc.tradex.TradexMod.mod;
import static io.github.gjum.mc.tradex.Utils.*;
import static io.github.gjum.mc.tradex.WaypointUtils.getMapModName;
import static io.github.gjum.mc.gui.Label.Alignment.*;
import static io.github.gjum.mc.gui.Vec2.Direction.HORIZONTAL;
import static io.github.gjum.mc.gui.Vec2.Direction.VERTICAL;
import static net.minecraft.ChatFormatting.*;

public class SearchGui extends GuiRoot {
	public String inputQuery = "";
	public String outputQuery = "";
	public boolean allowUnstocked = false;
	public boolean showFavoritesOnly = false;
	public SortMode sortMode = SortMode.closest;
	public @Nullable Exchanges.SearchResult searchResult;
	private @Nullable String searchError;

	public Button searchButton = new Button("Search").onClick(btn -> performSearch());

	public enum SortMode {
		closest("Closest"),
		cheapest("Cheapest"),
		latest("Latest"),
		stock("Stocked"),
		;

		public final String display;

		SortMode(String display) {
			this.display = display;
		}
	}

	public SearchGui(Screen parent) {
		super(parent, Component.literal("Tradex: Search shops"));
		// show some shops initially
		performSearch();
	}

	public void performSearch() {
		mc.doRunTask(() -> {
			searchResult = null;
			searchError = null;
			searchButton.setEnabled(false);
			rebuild();
			int limit = 20;
			if (!inputQuery.isEmpty() || !outputQuery.isEmpty()) limit = 100;
			final long updatedAfter = System.currentTimeMillis() - monthMs;
			// When showing favourites, always fetch unstocked too so all favourites appear
			boolean queryAllowUnstocked = allowUnstocked || showFavoritesOnly;
			var query = new SearchQuery(
					inputQuery, outputQuery,
					mod.getPlayerPos(),
					updatedAfter, queryAllowUnstocked, limit, sortMode.name());
			Exchanges.search(query)
					.thenAccept(this::displaySearchResults)
					.exceptionally(e -> {
						mc.doRunTask(() -> {
							searchError = e.getMessage();
							rebuild();
						});
						throw new RuntimeException(e);
					})
			;
		});
	}

	public void displaySearchResults(Exchanges.SearchResult result) {
		mc.doRunTask(() -> {
			this.searchResult = result;
			searchError = null;
			rebuild();
		});
	}

	private void sortBy(SortMode mode) {
		sortMode = mode;
		performSearch();
	}

	//? if >=1.21.11 {
	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		try {
			if (event.key() == GLFW.GLFW_KEY_ENTER) {
				performSearch();
			}
		} catch (Throwable e) {
			handleError(e);
		}
		return super.keyPressed(event);
	}
	//?} else {
	/*@Override
	public boolean keyPressed(int keyCode, int scanCode, int mods) {
		try {
			if (keyCode == GLFW.GLFW_KEY_ENTER) {
				performSearch();
			}
		} catch (Throwable e) {
			handleError(e);
		}
		return super.keyPressed(keyCode, scanCode, mods);
	}
	*///?}

	@Override
	public GuiElement build() {
		final Vec2 spacer = new Vec2(7, 7);
		final TextField outputTextField = new TextField(s -> {
			outputQuery = s;
			searchButton.setEnabled(true);
			return true;
		}, outputQuery, "beacon, enchanted diamond pickaxe, ...");
		final TextField inputTextField = new TextField(s -> {
			inputQuery = s;
			searchButton.setEnabled(true);
			return true;
		}, inputQuery, "exp, diamond, ...");

		outputTextField.setFocused(true);

		var maxTextFieldSize = new Vec2(200, 20);

		final var searchControls = new FlexListLayout(HORIZONTAL)
				.add(new Spacer())
				.add(new Label("I have: ").align(ALIGN_RIGHT).setHeight(20))
				.add(inputTextField.setWeight(new Vec2(999, 0)).setMaxSize(maxTextFieldSize))
				.add(new Spacer(spacer))
				.add(new Label("I want: ").align(ALIGN_RIGHT).setHeight(20))
				.add(outputTextField.setWeight(new Vec2(999, 0)).setMaxSize(maxTextFieldSize))
				.add(searchButton)
				.add(new Spacer());

		final var sortingControls = new FlexListLayout(HORIZONTAL)
				.add(new Spacer())
				.add(new Label("Sort by: ").align(ALIGN_RIGHT).setHeight(20))
				.add(buildSortButton(SortMode.closest))
				.add(buildSortButton(SortMode.cheapest))
				.add(buildSortButton(SortMode.latest))
				.add(buildSortButton(SortMode.stock))
				.add(new Spacer(spacer))
				.add(new Label("Show unstocked: ").align(ALIGN_RIGHT).setHeight(20))
				.add(new Button(allowUnstocked ? "Yes" : "No").onBtnClick(b -> {
					if ("Yes".equals(b.getText().getString())) {
						b.setText(Component.literal("No"));
						allowUnstocked = false;
					} else {
						b.setText(Component.literal("Yes"));
						allowUnstocked = true;
					}
					performSearch();
				}))
				.add(new Spacer(spacer))
				.add(new Label("Showing: ").align(ALIGN_RIGHT).setHeight(20))
				.add(new Button(showFavoritesOnly ? "Favourites" : "Everything").onBtnClick(b -> {
					showFavoritesOnly = !showFavoritesOnly;
					b.setText(Component.literal(showFavoritesOnly ? "Favourites" : "Everything"));
					rebuild();
				}))
				.add(new Spacer());

		Label statusLabel;
		final TableLayout resultsTable = new TableLayout();
		if (searchError != null) {
			statusLabel = new Label("Error: " + searchError).align(ALIGN_CENTER);
		} else if (searchResult == null) {
			statusLabel = new Label("Searching ...").align(ALIGN_CENTER);
		} else if (searchResult.exchanges.isEmpty()) {
			if (inputTextField.getText().isEmpty() && outputTextField.getText().isEmpty()) {
				statusLabel = new Label("Search for exchanges buying/selling certain items.").align(ALIGN_CENTER);
			} else {
				statusLabel = new Label("No exchanges found. Try a different item.").align(ALIGN_CENTER);
			}
			outputTextField.getTextField().setFocused(true);
		} else {
			final String world = mod.getCurrentWorldName();
			final Pos playerPos = mod.getPlayerPos();
			final Vec2 resultsListSpacer = new Vec2(5, 5);
			final FavoritesManager favorites = mod.favorites;
			List<Exchange> displayedExchanges = searchResult.exchanges;
			if (showFavoritesOnly) {
				displayedExchanges = searchResult.exchanges.stream()
						.filter(favorites::isFavorite)
						.collect(Collectors.toList());
			}
			if (displayedExchanges.isEmpty() && showFavoritesOnly) {
				statusLabel = new Label("No favourite exchanges found.").align(ALIGN_CENTER);
			} else if (showFavoritesOnly) {
				String filteredCount = String.valueOf(displayedExchanges.size());
				statusLabel = new Label("Showing " + filteredCount + " favourite(s):").align(ALIGN_CENTER);
			} else {
				String numResults = String.valueOf(searchResult.exchanges.size());
				if (searchResult.exchanges.size() > 99) numResults = "99+";
				statusLabel = new Label("Found " + numResults + " exchanges:").align(ALIGN_CENTER);
			}
			for (Exchange exchange : displayedExchanges) {
				resultsTable.addRow(Collections.singletonList(new Spacer(resultsListSpacer)));

				boolean isFav = favorites.isFavorite(exchange);
				String favSymbol = isFav ? "\u2605" : "\u2606"; // ★ or ☆
				Button favButton = new Button(favSymbol);
				final Exchange ex = exchange;
				favButton.onBtnClick(b -> {
					favorites.toggleFavorite(ex);
					rebuild();
				});
				favButton.setFixedSize(new Vec2(20, 20));

				FlexListLayout metaCol = buildMetaCol(world, playerPos, exchange);
				FlexListLayout inputCol = buildRuleCol("Input:", exchange.input);
				FlexListLayout outputCol = buildRuleCol("Output:", exchange.output);

				resultsTable.addRow(Arrays.asList(null, favButton, metaCol, inputCol, outputCol));
			}
		}
		resultsTable.addRow(Arrays.asList(new Spacer(spacer), new Spacer(), new Spacer(), new Spacer(), new Spacer()));

		final ScrollBox scroller = new ScrollBox(resultsTable);
		scroller.setWeight(new Vec2(Vec2.LARGE, Vec2.LARGE));

//		Button addWaypointsBtn = new Button("Show waypoints for all").onClick((btn) -> {
//			// TODO add waypoints for all
//		});
//		addWaypointsBtn.setEnabled(!exchanges.isEmpty());
//		addWaypointsBtn.setEnabled(false);

		final FlexListLayout bottomControls = new FlexListLayout(HORIZONTAL);
//		bottomControls.add(new Tooltip("Not implemented yet ...\nCheck github.com/Gjum/Tradex/releases",
//				addWaypointsBtn));
		bottomControls.add(new Button("Highlight search results in-game").onClick((btn) -> {
			if (searchResult == null) return;
			if (showFavoritesOnly) {
				List<Exchange> favOnly = searchResult.exchanges.stream()
						.filter(mod.favorites::isFavorite)
						.collect(Collectors.toList());
				mod.lastSearchResult = new Exchanges.SearchResult(favOnly);
				mod.lastSearchResult.ts = searchResult.ts;
				for (var exchange : favOnly) {
					mod.exploredExchanges.remove(exchange.pos);
				}
			} else {
				mod.lastSearchResult = searchResult;
				for (var exchange : searchResult.exchanges) {
					mod.exploredExchanges.remove(exchange.pos);
				}
			}
		}));

		bottomControls.add(new Spacer());

		bottomControls.add(new Button("Close").onClick((btn) -> {
			mc.setScreen(parentScreen);
		}));

		final FlexListLayout content = new FlexListLayout(VERTICAL);
		content.add(new Spacer(spacer));
		content.add(new Label("Tradex: Find Shops").align(ALIGN_CENTER));
		content.add(new Spacer(spacer));
		content.add(searchControls);
		content.add(sortingControls);
		content.add(new Spacer(spacer));
		content.add(statusLabel);
		content.add(new Spacer(spacer));
		content.add(scroller);
		content.add(new Spacer(spacer));
		content.add(bottomControls);

		return content;
	}

	@NotNull
	private GuiElement buildSortButton(@NotNull SortMode mode) {
		return new Button(mode.display)
				.setEnabled(!mode.equals(sortMode))
				.onClick((btn) -> sortBy(mode));
	}

	@NotNull
	public static FlexListLayout buildMetaCol(@Nullable String world, @Nullable Pos playerPos, @NotNull Exchange exchange) {
		final FlexListLayout metaCol = new FlexListLayout(VERTICAL);
		int lineHeight = 9;
		if (exchange.pos != null) {
			String locAbs = exchange.pos.toString();
			if (!Objects.equals(exchange.pos.world, world))
				locAbs += " " + exchange.pos.world;
			final Label coordsLabel = new Label(locAbs).align(ALIGN_LEFT).setHeight(lineHeight);

			final String mapMod = getMapModName();
			final String wpName = "Shop " + getExchangeName(exchange);
			final String cmd = null; // TODO re-enable getWaypointCommand(exchange.pos, wpName);
			final String tooltip;
			if (cmd != null) {
				tooltip = "Click to add " + mapMod + " waypoint.\n"
						+ wpName;
			} else {
				tooltip = "Click to copy waypoint in chat.";
				// tooltip += "\nInstall VoxelMap or JourneyMap to add waypoint.";
			}
			coordsLabel.onClick((btn) -> {
				if (cmd != null) {
					// TODO sendMessage(cmd);
				} else {
					try {
						final String suggestText = "[x:%s, y:%s, z:%s, name:\"%s\"]".formatted(
								exchange.pos.x, exchange.pos.y, exchange.pos.z, wpName);
						//? if >=1.21.11 {
						mc.setScreen(new ChatScreen(suggestText, false));
						//?} else {
						/*mc.setScreen(new ChatScreen(suggestText));
						*///?}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			metaCol.add(new Tooltip(tooltip, coordsLabel));

			if (playerPos != null) {
				final double distance = playerPos.distance(exchange.pos);
				final String distanceColor;
				if (distance <= 500) distanceColor = GREEN.toString();
				else if (distance <= 1000) distanceColor = YELLOW.toString();
				else distanceColor = GRAY.toString();
				final String distText = distanceColor + distanceToText(distance);
				String dirText = directionNameFromDelta(exchange.pos.minus(playerPos));
				var text = distText + RESET + " " + dirText;
				metaCol.add(new Label(text).align(ALIGN_LEFT).setHeight(lineHeight));
			}
		}
		if (exchange.stock >= 0) {
			final String stockColor;
			if (exchange.stock > 0) stockColor = GREEN.toString();
			else stockColor = GRAY.toString();
			final String stockText = stockColor + exchange.stock;

			final long age = System.currentTimeMillis() - exchange.time;
			final String ageColor;
			if (age <= weekMs) ageColor = GREEN.toString();
			else if (age <= monthMs) ageColor = YELLOW.toString();
			else ageColor = GRAY.toString();
			final String ageText = ageColor + durationToText(age);

			var text = stockText + " stocked" + RESET + " (" + ageText + " ago" + RESET + ")";
			metaCol.add(new Label(text).align(ALIGN_LEFT).setHeight(lineHeight));
		}
		if (exchange.message != null) {
			for (var line : exchange.message.split("\n")) {
				metaCol.add(new Label(line).align(ALIGN_LEFT).setHeight(lineHeight));
			}
		}
		return metaCol;
	}

	@NotNull
	private static String getExchangeName(@NotNull Exchange exchange) {
		final String input = formatRuleForShopName(exchange.input);
		if (exchange.output == null) return input + " donation";
		final String output = formatRuleForShopName(exchange.output);
		return input + " -> " + output;
	}

	@NotNull
	private static String formatRuleForShopName(Rule rule) {
		String mat = rule.material;
		if (rule.potionName != null) mat = rule.potionName;
		if (rule.customName != null) mat = '"' + rule.customName + '"';
		if (rule.bookGeneration != null) mat = " (" + rule.bookGeneration + ')';
		return rule.count + " " + mat;
	}

	private FlexListLayout buildRuleCol(@NotNull String title, @Nullable Rule rule) {
		int lineHeight = 9;
		FlexListLayout col = new FlexListLayout(VERTICAL);
		if (rule != null && rule.message != null) {
			for (var line : rule.message.split("\n")) {
				col.add(new Label(line).align(ALIGN_LEFT).setHeight(lineHeight));
			}
			return col;
		}
		col.add(new Label(GRAY + title).align(ALIGN_LEFT).setHeight(lineHeight));
		if (rule == null) {
			col.add(new Label(GRAY + "Nothing").align(ALIGN_LEFT).setHeight(lineHeight));
		} else {
			int numLores = rule.lore.size();
			String compacted = "" + rule.count;
			if (rule.isCompacted()) {
				compacted = "" + DARK_PURPLE + rule.count + " compacted" + RESET;
				numLores--; // if compacted is the only lore, don't show tooltip
			}
			final String numPrefix = compacted + " ";
			if (rule.customName != null) {
				col.add(new Label(
						numPrefix + "\"" + rule.customName + "\"").align(ALIGN_LEFT).setHeight(lineHeight));
				col.add(new Label(
						"(" + rule.material + ")").align(ALIGN_LEFT).setHeight(lineHeight));
			} else {
				col.add(new Label(
						numPrefix + rule.material).align(ALIGN_LEFT).setHeight(lineHeight));
			}
			if (rule.bookGeneration != null) col.add(new Label(
					GRAY + "By " + RESET + rule.bookAuthor + RESET + GRAY + ": " + RESET + rule.bookGeneration).align(ALIGN_LEFT).setHeight(lineHeight));
			if (rule.potionName != null) col.add(new Label(
					GRAY + "Effect: " + AQUA + rule.potionName).align(ALIGN_LEFT).setHeight(lineHeight));
			if (numLores > 0) {
				List<String> loreLines = rule.lore.stream()
						.filter(lore -> !"Compacted".equals(lore))
						.map(l -> DARK_PURPLE + l)
						.collect(Collectors.toList());
				if (loreLines.size() == 1 && loreLines.get(0).length() < 30) {
					col.add(new Label(DARK_PURPLE + loreLines.get(0)).align(ALIGN_LEFT).setHeight(lineHeight));
				} else {
					String tooltip = String.join("\n", loreLines);
					col.add(new Tooltip(tooltip, new Label(
							DARK_PURPLE + "Show additional lore").align(ALIGN_LEFT).setHeight(lineHeight)));
				}
			}
			// TODO display storedEnchants
			if (!rule.requiredEnchants.isEmpty()) {
				String enchStr = rule.requiredEnchants.entrySet().stream()
						.map(e -> {
							var eShort = enchantCodeByName.getOrDefault(e.getKey(), e.getKey());
							return eShort + e.getValue();
						})
						.sorted()
						.collect(Collectors.joining(" "));
				String text = GRAY + "With enchantments: " + GREEN + enchStr;
				col.add(new Label(text).align(ALIGN_LEFT).setHeight(lineHeight));
			}
			if (!rule.excludedEnchants.isEmpty()) {
				String enchStr = rule.excludedEnchants.stream()
						.sorted().collect(Collectors.joining(" "));
				String text = GRAY + "Without enchantments: " + RED + enchStr;
				col.add(new Label(text).align(ALIGN_LEFT).setHeight(lineHeight));
			}
			if (!rule.requiredEnchants.isEmpty() && !rule.excludedEnchants.isEmpty()) {
				String text = GRAY + "Other enchantments " +
						(rule.unlistedEnchantsAllowed ? GREEN + "allowed" : RED + "disallowed");
				col.add(new Label(text).align(ALIGN_LEFT).setHeight(lineHeight));
			}
		}
		return col;
	}
}
