package io.github.gjum.mc.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Scrolls the contained child {@link GuiElement}.
 */
public class ScrollBox extends GuiElement {
	private static final int scrollBarSize = 7;
	private static final int scrollBarAreaSize = scrollBarSize + 1;

	private @NotNull GuiElement child;
	private Vec2 scrollOffset = new Vec2(0, 0);

	private static Button dummyButton = new Button.Builder(Component.nullToEmpty(""), (b) -> {
	}).size(0, 0).build();

	public ScrollBox(@NotNull GuiElement child) {
		this.child = child;
		setMinSize(new Vec2(scrollBarAreaSize, 2 + scrollBarSize));
		setChild(child);
		setWeight(new Vec2(1, 1));
		updateSize(getMinSize());
	}

	public ScrollBox setChild(GuiElement child) {
		this.child = child;
		child.setParent(this);
		final Vec2 childMax = child.getMaxSize();
		setMaxSize(new Vec2(
				childMax.x + scrollBarAreaSize,
				childMax.y + 2));
		updateSize(getMinSize());
		return this;
	}

	@Override
	public void setPos(@NotNull Vec2 pos) {
		super.setPos(pos);
		child.setPos(new Vec2(
				pos.x + scrollOffset.x,
				pos.y + scrollOffset.y + 1));
	}

	@Override
	public void updateSize(Vec2 availableSize) {
		super.updateSize(availableSize);
		child.updateSize(new Vec2(getInnerWidth(), getInnerHeight()));
		// may not become larger than child
		super.updateSize(new Vec2(
				Math.min(getSize().x, child.getSize().x + scrollBarAreaSize),
				Math.min(getSize().y, child.getSize().y + 2)));
		clipScroll();
	}

	@Override
	public Vec2 getMinSize() {
		Vec2 minSize = super.getMinSize();
		int minX = Math.max(minSize.x, child.getMinSize().x + scrollBarAreaSize);
		return new Vec2(minX, minSize.y);
	}

	private int getInnerWidth() {
		return getSize().x - scrollBarAreaSize;
	}

	private int getInnerHeight() {
		return getSize().y - 2;
	}

	private boolean isMouseInsideChild(Vec2 mouse) {
		if (mouse == null) return false;
		if (getPos() == null) return false;
		if (mouse.x < getPos().x) return false;
		if (mouse.x >= getPos().x + getInnerWidth()) return false;
		if (mouse.y < getPos().y) return false;
		if (mouse.y >= getPos().y + getSize().y) return false;
		return true;
	}

	private boolean isMouseOnScrollBar(Vec2 mouse) {
		if (mouse == null) return false;
		if (getPos() == null) return false;
		if (mouse.x < getPos().x + getInnerWidth()) return false;
		if (mouse.x >= getPos().x + getSize().x) return false;
		if (mouse.y < getPos().y) return false;
		if (mouse.y >= getPos().y + getSize().y) return false;
		return true;
	}

	private Vec2 mouseChildTranslated(Vec2 mouse) {
		return isMouseInsideChild(mouse) ? mouse : new Vec2(Vec2.LARGE, Vec2.LARGE);
	}

	@Override
	public boolean handleMouseScrolled(Vec2 mouse, double rows) {
		final boolean mouseInsideChild = isMouseInsideChild(mouse);
		if (mouseInsideChild) {
			if (child.handleMouseScrolled(mouseChildTranslated(mouse), rows)) {
				return true;
			}
		}
		if (mouseInsideChild || isMouseOnScrollBar(mouse)) {
			double pixels = 20 * rows;
			scrollOffset = new Vec2(scrollOffset.x, scrollOffset.y + pixels);
			clipScroll();
			return true;
		}
		return false;
	}

	@Override
	public void handleMouseDragged(Vec2 mouse, Vec2 prevMouse, Vec2 dragStart, int mouseButton) {
		child.handleMouseDragged(mouseChildTranslated(mouse), mouseChildTranslated(prevMouse), mouseChildTranslated(dragStart), mouseButton);

		if (mouseButton != 0) return;
		if (mouse.y == prevMouse.y) return;

		final int delta;
		if (isMouseInsideChild(dragStart)) {
			delta = mouse.y - dragStart.y;
		} else if (isMouseOnScrollBar(dragStart)) {
			final int mouseDy = -(mouse.y - dragStart.y);
			final Vec2 childSize = child.getSize();
			if (childSize == null) return;
			if (childSize.y <= getInnerHeight()) return; // can't scroll, child too small
			final int scrollBarLength = Math.max(scrollBarSize, getInnerHeight() * getInnerHeight() / childSize.y);
			final int scrollBarTravel = getInnerHeight() - scrollBarLength;

			delta = mouseDy * Math.max(1, childSize.y - getInnerHeight()) / scrollBarTravel;
		} else return;

		scrollOffset = new Vec2(dragStartScrollOffset.x, dragStartScrollOffset.y + delta);
		clipScroll();
	}

