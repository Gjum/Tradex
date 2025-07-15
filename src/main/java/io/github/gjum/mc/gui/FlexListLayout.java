package io.github.gjum.mc.gui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import net.minecraft.client.gui.GuiGraphics;

import static io.github.gjum.mc.gui.Layoutable1D.computeLayout;

public class FlexListLayout extends GuiElement {
	private final Vec2.Direction direction;
	private final ArrayList<GuiElement> children = new ArrayList<>();
	@Nullable
	private Layoutable1D[] childLayouts;

	public FlexListLayout(Vec2.Direction direction) {
		this.direction = direction;
	}

	public FlexListLayout add(GuiElement child) {
		children.add(child);
		child.setParent(this);
		invalidateLayout();
		return this;
	}

	public FlexListLayout insert(int index, GuiElement child) {
		children.add(index, child);
		child.setParent(this);
		invalidateLayout();
		return this;
	}

	public FlexListLayout clear() {
		children.forEach(GuiElement::handleDestroyed);
		children.clear();
		invalidateLayout();
		return this;
	}

	@Override
	public void invalidateLayout() {
		super.invalidateLayout();
		childLayouts = null;
	}

	@Override
	public void updateSize(Vec2 sizeAvail) {
		childLayouts = new Layoutable1D[children.size()];
		for (int i = 0; i < childLayouts.length; i++) {
			GuiElement child = children.get(i);
			Vec2 minSize = child.getMinSize();
			Vec2 maxSize = child.getMaxSize();
			Vec2 weight = child.getWeight();

			childLayouts[i] = new Layoutable1D(
					minSize.getDim(direction),
					maxSize.getDim(direction),
					weight.getDim(direction));
		}

		int mainSize = computeLayout(sizeAvail.getDim(direction), childLayouts);

		int otherAvail = sizeAvail.getDim(direction.other());
		int otherSize = 0;
		int childPos = 0;
		for (int i = 0; i < childLayouts.length; i++) {
			childLayouts[i].pos = childPos;
			childPos += childLayouts[i].size;

			GuiElement child = children.get(i);
			child.updateSize(Vec2.setDims(
					childLayouts[i].size, otherAvail, direction));

			int childOtherSize = child.getSize().getDim(direction.other());
			otherSize = Math.max(otherSize, childOtherSize);
		}

		super.updateSize(Vec2.setDims(mainSize, otherSize, direction));
	}

	@Override
	public void setPos(@NotNull Vec2 pos) {
		if (childLayouts == null || childLayouts.length != children.size()) {
			throw new IllegalStateException("setPos() was called before setSize()");
		}
		super.setPos(pos);
		final int other = pos.getDim(direction.other());
		for (int i = 0; i < children.size(); i++) {
			GuiElement child = children.get(i);
			int main = childLayouts[i].pos + pos.getDim(direction);
			child.setPos(Vec2.setDims(main, other, direction));
		}
	}

	@NotNull
	@Override
	public Vec2 getWeight() {
		int sumMain = 0;
		int maxOther = 0;
		for (GuiElement child : children) {
			final Vec2 weight = child.getWeight();
			sumMain += weight.getDim(direction);
			maxOther = Math.max(maxOther, weight.getDim(direction.other()));
		}
		return Vec2.setDims(sumMain, maxOther, direction);
	}

	@Override
	public Vec2 getMaxSize() {
		int sumMain = 0;
		int maxOther = 0;
		for (GuiElement child : children) {
			final Vec2 maxSize = child.getMaxSize();
			sumMain += maxSize.getDim(direction);
			maxOther = Math.max(maxOther, maxSize.getDim(direction.other()));
		}
		return Vec2.setDims(sumMain, maxOther, direction);
	}

	@Override
	public Vec2 getMinSize() {
		int sumMain = 0;
		int maxOther = 0;
		for (GuiElement child : children) {
			final Vec2 minSize = child.getMinSize();
			sumMain += minSize.getDim(direction);
			maxOther = Math.max(maxOther, minSize.getDim(direction.other()));
		}
		return Vec2.setDims(sumMain, maxOther, direction);
	}

	@Override
	public void draw(GuiGraphics context, Vec2 mouse, Vec2 winSize, float partialTicks) {
		for (GuiElement child : children) {
			try {
				child.draw(context, mouse, winSize, partialTicks);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean drawOverlays(GuiGraphics context, Vec2 mouse, Vec2 winSize, float partialTicks) {
		for (GuiElement child : children) {
			try {
				if (child.drawOverlays(context, mouse, winSize, partialTicks)) {
					return true;
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	public boolean handleMouseClicked(Vec2 mouse, int mouseButton) {
		var clicked = false;
		for (GuiElement child : children) {
			try {
				// must call all children so text fields de-focus
				clicked = child.handleMouseClicked(mouse, mouseButton) || clicked;
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return clicked;
	}

	@Override
	public void handleMouseDragged(Vec2 mouse, Vec2 prevMouse, Vec2 dragStart, int mouseButton) {
		for (GuiElement child : children) {
			try {
				child.handleMouseDragged(mouse, prevMouse, dragStart, mouseButton);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void handleMouseReleased(Vec2 mouse, Vec2 dragStart, int state) {
		for (GuiElement child : children) {
			try {
				child.handleMouseReleased(mouse, dragStart, state);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean handleMouseScrolled(Vec2 mouse, double scrollAmount) {
		for (GuiElement child : children) {
			try {
				if (child.handleMouseScrolled(mouse, scrollAmount)) {
					return true;
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	public void handleKeyPressed(int keyCode, int scanCode, int mods) {
		for (GuiElement child : children) {
			try {
				child.handleKeyPressed(keyCode, scanCode, mods);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void handleCharTyped(char keyChar, int keyCode) {
		for (GuiElement child : children) {
			try {
				child.handleCharTyped(keyChar, keyCode);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void handleDestroyed() {
		for (GuiElement child : children) {
			try {
				child.handleDestroyed();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
}
