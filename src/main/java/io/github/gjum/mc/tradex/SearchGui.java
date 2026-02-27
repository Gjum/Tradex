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

/**
 * Enchantment filter mode for search queries.
 */
enum EnchantFilterMode {
	ANY("Any"),
	SPECIFIC("Specific");

	public final String display;

	EnchantFilterMode(String display) {
		this.display = display;
	}
}

public class SearchGui extends GuiRoot {
	public String inputQuery = "";
	public String outputQuery = "";
	public boolean allowUnstocked = false;
	public SortMode sortMode = SortMode.closest;
	public @Nullable Exchanges.SearchResult searchResult;
	private @Nullable String searchError;

	// Strict search mode for output filter
	// When true: only matches exact material names (e.g., "sand" matches only "sand", not "sandstone")
	// When false: matches partial material names (e.g., "sand" matches "sand", "sandstone", "soul sand")
	public boolean strictSearch = false;

	// Enchantment filter state
	public EnchantFilterMode enchantFilterMode = EnchantFilterMode.ANY;
	// For specific enchant search. Supports formats:
	// "silk touch" - any level
	// "sharpness 5" - exact level
	// "silk touch, efficiency 5, unbreaking" - multiple with or without levels
	public String enchantQuery = "";
	// When true in SPECIFIC mode: only the specified enchants allowed (no extras)
	// Empty query + restrictive = unenchanted items only
	public boolean restrictiveEnchants = false;

	// Guard to prevent concurrent searches
	private boolean isSearching = false;

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
		// Prevent concurrent searches that cause "too many streams" errors
		if (isSearching) return;
		isSearching = true;

