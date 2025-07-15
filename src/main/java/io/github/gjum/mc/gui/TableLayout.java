package io.github.gjum.mc.gui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

import static io.github.gjum.mc.gui.Layoutable1D.computeLayout;

public class TableLayout extends GuiElement {
	private final List<List<@Nullable GuiElement>> rows = new ArrayList<>();
	private int maxCols = 0;
	@Nullable
	private Layoutable1D[] rowLayouts;
	@Nullable
	private Layoutable1D[] colLayouts;

	// TODO spacing between cells (horizontal and vertical)
	// TODO cell alignment per row/col (left/center/right, top/center/bottom)

	/**
	 * @param row List of cells. May contain nulls for empty cells. May be shorter than other rows; the difference will be treated as nulls.
	 * @return this
	 */
	public TableLayout addRow(List<@Nullable GuiElement> row) {
		rows.add(row);
		for (GuiElement child : row) {
			if (child == null) continue;
			child.setParent(this);
		}
		if (maxCols < row.size()) maxCols = row.size();
		invalidateLayout();
		return this;
	}

	/**
	 * @param row List of cells. May contain nulls for empty cells. May be shorter than other rows; the difference will be treated as nulls.
	 * @return this
	 */
	public TableLayout insertRow(int index, List<@Nullable GuiElement> row) {
		rows.add(index, row);
		for (GuiElement child : row) {
			if (child == null) continue;
			child.setParent(this);
		}
		if (maxCols < row.size()) maxCols = row.size();
		invalidateLayout();
		return this;
	}

	/**
	 * @param row Must already have been added using addRow(row).
	 * @return this
	 */
	public TableLayout updateRow(List<@Nullable GuiElement> row) {
		for (@Nullable GuiElement child : row) {
			if (child == null) continue;
			child.setParent(this);
		}
		if (maxCols < row.size()) maxCols = row.size();
		invalidateLayout();
		return this;
	}

	public TableLayout clear() {
		rows.forEach(row -> row.forEach(guiElement -> {
			if (guiElement != null) guiElement.handleDestroyed();
		}));
		rows.clear();
		maxCols = 0;
		invalidateLayout();
		return this;
	}

	@Override
	public void invalidateLayout() {
		super.invalidateLayout();
		rowLayouts = null;
		colLayouts = null;
	}

	@Override
	public void updateSize(Vec2 sizeAvail) {
		rowLayouts = new Layoutable1D[rows.size()];
		for (int i = 0; i < rowLayouts.length; i++) {
			rowLayouts[i] = new Layoutable1D(0, 0, 0);
		}
		colLayouts = new Layoutable1D[maxCols];
		for (int i = 0; i < colLayouts.length; i++) {
			colLayouts[i] = new Layoutable1D(0, 0, 0);
		}
		for (int rowNr = 0; rowNr < rows.size(); rowNr++) {
			List<GuiElement> row = rows.get(rowNr);
			for (int colNr = 0; colNr < row.size(); colNr++) {
				GuiElement cell = row.get(colNr);
				if (cell == null) continue;
				Vec2 minSize = cell.getMinSize();
				Vec2 maxSize = cell.getMaxSize();
				Vec2 weight = cell.getWeight();
				rowLayouts[rowNr].update(minSize.y, maxSize.y, weight.y);
				colLayouts[colNr].update(minSize.x, maxSize.x, weight.x);
			}
		}

		super.updateSize(new Vec2(
				computeLayout(sizeAvail.x, colLayouts),
				computeLayout(sizeAvail.y, rowLayouts)));

		int rowPos = 0;
		for (Layoutable1D rowLayout : rowLayouts) {
			rowLayout.pos = rowPos;
			rowPos += rowLayout.size;
		}
		int colPos = 0;
		for (Layoutable1D colLayout : colLayouts) {
			colLayout.pos = colPos;
			colPos += colLayout.size;
		}

		for (int rowNr = 0; rowNr < rows.size(); rowNr++) {
			List<GuiElement> row = rows.get(rowNr);
			for (int colNr = 0; colNr < row.size(); colNr++) {
				GuiElement cell = row.get(colNr);
				if (cell == null) continue;
				final Vec2 weight = cell.getWeight();
				final Vec2 minSize = cell.getMinSize();
				cell.updateSize(new Vec2(
						weight.x > 0 ? colLayouts[colNr].size : minSize.x,
						weight.y > 0 ? rowLayouts[rowNr].size : minSize.y));
			}
		}
	}

	@Override
	public void setPos(@NotNull Vec2 pos) {
		if (rowLayouts == null || rowLayouts.length != rows.size()
				|| colLayouts == null || colLayouts.length != maxCols) {
			throw new IllegalStateException("setPos() was called before setSize()");
		}
		super.setPos(pos);
		for (int rowNr = 0; rowNr < rows.size(); rowNr++) {
			List<GuiElement> row = rows.get(rowNr);
			int y = rowLayouts[rowNr].pos + pos.y;
			for (int colNr = 0; colNr < row.size(); colNr++) {
				GuiElement child = row.get(colNr);
				if (child == null) continue;
				int x = colLayouts[colNr].pos + pos.x;
				child.setPos(new Vec2(x, y));
			}
		}
	}

