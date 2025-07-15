package io.github.gjum.mc.gui;

import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Renders a tooltip if the mouse is over the child.
 * Passes all events through to the child.
 * Uses child's layout constraints.
 */
public class Tooltip extends GuiElement {
	@NotNull
	private String text;
	@NotNull
	private GuiElement child;

	public Tooltip(@NotNull String text, @NotNull GuiElement child) {
		this.text = text;
		this.child = child;
	}

	public Tooltip setText(@NotNull String text) {
		this.text = text;
		return this;
	}

	public Tooltip setChild(GuiElement child) {
		this.child = child;
		return this;
	}

	@Override
	public boolean drawOverlays(GuiGraphics context, Vec2 mouse, Vec2 winSize, float partialTicks) {
		if (child.drawOverlays(context, mouse, winSize, partialTicks)) {
			return true;
		}

		if (getPos() == null) return false;
		if (getPos().x > mouse.x || mouse.x > getPos().x + getSize().x) return false;
		if (getPos().y > mouse.y || mouse.y > getPos().y + getSize().y) return false;

		int mouseX = mouse.x;
		int mouseY = mouse.y;
		final int lineHeight = 9;
		final int padding = 2;

		final String[] lines = text.replaceAll("\\r?\\n+$", "")
				.split("\\r?\\n");
		final int textHeight = lineHeight * lines.length;
		int textWidth = 0;
		for (String line : lines) {
			textWidth = Math.max(textWidth, mc.font.width(line));
		}
		final int boxWidth = textWidth - 1 + 2 * padding;
		final int boxHeight = textHeight - 1 + 2 * padding;
		mouseX -= padding;
		mouseY -= boxHeight;
		int left = Math.max(0, Math.min(mouseX, winSize.x - boxWidth));
		int top = Math.max(0, Math.min(mouseY, winSize.y - boxHeight));
		context.pose().pushPose();
		context.pose().translate(0.0f, 0.0f, 201.0f);
		context.fill(left, top, left + boxWidth, top + boxHeight, Color.BLACK.getRGB());
		for (String line : lines) {
			context.drawString(mc.font, line, left + padding, top + padding, Color.WHITE.getRGB());
			top += lineHeight;
		}
		context.pose().popPose();
		return true;
	}

	@NotNull
	@Override
	public Vec2 getPos() {
		return child.getPos();
	}

	@Override
	public void setPos(@NotNull Vec2 pos) {
		child.setPos(pos);
	}

	@Override
	public void draw(GuiGraphics context, Vec2 mouse, Vec2 winSize, float partialTicks) {
		child.draw(context, mouse, winSize, partialTicks);
	}

	@Override
	public boolean handleMouseClicked(Vec2 mouse, int mouseButton) {
		return child.handleMouseClicked(mouse, mouseButton);
	}

	@Override
	public void handleMouseDragged(Vec2 mouse, Vec2 prevMouse, Vec2 dragStart, int mouseButton) {
		child.handleMouseDragged(mouse, prevMouse, dragStart, mouseButton);
	}

	@Override
	public void handleMouseReleased(Vec2 mouse, Vec2 dragStart, int state) {
		child.handleMouseReleased(mouse, dragStart, state);
	}

	@Override
	public boolean handleMouseScrolled(Vec2 mouse, double scrollAmount) {
		return child.handleMouseScrolled(mouse, scrollAmount);
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

	@Override
	public Vec2 getSize() {
		return child.getSize();
	}

	@Override
	public void updateSize(Vec2 sizeAvail) {
		child.updateSize(sizeAvail);
	}

	@Override
	public GuiElement setFixedSize(Vec2 size) {
		return child.setFixedSize(size);
	}

	@NotNull
	@Override
	public Vec2 getWeight() {
		return child.getWeight();
	}

	@Override
	public GuiElement setWeight(@NotNull Vec2 weight) {
		return child.setWeight(weight);
	}

	@Override
	public Vec2 getMaxSize() {
		return child.getMaxSize();
	}

	@Override
	public GuiElement setMaxSize(Vec2 size) {
		return child.setMaxSize(size);
	}

	@Override
	public Vec2 getMinSize() {
		return child.getMinSize();
	}

	@Override
	public GuiElement setMinSize(Vec2 size) {
		return child.setMinSize(size);
	}
}