	private Vec2 dragStartScrollOffset = new Vec2(0, 0);

	@Override
	public boolean handleMouseClicked(Vec2 mouse, int mouseButton) {
		// TODO scroll page up/down when mouse on bar but outside handle
		dragStartScrollOffset = scrollOffset;
		return child.handleMouseClicked(mouseChildTranslated(mouse), mouseButton);
	}

	@Override
	public void handleMouseReleased(Vec2 mouse, Vec2 dragStart, int state) {
		child.handleMouseReleased(mouseChildTranslated(mouse), dragStart, state);
	}

	@Override
	public void handleKeyPressed(int keyCode, int scanCode, int mods) {
		child.handleKeyPressed(keyCode, scanCode, mods);
	}

	@Override
	public void handleCharTyped(char keyChar, int keyCode) {
		child.handleCharTyped(keyChar, keyCode);
	}

	@Override
	public void handleDestroyed() {
		child.handleDestroyed();
	}

	/**
	 * Ensure scroll offsets are within bounds.
	 */
	private void clipScroll() {
		if (child.getSize() == null) return;
		final int tooFarDown = getInnerHeight() - child.getSize().y - scrollOffset.y;
		if (tooFarDown > 0) {
			scrollOffset = new Vec2(scrollOffset.x, scrollOffset.y + tooFarDown);
		}
		if (scrollOffset.y > 0) {
			scrollOffset = new Vec2(scrollOffset.x, 0);
		}
		try {
			child.setPos(new Vec2(getPos().x + scrollOffset.x, getPos().y + scrollOffset.y));
		} catch (Throwable ignored) {
			// child size not yet initialized
			// TODO don't catch every exception here
		}
	}

	@Override
	public void draw(GuiGraphics context, Vec2 mouse, Vec2 winSize, float partialTicks) {
		if (child.getSize() == null) return;

		final int top = getPos().y + 1;
		final int bottom = top + getInnerHeight();

		context.flush();

		double guiScale = mc.getWindow().getGuiScale();
		RenderSystem.enableScissor(
				(int) (guiScale * getPos().x),
				(int) (guiScale * (mc.screen.height - bottom)),
				(int) (guiScale * getInnerWidth()),
				(int) (guiScale * getInnerHeight()));

		child.draw(context, mouse, winSize, partialTicks);

		// draw text buffer inside clipped area
		context.flush();

		// TODO nestable clipping: restore previous edges
		RenderSystem.disableScissor();

		if (child.getSize().y <= getInnerHeight())
			return; // nothing to scroll, hide scroll bar; also prevents potential divide by zero

		final int lineColor = Color.GRAY.getRGB();
		final int scrollBarColor = Color.GRAY.getRGB();
		final int scrollBarTrackColor = Color.BLACK.getRGB();

		// lines at top and bottom
		if (scrollOffset.y < 0) {
			context.fill(
					getPos().x,
					getPos().y,
					getPos().x + getSize().x,
					top,
					lineColor);
		}
		if (-scrollOffset.y < child.getSize().y - getInnerHeight()) {
			context.fill(
					getPos().x,
					getPos().y + getSize().y - 1,
					getPos().x + getSize().x,
					getPos().y + getSize().y,
					lineColor);
		}

		// scroll bar track
		context.fill(
				getPos().x + getSize().x - scrollBarSize,
				top,
				getPos().x + getSize().x,
				bottom,
				scrollBarTrackColor);

		// scroll bar
		final int scrollBarLength = Math.max(scrollBarSize, getInnerHeight() * getInnerHeight() / child.getSize().y);
		final int scrollBarTravel = getInnerHeight() - scrollBarLength;
		final int scrollBarOffset = scrollBarTravel * -scrollOffset.y / Math.max(1, child.getSize().y - getInnerHeight());
		context.fill(
				getPos().x + getSize().x - scrollBarSize,
				top + scrollBarOffset,
				getPos().x + getSize().x,
				top + scrollBarOffset + scrollBarLength,
				scrollBarColor);
	}

	@Override
	public boolean drawOverlays(GuiGraphics context, Vec2 mouse, Vec2 winSize, float partialTicks) {
		return child.drawOverlays(context, mouseChildTranslated(mouse), winSize, partialTicks);
	}
}