		mc.doRunTask(() -> {
			searchResult = null;
			searchError = null;
			searchButton.setEnabled(false);
			rebuild();
			int limit = 20;
			if (!inputQuery.isEmpty() || !outputQuery.isEmpty()) limit = 100;
			final long updatedAfter = System.currentTimeMillis() - monthMs;

			var query = new SearchQuery(
					inputQuery, outputQuery,
					mod.getPlayerPos(),
					updatedAfter, allowUnstocked, limit, sortMode.name()
			);
			Exchanges.search(query)
					.thenAccept(result -> {
						isSearching = false;
						// Apply both enchant filter and strict search filter
						var filteredResult = applyClientSideEnchantFilter(result);
						filteredResult = applyStrictSearchFilter(filteredResult);
						displaySearchResults(filteredResult);
					})
					.exceptionally(e -> {
						isSearching = false;
						mc.doRunTask(() -> {
							searchError = e.getMessage();
							searchButton.setEnabled(true);
							rebuild();
						});
						return null;
					})
			;
		});
	}

	/**
	 * Parsed enchant filter with name and optional level constraint.
	 */
	private record EnchantFilter(String name, @Nullable Integer level) {}

	/**
	 * Parse enchant query string into list of EnchantFilter objects.
	 * Supports formats:
	 *   "silk touch" - any level
	 *   "sharpness 5" - exact level 5
	 *   "silk touch, efficiency 5, unbreaking 3" - multiple
	 *   "ST, E5, U3" - codes with levels (comma-separated)
	 *   "ST E5 U3" - codes with levels (space-separated)
	 */
	private List<EnchantFilter> parseEnchantFilters(String query) {
		if (query == null || query.isBlank()) return new ArrayList<>();
		
		// If no commas/semicolons, split on spaces (for short codes like "E5 U3 ST")
		// Otherwise split on commas/semicolons (for "efficiency 5, unbreaking 3")
		String delimiter = query.contains(",") || query.contains(";") ? "[,;]" : "[\\s]+";
		
		return Arrays.stream(query.split(delimiter))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.map(this::parseOneEnchantFilter)
				.collect(Collectors.toList());
	}

	/**
	 * Parse a single enchant filter like "sharpness 5" or "ST" or "efficiency".
	 */
	private EnchantFilter parseOneEnchantFilter(String input) {
		input = input.trim();
		
		// Try to extract trailing number for level
		Integer level = null;
		String name = input;
		
		// Check for patterns like "sharpness 5" or "sharpness5" or "E5"
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(.+?)\\s*(\\d+)$").matcher(input);
		if (m.matches()) {
			name = m.group(1).trim();
			try {
				level = Integer.parseInt(m.group(2));
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		
		// Normalize the name (handle codes like "ST", "E", etc.)
		name = normalizeEnchantName(name);
		
		return new EnchantFilter(name, level);
	}

	/**
	 * Parse enchant query into just the names (for server query).
	 */
	private List<String> parseEnchantNames(String query) {
		return parseEnchantFilters(query).stream()
				.map(EnchantFilter::name)
				.collect(Collectors.toList());
	}

	/**
	 * Normalize enchant name - handle codes like "ST" for "Silk Touch".
	 */
	private String normalizeEnchantName(String input) {
		String normalized = input.trim().toLowerCase();
		// Check if it's a code (e.g., "ST", "E", "U")
		String fullName = enchantNameByCode.get(input.trim().toUpperCase());
		if (fullName != null) return fullName.toLowerCase();
		// Also try with original casing
		fullName = enchantNameByCode.get(input.trim());
		if (fullName != null) return fullName.toLowerCase();
		return normalized;
	}

	/**
	 * Apply client-side enchantment filtering to search results.
	 * This handles filtering that the server may not support.
	 */
	private Exchanges.SearchResult applyClientSideEnchantFilter(Exchanges.SearchResult result) {
		if (result == null || result.exchanges == null) return result;

		// If no enchant filter is active, return as-is
		// Filter is active when: mode is SPECIFIC, or (mode is SPECIFIC and restrictive+empty)
		if (enchantFilterMode == EnchantFilterMode.ANY) {
			return result;
		}
		
		// In SPECIFIC mode, always apply filter (even if query is empty, restrictive mode handles it)
		List<EnchantFilter> requiredEnchants = parseEnchantFilters(enchantQuery);

		List<Exchange> filtered = result.exchanges.stream()
				.filter(exchange -> matchesEnchantFilter(exchange, requiredEnchants))
				.collect(Collectors.toList());

		return new Exchanges.SearchResult(filtered);
	}

	/**
	 * Apply client-side strict search filtering to search results.
	 * When strictSearch is enabled, only exchanges where the output material name
	 * exactly matches the search query (case-insensitive) are shown.
	 * This prevents partial matches like "sand" matching "sandstone" or "soul sand".
	 */
	private Exchanges.SearchResult applyStrictSearchFilter(Exchanges.SearchResult result) {
		if (result == null || result.exchanges == null) return result;

		// If strict search is disabled or no output query, return as-is
		if (!strictSearch || outputQuery.isBlank()) {
			return result;
		}

		// Normalize the search query for comparison
		String normalizedQuery = outputQuery.trim().toLowerCase();

		List<Exchange> filtered = result.exchanges.stream()
				.filter(exchange -> {
					// Check if the output material matches exactly
					if (exchange.output != null && exchange.output.material != null) {
						String normalizedMaterial = exchange.output.material.toLowerCase().trim();
						return normalizedMaterial.equals(normalizedQuery);
					}
					return false;
				})
				.collect(Collectors.toList());

		return new Exchanges.SearchResult(filtered);
	}

	/**
	 * Check if an exchange matches the current enchantment filter settings.
	 */
	private boolean matchesEnchantFilter(Exchange exchange, List<EnchantFilter> requiredEnchants) {
		// For enchantment filtering, we care about what we're receiving (the output)
		// since users are searching for items to buy, not what they need to give
		if (exchange.output != null) {
			return matchesRuleEnchantFilter(exchange.output, requiredEnchants);
		}
		
		// If there's no output, fall back to checking the input
		return matchesRuleEnchantFilter(exchange.input, requiredEnchants);
	}

	/**
	 * Check if a rule matches the enchantment filter.
	 */
	private boolean matchesRuleEnchantFilter(Rule rule, List<EnchantFilter> requiredEnchants) {
		if (rule == null) return true;

		// Get all enchants from rule (both required and stored)
		Map<String, Integer> allEnchants = new HashMap<>();
		if (rule.requiredEnchants != null) allEnchants.putAll(rule.requiredEnchants);
		if (rule.storedEnchants != null) allEnchants.putAll(rule.storedEnchants);

		boolean hasAnyEnchants = !allEnchants.isEmpty();

		// Handle SPECIFIC mode
		if (enchantFilterMode == EnchantFilterMode.SPECIFIC) {
			// Restrictive mode with empty query = unenchanted only
			if (restrictiveEnchants && requiredEnchants.isEmpty()) {
				return !hasAnyEnchants;
			}

			if (requiredEnchants.isEmpty()) return true;

			// Check if all required enchants are present
			for (EnchantFilter filter : requiredEnchants) {
				boolean found = false;
				for (Map.Entry<String, Integer> entry : allEnchants.entrySet()) {
					String enchantName = entry.getKey().toLowerCase();
					int level = entry.getValue();

					// Fuzzy match: check if enchant name contains the search term
					if (enchantName.contains(filter.name()) || filter.name().contains(enchantName)) {
						// Check level constraint if specified in the filter
						if (filter.level() != null && level != filter.level()) continue;
						found = true;
						break;
					}
				}
				if (!found) return false;
			}

			// In restrictive mode, check that NO extra enchants exist beyond what's required
			if (restrictiveEnchants) {
				for (Map.Entry<String, Integer> entry : allEnchants.entrySet()) {
					String enchantName = entry.getKey().toLowerCase();
					int level = entry.getValue();
					
					boolean isAllowed = false;
					for (EnchantFilter filter : requiredEnchants) {
						if (enchantName.contains(filter.name()) || filter.name().contains(enchantName)) {
							// If level specified, must match exactly
							if (filter.level() == null || level == filter.level()) {
								isAllowed = true;
								break;
							}
						}
					}
					if (!isAllowed) return false; // Extra enchant not in filter
				}
			}

			return true;
		}

		return true; // ANY mode
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
				.add(new Spacer(spacer))
				.add(new Label("Strict: ").align(ALIGN_RIGHT).setHeight(20))
				.add(new Button(strictSearch ? "Yes" : "No").onBtnClick(b -> {
					if ("Yes".equals(b.getText().getString())) {
						b.setText(Component.literal("No"));
						strictSearch = false;
					} else {
						b.setText(Component.literal("Yes"));
						strictSearch = true;
					}
					// Re-apply filtering to current results if available
					if (searchResult != null && this.searchResult != null) {
						var filteredResult = applyStrictSearchFilter(this.searchResult);
						displaySearchResults(filteredResult);
					} else {
						performSearch();
					}
				}))
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
				.add(new Spacer());

		// Enchantment filter controls
		final TextField enchantTextField = new TextField(s -> {
			enchantQuery = s;
			searchButton.setEnabled(true);
			return true;
		}, enchantQuery, "ST E U3 / efficiency 5, unbreaking, ...");

		final var enchantFilterControls = new FlexListLayout(HORIZONTAL)
				.add(new Spacer())
				.add(new Label("Enchants: ").align(ALIGN_RIGHT).setHeight(20))
				.add(buildEnchantModeButton(EnchantFilterMode.ANY))
				.add(buildEnchantModeButton(EnchantFilterMode.SPECIFIC))
				.add(new Spacer(spacer))
				.add(enchantTextField.setWeight(new Vec2(999, 0)).setMaxSize(new Vec2(200, 20)))
				.add(new Spacer(spacer))
				.add(new Label("Only these: ").align(ALIGN_RIGHT).setHeight(20))
				.add(new Button(restrictiveEnchants ? "Yes" : "No").onBtnClick(b -> {
					if ("Yes".equals(b.getText().getString())) {
						b.setText(Component.literal("No"));
						restrictiveEnchants = false;
					} else {
						b.setText(Component.literal("Yes"));
						restrictiveEnchants = true;
					}
					performSearch();
				}).setEnabled(enchantFilterMode == EnchantFilterMode.SPECIFIC))
				.add(new Spacer());

		// Show/hide enchant text field based on mode
		enchantTextField.setEnabled(enchantFilterMode == EnchantFilterMode.SPECIFIC);

		final Label statusLabel;
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
			String numResults = String.valueOf(searchResult.exchanges.size());
			if (searchResult.exchanges.size() > 99) numResults = "99+";
			statusLabel = new Label("Found " + numResults + " exchanges:").align(ALIGN_CENTER);
			final String world = mod.getCurrentWorldName();
			final Pos playerPos = mod.getPlayerPos();
			final Vec2 resultsListSpacer = new Vec2(5, 5);
			for (Exchange exchange : searchResult.exchanges) {
				resultsTable.addRow(Collections.singletonList(new Spacer(resultsListSpacer)));

				FlexListLayout metaCol = buildMetaCol(world, playerPos, exchange);
				FlexListLayout inputCol = buildRuleCol("Input:", exchange.input);
				FlexListLayout outputCol = buildRuleCol("Output:", exchange.output);

				resultsTable.addRow(Arrays.asList(null, metaCol, inputCol, outputCol));
			}
		}
		resultsTable.addRow(Arrays.asList(new Spacer(spacer), new Spacer(), new Spacer(), new Spacer()));

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
			mod.searchSource.setSearchResult(searchResult);
			for (var exchange : searchResult.exchanges) {
				mod.searchSource.unsuppress(exchange.pos);
			}
		}));

		bottomControls.add(new Spacer());

		// Toggle button for highlighting all nearby exchanges
		String nearbyBtnText = mod.nearbySource.hasActiveHighlights() ? "Clear Nearby" : "Highlight Nearby";
		bottomControls.add(new Button(nearbyBtnText).onClick((btn) -> {
			if (mod.nearbySource.hasActiveHighlights()) {
				// Toggle off - clear the nearby highlights
				mod.nearbySource.reset();
				rebuild();
			} else {
				// Toggle on - search for nearby exchanges and highlight them
				var query = new SearchQuery(
						"", "", // no input/output filter - get all exchanges
						mod.getPlayerPos(),
						System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000, // last 30 days
						true, // allow unstocked
						100, // limit
						"closest" // sort by closest
				);
				Exchanges.search(query)
						.thenAccept(result -> {
							if (result != null && !result.exchanges.isEmpty()) {
								mod.nearbySource.setSearchResult(result);
								for (var exchange : result.exchanges) {
									mod.nearbySource.unsuppress(exchange.pos);
								}
							}
							mc.doRunTask(this::rebuild);
						})
						.exceptionally(e -> {
							e.printStackTrace();
							return null;
						});
			}
		}));

		bottomControls.add(new Spacer());

		bottomControls.add(new Button("Clear Highlights").onClick((btn) -> {
			mod.highlightManager.clearAll();
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
		content.add(enchantFilterControls);
		content.add(new Spacer(spacer));
		content.add(statusLabel);
		content.add(new Spacer(spacer));
		content.add(scroller);
		content.add(new Spacer(spacer));
		content.add(bottomControls);

		return content;
	}

	@NotNull
	private GuiElement buildEnchantModeButton(@NotNull EnchantFilterMode mode) {
		return new Button(mode.display)
				.setEnabled(!mode.equals(enchantFilterMode))
				.onClick((btn) -> {
					enchantFilterMode = mode;
					if (mode != EnchantFilterMode.SPECIFIC) {
						enchantQuery = ""; // Clear enchant query when not in specific mode
						restrictiveEnchants = false; // Reset restrictive toggle
					}
					performSearch();
				});
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
