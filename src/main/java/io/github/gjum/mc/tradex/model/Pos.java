package io.github.gjum.mc.tradex.model;

import com.google.gson.annotations.Expose;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Pos {
	@Expose
	public final @NotNull String server;
	@Expose
	public final @NotNull String world;
	@Expose
	public final int x;
	@Expose
	public final int y;
	@Expose
	public final int z;

	public Pos(@NotNull String server, @NotNull String world, int x, int y, int z) {
		this.server = server;
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public boolean equals(Object other) {
		if (this == other) {
			return true;
		} else if (!(other instanceof Pos otherPos)) {
			return false;
		} else if (this.x != otherPos.x) {
			return false;
		} else if (this.y != otherPos.y) {
			return false;
		} else if (!this.world.equals(otherPos.world)) {
			return false;
		} else if (!this.server.equals(otherPos.server)) {
			return false;
		} else {
			return this.z == otherPos.z;
		}
	}

	public int hashCode() {
		return Objects.hash(server, world, x, y, z);
	}

	public String toString() {
		return "[" + world + " " + x + " " + y + " " + z + "]";
	}

	public Pos minus(Pos other) {
		return new Pos(server, world, x - other.x, y - other.y, z - other.z);
	}

	public double distance(double xIn, double yIn, double zIn) {
		double d0 = (double) this.x - xIn;
		double d1 = (double) this.y - yIn;
		double d2 = (double) this.z - zIn;
		return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
	}

	public double distance(Pos to) {
		return this.distance(to.x, to.y, to.z);
	}

	public BlockPos block() {
		return new BlockPos(x, y, z);
	}
}
