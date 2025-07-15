package io.github.gjum.mc.gui;

import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class Label extends Clickable {
	public enum Alignment {ALIGN_LEFT, ALIGN_CENTER, ALIGN_RIGHT}

	private Component text;
	public Alignment alignment;
	// on top of text, can be changed separately
	private int color = Color.WHITE.getRGB();
	private int height = 20;

	public Label(String text) {
		this(Component.nullToEmpty(text));
	}

	public Label(Component text) {
		super();
		this.text = text;
		this.alignment = Alignment.ALIGN_LEFT;
		int width = mc.font.width(text) - 1; // subtract space after last char
		setMinSize(new Vec2(width, height));
		setMaxSize(new Vec2(9999999, height));
		if (alignment == Alignment.ALIGN_CENTER) setWeight(new Vec2(1, 0));
		else setWeight(new Vec2(0, 0));
	}

	public Label align(Alignment alignment) {
		this.alignment = alignment;
		return this;
	}

	public Label setText(String text) {
		return setText(Component.nullToEmpty(text));
	}

	public Label setText(Component text) {
		this.text = text;
		int width = mc.font.width(text) - 1; // subtract space after last char
		if (getMinSize().x < width) {
			setMinSize(new Vec2(width, height));
		}
		return this;
	}

	public Label setColor(Color color) {
		this.color = color.getRGB();
		return this;
	}

	public Label setHeight(int height) {
		this.height = height;
		setMinSize(new Vec2(getMinSize().x, height));
		setMaxSize(new Vec2(getMaxSize().x, height));
		return this;
	}

	@Override
	public void draw(GuiGraphics context, Vec2 mouse, Vec2 winSize, float partialTicks) {
		final int x;
		switch (alignment) {
			case ALIGN_CENTER: {
				int w = mc.font.width(text) - 1; // subtract space after last char
				x = getPos().x + (getSize().x - w) / 2;
				break;
			}
			case ALIGN_LEFT: {
				x = getPos().x;
				break;
			}
			case ALIGN_RIGHT: {
				int w = mc.font.width(text) - 1; // subtract space after last char
				x = getPos().x + (getSize().x - w);
				break;
			}
			default:
				throw new IllegalStateException("Unexpected alignment " + alignment);
		}
		// TODO configure vertical alignment
		final int dy = (getSize().y - mc.font.lineHeight) / 2;

		Component displayText = text;
		if (clickHandler != null && isMouseInside(mouse)) {
			// make text feel clickable
			displayText = MutableComponent.create(displayText.getContents()).setStyle
					(displayText.getStyle().withUnderlined(true));
		}
		context.drawString(mc.font, displayText, x, getPos().y + 1 + dy, color);
	}
}
