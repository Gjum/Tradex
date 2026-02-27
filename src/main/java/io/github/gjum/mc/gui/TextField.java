package io.github.gjum.mc.gui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.mojang.blaze3d.platform.InputConstants;

import java.awt.Color;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class TextField extends Clickable {
	private EditBox textField;
	private @Nullable Predicate<String> validator;
	private int textColor = Color.WHITE.getRGB();
	private static final int mutedColor = 0xFF555566;
	/**
	 * Displayed in mutedColor when textField is empty.
	 */
	private @Nullable String hint;
	private @Nullable Consumer<String> enterHandler = null;
	private boolean enabled = true;

	private final Consumer<String> guiResponder = (s) -> handleChanged();

	public TextField(@Nullable Predicate<String> validator, @Nullable String text) {
		this(validator, text, null);
	}

	public TextField(@Nullable Predicate<String> validator, @Nullable String text, @Nullable String hint) {
		this.validator = validator;
		this.hint = hint;

		textField = new EditBox(mc.font, 0, 0, 0, 0, Component.literal(""));
		textField.setMaxLength(9999999);
		textField.setResponder(guiResponder);
		if (text != null) setText(text);
		textField.moveCursorToStart(false); // make start of text visible if it's longer than the EditBox
		handleChanged();

		setMinSize(new Vec2(50, 20));
		setMaxSize(new Vec2(Vec2.LARGE, 20));
		setWeight(new Vec2(1, 0));
	}

	private void handleChanged() {
		if (validator == null) return;
		boolean valid = false;
		try {
			valid = validator.test(getText());
		} catch (Throwable e) {
			e.printStackTrace();
		}
		if (valid) setColor(Color.WHITE);
		else setColor(Color.RED);
	}

	public EditBox getTextField() {
		return textField;
	}

	public String getText() {
		return textField.getValue();
	}

	public TextField setText(String text) {
		textField.setValue(text);
		textField.moveCursorToEnd(false);
		handleChanged();
		return this;
	}

	@Override
	public boolean isEnabled() {
		return enabled; // textField.isEditable() is private
	}

	public TextField setEnabled(boolean enabled) {
		this.enabled = enabled;
		textField.setEditable(enabled);
		return this;
	}

	/**
	 * @deprecated Does not work on 1.20 yet
	 */
	@Deprecated
	public TextField setFocused(boolean focused) {
//		if (textField.isFocused()) return this;
//		textField.setFocused(focused);
//		var shiftKeyPressed = false;
//		if (focused) textField.setCursorToEnd(shiftKeyPressed);
		return this;
	}

	public TextField setColor(Color color) {
		textColor = color.getRGB();
		textField.setTextColor(textColor);
		return this;
	}

	public TextField onEnter(@Nullable Consumer<String> enterHandler) {
		this.enterHandler = enterHandler;
		return this;
	}

	@Override
	public void draw(GuiGraphics context, Vec2 mouse, Vec2 winSize, float partialTicks) {
		textField.render(context, mouse.x, mouse.y, partialTicks);
		if (textField.getValue().isEmpty() && hint != null && !hint.isEmpty()) {
			int x = textField.getX() + 4;
			int y = textField.getY() + (getSize().y - 4 - 8) / 2;
			String hintTrimmed = mc.font.substrByWidth(
					Component.literal(hint), getSize().x - 8).getString();
			context.drawString(mc.font, hintTrimmed, x, y, mutedColor, false);
		}
	}

	@Override
	public void setPos(@NotNull Vec2 pos) {
		super.setPos(pos);
		textField.setX(pos.x + 2);
		textField.setY(pos.y + 2);
	}

	@Override
	public void updateSize(Vec2 size) {
		super.updateSize(size);
		textField.setSize(getSize().x - 4, getSize().y - 4);
	}

	@Override
	public void handleKeyPressed(int keyCode, int scanCode, int mods) {
		//? if >=1.21.11 {
		textField.keyPressed(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, mods));
		//?} else {
		/*textField.keyPressed(keyCode, scanCode, mods);
		*///?}

		if (textField.canConsumeInput()) {
			if (keyCode == InputConstants.KEY_RETURN && enterHandler != null) {
				try {
					boolean valid = validator == null || validator.test(getText());
					if (valid) {
						enterHandler.accept(getText());
						mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
					} else {
						mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F));
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void handleCharTyped(char keyChar, int keyCode) {
		//? if >=1.21.11 {
		textField.charTyped(new net.minecraft.client.input.CharacterEvent((int) keyChar, keyCode));
		//?} else {
		/*textField.charTyped(keyChar, keyCode);
		*///?}
	}

	@Override
	public boolean handleMouseClicked(Vec2 mouse, int mouseButton) {
		//? if >=1.21.11 {
		var clicked = textField.mouseClicked(
				new net.minecraft.client.input.MouseButtonEvent(mouse.x, mouse.y,
						new net.minecraft.client.input.MouseButtonInfo(mouseButton, 0)),
				false);
		//?} else {
		/*var clicked = textField.mouseClicked(mouse.x, mouse.y, mouseButton);
		*///?}
		textField.setFocused(clicked);
		return clicked;
	}
}