	@Override
	public Vec2 getSize() {
		if (rowLayouts != null && rowLayouts.length == rows.size() && rowLayouts.length > 0
				&& colLayouts != null && colLayouts.length == maxCols && colLayouts.length > 0) {
			final Layoutable1D lastCol = colLayouts[colLayouts.length - 1];
			final int colSum = lastCol.pos - colLayouts[0].pos + lastCol.size;
			final Layoutable1D lastRow = rowLayouts[rowLayouts.length - 1];
			final int rowSum = lastRow.pos - rowLayouts[0].pos + lastRow.size;
			return new Vec2(colSum, rowSum);
		}
		int maxSumW = 0;
		int sumMaxH = 0;
		for (List<GuiElement> row : rows) {
			int sumW = 0;
			int maxH = 0;
			for (GuiElement child : row) {
				if (child == null) continue;
				final Vec2 size = child.getSize();
				sumW += size.x;
				maxH = Math.max(maxH, size.y);
			}
			maxSumW = Math.max(maxSumW, sumW);
			sumMaxH += maxH;
		}
		return new Vec2(maxSumW, sumMaxH);
	}

	@NotNull
	@Override
	public Vec2 getWeight() {
		int maxX = 0;
		int sumY = 0;
		for (List<GuiElement> row : rows) {
			int sumX = 0;
			int maxY = 0;
			for (GuiElement child : row) {
				if (child == null) continue;
				final Vec2 weight = child.getWeight();
				sumX += weight.x;
				maxY = Math.max(maxY, weight.y);
			}
			maxX = Math.max(maxX, sumX);
			sumY += maxY;
		}
		return new Vec2(maxX, sumY);
	}

	@Override
	public Vec2 getMaxSize() {
		int maxW = 0;
		int sumH = 0;
		for (List<GuiElement> row : rows) {
			int sumW = 0;
			int maxH = 0;
			for (GuiElement child : row) {
				if (child == null) continue;
				final Vec2 size = child.getMaxSize();
				sumW += size.x;
				maxH = Math.max(maxH, size.y);
			}
			maxW = Math.max(maxW, sumW);
			sumH += maxH;
		}
		return new Vec2(maxW, sumH);
	}

	@Override
	public Vec2 getMinSize() {
		int maxW = 0;
		int sumH = 0;
		for (List<GuiElement> row : rows) {
			int sumW = 0;
			int maxH = 0;
			for (GuiElement child : row) {
				if (child == null) continue;
				final Vec2 size = child.getMinSize();
				sumW += size.x;
				maxH = Math.max(maxH, size.y);
			}
			maxW = Math.max(maxW, sumW);
			sumH += maxH;
		}
		return new Vec2(maxW, sumH);
	}

	@Override
	public void draw(GuiGraphics context, Vec2 mouse, Vec2 winSize, float partialTicks) {
		for (List<GuiElement> row : rows) {
			for (GuiElement child : row) {
				if (child == null) continue;
				try {
					child.draw(context, mouse, winSize, partialTicks);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public boolean drawOverlays(GuiGraphics context, Vec2 mouse, Vec2 winSize, float partialTicks) {
		for (List<GuiElement> row : rows) {
			for (GuiElement child : row) {
				if (child == null) continue;
				try {
					if (child.drawOverlays(context, mouse, winSize, partialTicks)) {
						return true;
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	@Override
	public boolean handleMouseClicked(Vec2 mouse, int mouseButton) {
		for (List<GuiElement> row : rows) {
			for (GuiElement child : row) {
				if (child == null) continue;
				try {
					if (child.handleMouseClicked(mouse, mouseButton)) {
						return true;
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	@Override
	public void handleMouseDragged(Vec2 mouse, Vec2 prevMouse, Vec2 dragStart, int mouseButton) {
		for (List<GuiElement> row : rows) {
			for (GuiElement child : row) {
				if (child == null) continue;
				try {
					child.handleMouseDragged(mouse, prevMouse, dragStart, mouseButton);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void handleMouseReleased(Vec2 mouse, Vec2 dragStart, int state) {
		for (List<GuiElement> row : rows) {
			for (GuiElement child : row) {
				if (child == null) continue;
				try {
					child.handleMouseReleased(mouse, dragStart, state);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public boolean handleMouseScrolled(Vec2 mouse, double scrollAmount) {
		for (List<GuiElement> row : rows) {
			for (GuiElement child : row) {
				if (child == null) continue;
				try {
					if (child.handleMouseScrolled(mouse, scrollAmount)) {
						return true;
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	@Override
	public void handleKeyPressed(int keyCode, int scanCode, int mods) {
		for (List<GuiElement> row : rows) {
			for (GuiElement child : row) {
				if (child == null) continue;
				try {
					child.handleKeyPressed(keyCode, scanCode, mods);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void handleCharTyped(char keyChar, int keyCode) {
		for (List<GuiElement> row : rows) {
			for (GuiElement child : row) {
				if (child == null) continue;
				try {
					child.handleCharTyped(keyChar, keyCode);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void handleDestroyed() {
		for (List<GuiElement> row : rows) {
			for (GuiElement child : row) {
				if (child == null) continue;
				try {
					child.handleDestroyed();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}
}
