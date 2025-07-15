package io.github.gjum.mc.tradex.model;

import com.google.gson.annotations.Expose;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Exchange {
	@Expose
	public long time;
	/**
	 * For double chests, the north/west-most block.
	 */
	@Expose
	public @NotNull Pos pos;
	/**
	 * For double chests, location of the adjacent chest block:
	 * 0=none, 1=east, 2=south. See `getAdjacentPos()`.
	 * This way we can delete the exchanges at that location in favor of this one.
	 */
	@Expose
	public byte adjacent;
	/**
	 * For when multiple exchanges are available in one container (e.g., chest). 1-based.
	 */
	@Expose
	public int index;
	/**
	 * How many exchanges are available in this container .
	 */
	@Expose
	public int multi;
	@Expose
	public int stock;
	@Expose
	public @NotNull Rule input;
	@Expose
	public @Nullable Rule output;
	public @Nullable String message;

	public void fixNulls() {
		if (input != null) input.fixNulls();
		if (output != null) output.fixNulls();
		if (index < 1) index = 1;
	}

	/**
	 * @return output items per input item, counting compacted items as 64
	 */
	public double getRateOutPerIn() {
		if (output == null) return 0;
		return (double) output.countDecompacted()
				/ input.countDecompacted();
	}

	private BlockPos getAdjacentPos() {
		if (adjacent == 1) return pos.block().east();
		if (adjacent == 2) return pos.block().south();
		return pos.block();
	}

	public AABB aabb() {
		var aabb = new AABB(pos.block());
		if (adjacent != 0) aabb = aabb.minmax(new AABB(getAdjacentPos()));
		return aabb;
	}
}
