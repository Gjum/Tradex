package io.github.gjum.mc.gui;

import java.util.*;
import java.util.stream.Collectors;

class Layoutable1D {
	int minSize, maxSize, weight;
	int size = 0;
	int pos = 0;

	Layoutable1D(int minSize, int maxSize, int weight) {
		this.minSize = minSize;
		this.maxSize = maxSize;
		this.weight = weight;
	}

	void update(int minSizeNew, int maxSizeNew, int weightNew) {
		minSize = Math.max(minSize, minSizeNew);
		maxSize = Math.max(maxSize, maxSizeNew);
		weight = Math.max(weight, weightNew);
	}

	int getWeight() {
		return weight;
	}

	/**
	 * @param available Pixels available for distribution among cells.
	 * @param cells     Layout constraints; the output sizes will be written into these.
	 * @return Used pixels (distributed to cells). May be less than `available` when the sum of all children maxWidth's is too small.
	 */
	public static int computeLayout(int available, Layoutable1D[] cells) {
		final ArrayList<Layoutable1D> flex = new ArrayList<>();
		int distributable = available;
		int totalWeights = 0;
		for (Layoutable1D cell : cells) {
			cell.size = cell.minSize;
			distributable -= cell.minSize;
			final int weight = cell.weight;
			if (weight > 0 && cell.size < cell.maxSize) {
				// flexible, assign later
				totalWeights += weight;
				flex.add(cell);
			}
		}

		while (distributable > 0) {
			final ArrayList<Layoutable1D> currentFlex = new ArrayList<>(flex);
			flex.clear();

			final int currentWeights = totalWeights;
			totalWeights = 0;

			int currentDistributable = distributable;

			for (Layoutable1D cell : currentFlex) {
				final int oldSize = cell.size;
				// since we use integer rounding here, the last few pixels need manual distribution, see below
				int newSize = oldSize + distributable * cell.weight / currentWeights;
				if (newSize < cell.maxSize) {
					// still flexible, assign later
					totalWeights += cell.weight;
					flex.add(cell);
				} else {
					newSize = cell.maxSize;
				}
				cell.size = newSize;
				currentDistributable -= newSize - oldSize;
			}

			// ensure loop termination: distributable must be strictly decreasing
			if (distributable <= currentDistributable) {
				break;
			}

			distributable = currentDistributable;
		}

		// Distribute the last few pixels, this is < flex.size() and only occurs
		// because we use integer rounding while distributing.
		final List<Layoutable1D> cellsByWeight = flex.stream()
				.sorted(Comparator.comparing(Layoutable1D::getWeight)
						.reversed()) // distribute to most-weight cells first
				.collect(Collectors.toList());
		for (Layoutable1D cell : cellsByWeight) {
			if (distributable > 0) {
				cell.size += 1;
				distributable -= 1;
			}
		}

		// practically the same as returning available,
		// unless there are no flex elements for a direction
		return available - distributable;
	}
}
