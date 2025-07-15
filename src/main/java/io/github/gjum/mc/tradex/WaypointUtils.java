package io.github.gjum.mc.tradex;


import io.github.gjum.mc.tradex.model.Pos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static io.github.gjum.mc.tradex.TradexMod.LOG;

public class WaypointUtils {
	private static String mapModName;
	private static String waypointCommandFormat;

	static {
		init();
	}

	private static void init() {
		try {
			// XXX new map mod classes
			Class.forName("com.mamiyaotaru.voxelmap.VoxelMap");
			mapModName = "VoxelMap";
			waypointCommandFormat = "/newWaypoint name:%s, x:%s, y:%s, z:%s";
			LOG.info("Found VoxelMap");
			return;
		} catch (ClassNotFoundException var1) {
		}
		try {
			// XXX new map mod classes
			Class.forName("journeymap.common.Journeymap");
			mapModName = "JourneyMap";
			waypointCommandFormat = "/jm wpedit [name:%s, x:%s, y:%s, z:%s]";
			LOG.info("Found JourneyMap");
			return;
		} catch (ClassNotFoundException var1) {
		}
		LOG.info("Neither VoxelMap nor JourneyMap are installed");
	}

	public static @Nullable String getMapModName() {
		return mapModName;
	}

	public static @Nullable String getWaypointCommand(@NotNull Pos pos, @NotNull String name) {
		if (waypointCommandFormat == null) return null;
		return String.format(waypointCommandFormat, name, pos.x, pos.y, pos.z);
	}
}
