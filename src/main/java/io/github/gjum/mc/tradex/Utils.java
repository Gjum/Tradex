package io.github.gjum.mc.tradex;

import io.github.gjum.mc.tradex.model.Pos;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.function.Function;

public class Utils {
	public static final long weekMs = 7L * 24 * 60 * 60 * 1000;
	public static final long monthMs = 31L * 24 * 60 * 60 * 1000;

	public static final Minecraft mc = Minecraft.getInstance();

	@NotNull
	public static <T> T nonNullOr(@Nullable T input, @NotNull T defaultVal) {
		if (input == null) return defaultVal;
		return input;
	}

	@Nullable
	public static <T, U> U mapNonNull(@Nullable T input, @NotNull Function<T, U> transform) {
		if (input == null) return null;
		return transform.apply(input);
	}

	@Nullable
	public static Integer intOrNull(@Nullable String s) {
		if (s == null) return null;
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@NotNull
	public static String worldNameFromDimension(int dimension) {
		if (0 == dimension) {
			return "world";
		} else if (1 == dimension) {
			return "the_end";
		} else if (-1 == dimension) {
			return "nether";
		} else {
			return "dim" + dimension;
		}
	}

	private static final NumberFormat fmt1d = new DecimalFormat("#0.0");

	@NotNull
	public static String distanceToText(double delta) {
		if (delta >= 10000) return (int) (delta / 1000) + "km";
		if (delta > 1000) return fmt1d.format(delta / 1000) + "km";
		return (int) delta + "m";
	}

	@NotNull
	public static String durationToText(long delta) {
		if (delta < 0) delta = -delta;
		if (delta < 1000) return delta + "ms";
		delta /= 1000; // to seconds
		if (delta < 60) return delta + "s";
		if (delta < 3600) return (delta / 60) + "min";
		delta /= 3600; // to hours
		if (delta < 24) return delta + "h";
		delta /= 24; // to days
		String s;
		if (delta < 7) s = delta + " day";
		else if (delta < 31) s = (delta / 7) + " week";
		else s = (delta / 31) + " month";
		if (!s.startsWith("1 ")) s += "s"; // pluralize
		return s;
	}

	public static final String[] compassNames = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

	@NotNull
	public static String directionNameFromDelta(final Pos pos) {
		return directionNameFromDelta(pos.x, pos.y, pos.z);
	}

	@NotNull
	public static String directionNameFromDelta(final int dx, final int dy, final int dz) {
		final double length = Math.sqrt(dx * dx + dy * dy + dz * dz);

		// check if up or down rather than to the side
		final double horizLen = Math.sqrt(dx * dx + dz * dz);
		if (horizLen < Math.abs(dy)) return dy > 0 ? "up" : "down";

		// cross = Vec(dx, dy, dz).crossProduct(dx, 0, dz)
		final double crossX = dy * dz;
		// crossY = 0
		final double crossZ = -(dy * dx);
		final double crossLen = Math.sqrt(crossX * crossX + crossZ * crossZ);
		final double pitch = Math.asin(crossLen / length / horizLen);
		if (pitch > Math.PI / 4) return dy > 0 ? "up" : "down";

		// to the side: calculate bearing
		final double yawRadians = Math.atan2(-dz, dx);
		final double yawEights = yawRadians * 8 / (2 * Math.PI);
		final int alignedIndex = 2 - (int) Math.round(yawEights);
		return compassNames[(alignedIndex + 8) % 8];
	}

	public static void showChat(Component text) {
		mc.gui.getChat().addMessage(text);
	}

	public static final HashMap<String, String> enchantCodeByName = new HashMap<>();
	public static final HashMap<String, String> enchantNameByCode = new HashMap<>();

	static {
		for (String s : new String[]{
				"AA;Aqua Affinity",
				"BP;Blast Protection",
				"BoA;Bane of Arthropods",
				"CoB;Curse of Binding",
				"CoV;Curse of Vanishing",
				"DS;Depth Strider",
				"E;Efficiency",
				"F;Fortune",
				"FA;Fire Aspect",
				"FF;Feather Falling",
				"FP;Fire Protection",
				"FW;Frost Walker",
				"Fl;Flame",
				"I;Infinity",
				"K;Knockback",
				"L;Looting",
				"LS;Luck of the Sea",
				"Lu;Lure",
				"M;Mending",
				"P;Protection",
				"PP;Projectile Protection",
				"Po;Power",
				"Pu;Punch",
				"R;Respiration",
				"S;Sharpness",
				"SE;Sweeping Edge",
				"ST;Silk Touch",
				"Sm;Smite",
				"T;Thorns",
				"U;Unbreaking",
		}) {
			final String[] split = s.split(";");
			enchantNameByCode.put(split[0], split[1]);
			enchantCodeByName.put(split[1], split[0]);
		}
	}
}
