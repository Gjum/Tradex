package io.github.gjum.mc.tradex.model;

import com.google.gson.annotations.Expose;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Only contains data that is shown in exchange info chat messages.
 */
public class Rule {
	@Expose
	public int count;
	/**
	 * May encode durability as colon-separated number ("Wool:2")
	 * or as custom display string ("Magenta Wool").
	 * May be book title.
	 * May be potion name: https://github.com/CivMC/Civ/blob/main/plugins/itemexchange-paper/src/main/java/com/untamedears/itemexchange/rules/modifiers/PotionModifier.java#L131
	 */
	@Expose
	public @NotNull String material;
	@Expose
	public @Nullable String customName;
	@Expose
	public @Nullable String potionName;
	@Expose
	public @Nullable String bookAuthor;
	@Expose
	public @Nullable String bookGeneration;
	/**
	 * enchanted books
	 */
	@Expose
	public @NotNull Map<String, Integer> storedEnchants = new HashMap<>();
	@Expose
	public @NotNull Map<String, Integer> requiredEnchants = new HashMap<>();
	@Expose
	public @NotNull Set<String> excludedEnchants = new HashSet<>();
	@Expose
	public boolean unlistedEnchantsAllowed;
	/**
	 * Undamaged, Damaged, Any
	 */
	@Expose
	public @Nullable String condition;
	/**
	 * 0 means never repaired, negative means "or less"
	 */
	@Expose
	public int repairLevel;
	@Expose
	public @NotNull List<String> lore = new ArrayList<>();
	public @Nullable String message;

	public Rule(int count, @NotNull String material) {
		this.count = count;
		this.material = material;
	}

	public void fixNulls() {
		if (storedEnchants == null) storedEnchants = new HashMap<>();
		if (requiredEnchants == null) requiredEnchants = new HashMap<>();
		if (excludedEnchants == null) excludedEnchants = new HashSet<>();
		if (lore == null) lore = new ArrayList<>();
	}

	public boolean isCompacted() {
		// Heuristic detection for "compacted" items via lore.
		if (lore != null && !lore.isEmpty()) {
			String loreText = String.join(" ", lore);
			if (loreText.matches("(?i).*compacted item.*")) return true;
			if (loreText.matches("(?i).*\\bcompacted\\b.*")) return true;
		}
		return false;
	}

	public int countDecompacted() {
		return count * compactMultiplier();
	}

	public int compactMultiplier() {
		if (!isCompacted()) return 1;
		// var key = ResourceKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace(material));
		// var item = mc.level.registryAccess().get(key).map(Holder.Reference::value).orElse(Items.STONE);
		// return switch (item.getDefaultMaxStackSize()) { case 1 -> 8; case 16 -> 16; default -> 64; };
		if (bookAuthor != null) return 8;
		if (potionName != null) return 8;
		if (material.equals("Ender Pearl")) return 8;
		if (material.equals("Bow")) return 8;
		if (material.endsWith(" Pickaxe")) return 8;
		if (material.endsWith(" Axe")) return 8;
		if (material.endsWith(" Shovel")) return 8;
		if (material.endsWith(" Helmet")) return 8;
		if (material.endsWith(" Chestplate")) return 8;
		if (material.endsWith(" Leggings")) return 8;
		if (material.endsWith(" Boots")) return 8;
		if (material.endsWith("Egg")) return 16;
		return 64;
	}
}
