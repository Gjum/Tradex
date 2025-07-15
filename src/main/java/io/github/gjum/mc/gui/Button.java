package io.github.gjum.mc.gui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class Button extends Clickable {
	private Component text;

	private net.minecraft.client.gui.components.Button button;

	public Button(@Nullable String text) {
		this(text == null ? null : Component.literal(text));
	}

	public Button(@Nullable Component text) {
		if (text == null) text = Component.literal("");
		this.text = text;

		final int textWidth = mc.font.width(text);
		setMinSize(new Vec2(Math.max(20, textWidth + 10), 20));
		setMaxSize(new Vec2(380, 20));

		button = net.minecraft.client.gui.components.Button.builder(text, null)
				.bounds(0, 0, getSize().x, getSize().y)
				.build();
	}

	public Component getText() {
		return text;
	}

	public Button setText(Component text) {
		this.text = text;
		final int textWidth = mc.font.width(text);
		// TODO instead of replacing, add this as an additional constraint
		setMinSize(new Vec2(Math.max(20, textWidth + 10), getMinSize().y));

		button.setMessage(text);
		return this;
	}

	@Override
	public boolean isEnabled() {
		return button.active;
	}

	public Button setEnabled(boolean enabled) {
		button.active = enabled;
		return this;
	}

	// type erasure
	public Button onBtnClick(@Nullable Consumer<Button> clickHandler) {
		return super.onClick(clickHandler);
	}

	@Override
	public void draw(GuiGraphics context, Vec2 mouse, Vec2 winSize, float partialTicks) {
		button.render(context, mouse.x, mouse.y, partialTicks);
	}

	@Override
	public void setPos(@NotNull Vec2 pos) {
		super.setPos(pos);
		button.setX(pos.x);
		button.setY(pos.y);
	}

	@Override
	public void updateSize(Vec2 size) {
		super.updateSize(size);
		button.setSize(getSize().x, getSize().y);
	}
}
