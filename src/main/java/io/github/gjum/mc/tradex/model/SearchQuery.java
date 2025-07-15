package io.github.gjum.mc.tradex.model;

import com.google.gson.annotations.Expose;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

public class SearchQuery {
	@Expose
	public @Nullable String input;
	@Expose
	public @Nullable String output;
	@Expose
	public @Nullable Pos pos;
	@Expose
	public @Nullable String sortMode;
	@Expose
	public long updatedAfter = 0;
	@Expose
	public boolean allowUnstocked = false;
	@Expose
	public int limit = 100;

	public SearchQuery(
			@Nullable String input,
			@Nullable String output,
			@Nullable Pos pos,
			long updatedAfter,
			boolean allowUnstocked,
			int limit,
			@Nullable String sortMode
	) {
		this.input = input;
		this.output = output;
		this.pos = pos;
		this.sortMode = sortMode;
		this.updatedAfter = updatedAfter;
		this.allowUnstocked = allowUnstocked;
		this.limit = limit;
	}

	public static @NotNull String getSpecForRule(@Nullable Rule rule) {
		if (rule == null) return "";
		String s = "";
		if (rule.isCompacted()) s += " compacted";
		if (!rule.requiredEnchants.isEmpty()) {
			s += " enchanted " + rule.requiredEnchants.entrySet().stream()
					.map(e -> e.getKey() + e.getValue())
					.sorted()
					.collect(Collectors.joining(""));
		}
		if (rule.potionName != null) s += " " + rule.potionName;
		s += " " + normalizeMaterialNames(rule.material);
		if (rule.customName != null) s += " \"" + rule.customName + "\"";
		else if (rule.bookGeneration != null) s += " \"" + rule.bookGeneration + "\"";
		if (rule.bookAuthor != null) s += " by " + rule.bookAuthor;
		if (!rule.lore.isEmpty()) s += " " + String.join(" ", rule.lore);
		return s.trim().toLowerCase();
	}

	public static @NotNull String normalizeMaterialNames(@NotNull String material) {
		material = material.toLowerCase();
		material = material.replaceAll("s$", ""); // no plurals
		material = material.replaceAll("^blocks? of | block$", "");
		material = material.replaceAll("^bottles? +o.? +enchanting$", "exp");
		material = material.replaceAll("^e?xp(erience)?( bottle)?$", "exp");
		material = material.replaceAll("^emerald$", "exp");
		material = material.replaceAll("^reed$", "sugarcane");
		material = material.replaceAll("^obby$", "obsidian");
		material = material.replaceAll("^hay *(bale)?$", "wheat");
		return material;
	}
}
